#!/usr/bin/env bash
#
# 把 content/legacy/*.md 导入博客（调用后台接口，等价于逐篇手工发布）
#
# 用法：
#   scripts/import-posts.sh                                   # 默认 http://127.0.0.1:8080
#   BASE=http://127.0.0.1:8080 USER=admin PASS=xxx scripts/import-posts.sh
#   DIR=content/legacy scripts/import-posts.sh                # 指定目录
#
# 每个 .md 文件首部是简单的 front matter：
#   ---
#   title: 文章标题
#   tags: 标签1,标签2
#   summary: 摘要
#   ---
#   正文（Markdown）
#
# 已存在同名文章会跳过，因此可以重复执行。

set -euo pipefail

BASE="${BASE:-http://127.0.0.1:8080}"
USER="${USER:-admin}"
PASS="${PASS:-}"
DIR="${DIR:-content/legacy}"
COOKIE="$(mktemp)"
trap 'rm -f "$COOKIE"' EXIT

command -v curl >/dev/null || { echo "需要 curl" >&2; exit 1; }
[ -d "$DIR" ] || { echo "找不到目录：${DIR}（在项目根目录执行）" >&2; exit 1; }
[ -n "$PASS" ] || { echo "请通过环境变量提供后台口令：PASS=xxx scripts/import-posts.sh" >&2; exit 1; }

log() { printf '%s\n' "$1"; }

# 取 CSRF 令牌：优先 meta（后台页），其次表单隐藏域（登录页）
csrf_from() {
    if [ -n "${1:-}" ] && [ -f "$1" ]; then
        grep -oE 'name="_csrf" content="[^"]*"' "$1" | head -1 | sed -E 's/.*content="([^"]*)".*/\1/' && return 0
        grep -oE 'name="_csrf" value="[^"]*"' "$1" | head -1 | sed -E 's/.*value="([^"]*)".*/\1/' && return 0
    fi
    echo ""
}

log "→ 登录 $BASE"
curl -sS -c "$COOKIE" -o /tmp/cxh-login.html "$BASE/login"
TOKEN="$(csrf_from /tmp/cxh-login.html)"
curl -sS -b "$COOKIE" -c "$COOKIE" -o /dev/null \
    --data-urlencode "username=$USER" --data-urlencode "password=$PASS" \
    --data-urlencode "_csrf=$TOKEN" "$BASE/login"

# 登录后会话与令牌都会轮换，重新取一次
curl -sS -b "$COOKIE" -c "$COOKIE" -o /tmp/cxh-new.html "$BASE/admin/posts/new"
TOKEN="$(csrf_from /tmp/cxh-new.html)"
if [ -z "$TOKEN" ]; then
    log "✗ 登录失败（拿不到 CSRF 令牌）。请检查口令是否正确：BASE=$BASE USER=$USER"
    exit 1
fi
log "✓ 登录成功"

# 现有文章标题（用于跳过重复导入）
curl -sS -b "$COOKIE" -o /tmp/cxh-list.html "$BASE/admin/posts?size=999"
EXISTING="$(grep -oE '>[^<>]{4,60}</a>' /tmp/cxh-list.html | sed 's/[<>]//g;s|/a$||' || true)"

OK=0 SKIP=0 FAIL=0
for file in "$DIR"/*.md; do
    [ -f "$file" ] || continue
    title="$(awk -v k="title" '/^---$/{n++;next} n==1 && index($0,k": ")==1{sub(k": ","");print;exit}' "$file")"
    tags="$(awk -v k="tags" '/^---$/{n++;next} n==1 && index($0,k": ")==1{sub(k": ","");print;exit}' "$file")"
    summary="$(awk -v k="summary" '/^---$/{n++;next} n==1 && index($0,k": ")==1{sub(k": ","");print;exit}' "$file")"
    body="$(awk '/^---$/{n++;next} n>=2{print}' "$file")"

    if [ -z "$title" ] || [ -z "$body" ]; then
        log "  ✗ $(basename "$file")：缺少标题或正文"
        FAIL=$((FAIL+1)); continue
    fi
    if printf '%s' "$EXISTING" | grep -qF "$title"; then
        log "  - 跳过（已存在）：$title"
        SKIP=$((SKIP+1)); continue
    fi

    code="$(curl -sS -b "$COOKIE" -c "$COOKIE" -o /dev/null -w '%{http_code}' \
        --data-urlencode "title=$title" \
        --data-urlencode "tagsText=$tags" \
        --data-urlencode "summary=$summary" \
        --data-urlencode "contentMarkdown=$body" \
        --data-urlencode "status=PUBLISHED" \
        --data-urlencode "_csrf=$TOKEN" \
        "$BASE/admin/posts")"

    if [ "$code" = "302" ]; then
        log "  ✓ $title"
        OK=$((OK+1))
    else
        log "  ✗ ${title}（HTTP ${code}）"
        FAIL=$((FAIL+1))
    fi
done

log ""
log "导入完成：成功 ${OK}，跳过 ${SKIP}，失败 $FAIL"
[ "$FAIL" = "0" ] || exit 1
