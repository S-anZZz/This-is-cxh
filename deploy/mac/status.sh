#!/usr/bin/env bash
# 查看 Mac 本机部署状态：launchd 服务、端口、隧道、公网
set -uo pipefail
PROJECT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DOMAIN="${DOMAIN:-cxh327.dpdns.org}"
PORT="$(grep -E '^SERVER_PORT=' "$PROJECT/runtime/env" 2>/dev/null | cut -d= -f2- || echo 8080)"

loaded() { launchctl print "gui/$(id -u)/$1" >/dev/null 2>&1 && echo "已加载" || echo "未加载"; }

# cloudflared 的连接日志走 stderr，两个文件都统计
CONN_COUNT="$(cat "$PROJECT/logs/mac-tunnel.log" "$PROJECT/logs/mac-tunnel.err.log" 2>/dev/null | grep -c 'Registered tunnel connection' || true)"

printf '%-10s %s\n' "应用服务" "$(loaded com.cxh.blog)"
printf '%-10s %s\n' "隧道服务" "$(loaded com.cxh.tunnel)"
printf '%-10s %s\n' "本地端口" "$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 "http://127.0.0.1:${PORT}/actuator/health" 2>/dev/null || echo 000) (127.0.0.1:${PORT})"
printf '%-10s %s\n' "健康检查" "$(curl -s --max-time 3 "http://127.0.0.1:${PORT}/actuator/health" 2>/dev/null || echo 无响应)"
printf '%-10s %s\n' "隧道注册" "${CONN_COUNT:-0} 条连接记录"
printf '%-10s %s\n' "公网访问" "$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 "https://${DOMAIN}/actuator/health" 2>/dev/null || echo 000) (https://${DOMAIN})"
echo
echo "最近应用日志："; tail -3 "$PROJECT/logs/mac-app.log" 2>/dev/null | sed 's/^/  /'
echo "最近隧道日志："; tail -3 "$PROJECT/logs/mac-tunnel.err.log" 2>/dev/null | sed 's/^/  /'
