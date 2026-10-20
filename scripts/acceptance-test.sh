#!/usr/bin/env bash
#
# cxh 博客 —— 完整性（验收）测试脚本
#
# 作用：在干净的临时数据库上启动真实应用，按用例表逐条校验，
#       输出 PASS/FAIL 明细与汇总，任何一条失败即以非 0 退出。
#
# 用法：
#   scripts/acceptance-test.sh              # 缺 jar 时自动构建
#   JAR=/path/to/cxh-blog.jar scripts/acceptance-test.sh
#   PORT=18150 KEEP=1 scripts/acceptance-test.sh   # 保留临时目录便于排查
#
# 覆盖范围：公开页面渲染、订阅与站点地图、鉴权与安全、后台 CRUD 与外观编辑、
#          异常与边界输入、数据完整性。设计原则是「只依赖 curl 与 python3」，
#          因此在 CI 或任意装有 JDK 的机器上都能跑。

set -u

PORT="${PORT:-18150}"
BASE="http://127.0.0.1:${PORT}"
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
JAR="${JAR:-$ROOT/target/cxh-blog.jar}"
# 归一化为绝对路径：应用进程在临时目录里启动，相对路径会失效
JAR="$(cd "$(dirname "$JAR")" 2>/dev/null && pwd)/$(basename "$JAR")"
WORK="$(mktemp -d "${TMPDIR:-/tmp}/cxh-acceptance.XXXXXX")"
JAVA_BIN="${JAVA_HOME:+$JAVA_HOME/bin/}java"
COOKIES="$WORK/cookies.txt"
APP_PID=""

PASS=0
FAIL=0
FAILED_CASES=()

# ---------------------------------------------------------------- 基础设施

cleanup() {
    if [ -n "$APP_PID" ] && kill -0 "$APP_PID" 2>/dev/null; then
        kill "$APP_PID" 2>/dev/null || true
        for _ in $(seq 1 20); do kill -0 "$APP_PID" 2>/dev/null || break; sleep 0.5; done
        kill -9 "$APP_PID" 2>/dev/null || true
    fi
    if [ "${KEEP:-0}" = "1" ]; then
        echo "临时目录已保留：$WORK"
    else
        rm -rf "$WORK"
    fi
}
trap cleanup EXIT

record() { # id, 描述, 是否通过(0/1), 实际结果
    local id="$1" desc="$2" ok="$3" actual="$4"
    if [ "$ok" = "0" ]; then
        PASS=$((PASS + 1))
        printf '%-6s %-44s %-5s %s\n' "$id" "$desc" "PASS" "$actual"
    else
        FAIL=$((FAIL + 1))
        FAILED_CASES+=("$id $desc → $actual")
        printf '%-6s %-44s %-5s %s\n' "$id" "$desc" "FAIL" "$actual"
    fi
}

# 断言 HTTP 状态码
status_is() {
    local id="$1" desc="$2" want="$3"; shift 3
    local got
    got="$(curl -s -o /dev/null -w '%{http_code}' "$@")"
    [ "$got" = "$want" ]; record "$id" "$desc" $? "期望 ${want}，实际 $got"
}

# 断言响应体包含某段文本
body_has() {
    local id="$1" desc="$2" needle="$3"; shift 3
    local body
    body="$(curl -s "$@")"
    case "$body" in
        *"$needle"*) record "$id" "$desc" 0 "命中" ;;
        *) record "$id" "$desc" 1 "未命中：$needle" ;;
    esac
}

# 断言响应体不包含某段文本
body_lacks() {
    local id="$1" desc="$2" needle="$3"; shift 3
    local body
    body="$(curl -s "$@")"
    case "$body" in
        *"$needle"*) record "$id" "$desc" 1 "不应出现：$needle" ;;
        *) record "$id" "$desc" 0 "未出现" ;;
    esac
}

# 断言响应头包含某个键值
header_has() {
    local id="$1" desc="$2" name="$3" needle="$4"; shift 4
    local value
    value="$(curl -s -D- -o /dev/null "$@" | tr -d '\r' | awk -F': ' -v n="$name" 'tolower($1)==tolower(n){print $2; exit}')"
    case "$value" in
        *"$needle"*) record "$id" "$desc" 0 "$name 含 $needle" ;;
        *) record "$id" "$desc" 1 "$name=$value" ;;
    esac
}

section() { printf '\n=== %s ===\n' "$1"; }

# 取得当前页面的 CSRF 令牌（表单隐藏域 或 meta 标签）
csrf_from_form() { grep -oE 'name="_csrf" value="[^"]*"' "$1" | head -1 | sed -E 's/.*value="([^"]*)".*/\1/'; }
csrf_from_meta() { grep -oE 'name="_csrf" content="[^"]*"' "$1" | head -1 | sed -E 's/.*content="([^"]*)".*/\1/'; }

xml_ok() { # 用解析器验证 XML 良构（没有 python3 时退化为基本检查）
    if command -v python3 >/dev/null 2>&1; then
        python3 -c "import sys,xml.dom.minidom as m; m.parseString(sys.stdin.read()); print('well-formed')" 2>/dev/null
    else
        echo "skip"
    fi
}

# ---------------------------------------------------------------- 启动应用

if [ ! -f "$JAR" ]; then
    echo "未找到 ${JAR}，先执行构建…"
    (cd "$ROOT" && ./mvnw -B -ntp -q package -DskipTests) || { echo "构建失败"; exit 2; }
fi

echo "启动应用：$JAR （端口 ${PORT}，临时数据目录 $WORK/data）"
(
    cd "$WORK" || exit 1
    SERVER_PORT="$PORT" nohup "$JAVA_BIN" -jar "$JAR" --server.port="$PORT" > "$WORK/app.log" 2>&1 &
    echo $! > "$WORK/app.pid"
)
APP_PID="$(cat "$WORK/app.pid" 2>/dev/null || true)"

UP=0
for _ in $(seq 1 60); do
    if [ "$(curl -s -o /dev/null -w '%{http_code}' --max-time 3 "$BASE/" 2>/dev/null)" = "200" ]; then UP=1; break; fi
    if grep -q "APPLICATION FAILED TO START" "$WORK/app.log" 2>/dev/null; then break; fi
    sleep 1
done
if [ "$UP" != "1" ]; then
    echo "应用启动失败，日志末尾："
    tail -20 "$WORK/app.log"
    exit 2
fi
echo "应用已就绪（PID ${APP_PID}）"

# Spring Boot 的 Web 容器先于 ApplicationRunner 就绪，所以「首页 200」不代表种子数据写完。
# 这里再等一次「列表页能取到文章」，消除测试与种子写入之间的竞态。
for _ in $(seq 1 30); do
    if curl -s "$BASE/posts" | grep -qE '/posts/[^"]+'; then break; fi
    sleep 1
done

# ---------------------------------------------------------------- 公开页面

section "A. 公开页面渲染"
status_is A01 "首页 200"                    200 "$BASE/"
body_has  A02 "首页含站点标题"              "cxh 的博客" "$BASE/"
body_has  A03 "首页含最新文章区"            "最新文章" "$BASE/"
body_has  A04 "首页仅引用兜底样式在先"      "css/blog.css" "$BASE/"
status_is A05 "文章列表 200"                200 "$BASE/posts"
status_is A06 "列表超页码不报错"            200 "$BASE/posts?page=99999"
status_is A07 "列表负数页码被夹取"          200 "$BASE/posts?page=-1"
status_is A08 "标签页 200"                  200 "$BASE/tags"
status_is A09 "归档页 200"                  200 "$BASE/archive"
status_is A10 "关于页 200"                  200 "$BASE/about"
status_is A11 "搜索页 200"                  200 "$BASE/search"
body_has  A12 "搜索命中已发布文章"          "找到" "$BASE/search?q=Spring"
body_has  A13 "搜索未命中给空态"            "没有匹配" -G --data-urlencode "q=zzz不存在的词zzz" "$BASE/search"
status_is A14 "通配符 % 不应全表命中"       200 "$BASE/search?q=%25"
body_has  A15 "通配符 % 返回空态"           "没有匹配" "$BASE/search?q=%25"
body_lacks A16 "关键词被 HTML 转义"         "<script>alert(1)</script>" -G --data-urlencode "q=<script>alert(1)</script>" "$BASE/search"
status_is A17 "静态样式表 200"              200 "$BASE/css/blog.css"
status_is A18 "前台脚本 200"                200 "$BASE/js/blog.js"
body_has  A19 "图标可访问"                  "<svg" "$BASE/images/favicon.svg"
# 先取一篇文章的 slug，再断言详情页渲染——取不到就直接报错，
# 避免拼出空 slug 的地址（那样失败原因会被掩盖成一句“未命中”）
FIRST_SLUG="$(curl -s "$BASE/posts" | grep -oE '/posts/[^"]+' | head -1 | sed 's|/posts/||')"
if [ -z "$FIRST_SLUG" ]; then
    record A20 "文章详情渲染 Markdown" 1 "列表页取不到任何文章链接"
else
    body_has A20 "文章详情渲染 Markdown（${FIRST_SLUG}）" "<h1" "$BASE/posts/$FIRST_SLUG"
fi

section "B. 主题样式表（风格可编辑的核心通道）"
header_has B01 "theme.css 内容类型正确"     "Content-Type" "text/css" "$BASE/theme.css"
header_has B02 "theme.css 禁止缓存"         "Cache-Control" "no-store" "$BASE/theme.css"
body_has   B03 "含主色变量"                 "--cxh-primary:" "$BASE/theme.css"
body_has   B04 "含排版变量"                 "--cxh-content-width:" "$BASE/theme.css"
body_has   B05 "含派生圆角变量"             "--cxh-radius-sm:" "$BASE/theme.css"

section "C. 订阅与站点地图"
status_is C01 "feed.xml 200"                200 "$BASE/feed.xml"
header_has C02 "feed 内容类型"              "Content-Type" "application/rss+xml" "$BASE/feed.xml"
status_is C03 "sitemap.xml 200"             200 "$BASE/sitemap.xml"
status_is C04 "robots.txt 200"              200 "$BASE/robots.txt"
body_has  C05 "robots 指向 sitemap"         "Sitemap: http" "$BASE/robots.txt"
body_has  C06 "robots 屏蔽后台"             "Disallow: /admin/" "$BASE/robots.txt"
FEED_XML="$(curl -s "$BASE/feed.xml")"
if [ "$(printf '%s' "$FEED_XML" | xml_ok)" = "well-formed" ]; then
    record C07 "feed 是良构 XML" 0 "解析通过"
else
    record C07 "feed 是良构 XML" 1 "解析失败或缺少 python3"
fi
SITEMAP_XML="$(curl -s "$BASE/sitemap.xml")"
if [ "$(printf '%s' "$SITEMAP_XML" | xml_ok)" = "well-formed" ]; then
    record C08 "sitemap 是良构 XML" 0 "解析通过"
else
    record C08 "sitemap 是良构 XML" 1 "解析失败或缺少 python3"
fi

section "D. 鉴权与安全基线"
status_is D01 "未登录访问后台被重定向"      302 "$BASE/admin"
status_is D02 "未登录访问外观页被重定向"    302 "$BASE/admin/appearance"
status_is D03 "未登录访问文章管理被重定向"  302 "$BASE/admin/posts"
status_is D04 "健康检查公开"                200 "$BASE/actuator/health"
status_is D05 "非公开 actuator 端点被拒"    302 "$BASE/actuator/env"
status_is D06 "白名单外的路径被默认拒绝"    302 "$BASE/no-such-page"
# 说明：安全策略是「默认拒绝 + 白名单放行」，未在白名单中声明的路径一律拒绝并跳转登录页。
# 因此只有白名单内的路径（如 /posts/**）才会产生 404 语义 —— 见 D07。
status_is D07 "不存在的文章 404"            404 "$BASE/posts/no-such-post"
status_is D08 "POST 无 CSRF 令牌被拒"       403 -X POST "$BASE/admin/appearance"
status_is D09 "PUT 无 CSRF 令牌被拒"        403 -X PUT "$BASE/posts"
header_has D10 "X-Content-Type-Options"     "X-Content-Type-Options" "nosniff" "$BASE/"
header_has D11 "X-Frame-Options"            "X-Frame-Options" "SAMEORIGIN" "$BASE/"
header_has D12 "内容安全策略存在"           "Content-Security-Policy" "default-src 'self'" "$BASE/"
header_has D13 "CSP 禁止内联脚本"           "Content-Security-Policy" "script-src 'self'" "$BASE/"
body_has   D14 "错误响应为 JSON（Accept）"  "\"success\":false" -H "Accept: application/json" "$BASE/posts/no-such-post"

section "E. 登录流程"
# 注意：会话相关的请求必须同时带 -b（读）与 -c（写），否则会新建会话导致令牌不匹配
curl -s -b "$COOKIES" -c "$COOKIES" -o "$WORK/login.html" "$BASE/login"
body_has E01 "登录页含 CSRF 隐藏域" "_csrf" -b "$COOKIES" -c "$COOKIES" "$BASE/login"
LOGIN_TOKEN="$(csrf_from_form "$WORK/login.html")"
status_is E02 "错误密码不通过" 302 -b "$COOKIES" -c "$COOKIES" -o /dev/null \
    -d "username=admin&password=wrong-password&_csrf=$LOGIN_TOKEN" "$BASE/login"
body_has E03 "错误密码给出提示" "不正确" -b "$COOKIES" -c "$COOKIES" "$BASE/login?error"
# 登录成功后会话与 CSRF 令牌都会轮换，因此这里重新取一次令牌
curl -s -b "$COOKIES" -c "$COOKIES" -o "$WORK/login2.html" "$BASE/login"
LOGIN_TOKEN="$(csrf_from_form "$WORK/login2.html")"
status_is E04 "正确密码登录成功" 302 -b "$COOKIES" -c "$COOKIES" -o /dev/null \
    -d "username=admin&password=cxh@2024&_csrf=$LOGIN_TOKEN" "$BASE/login"
body_has E05 "登录后可见后台概览" "文章总数" -b "$COOKIES" -c "$COOKIES" "$BASE/admin"

# 后台页面的 CSRF 令牌（登录后会话令牌会轮换，必须重新获取）
curl -s -b "$COOKIES" -c "$COOKIES" -o "$WORK/appearance.html" "$BASE/admin/appearance"
CSRF="$(csrf_from_meta "$WORK/appearance.html")"

section "F. 后台文章管理"
status_is F01 "文章列表页 200" 200 -b "$COOKIES" "$BASE/admin/posts"
status_is F02 "按状态筛选 200" 200 -b "$COOKIES" "$BASE/admin/posts?status=DRAFT"
status_is F03 "新建页 200" 200 -b "$COOKIES" "$BASE/admin/posts/new"
STAMP="$(date +%s)"
TITLE="验收测试文章 ${STAMP}"
status_is F04 "保存并重定向" 302 -b "$COOKIES" -o /dev/null \
    --data-urlencode "title=$TITLE" --data-urlencode "summary=验收摘要" \
    --data-urlencode "tagsText=验收、测试" --data-urlencode "contentMarkdown=## 验收正文

- 列表项" \
    --data-urlencode "status=PUBLISHED" --data-urlencode "_csrf=$CSRF" "$BASE/admin/posts"
body_has F05 "新文章出现在前台列表" "$TITLE" "$BASE/posts"
body_has F06 "新文章可搜索到" "找到" "$BASE/search?q=$STAMP"
DRAFT_MARK="草稿标记${STAMP}"
status_is F07 "保存草稿" 302 -b "$COOKIES" -o /dev/null \
    --data-urlencode "title=$DRAFT_MARK" --data-urlencode "contentMarkdown=草稿正文" \
    --data-urlencode "status=DRAFT" --data-urlencode "_csrf=$CSRF" "$BASE/admin/posts"
body_lacks F08 "草稿不进公开列表" "$DRAFT_MARK" "$BASE/posts"
body_has F09 "草稿搜不到（返回空态）" "没有匹配" -G --data-urlencode "q=$DRAFT_MARK" "$BASE/search"
body_has F10 "草稿在后台可见" "$DRAFT_MARK" -b "$COOKIES" "$BASE/admin/posts?status=DRAFT"
status_is F11 "标题为空被拦下" 200 -b "$COOKIES" -o /dev/null \
    --data-urlencode "title=" --data-urlencode "contentMarkdown=x" \
    --data-urlencode "status=DRAFT" --data-urlencode "_csrf=$CSRF" "$BASE/admin/posts"
body_has F12 "标题校验提示可见" "标题不能为空" -b "$COOKIES" \
    --data-urlencode "title=" --data-urlencode "contentMarkdown=x" \
    --data-urlencode "status=DRAFT" --data-urlencode "_csrf=$CSRF" "$BASE/admin/posts"
body_has F13 "Markdown 预览接口可用" "<h1>标题</h1>" -b "$COOKIES" \
    --data-urlencode "contentMarkdown=# 标题" --data-urlencode "_csrf=$CSRF" "$BASE/admin/posts/preview"
status_is F14 "预览接口需要登录" 302 "$BASE/admin/posts/preview"

section "G. 外观自由编辑（本项目核心功能）"
body_has G01 "编辑页含取色器" "id=\"primaryColor\"" -b "$COOKIES" "$BASE/admin/appearance"
body_has G02 "编辑页含预置主题卡片" "preset-card" -b "$COOKIES" "$BASE/admin/appearance"
body_has G03 "编辑页含实时预览框" "preview-frame" -b "$COOKIES" "$BASE/admin/appearance"
body_has G04 "编辑页含自定义 CSS" "id=\"customCss\"" -b "$COOKIES" "$BASE/admin/appearance"
status_is G05 "非法颜色被校验拦下" 200 -b "$COOKIES" -o /dev/null \
    --data-urlencode "siteTitle=标题" --data-urlencode "authorName=cxh" \
    --data-urlencode "preset=ELEGANT_LIGHT" --data-urlencode "primaryColor=red;" \
    --data-urlencode "headingFont=MONO" --data-urlencode "bodyFont=MONO" \
    --data-urlencode "baseFontSize=17" --data-urlencode "contentWidth=780" \
    --data-urlencode "cornerRadius=14" --data-urlencode "_csrf=$CSRF" "$BASE/admin/appearance"
body_has G06 "非法颜色给出提示" "主色需为" -b "$COOKIES" \
    --data-urlencode "siteTitle=标题" --data-urlencode "authorName=cxh" \
    --data-urlencode "preset=ELEGANT_LIGHT" --data-urlencode "primaryColor=red;" \
    --data-urlencode "headingFont=MONO" --data-urlencode "bodyFont=MONO" \
    --data-urlencode "baseFontSize=17" --data-urlencode "contentWidth=780" \
    --data-urlencode "cornerRadius=14" --data-urlencode "_csrf=$CSRF" "$BASE/admin/appearance"
status_is G07 "保存合法外观" 302 -b "$COOKIES" -o /dev/null \
    --data-urlencode "siteTitle=验收站点" --data-urlencode "siteSubtitle=验收副标题" \
    --data-urlencode "authorName=cxh" --data-urlencode "introMarkdown=## 验收简介" \
    --data-urlencode "preset=MIDNIGHT_DARK" --data-urlencode "primaryColor=#22d3ee" \
    --data-urlencode "accentColor=#a78bfa" --data-urlencode "pageBackground=#0b1220" \
    --data-urlencode "surfaceBackground=#131a26" --data-urlencode "textColor=#e5e7eb" \
    --data-urlencode "mutedTextColor=#94a3b8" --data-urlencode "headingFont=MODERN_SERIF" \
    --data-urlencode "bodyFont=HUMANIST_SANS" --data-urlencode "baseFontSize=18" \
    --data-urlencode "contentWidth=860" --data-urlencode "cornerRadius=20" \
    --data-urlencode "customCss=.post-card { border: 0; }" --data-urlencode "_csrf=$CSRF" \
    "$BASE/admin/appearance"
body_has G08 "样式表立即反映新主色" "--cxh-primary: #22d3ee;" "$BASE/theme.css"
body_has G09 "样式表立即反映新字号" "--cxh-font-size: 18px;" "$BASE/theme.css"
body_has G10 "自定义 CSS 已注入" ".post-card { border: 0; }" "$BASE/theme.css"
body_has G11 "首页立即反映新站名" "验收站点" "$BASE/"
body_has G12 "关于页立即反映新简介" "验收简介" "$BASE/about"
status_is G13 "预览接口不写库仍可用" 200 -b "$COOKIES" -o /dev/null \
    --data-urlencode "preset=ELEGANT_LIGHT" --data-urlencode "primaryColor=#ff0000" \
    --data-urlencode "_csrf=$CSRF" "$BASE/admin/appearance/preview"
body_lacks G14 "预览未污染已保存风格" "--cxh-primary: #ff0000;" "$BASE/theme.css"
status_is G15 "一键套用预置主题" 302 -b "$COOKIES" -o /dev/null \
    --data-urlencode "preset=WARM_PAPER" --data-urlencode "_csrf=$CSRF" "$BASE/admin/appearance/preset"
body_has G16 "预置主题换了配色" "--cxh-primary: #b45309;" "$BASE/theme.css"
body_has G17 "预置主题同时换字体" "Iowan Old Style" "$BASE/theme.css"
status_is G18 "预置主题列表接口" 200 -b "$COOKIES" -H "Accept: application/json" "$BASE/admin/appearance/presets"

section "H. 异常与边界输入（缺陷回归）"
head -c 20000 /dev/zero | tr '\0' 'x' > "$WORK/oversized.txt"   # 20000 字，超过 15000 上限
status_is H01 "非法枚举参数返回 400" 400 -b "$COOKIES" "$BASE/admin/posts?status=NOT_A_STATUS"
status_is H02 "非法路径变量返回 400" 400 -b "$COOKIES" "$BASE/admin/posts/abc/edit"
status_is H03 "非法分页参数返回 400" 400 "$BASE/posts?page=abc"
status_is H04 "只允许 POST 的地址发 GET 返回 405" 405 -b "$COOKIES" "$BASE/admin/posts/1/delete"
status_is H05 "不存在的文章编辑页 404" 404 -b "$COOKIES" "$BASE/admin/posts/999999/edit"
body_lacks H06 "错误页标题不含未解析表达式" "?:" "$BASE/posts/no-such-post"
body_lacks H07 "错误页标题不含 null" "null ·" "$BASE/posts/no-such-post"
body_has H08 "绑定失败给出中文提示" "请选择有效的文章状态" -b "$COOKIES" \
    --data-urlencode "title=非法状态" --data-urlencode "contentMarkdown=正文" \
    --data-urlencode "status=NOT_A_STATUS" --data-urlencode "_csrf=$CSRF" "$BASE/admin/posts"
body_has H09 "超长正文被校验拦下" "正文最长 15000 字" -b "$COOKIES" \
    --data-urlencode "title=超长正文" --data-urlencode "contentMarkdown@$WORK/oversized.txt" \
    --data-urlencode "status=DRAFT" --data-urlencode "_csrf=$CSRF" "$BASE/admin/posts"
body_lacks H10 "后台模板无内联事件处理器" "onsubmit=" -b "$COOKIES" "$BASE/admin/posts"
body_has H11 "删除确认走外部脚本" "data-confirm=" -b "$COOKIES" "$BASE/admin/posts"

section "J. 图片上传（新增能力）"
# 造一张真实 PNG（用 python3 生成，避免依赖外部图片文件）
if command -v python3 >/dev/null 2>&1; then
    python3 - "$WORK/upload-test.png" <<'PY'
import struct, sys, zlib
path = sys.argv[1]
w = h = 120
raw = b''.join(b'\x00' + bytes([30, 90, 200] * w) for _ in range(h))
def chunk(tag, data):
    c = tag + data
    return struct.pack('>I', len(data)) + c + struct.pack('>I', zlib.crc32(c) & 0xffffffff)
png = (b'\x89PNG\r\n\x1a\n'
       + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 2, 0, 0, 0))
       + chunk(b'IDAT', zlib.compress(raw))
       + chunk(b'IEND', b''))
open(path, 'wb').write(png)
PY
    printf 'not-an-image' > "$WORK/upload-fake.png"

    status_is J01 "未登录不能上传（CSRF 先拒）" 403 -X POST -F "file=@$WORK/upload-test.png;type=image/png" "$BASE/api/media/images"
    status_is J02 "已登录但无 CSRF 被拒" 403 -b "$COOKIES" -X POST \
        -F "file=@$WORK/upload-test.png;type=image/png" "$BASE/api/media/images"

    UPLOAD_JSON="$(curl -s -b "$COOKIES" -X POST -H "X-CSRF-TOKEN: $CSRF" \
        -F "file=@$WORK/upload-test.png;type=image/png" -F "alt=验收上传图" "$BASE/api/media/images")"
    case "$UPLOAD_JSON" in
        *'"success":true'*) record J03 "上传成功并返回元数据" 0 "命中" ;;
        *) record J03 "上传成功并返回元数据" 1 "$(printf '%s' "$UPLOAD_JSON" | head -c 80)" ;;
    esac
    UPLOAD_URL="$(printf '%s' "$UPLOAD_JSON" | sed -n 's/.*"url":"\([^"]*\)".*/\1/p')"
    THUMB_URL="$(printf '%s' "$UPLOAD_JSON" | sed -n 's/.*"thumbnailUrl":"\([^"]*\)".*/\1/p')"
    body_has J04 "返回可粘贴的相册 Markdown" "[![验收上传图](" "$BASE/api/media/images" \
        -b "$COOKIES" -X POST -H "X-CSRF-TOKEN: $CSRF" \
        -F "file=@$WORK/upload-test.png;type=image/png" -F "alt=验收上传图"

    if [ -n "$UPLOAD_URL" ] && [ -n "$THUMB_URL" ]; then
        status_is J05 "上传的图片可公开访问" 200 "$BASE$UPLOAD_URL"
        status_is J06 "缩略图可公开访问" 200 "$BASE$THUMB_URL"
    else
        record J05 "上传的图片可公开访问" 1 "未拿到 url"
        record J06 "缩略图可公开访问" 1 "未拿到 thumbnailUrl"
    fi

    status_is J07 "伪造图片内容被拒绝" 400 -b "$COOKIES" -X POST -H "X-CSRF-TOKEN: $CSRF" \
        -F "file=@$WORK/upload-fake.png;type=image/png" "$BASE/api/media/images"
    body_has J08 "伪造内容返回可读提示" "无法识别的图片内容" -b "$COOKIES" -X POST \
        -H "X-CSRF-TOKEN: $CSRF" -F "file=@$WORK/upload-fake.png;type=image/png" "$BASE/api/media/images"
else
    record J01 "图片上传用例" 1 "缺少 python3，无法生成测试图片"
fi

section "I. 数据完整性"
SEARCH_HIT="$(curl -s -G --data-urlencode "q=$STAMP" "$BASE/search")"
SLUG_A="$(printf '%s' "$SEARCH_HIT" | grep -oE '/posts/[^"]+' | head -1 | sed 's|/posts/||')"
body_has I01 "搜索结果可定位到文章" "/posts/" -G --data-urlencode "q=$STAMP" "$BASE/search"
body_has I02 "文章详情渲染 Markdown 标题" "<h2>验收正文</h2>" "$BASE/posts/$SLUG_A"
body_has I03 "详情页显示阅读量与时长" "次浏览" "$BASE/posts/$SLUG_A"
body_has I04 "sitemap 收录新文章" "$SLUG_A" "$BASE/sitemap.xml"
body_has I05 "feed 收录新文章" "$TITLE" "$BASE/feed.xml"
status_is I06 "登出接口可用" 302 -b "$COOKIES" -c "$COOKIES" -X POST \
    -d "_csrf=$CSRF" "$BASE/logout"
status_is I07 "登出后后台不可访问" 302 -b "$COOKIES" "$BASE/admin"

# ---------------------------------------------------------------- 汇总

printf '\n%s\n' "──────────────────────────────────────────────"
printf '用例总数：%d    通过：%d    失败：%d\n' "$((PASS + FAIL))" "$PASS" "$FAIL"
if [ "$FAIL" -gt 0 ]; then
    printf '\n失败用例：\n'
    for case in "${FAILED_CASES[@]}"; do printf '  - %s\n' "$case"; done
    exit 1
fi
echo "全部通过 ✅"
exit 0
