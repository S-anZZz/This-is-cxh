#!/usr/bin/env bash
#
# 静态检查所有 shell 脚本：变量名后面紧跟中文字符时，bash 会把该字符并进变量名，
# 导致 unbound variable（本项目里已踩过多次）。统一要求写成 ${VAR} 形式。
# 因此把这条规则固化为检查项，并顺带做 bash -n 语法检查。
#
# 用法：scripts/check-scripts.sh
set -uo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."

fail=0
for f in scripts/*.sh deploy/*/*.sh; do
    [ -f "$f" ] || continue
    # 用 python 判定非 ASCII，避免 grep 的转义陷阱
    if ! python3 - "$f" <<'PY'
import re, sys, pathlib
pat = re.compile(r'\$[A-Za-z_][A-Za-z0-9_]*[^\x00-\x7F]')
s = pathlib.Path(sys.argv[1]).read_text(encoding="utf-8")
hits = pat.findall(s)
if hits:
    print(f"  ❌ {sys.argv[1]}: {hits[:5]}")
    sys.exit(1)
PY
    then fail=1; fi
    bash -n "$f" || { echo "  ❌ 语法错误: $f"; fail=1; }
done

if [ "$fail" = "0" ]; then
    echo "✅ 所有 shell 脚本通过检查"
else
    echo "❌ 存在未通过检查的脚本"
fi
exit "$fail"
