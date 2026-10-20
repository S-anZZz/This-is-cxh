#!/usr/bin/env bash
#
# cxh 博客启动脚本
#
#   scripts/run.sh                 前台启动（Ctrl+C 停止）
#   scripts/run.sh -d              后台启动，日志写 logs/app.log，PID 写 logs/app.pid
#   scripts/run.sh stop            停止后台实例
#   scripts/run.sh status          查看运行状态
#   PORT=9090 scripts/run.sh -d    指定端口
#
# 说明：jar 不存在时会自动用 Maven Wrapper 构建（首次需联网下载依赖）。

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

PORT="${PORT:-8080}"
JAR="$ROOT/target/cxh-blog.jar"
LOG_DIR="$ROOT/logs"
PID_FILE="$LOG_DIR/app.pid"
LOG_FILE="$LOG_DIR/app.log"
JAVA_BIN="${JAVA_HOME:+$JAVA_HOME/bin/}java"

mkdir -p "$LOG_DIR"

running_pid() {
    [ -f "$PID_FILE" ] || return 1
    local pid
    pid="$(cat "$PID_FILE" 2>/dev/null || true)"
    [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null && { echo "$pid"; return 0; }
    return 1
}

ensure_jar() {
    if [ -f "$JAR" ]; then
        return
    fi
    echo "未找到 ${JAR}，先用 Maven Wrapper 构建（首次会下载依赖，请稍候）…"
    ./mvnw -B -ntp package -DskipTests
}

wait_ready() {
    local port="$1" tries=60
    for _ in $(seq 1 "$tries"); do
        if [ "$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 "http://127.0.0.1:${port}/" 2>/dev/null)" = "200" ]; then
            return 0
        fi
        sleep 1
    done
    return 1
}

show_urls() {
    cat <<EOF

  前台    http://localhost:${PORT}/
  后台    http://localhost:${PORT}/admin      账号 admin / 口令 cxh@2024
  订阅    http://localhost:${PORT}/feed.xml

EOF
}

case "${1:-}" in
    stop)
        if pid="$(running_pid)"; then
            kill "$pid"
            for _ in $(seq 1 20); do kill -0 "$pid" 2>/dev/null || break; sleep 0.5; done
            kill -9 "$pid" 2>/dev/null || true
            rm -f "$PID_FILE"
            echo "已停止（PID ${pid}）"
        else
            echo "没有正在运行的后台实例"
        fi
        exit 0
        ;;
    status)
        if pid="$(running_pid)"; then
            echo "运行中：PID ${pid}，端口 ${PORT}"
            curl -s -o /dev/null -w "健康检查：%{http_code}\n" "http://127.0.0.1:${PORT}/actuator/health" || true
        else
            echo "未运行"
        fi
        exit 0
        ;;
    -d|--daemon)
        if pid="$(running_pid)"; then
            echo "已经在运行（PID ${pid}）；如需重启请先执行 scripts/run.sh stop"
            show_urls
            exit 0
        fi
        ensure_jar
        nohup "$JAVA_BIN" -jar "$JAR" --server.port="$PORT" > "$LOG_FILE" 2>&1 &
        echo $! > "$PID_FILE"
        if wait_ready "$PORT"; then
            echo "已后台启动（PID $(cat "$PID_FILE")），日志：$LOG_FILE"
            show_urls
        else
            echo "启动失败，日志末尾："
            tail -20 "$LOG_FILE"
            exit 1
        fi
        exit 0
        ;;
    ""|--foreground)
        ensure_jar
        echo "前台启动（端口 ${PORT}，Ctrl+C 停止）"
        show_urls
        exec "$JAVA_BIN" -jar "$JAR" --server.port="$PORT"
        ;;
    *)
        echo "用法：scripts/run.sh [-d|stop|status]" >&2
        exit 2
        ;;
esac
