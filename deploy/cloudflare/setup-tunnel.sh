#!/usr/bin/env bash
#
# cxh 博客 · Cloudflare Tunnel 安装脚本（Linux 源站）
#
# 前置条件（只需做一次）：
#   1) 已安装 cloudflared
#   2) 已执行 cloudflared tunnel login（浏览器授权你的 Cloudflare 账号）
#   3) 已执行 cloudflared tunnel create cxh-blog，并拿到隧道 UUID
#
# 用法：
#   sudo DOMAIN=cxh327.dpdns.org bash deploy/cloudflare/setup-tunnel.sh
#   sudo DOMAIN=cxh327.dpdns.org TUNNEL_ID=<uuid> bash deploy/cloudflare/setup-tunnel.sh
#
# 作用：把凭据与配置放到 /etc/cloudflared，注册 DNS，安装并启动 systemd 服务。
# 说明：走隧道时不需要开放任何入站端口，也不需要本地证书（TLS 由 Cloudflare 边缘终结）。

set -euo pipefail

DOMAIN="${DOMAIN:-cxh327.dpdns.org}"
APP_PORT="${APP_PORT:-8080}"
TUNNEL_NAME="${TUNNEL_NAME:-cxh-blog}"
CFG_DIR=/etc/cloudflared
SERVICE=cxh-blog-tunnel

log()  { printf '\n\033[1;34m==> %s\033[0m\n' "$1"; }
warn() { printf '\033[1;33m[注意] %s\033[0m\n' "$1"; }
die()  { printf '\033[1;31m[失败] %s\033[0m\n' "$1" >&2; exit 1; }

[ "$(id -u)" = "0" ] || die "请用 root 执行（sudo）"
command -v cloudflared >/dev/null || die "未安装 cloudflared，见 deploy/cloudflare/README.md 的安装步骤"

# 找凭据：优先用 TUNNEL_ID 环境变量，否则取 ~/.cloudflared 下最新的 json
TUNNEL_ID="${TUNNEL_ID:-}"
if [ -z "$TUNNEL_ID" ]; then
    CRED="$(ls -1t /root/.cloudflared/*.json 2>/dev/null | head -1 || true)"
    [ -n "$CRED" ] || die "找不到隧道凭据，请先执行：cloudflared tunnel login && cloudflared tunnel create $TUNNEL_NAME"
    TUNNEL_ID="$(basename "$CRED" .json)"
else
    CRED="/root/.cloudflared/${TUNNEL_ID}.json"
fi
[ -f "$CRED" ] || die "找不到凭据文件：$CRED"

log "隧道 ID：$TUNNEL_ID"
install -d -m 700 "$CFG_DIR"
install -m 600 "$CRED" "$CFG_DIR/${TUNNEL_ID}.json"

log "写入配置 $CFG_DIR/config.yml"
cat > "$CFG_DIR/config.yml" <<EOF
# 由 deploy/cloudflare/setup-tunnel.sh 生成
tunnel: ${TUNNEL_ID}
credentials-file: ${CFG_DIR}/${TUNNEL_ID}.json

ingress:
  - hostname: ${DOMAIN}
    service: http://127.0.0.1:${APP_PORT}
  - service: http_status:404
EOF
chmod 600 "$CFG_DIR/config.yml"

log "注册 DNS 路由（$DOMAIN → 隧道）"
cloudflared tunnel route dns "$TUNNEL_NAME" "$DOMAIN" || \
    warn "DNS 路由已存在或需手工在面板添加（保持橙云）"

log "安装 systemd 服务"
cat > "/etc/systemd/system/${SERVICE}.service" <<EOF
[Unit]
Description=Cloudflare Tunnel for cxh blog
After=network-online.target cxh-blog.service
Wants=network-online.target

[Service]
Type=notify
ExecStart=/usr/bin/cloudflared --no-autoupdate --config ${CFG_DIR}/config.yml tunnel run
Restart=always
RestartSec=5s
User=root

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
systemctl enable "$SERVICE" >/dev/null 2>&1
systemctl restart "$SERVICE"

log "等待隧道连接"
for _ in $(seq 1 30); do
    if journalctl -u "$SERVICE" -n 50 --no-pager 2>/dev/null | grep -q "Registered tunnel connection"; then
        log "隧道已连接"
        break
    fi
    sleep 1
done

cat <<EOF

========================================================
完成

  隧道服务 : systemctl status $SERVICE
  隧道日志 : journalctl -u $SERVICE -f

请在 Cloudflare 面板确认：
  1) DNS 中 ${DOMAIN} 指向 ${TUNNEL_ID}.cfargotunnel.com，且保持「已代理（橙云）」
  2) SSL/TLS 模式为 Full 或 Full (strict)
  3) 删除原先指向 GitHub Pages 的记录

验证：
  curl -sI https://${DOMAIN}/ | head -3
  curl -s  https://${DOMAIN}/actuator/health
========================================================
EOF
