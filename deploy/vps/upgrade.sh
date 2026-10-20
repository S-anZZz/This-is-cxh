#!/usr/bin/env bash
#
# cxh 博客 · 升级脚本（停机时间约 5–10 秒，失败自动回滚）
#
# 用法：
#   sudo bash deploy/vps/upgrade.sh /tmp/cxh-blog.jar
#
# 做了什么：
#   1) 备份当前 jar（带时间戳，保留最近 3 份）
#   2) 换上新 jar → 重启服务 → 轮询健康检查
#   3) 健康检查失败则自动回滚到旧 jar 并重启
#
# 注意：数据库结构由应用启动时的 Flyway 迁移，升级前建议先跑一次备份：
#   BACKUP_DIR=/var/backups/cxh scripts/backup.sh --mysql

set -euo pipefail

APP_DIR=/opt/cxh-blog
SERVICE=cxh-blog
JAR_TARGET="$APP_DIR/cxh-blog.jar"
BACKUP_DIR="$APP_DIR/jar-backups"
NEW_JAR="${1:-}"

log()  { printf '\n\033[1;34m==> %s\033[0m\n' "$1"; }
die()  { printf '\033[1;31m[失败] %s\033[0m\n' "$1" >&2; exit 1; }

[ "$(id -u)" = "0" ] || die "请用 root 执行：sudo bash deploy/vps/upgrade.sh <新 jar>"
[ -n "$NEW_JAR" ] || die "用法：sudo bash deploy/vps/upgrade.sh /tmp/cxh-blog.jar"
[ -f "$NEW_JAR" ] || die "找不到新 jar：$NEW_JAR"

healthy() {
    for _ in $(seq 1 60); do
        if curl -fsS --max-time 3 http://127.0.0.1:8080/actuator/health >/dev/null 2>&1; then
            return 0
        fi
        sleep 1
    done
    return 1
}

log "备份当前 jar"
install -d -o cxh -g cxh -m 755 "$BACKUP_DIR"
STAMP="$(date +%Y%m%d-%H%M%S)"
if [ -f "$JAR_TARGET" ]; then
    install -o cxh -g cxh -m 640 "$JAR_TARGET" "$BACKUP_DIR/cxh-blog-$STAMP.jar"
    log "已备份到 $BACKUP_DIR/cxh-blog-$STAMP.jar"
    # 只保留最近 3 份
    ls -1t "$BACKUP_DIR"/cxh-blog-*.jar 2>/dev/null | tail -n +4 | xargs -r rm -f
fi

log "替换 jar 并重启"
systemctl stop "$SERVICE"
install -o cxh -g cxh -m 640 "$NEW_JAR" "$JAR_TARGET"
systemctl start "$SERVICE"

if healthy; then
    log "升级成功，当前版本已就绪"
    systemctl status "$SERVICE" --no-pager | head -5 | sed 's/^/    /'
    exit 0
fi

log "健康检查未通过，开始回滚"
journalctl -u "$SERVICE" -n 30 --no-pager | sed 's/^/    /'
LATEST_BACKUP="$(ls -1t "$BACKUP_DIR"/cxh-blog-*.jar 2>/dev/null | head -1 || true)"
if [ -n "$LATEST_BACKUP" ]; then
    systemctl stop "$SERVICE"
    install -o cxh -g cxh -m 640 "$LATEST_BACKUP" "$JAR_TARGET"
    systemctl start "$SERVICE"
    if healthy; then
        log "已回滚到 $LATEST_BACKUP 并恢复服务"
    else
        die "回滚后仍未就绪，请人工检查：journalctl -u $SERVICE -f"
    fi
else
    die "没有可用备份，无法回滚，请人工检查"
fi
exit 1
