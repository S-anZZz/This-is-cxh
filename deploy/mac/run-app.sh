#!/bin/bash
#
# 启动脚本：由 launchd 调用（也可手工执行，便于排错）
#
# 职责：
#   1) 读取 runtime/env 里的配置（含后台口令，权限 600）
#   2) 工作目录切到项目根，使 ./data 与 ./uploads 落在项目内（便于备份）
#   3) 默认用 caffeinate 阻止系统休眠，断电/合盖仍可能中断（见 CXH_KEEP_AWAKE）

set -euo pipefail

PROJECT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$PROJECT"

ENV_FILE="$PROJECT/runtime/env"
[ -f "$ENV_FILE" ] || { echo "[cxh] 缺少 ${ENV_FILE}，请先运行 deploy/mac/install.sh" >&2; exit 1; }
# shellcheck disable=SC1090
set -a; . "$ENV_FILE"; set +a

# Java：优先 JAVA_BIN，其次 env 里的 JAVA_HOME，最后用系统 java
if [ -n "${JAVA_BIN:-}" ]; then
    JAVA="$JAVA_BIN"
elif [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVA="$JAVA_HOME/bin/java"
else
    JAVA="$(command -v java || true)"
fi
[ -n "$JAVA" ] && [ -x "$JAVA" ] || { echo "[cxh] 找不到可用的 java" >&2; exit 1; }

JAR="$PROJECT/runtime/cxh-blog.jar"
[ -f "$JAR" ] || { echo "[cxh] 缺少 ${JAR}，请先运行 deploy/mac/install.sh" >&2; exit 1; }

echo "[cxh] 启动: $JAVA ${JAVA_OPTS:--Xmx512m} -jar $JAR (工作目录 $PROJECT)"

# shellcheck disable=SC2086
if [ "${CXH_KEEP_AWAKE:-1}" = "1" ]; then
    # -s 仅在接电源时阻止系统睡眠；不改变显示器休眠
    exec /usr/bin/caffeinate -s "$JAVA" ${JAVA_OPTS:--Xmx512m} -jar "$JAR"
fi
# shellcheck disable=SC2086
exec "$JAVA" ${JAVA_OPTS:--Xmx512m} -jar "$JAR"
