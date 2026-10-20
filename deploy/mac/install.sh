#!/usr/bin/env bash
#
# cxh 博客 · macOS 本机部署（launchd + Cloudflare Tunnel）
#
# 前提：
#   1) 已安装 JDK 17+（java -version 能跑）
#   2) 已安装 cloudflared（brew install cloudflared 或放在 .tools/ 下）
#   3) 已完成 Cloudflare 登录与隧道创建：
#        cloudflared tunnel login
#        cloudflared tunnel create cxh-blog
#   4) 域名 cxh327.dpdns.org 的 zone 在你的 Cloudflare 账号里（已确认满足）
#
# 用法：
#   bash deploy/mac/install.sh
#   DOMAIN=cxh327.dpdns.org TUNNEL=cxh-blog bash deploy/mac/install.sh
#
# 做的事：
#   建 runtime 目录 → 生成随机后台口令 → 复制 jar → 写 Cloudflare 配置 →
#   注册 DNS → 生成并加载两个 launchd 服务（应用 + 隧道）→ 健康检查
#
# 卸载：bash deploy/mac/uninstall.sh

set -euo pipefail

DOMAIN="${DOMAIN:-cxh327.dpdns.org}"
TUNNEL="${TUNNEL:-cxh-blog}"
APP_PORT="${APP_PORT:-8080}"
LABEL_APP="com.cxh.blog"
LABEL_TUNNEL="com.cxh.tunnel"

PROJECT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
RUNTIME="$PROJECT/runtime"
ENV_FILE="$RUNTIME/env"
AGENTS="$HOME/Library/LaunchAgents"
CF_DIR="$HOME/.cloudflared"
CF_CONFIG="$CF_DIR/config.yml"

log()  { printf '\n\033[1;34m==> %s\033[0m\n' "$1"; }
warn() { printf '\033[1;33m[注意] %s\033[0m\n' "$1"; }
die()  { printf '\033[1;31m[失败] %s\033[0m\n' "$1" >&2; exit 1; }

# ---------------------------------------------------------------- 0 前置检查
log "检查运行环境"
java -version >/dev/null 2>&1 || die "找不到 java，请先安装 JDK 17+"

CF_BIN="${CF_BIN:-}"
if [ -z "$CF_BIN" ]; then
    if command -v cloudflared >/dev/null 2>&1; then
        CF_BIN="$(command -v cloudflared)"
    elif [ -x "$PROJECT/.tools/cloudflared" ]; then
        CF_BIN="$PROJECT/.tools/cloudflared"
    else
        die "找不到 cloudflared：请 brew install cloudflared，或放到 .tools/cloudflared"
    fi
fi
log "cloudflared: $CF_BIN"

# 找隧道凭据（cloudflared tunnel create 生成 <UUID>.json）
CRED=""
if [ -d "$CF_DIR" ]; then
    CRED="$(ls -1t "$CF_DIR"/*.json 2>/dev/null | head -1 || true)"
fi
[ -n "$CRED" ] || die "找不到隧道凭据。请先执行：
  cloudflared tunnel login
  cloudflared tunnel create $TUNNEL"
TUNNEL_ID="$(basename "$CRED" .json)"
log "隧道 ID: ${TUNNEL_ID}（凭据 ${CRED}）"

# 端口占用检查（排除本服务自己在跑的情况）
if lsof -nP -iTCP:"$APP_PORT" -sTCP:LISTEN >/dev/null 2>&1; then
    if launchctl print "gui/$(id -u)/$LABEL_APP" >/dev/null 2>&1; then
        warn "端口 $APP_PORT 已被本服务占用，稍后会先卸载再重启"
    else
        die "端口 $APP_PORT 已被其他进程占用，请改 APP_PORT 或释放该端口：
  lsof -nP -iTCP:$APP_PORT -sTCP:LISTEN"
    fi
fi

# ---------------------------------------------------------------- 1 运行时
log "准备运行时目录"
mkdir -p "$RUNTIME" "$PROJECT/logs"
chmod 700 "$RUNTIME"

# jar：优先用刚构建的 target/cxh-blog.jar
JAR_SRC="$PROJECT/target/cxh-blog.jar"
[ -f "$JAR_SRC" ] || die "找不到 ${JAR_SRC}，请先执行：./mvnw clean package -DskipTests"
install -m 644 "$JAR_SRC" "$RUNTIME/cxh-blog.jar"
log "已安装 jar（$(du -h "$RUNTIME/cxh-blog.jar" | cut -f1)）"

# 环境变量文件（含口令，600）
# 优先固定到 JDK 17（项目以 release 17 编译），找不到就用系统 java
JAVA17_HOME="$(/usr/libexec/java_home -v 17 2>/dev/null || true)"
if [ -n "$JAVA17_HOME" ] && [ -x "$JAVA17_HOME/bin/java" ]; then
    JAVA_BIN_LINE="JAVA_BIN=$JAVA17_HOME/bin/java"
    log "使用 JDK 17: $JAVA17_HOME"
else
    JAVA_BIN_LINE="# JAVA_BIN 未固定，使用系统 java"
    warn "未找到 JDK 17，将使用系统默认 java"
fi

if [ -f "$ENV_FILE" ]; then
    log "已存在 ${ENV_FILE}，保留原配置"
    # 回读时去掉外层引号，避免把 "xxx" 连引号一起打印
    ADMIN_PASSWORD="$(grep -E '^CXH_ADMIN_PASSWORD=' "$ENV_FILE" | head -1 | cut -d= -f2- | sed -e 's/^"//' -e 's/"$//' || true)"
else
    ADMIN_PASSWORD="$(openssl rand -base64 24 | tr -dc 'A-Za-z0-9' | head -c 16)"
    cat > "$ENV_FILE" <<EOF
# 由 deploy/mac/install.sh 生成；含口令，权限 600，不要提交到仓库
# H2 文件库（默认），数据落在项目 ./data
CXH_ADMIN_USERNAME=admin
CXH_ADMIN_PASSWORD="$ADMIN_PASSWORD"
$JAVA_BIN_LINE
SERVER_PORT=$APP_PORT
CXH_MEDIA_LOCATION=$PROJECT/uploads
JAVA_OPTS="-Xmx512m -Duser.timezone=Asia/Shanghai -Djava.awt.headless=true"
# 1=接电源时阻止系统休眠，0=不阻止
CXH_KEEP_AWAKE=1
EOF
    chmod 600 "$ENV_FILE"
    warn "已生成后台口令，请立即记录（之后可改 ${ENV_FILE}）"
    printf '    后台地址: http://127.0.0.1:%s/admin\n    账号: admin\n    口令: %s\n' "$APP_PORT" "$ADMIN_PASSWORD"
fi
chmod 600 "$ENV_FILE"

# ---------------------------------------------------------------- 2 Cloudflare
log "写入 Cloudflare 隧道配置 $CF_CONFIG"
mkdir -p "$CF_DIR"
cat > "$CF_CONFIG" <<EOF
# 由 deploy/mac/install.sh 生成
tunnel: ${TUNNEL_ID}
credentials-file: ${CF_DIR}/${TUNNEL_ID}.json

ingress:
  - hostname: ${DOMAIN}
    service: http://127.0.0.1:${APP_PORT}
  - service: http_status:404
EOF
chmod 600 "$CF_CONFIG"

log "注册 DNS 路由（$DOMAIN → 隧道）"
# 该名字上若已有 A/AAAA/CNAME（例如此前指向 GitHub Pages），Cloudflare 不允许同名 CNAME 共存，
# 此时需要用 --overwrite-dns 覆盖。cloudflared 在"已配置正确"时返回 0，失败时也不一定非 0，
# 因此按输出内容判断，而不是只看退出码。
ROUTE_OUT="$("$CF_BIN" tunnel route dns "$TUNNEL" "$DOMAIN" 2>&1)" || true
printf '%s\n' "$ROUTE_OUT" | sed 's/^/    /'
case "$ROUTE_OUT" in
    *"Failed to add route"*|*"already exists"*)
        warn "$DOMAIN 上已有其他 DNS 记录，将用隧道路由覆盖（原记录会失效）"
        "$CF_BIN" tunnel route dns --overwrite-dns "$TUNNEL" "$DOMAIN" 2>&1 | sed 's/^/    /' || \
            die "DNS 路由失败，请到 Cloudflare 面板手工添加：$DOMAIN CNAME ${TUNNEL_ID}.cfargotunnel.com（橙云）"
        ;;
esac

# ---------------------------------------------------------------- 3 launchd
log "安装 launchd 服务"
mkdir -p "$AGENTS"

render() {  # render <模板> <输出> <额外替换...>
    local tpl="$1" out="$2"
    sed -e "s|@PROJECT@|$PROJECT|g" \
        -e "s|@CF_BIN@|$CF_BIN|g" \
        -e "s|@CONFIG@|$CF_CONFIG|g" \
        -e "s|@LABEL@|$LABEL_APP|g" "$tpl" > "$out"
}

render "$PROJECT/deploy/mac/com.cxh.blog.plist.template" "$AGENTS/$LABEL_APP.plist"
sed -e "s|@PROJECT@|$PROJECT|g" \
    -e "s|@CF_BIN@|$CF_BIN|g" \
    -e "s|@CONFIG@|$CF_CONFIG|g" \
    -e "s|@LABEL@|$LABEL_TUNNEL|g" \
    "$PROJECT/deploy/mac/com.cxh.tunnel.plist.template" > "$AGENTS/$LABEL_TUNNEL.plist"

plutil -lint "$AGENTS/$LABEL_APP.plist" >/dev/null || die "plist 格式错误：$AGENTS/$LABEL_APP.plist"
plutil -lint "$AGENTS/$LABEL_TUNNEL.plist" >/dev/null || die "plist 格式错误：$AGENTS/$LABEL_TUNNEL.plist"
log "plist 校验通过"

# 先卸载旧的（若在运行），再加载，保证配置生效
for label in "$LABEL_APP" "$LABEL_TUNNEL"; do
    launchctl bootout "gui/$(id -u)/$label" 2>/dev/null || true
done
sleep 1
for label in "$LABEL_APP" "$LABEL_TUNNEL"; do
    launchctl bootstrap "gui/$(id -u)" "$AGENTS/$label.plist" || die "加载 $label 失败"
    launchctl enable "gui/$(id -u)/$label" 2>/dev/null || true
done
log "服务已加载"

# ---------------------------------------------------------------- 4 健康检查
log "等待应用就绪（最多 90 秒）"
READY=0
for _ in $(seq 1 90); do
    if curl -fsS --max-time 3 "http://127.0.0.1:$APP_PORT/actuator/health" >/dev/null 2>&1; then READY=1; break; fi
    sleep 1
done
if [ "$READY" != "1" ]; then
    warn "应用未就绪，日志如下："
    tail -30 "$PROJECT/logs/mac-app.err.log" 2>/dev/null | sed 's/^/    /'
    tail -30 "$PROJECT/logs/mac-app.log" 2>/dev/null | sed 's/^/    /'
    die "启动失败，请查看 logs/mac-app.log"
fi
log "应用就绪: http://127.0.0.1:$APP_PORT"

log "等待隧道注册（最多 60 秒）"
for _ in $(seq 1 60); do
    if grep -q "Registered tunnel connection" "$PROJECT/logs/mac-tunnel.log" 2>/dev/null; then
        log "隧道已连接"; break
    fi
    sleep 1
done

log "公网访问检查（DNS 生效可能需要 1-2 分钟）"
for _ in $(seq 1 12); do
    CODE="$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 "https://$DOMAIN/actuator/health" 2>/dev/null || echo 000)"
    if [ "$CODE" = "200" ]; then log "公网可用: https://$DOMAIN"; break; fi
    sleep 5
done

cat <<EOF

========================================================
部署完成

  后台地址 : https://$DOMAIN/admin
  后台账号 : admin
  后台口令 : $ADMIN_PASSWORD

  服务管理 :
    bash deploy/mac/status.sh               # 查看状态
    launchctl kickstart -k gui/$(id -u)/$LABEL_APP     # 重启应用
    launchctl kickstart -k gui/$(id -u)/$LABEL_TUNNEL  # 重启隧道
    bash deploy/mac/uninstall.sh            # 卸载

  日志 :
    $PROJECT/logs/mac-app.log
    $PROJECT/logs/mac-tunnel.log

  备份 :
    scripts/backup.sh --h2

  注意 : Mac 需保持开机联网；合盖或断网时站点会 502。
========================================================
EOF
