#!/usr/bin/env bash
# 查看 Mac 本机部署状态：launchd 服务、端口、隧道、公网
set -uo pipefail
PROJECT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DOMAIN="${DOMAIN:-cxh327.dpdns.org}"
PORT="$(grep -E '^SERVER_PORT=' "$PROJECT/runtime/env" 2>/dev/null | cut -d= -f2- || echo 8080)"

printf '%-10s %s\n' "应用服务" "$(launchctl print "gui/$(id -u)/com.cxh.blog" >/dev/null 2>&1 && echo "已加载" || echo "未加载")"
printf '%-10s %s\n' "隧道服务" "$(launchctl print "gui/$(id -u)/com.cxh.tunnel" >/dev/null 2>&1 && echo "已加载" || echo "未加载")"
printf '%-10s %s\n' "本地端口" "$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 "http://127.0.0.1:${PORT}/actuator/health" 2>/dev/null || echo 000) (127.0.0.1:${PORT})"
printf '%-10s %s\n' "健康检查" "$(curl -s --max-time 3 "http://127.0.0.1:${PORT}/actuator/health" 2>/dev/null || echo 无响应)"
printf '%-10s %s\n' "隧道注册" "$(grep -c 'Registered tunnel connection' "$PROJECT/logs/mac-tunnel.log" 2>/dev/null || echo 0) 条连接记录"
printf '%-10s %s\n' "公网访问" "$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 "https://${DOMAIN}/actuator/health" 2>/dev/null || echo 000) (https://${DOMAIN})"
echo
echo "最近应用日志："; tail -5 "$PROJECT/logs/mac-app.log" 2>/dev/null | sed 's/^/  /'
echo "最近隧道日志："; tail -5 "$PROJECT/logs/mac-tunnel.log" 2>/dev/null | sed 's/^/  /'
