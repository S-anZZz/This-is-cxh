#!/usr/bin/env bash
# 卸载 Mac 本机部署：停止并移除 launchd 服务（保留数据与上传）
set -euo pipefail
AGENTS="$HOME/Library/LaunchAgents"
for label in com.cxh.blog com.cxh.tunnel; do
    if launchctl print "gui/$(id -u)/$label" >/dev/null 2>&1; then
        launchctl bootout "gui/$(id -u)/$label" 2>/dev/null || true
        echo "已停止 $label"
    fi
    if [ -f "$AGENTS/$label.plist" ]; then rm -f "$AGENTS/$label.plist"; echo "已删除 $label.plist"; fi
done
echo
echo "服务已卸载。数据仍保留："
echo "  runtime/（jar 与 env）  data/  uploads/  logs/"
echo "如需彻底清理：rm -rf runtime data uploads logs"
