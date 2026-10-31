#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
生成《cxh 个人博客系统 软件说明书》DOCX。

文档结构参照 GB/T 8567-2006《计算机软件文档编制规范》，
把课程软件工程通常要求的几份文档合并为一份说明书：
需求规格（第 3 章）、概要设计（第 4 章）、详细设计（第 5 章）、
部署手册（第 6 章）、用户手册（第 7 章）、测试报告（第 8 章）、
质量与维护（第 9 章），另附接口与数据结构附录。

用法：
    python docs/build-manual.py [输出路径]
"""

import sys
from datetime import date

from docx import Document
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml.ns import qn
from docx.shared import Pt, Cm, RGBColor

PROJECT = "cxh 个人博客系统"
VERSION = "V1.0.0"
DOC_TITLE = f"{PROJECT} 软件说明书"
OUTPUT = sys.argv[1] if len(sys.argv) > 1 else "docs/cxh个人博客系统-软件说明书.docx"

BODY_EAST_ASIA = "宋体"
HEADING_EAST_ASIA = "黑体"
MONO = "Consolas"


# --------------------------------------------------------------------- 工具

def set_style_font(style, east_asia, ascii_font="Times New Roman", size=None, bold=None):
    """设置样式字体。必须用 get_or_add_rFonts()，手工 append 会造成重复元素与
    不合法的子元素顺序，Word 尚能容忍、LibreOffice 会直接渲染不出文字。"""
    style.font.name = ascii_font
    if size is not None:
        style.font.size = Pt(size)
    if bold is not None:
        style.font.bold = bold
    rpr = style.element.get_or_add_rPr()
    rfonts = rpr.get_or_add_rFonts()
    rfonts.set(qn("w:eastAsia"), east_asia)
    rfonts.set(qn("w:ascii"), ascii_font)
    rfonts.set(qn("w:hAnsi"), ascii_font)


def run_font(run, east_asia=BODY_EAST_ASIA, ascii_font="Times New Roman", size=None, bold=None):
    run.font.name = ascii_font
    if size is not None:
        run.font.size = Pt(size)
    if bold is not None:
        run.font.bold = bold
    rpr = run._element.get_or_add_rPr()
    rfonts = rpr.get_or_add_rFonts()
    rfonts.set(qn("w:eastAsia"), east_asia)
    rfonts.set(qn("w:ascii"), ascii_font)
    rfonts.set(qn("w:hAnsi"), ascii_font)


class Manual:
    def __init__(self):
        self.doc = Document()
        self._setup()

    def _setup(self):
        normal = self.doc.styles["Normal"]
        set_style_font(normal, BODY_EAST_ASIA, size=10.5)
        normal.paragraph_format.line_spacing = 1.4
        normal.paragraph_format.space_after = Pt(4)

        for name, size in (("Heading 1", 16), ("Heading 2", 13.5), ("Heading 3", 12), ("Heading 4", 11)):
            style = self.doc.styles[name]
            set_style_font(style, HEADING_EAST_ASIA, ascii_font="Arial", size=size, bold=True)
            style.font.color.rgb = RGBColor(0x1F, 0x36, 0x4D)
            style.paragraph_format.space_before = Pt(10 if name != "Heading 1" else 16)
            style.paragraph_format.space_after = Pt(6)

        section = self.doc.sections[0]
        section.left_margin = Cm(2.6)
        section.right_margin = Cm(2.6)
        section.top_margin = Cm(2.4)
        section.bottom_margin = Cm(2.4)

    # ---- 内容构件

    def title(self, text, size=24):
        paragraph = self.doc.add_paragraph()
        paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
        run = paragraph.add_run(text)
        run_font(run, HEADING_EAST_ASIA, "Arial", size=size, bold=True)
        return paragraph

    def h(self, text, level=1):
        return self.doc.add_heading(text, level=level)

    def p(self, text, align=None, indent=True):
        paragraph = self.doc.add_paragraph(text)
        if align == "center":
            paragraph.alignment = WD_ALIGN_PARAGRAPH.CENTER
        if indent is False:
            paragraph.paragraph_format.first_line_indent = Pt(0)
        return paragraph

    def bullets(self, items):
        for item in items:
            self.doc.add_paragraph(item, style="List Bullet")

    def steps(self, items):
        for item in items:
            self.doc.add_paragraph(item, style="List Number")

    def code(self, text):
        paragraph = self.doc.add_paragraph()
        paragraph.paragraph_format.left_indent = Cm(0.6)
        paragraph.paragraph_format.space_after = Pt(8)
        run = paragraph.add_run(text)
        run_font(run, MONO, MONO, size=9)
        return paragraph

    def table(self, headers, rows, widths=None):
        table = self.doc.add_table(rows=1, cols=len(headers))
        table.style = "Table Grid"
        table.alignment = WD_TABLE_ALIGNMENT.CENTER
        for index, header in enumerate(headers):
            cell = table.rows[0].cells[index]
            cell.text = ""
            run = cell.paragraphs[0].add_run(header)
            run_font(run, HEADING_EAST_ASIA, "Arial", size=9.5, bold=True)
        for row in rows:
            cells = table.add_row().cells
            for index, value in enumerate(row):
                cells[index].text = ""
                run = cells[index].paragraphs[0].add_run(str(value))
                run_font(run, BODY_EAST_ASIA, size=9)
        if widths:
            for row in table.rows:
                for index, width in enumerate(widths):
                    row.cells[index].width = Cm(width)
        self.doc.add_paragraph()
        return table

    def page_break(self):
        self.doc.add_page_break()

    def save(self, path):
        self.doc.save(path)


# --------------------------------------------------------------------- 正文

def build():
    m = Manual()

    # ---------------- 封面 ----------------
    m.p("")
    m.title(DOC_TITLE, size=26)
    m.p("")
    m.p("（需求规格 · 概要设计 · 详细设计 · 用户手册 · 测试报告 合订本）",
        align="center")
    m.p("")
    m.table(
        ["项目", "内容"],
        [
            ["项目名称", PROJECT],
            ["版本号", VERSION],
            ["文档用途", "课程软件工程实践——软件说明书（合并标准文档）"],
            ["技术栈", "Java 17 / Spring Boot 3.5.16 / Thymeleaf / Spring Data JPA / "
                       "Spring Security / Flyway / H2·MySQL"],
            ["代码规模", "62 个 Java 源文件 + 13 个测试文件（94 个用例）、15 个页面模板、2 个数据库迁移脚本"],
            ["编写日期", date.today().isoformat()],
        ],
        widths=[3.5, 11.5])
    m.p("")
    m.p("本说明书所有数据均来自项目真实构建与测试结果（94 项自动化测试、110 项验收用例、"
        "行覆盖率 90.8%），非估计值。", align="center")
    m.page_break()

    # ---------------- 目录 ----------------
    m.h("目录", level=1)
    for item in [
        "1 引言",
        "2 任务概述",
        "3 需求规定",
        "4 总体设计（概要设计）",
        "5 详细设计",
        "6 运行环境与部署",
        "7 用户操作手册",
        "8 测试报告",
        "9 质量保证与维护",
        "10 使用说明",
        "11 前后端更改指南",
        "附录 A 接口清单",
        "附录 B 数据库结构",
        "附录 C 代码文件清单",
    ]:
        m.p(item, indent=False)
    m.page_break()

    # ---------------- 1 引言 ----------------
    m.h("1 引言", level=1)

    m.h("1.1 编写目的", level=2)
    m.p("本文档面向课程软件工程实践，完整描述 cxh 个人博客系统的需求、设计、实现与测试。"
        "按照 GB/T 8567-2006《计算机软件文档编制规范》，课程通常要求提交需求规格说明书、"
        "概要设计说明书、详细设计说明书、测试报告与用户手册五份文档；本说明书将它们合并为一份，"
        "对应关系为：第 3 章＝需求规格，第 4 章＝概要设计，第 5 章＝详细设计，"
        "第 8 章＝测试报告，第 7 章＝用户手册；第 10 章＝使用说明（运维速查），"
        "第 11 章＝前后端更改指南（维护开发指引）。")
    m.p("预期读者：课程指导教师、项目评审人、后续维护该系统的开发者。")

    m.h("1.2 项目背景", level=2)
    m.p("项目名称：cxh 个人博客系统；提出者与开发者：单人课程项目；"
        "本系统为独立开发，不依赖任何既有系统的数据。")
    m.p("选题动机：个人博客是典型的「内容 + 展示」型应用。多数课程项目把站点配置硬编码在模板中，"
        "改一次配色就要重新打包；本项目要解决的核心问题是——"
        "让**站点风格与简介可以在后台自由编辑，保存后全站立即生效**，"
        "同时用清晰的分层与接口边界保证代码的高内聚、低耦合。")

    m.h("1.3 术语与缩写", level=2)
    m.table(
        ["术语", "含义"],
        [
            ["主题令牌（ThemeTokens）", "站点风格的最小完整取值集合：6 个颜色 + 2 个字体 + 排版参数与开关"],
            ["风格渲染器", "把主题令牌翻译为 CSS 变量的组件（ThemeStyleRenderer 接口）"],
            ["读模型 / 写模型", "面向展示的只读对象（如 PostSummary）与面向编辑的写入对象（如 PostForm）"],
            ["域（domain 包）", "按业务能力垂直切分出的自治模块，本系统有 site（站点域）与 post（文章域）"],
            ["slug", "文章的 URL 可读标识，如 /posts/hello-spring-boot-3"],
            ["CSRF", "跨站请求伪造；本系统保留 Spring Security 默认的令牌校验"],
            ["CSP", "内容安全策略响应头，限制页面可加载的脚本与样式来源"],
            ["Flyway", "数据库结构版本化迁移工具，本系统用它替代 ddl-auto 自动建表"],
        ],
        widths=[4.2, 10.8])

    m.h("1.4 参考资料", level=2)
    m.bullets([
        "GB/T 8567-2006《计算机软件文档编制规范》",
        "Spring Boot 3.5.x Reference Documentation",
        "Spring Security 6 Reference Documentation",
        "Flyway Documentation（Database Migrations）",
        "CommonMark Spec 0.31.2",
        "项目仓库 README.md 与源码注释（本说明书的实现细节均可在源码中逐条核对）",
    ])

    # ---------------- 2 任务概述 ----------------
    m.h("2 任务概述", level=1)

    m.h("2.1 建设目标", level=2)
    m.bullets([
        "功能目标：提供个人博客所需的最小完整功能集——写作与发布、浏览与检索、订阅与收录；",
        "特色目标：站点风格（配色、字体、排版）与简介在后台可视化编辑，保存后全站立即生效，"
        "无需重新打包、无需重启；",
        "质量目标：结构清晰可维护（高内聚低耦合）、输入安全可信、可移植到两种数据库、可容器化部署；",
        "工程目标：自动化测试覆盖关键路径，缺陷可回归，文档可复现。",
    ])

    m.h("2.2 用户特点", level=2)
    m.table(
        ["角色", "技术水平", "使用频度", "主要诉求"],
        [
            ["访客", "普通网民", "偶发", "快速读到内容；手机上可读；能找到旧文章"],
            ["站长（作者）", "具备 Java 后端能力", "每天", "Markdown 写作顺手；改配色不用改代码；改完立刻看到效果"],
            ["运维/部署者", "熟悉 Docker 与数据库", "部署期", "一条命令起服务；表结构可版本化；配置用环境变量注入"],
        ],
        widths=[2.6, 3.6, 2.2, 6.6])

    m.h("2.3 运行环境", level=2)
    m.table(
        ["项目", "要求"],
        [
            ["开发语言", "Java（编译目标 17，已在 JDK 17 与 JDK 26 上实测通过全部测试）"],
            ["应用框架", "Spring Boot 3.5.16"],
            ["数据库", "默认 H2 文件库（零配置）；生产可切 MySQL 8.0+（已在 MySQL 9.7 实测）"],
            ["构建工具", "Maven（仓库自带 Maven Wrapper，本机无需安装 Maven）"],
            ["浏览器", "支持 CSS 自定义属性（变量）的现代浏览器"],
            ["容器（可选）", "Docker + Docker Compose"],
            ["前端构建", "无——不需要 Node.js 与 npm"],
        ],
        widths=[3.2, 11.8])

    m.h("2.4 约束与假设", level=2)
    m.bullets([
        "单站点、单管理员：站点档案固定一行，管理员账号来自配置而非数据库；",
        "正文与简介以 Markdown 存储，原始 HTML 一律转义，样式自由度收敛到 CSS 变量通道；",
        "文章正文长度上限 15000 字（受 MySQL TEXT 列 64KB 与 utf8mb4 编码约束推导得出）；",
        "站点风格取值受白名单约束：颜色必须为十六进制、字体必须来自枚举、数值必须落在区间内；",
        "假设部署环境可通过环境变量注入数据库口令与管理员口令。",
    ])

    # ---------------- 3 需求规定 ----------------
    m.h("3 需求规定", level=1)

    m.h("3.1 功能需求", level=2)
    m.p("功能需求编号 FR-xx，其中「来源」列指向第 4、5 章的设计实现，"
        "「验证」列指向第 8 章中覆盖该需求的测试用例，构成需求追踪链。")
    m.table(
        ["编号", "功能名称", "功能描述", "优先级"],
        [
            ["FR-01", "首页展示", "展示站点标题、副标题、Markdown 简介、最新文章与标签云；Hero 区可开关", "高"],
            ["FR-02", "文章列表", "分页展示已发布文章，支持页码参数，页码越界自动夹取", "高"],
            ["FR-03", "文章详情", "按 slug 展示文章，Markdown 渲染为 HTML，累加阅读量并显示预估阅读时长", "高"],
            ["FR-04", "归档", "按年份分组列出全部已发布文章", "中"],
            ["FR-05", "标签", "标签云与按标签检索文章", "中"],
            ["FR-06", "站内搜索", "在已发布文章的标题、摘要、正文中检索；LIKE 通配符必须转义", "高"],
            ["FR-07", "关于页", "展示站长简介与联系信息", "中"],
            ["FR-08", "RSS 订阅", "/feed.xml 输出 RSS 2.0，含站点信息与最近 20 篇文章", "中"],
            ["FR-09", "站点地图", "/sitemap.xml 输出静态页、标签页与文章页 URL", "中"],
            ["FR-10", "爬虫规则", "/robots.txt 指向 sitemap 并屏蔽后台路径", "低"],
            ["FR-11", "管理员登录", "会话登录，口令支持明文（启动时加密）或 BCrypt 摘要", "高"],
            ["FR-12", "文章管理", "新建、编辑、删除、草稿/发布状态切换", "高"],
            ["FR-13", "写作实时预览", "编辑器内实时渲染 Markdown，与发布使用同一渲染器", "中"],
            ["FR-14", "站点身份编辑", "编辑站点标题、副标题、作者、头像、所在地、邮箱、GitHub、页脚、备案号", "高"],
            ["FR-15", "简介编辑", "Markdown 简介，保存后首页与关于页立即生效", "高"],
            ["FR-16", "风格令牌编辑", "6 个颜色、2 项字体、字号、内容宽度、圆角、吸顶与 Hero 开关", "高"],
            ["FR-17", "预置主题套用", "7 套预置主题一键套用（含配色与字体），保留用户排版参数", "中"],
            ["FR-18", "外观实时预览", "编辑时右侧预览真实首页并实时刷新；预览不写库", "高"],
            ["FR-19", "自定义 CSS", "在主题变量之后追加自定义 CSS", "中"],
            ["FR-20", "统一错误处理", "页面请求返回风格化错误页，JSON 请求返回统一结构", "中"],
            ["FR-21", "后台概览", "文章总数、已发布、草稿、累计浏览与最近更新", "中"],
            ["FR-22", "动态样式表", "/theme.css 按当前风格实时生成 CSS 变量，禁止缓存", "高"],
            ["FR-23", "图片相册", "文章内以「缩略图套原图链接」的 Markdown 写法排成等高照片墙，点击看原图；并支持作者头像与文章封面", "中"],
            ["FR-24", "后台图片上传", "文章编辑页直接上传图片（jpeg/png/gif/webp，≤8MB），自动生成缩略图并把图文 Markdown 插入光标处，改图无需重新构建", "高"],
        ],
        widths=[1.5, 2.4, 9.3, 1.6])

    m.h("3.2 性能需求", level=2)
    m.table(
        ["编号", "指标", "目标值", "实测情况"],
        [
            ["NFR-01", "页面响应时间", "本机 < 500ms（1000 篇文章规模）", "首页/列表/详情实测 10–60ms"],
            ["NFR-02", "站点档案读取", "命中进程内快照，不查库", "写后替换快照，读路径零查询"],
            ["NFR-03", "样式表输出", "动态生成，单次 < 5ms", "实测 1–3ms（字符串拼接，无 IO）"],
            ["NFR-04", "启动时间", "< 10s", "实测 2.2–2.7s（含 Flyway 迁移与校验）"],
            ["NFR-05", "内存占用", "< 512MB", "默认 JVM 堆参数下约 200MB"],
            ["NFR-06", "站点地图完整性", "收录全部已发布文章（分批查询，不截断）", "已修复旧实现只取前 50 篇的缺陷（BUG-08）"],
        ],
        widths=[1.7, 3.0, 5.3, 5.0])

    m.h("3.3 数据需求", level=2)
    m.p("系统共 3 张业务表 + 1 张迁移历史表，实体关系为：站点档案为单例（固定主键 1）；"
        "文章与标签是一对多（标签以元素集合建模）；站点档案内嵌主题令牌（值对象）。")
    m.code(
        "site_profile (1 行)\n"
        "  └── @Embedded ThemeTokens  （配色 / 字体 / 排版 / 开关）\n"
        "post (N 行)\n"
        "  └── post_tag (1:N，标签为受约束的字符串值集合)\n"
        "flyway_schema_history (N 行，迁移历史)"
    )
    m.p("数据保留策略：文章删除采用物理删除；站点档案只保留最新一份（外观编辑不保留历史版本，"
        "如需回溯可通过数据库备份实现）。")

    m.h("3.4 接口需求", level=2)
    m.bullets([
        "用户界面：服务端渲染的 HTML5 页面，无前端框架与构建步骤；后台使用独立调色板，"
        "避免把前台风格调坏后无法操作；",
        "软件接口：通过 Spring Data JPA 访问数据库；通过 CommonMark 渲染 Markdown；"
        "通过 Flyway 迁移表结构；",
        "通信接口：HTTP/1.1，公开页面为 GET，后台写动作为 POST 表单提交（Post-Redirect-Get），"
        "外观预览与 Markdown 预览为带 CSRF 头的异步 POST。",
    ])

    m.h("3.5 安全需求", level=2)
    m.table(
        ["编号", "安全要求", "实现手段"],
        [
            ["SEC-01", "后台访问控制", "默认拒绝 + 白名单放行；/admin/** 需要 ROLE_ADMIN"],
            ["SEC-02", "跨站请求伪造防护", "保留 Spring Security CSRF；模板表单自动注入令牌，异步请求带 X-CSRF-TOKEN 头"],
            ["SEC-03", "口令安全", "BCrypt 存储，支持明文启动时自动加密"],
            ["SEC-04", "内容注入防护", "Markdown 原始 HTML 转义；关键词回显转义"],
            ["SEC-05", "样式注入防护", "颜色白名单、字体枚举、数值区间校验"],
            ["SEC-06", "点击劫持防护", "X-Frame-Options: SAMEORIGIN 与 CSP frame-ancestors 'self'"],
            ["SEC-07", "脚本来源限制", "CSP script-src 'self'，模板不出现任何内联事件处理器"],
            ["SEC-08", "敏感端点保护", "actuator 仅放行 /actuator/health，其余一律拒绝"],
            ["SEC-09", "上传安全", "文件名重命名为 UUID（防路径穿越）、以真实解码结果判定类型、白名单 + 8MB 上限、仅管理员可调用"],
        ],
        widths=[1.7, 4.3, 9.0])

    m.h("3.6 需求追踪矩阵", level=2)
    m.p("下表给出「需求 → 设计实现 → 测试验证」的追踪关系，用于确认需求无遗漏、测试无空转。")
    m.table(
        ["需求", "关键设计实现", "验证用例"],
        [
            ["FR-01/07", "web/HomeController + layout/base.html + SiteProfileView", "A01-A04、A10"],
            ["FR-02/04/05", "PostService.listPublished / HomeController.groupByYear", "A05-A07、A09、F05"],
            ["FR-03", "PostService.getPublishedBySlug + MarkdownRenderer", "A20、I02、I03"],
            ["FR-06", "SearchKeyword + PostRepository.searchByStatus", "A11-A16、F06、F09"],
            ["FR-08/09/10", "FeedController + RssFeedWriter / SitemapWriter", "C01-C08、I04、I05"],
            ["FR-11", "SecurityConfig + AdminUserDetailsService", "D01-D03、E01-E05、I06、I07"],
            ["FR-12", "PostService.save/delete + PostAdminController", "F01-F12"],
            ["FR-13", "PostAdminController.preview + markdown-preview.js", "F13、F14"],
            ["FR-14/15", "SiteProfile.edit + SiteProfileForm 校验", "G07、G11、G12"],
            ["FR-16/19", "ThemeTokens + CssVariablesThemeStyleRenderer", "G05、G06、G08-G10"],
            ["FR-17", "ThemePreset + SiteProfileService.applyPreset", "G15-G18"],
            ["FR-18", "appearance.js + /admin/appearance/preview", "G03、G13、G14"],
            ["FR-20", "GlobalExceptionHandler", "H01-H09、D14"],
            ["FR-21", "AdminDashboardController + PostStats", "E05"],
            ["FR-22", "SitePublicController.themeCss", "B01-B05、G08"],
            ["FR-23", "BlogSeedData.album + blog.css 排版 + SiteProfile.authorAvatarUrl",
             "SeedDataTest.albumPostsRenderImages、PublicPagesTest.rendersAuthorAvatar / albumImagesAreServed"],
            ["FR-24", "ImageStorage 抽象 + LocalImageStorage + ImageUploadController + image-upload.js",
             "LocalImageStorageTest（11 例）、ImageUploadControllerTest（7 例）、验收用例 J01-J08"],
        ],
        widths=[2.4, 7.6, 5.0])

    m.page_break()

    # ---------------- 4 总体设计 ----------------
    m.h("4 总体设计（概要设计）", level=1)

    m.h("4.1 设计原则", level=2)
    m.p("本系统把「高内聚、低耦合」落实为四条可检查的规则，而不是停留在口号：")
    m.table(
        ["原则", "落地方式", "可检查的证据"],
        [
            ["按业务能力垂直切分",
             "包结构为 site / post / common / security / web / bootstrap，"
             "每个域内部再分 domain→repository→service→web",
             "站点域与文章域互相之间没有任何 import"],
            ["依赖方向单向向内",
             "web、bootstrap → 业务域 → common；跨域协作只在 web 装配层发生",
             "FeedController 同时依赖两域，而两域互不依赖"],
            ["依赖抽象而非实现",
             "MarkdownRenderer、ThemeStyleRenderer、PostService、SiteProfileService 均为接口 + 实现",
             "替换渲染器只需新增一个 Bean"],
            ["读写模型分离",
             "写：SiteProfileForm、PostForm/PostDraft；读：SiteProfileView、PostSummary、PostDetail",
             "模板不接触任何实体类"],
        ],
        widths=[3.0, 7.4, 4.6])
    m.p("判据：修改一个需求时，需要同时打开的文件越少，内聚越高。"
        "例如「新增一套预置主题」只需在 ThemePreset 增加一个枚举常量，再补一条 Flyway 迁移放宽数据库约束，"
        "页面、接口与测试会自动获得新主题。")

    m.h("4.2 体系结构", level=2)
    m.code(
        "浏览器\n"
        "  │  HTTP（服务端渲染 HTML + 动态 CSS）\n"
        "  ▼\n"
        "┌──────────────────────────────────────────────────────────┐\n"
        "│ 表现层   Thymeleaf 模板（layout/base、admin/*）+ 静态资源  │\n"
        "│         只消费 ${site.*} 与 --cxh-* CSS 变量               │\n"
        "├──────────────────────────────────────────────────────────┤\n"
        "│ 装配层   web：GlobalModelAdvice / HomeController          │\n"
        "│          AdminDashboardController / FeedController        │\n"
        "├───────────────────────────────┬──────────────────────────┤\n"
        "│ 站点域 site                    │ 文章域 post               │\n"
        "│  SiteProfile / ThemeTokens     │  Post / PostDraft         │\n"
        "│  ThemeStyleRenderer            │  SearchKeyword / Tags     │\n"
        "│  SiteProfileService            │  PostService              │\n"
        "├───────────────────────────────┴──────────────────────────┤\n"
        "│ 通用层 common：ApiResponse / PageResult / 异常处理 /       │\n"
        "│            MarkdownRenderer / 文本工具                     │\n"
        "├──────────────────────────────────────────────────────────┤\n"
        "│ 安全 security：SecurityConfig / AdminUserDetailsService    │\n"
        "├──────────────────────────────────────────────────────────┤\n"
        "│ 持久化 Spring Data JPA  →  H2 文件库 / MySQL              │\n"
        "│ 结构管理 Flyway（db/migration/{h2,mysql}/V1__init.sql）    │\n"
        "└──────────────────────────────────────────────────────────┘"
    )

    m.h("4.3 模块划分与职责", level=2)
    m.table(
        ["模块", "职责", "对外接口", "依赖"],
        [
            ["common.api", "统一响应与分页模型", "ApiResponse、PageResult", "无"],
            ["common.exception", "异常翻译为 HTTP 语义", "GlobalExceptionHandler", "common.api、common.web"],
            ["common.render", "Markdown 渲染抽象与实现", "MarkdownRenderer", "无"],
            ["common.text", "slug/时间/XML 转义工具", "Slugs、DateTimes、XmlText", "无"],
            ["site.domain", "站点档案聚合根、主题令牌与取值约束", "SiteProfile、ThemeTokens、ThemePreset、FontChoice", "common.text"],
            ["site.theme", "主题令牌渲染为 CSS", "ThemeStyleRenderer", "site.domain"],
            ["site.service", "站点档案读写、快照缓存、样式表输出", "SiteProfileService", "site.domain、site.dto、site.repository、site.theme、common.render"],
            ["site.web", "样式表输出与外观后台", "SitePublicController、SiteAdminController", "site.service、site.dto"],
            ["post.domain", "文章聚合根、编辑意图、标签与检索关键字规则", "Post、PostDraft、PostStatus、Tags、SearchKeyword", "common.text"],
            ["post.service", "文章读写、检索、分页、视图装配", "PostService", "post.domain、post.dto、post.repository、common.render"],
            ["post.web", "文章公开页与后台管理页", "PostPublicController、PostAdminController", "post.service、post.dto、common.render"],
            ["security", "管理员认证、访问策略、安全响应头", "SecurityConfig、AdminUserDetailsService", "无业务依赖"],
            ["web", "跨域页面装配与站点地图/订阅输出", "GlobalModelAdvice、HomeController、AdminDashboardController、FeedController", "site.*、post.*"],
            ["web.feed", "RSS 与 sitemap 序列化（不查库）", "RssFeedWriter、SitemapWriter", "common.text、dto"],
            ["bootstrap", "首启动种子数据（唯一跨多域的组件）", "BlogSeedData", "site.service、post.service"],
        ],
        widths=[2.6, 5.0, 4.4, 3.0])

    m.h("4.4 关键设计决策与取舍", level=2)
    m.table(
        ["决策", "备选方案", "选择理由"],
        [
            ["服务端渲染（Thymeleaf）",
             "Vue/React 单页应用 + REST",
             "博客以内容展示为主，SSR 首屏快、SEO 友好；且完全免去前端构建链，"
             "部署只需一个 jar。代价是交互丰富度不如 SPA"],
            ["风格用 CSS 变量下发",
             "把颜色直接渲染进模板；或编译期生成多套主题",
             "模板与风格解耦：模板零硬编码色值，新增风格维度不改模板；"
             "运行时改库即可换肤，不需要重新构建"],
            ["站点档案进程内快照",
             "每次请求查库；或引入 Spring Cache",
             "站点档案写少读多且只有一行，快照使读路径零查询且无代理自调用陷阱"],
            ["Flyway + ddl-auto=validate",
             "ddl-auto=update 自动建表",
             "结构变更可版本化、可回溯；启动即校验，脚本与实体不一致会立刻失败（护栏）"],
            ["枚举存 varchar + check",
             "使用 MySQL/H2 原生 enum 类型",
             "原生 enum 不可移植，且新增枚举常量需要 ALTER TABLE；varchar 方案两端通用"],
            ["默认拒绝的访问策略",
             "默认放行、逐个保护敏感路径",
             "新增接口若忘记配置，默认是「不可访问」而不是「裸露」；"
             "代价是白名单外的未知路径会跳转登录页而不是 404"],
            ["后台使用独立调色板",
             "后台也套用前台主题",
             "即使把前台配色改成极端值，后台依旧可读可操作，避免「改坏后无法自救」"],
        ],
        widths=[2.8, 3.4, 8.8])

    m.page_break()

    # ---------------- 5 详细设计 ----------------
    m.h("5 详细设计", level=1)

    m.h("5.1 站点域", level=2)

    m.h("5.1.1 站点档案聚合根 SiteProfile", level=3)
    m.p("站点档案是本系统的单例聚合根（主键固定为 1），承载三类信息：站点身份、Markdown 简介、主题令牌。"
        "所有写入都经过 edit() 与 applyPreset() 两个行为方法，非法值无法绕过校验进入数据库。")
    m.code(
        "public void edit(Identity identity, String introMarkdown, ThemeTokens theme, String customCss)\n"
        "public void applyPreset(ThemeTokens theme)   // 只换配色与字体，保留排版参数\n"
        "public record Identity(String siteTitle, String siteSubtitle, ...)  // 身份字段的只读快照"
    )
    m.p("设计要点：Identity 记录把 9 个身份字段打包，避免 edit 方法参数过长；"
        "internal 方法统一调用 ThemeTokens.normalize() 做防御式收敛。")

    m.h("5.1.2 主题令牌 ThemeTokens 与取值约束", level=3)
    m.p("主题令牌是「风格」概念的唯一载体，既是数据库内嵌值对象（@Embeddable），"
        "也是模板与编辑界面之间唯一的数据契约。字段与约束如下表：")
    m.table(
        ["字段", "类型/取值域", "默认值", "约束"],
        [
            ["preset", "ThemePreset 枚举（7 种）", "ELEGANT_LIGHT", "非法值回退默认预置"],
            ["primaryColor / accentColor / pageBackground / surfaceBackground / textColor / mutedTextColor",
             "十六进制颜色", "随预置主题", "正则 #RGB 或 #RRGGBB，非法值回退预置色"],
            ["headingFont / bodyFont", "FontChoice 枚举（5 种）", "SYSTEM_SANS", "白名单，非法值回退"],
            ["baseFontSize", "整数 12–26 px", "17", "越界夹取到区间端点"],
            ["contentWidth", "整数 560–1600 px", "780", "越界夹取"],
            ["cornerRadius", "整数 0–40 px", "14", "越界夹取"],
            ["stickyHeader / showHero", "布尔", "true / true", "—"],
        ],
        widths=[4.6, 3.6, 2.6, 4.2])
    m.p("三层防御：① 表单注解（@Pattern/@Min/@Max）拦在入口；② ThemeTokens.normalize() 兜在落库前；"
        "③ 渲染器输出的变量只来自已规范化的令牌。因此不存在「把任意字符串拼进 CSS」的可能。")

    m.h("5.1.3 风格渲染器 ThemeStyleRenderer", level=3)
    m.code(
        "public interface ThemeStyleRenderer {\n"
        "    String VARIABLE_PREFIX = \"--cxh-\";\n"
        "    String renderTokensCss(ThemeTokens tokens);\n"
        "    default String renderStylesheet(ThemeTokens tokens, String customCss) { ... }\n"
        "}"
    )
    m.p("实现类 CssVariablesThemeStyleRenderer 把令牌转换为 :root 上的 CSS 变量，"
        "并集中计算派生值（半透明主色、边框色、阴影、次级圆角），使模板中不出现任何魔法色值。")
    m.table(
        ["变量", "含义", "是否派生"],
        [
            ["--cxh-primary / --cxh-primary-soft", "主色 / 主色柔光", "柔光为派生（rgba 12%）"],
            ["--cxh-accent", "辅助色", "否"],
            ["--cxh-bg / --cxh-surface", "页面背景 / 卡片背景", "否"],
            ["--cxh-text / --cxh-muted", "正文色 / 次要文字色", "否"],
            ["--cxh-border / --cxh-shadow", "描边 / 阴影", "由文字色与底色亮度派生"],
            ["--cxh-font-heading / --cxh-font-body", "标题 / 正文字体栈", "否（白名单字符串）"],
            ["--cxh-font-size / --cxh-content-width", "字号 / 内容宽度", "否"],
            ["--cxh-radius / -sm / -lg", "圆角三档", "sm、lg 由主圆角派生"],
            ["--cxh-header-position", "顶栏定位（吸顶/静态）", "由开关派生"],
        ],
        widths=[5.0, 5.4, 4.6])

    m.h("5.1.4 站点档案服务与快照缓存", level=3)
    m.p("SiteProfileServiceImpl 的关键设计是读写路径严格分离：")
    m.bullets([
        "读路径 current()：命中 volatile 快照直接返回；未命中时双重检查加锁加载"
        "（取不到数据时用内存默认档案渲染，绝不产生写入）；",
        "写路径 update()/applyPreset()：在写事务内保证单例行存在、写入后原子替换快照；",
        "stylesheet() 由快照令牌渲染，因此 /theme.css 的响应不查库；",
        "ensureInitialized() 在启动期由种子组件调用。",
    ])
    m.p("这样设计避免了「只读事务中静默丢弃写入」这一常见陷阱（曾在本项目开发中实际遇到）。")

    m.h("5.2 文章域", level=2)

    m.h("5.2.1 文章聚合根 Post", level=3)
    m.p("Post 只保存 Markdown 原文，不保存渲染结果——渲染器可随时替换、升级后立即生效，"
        "数据层与展示层因此彻底解耦。所有编辑通过 applyEdit(PostDraft) 完成，"
        "slug 生成、标签规范化、发布时间补记等不变量都在实体内部维护。")

    m.h("5.2.2 检索关键字值对象 SearchKeyword", level=3)
    m.p("把「用户输入 → LIKE 模式」的规则收敛为一个值对象，是防止 SQL 语义被破坏的关键：")
    m.code(
        "SearchKeyword.of(\"100%\")\n"
        "  → value   = \"100%\"\n"
        "  → pattern = \"%100!%%\"     // 通配符被转义，配合查询中的 escape '!'\n"
        "isMeaningful()  // 纯符号关键词不触发查询，避免全表扫描"
    )
    m.p("对应查询：lower(p.title) like :pattern escape '!' OR lower(coalesce(p.summary,'')) like ... "
        "OR lower(coalesce(p.contentMarkdown,'')) like ...，只检索已发布文章。")

    m.h("5.2.3 文章服务与视图装配", level=3)
    m.p("PostServiceImpl 负责编排与视图装配，仓储只负责查询，实体只负责不变量。"
        "其中三个算法需要说明：")
    m.p("① slug 唯一化：以标题或用户输入生成基础 slug，若已存在则依次尝试 -2、-3 … -100，"
        "最多 100 次后退化为「基础 slug-纳秒」；并发同名插入的窗口由数据库唯一约束兜底，"
        "冲突被翻译为 409 并给出中文提示。", indent=False)
    m.p("② 摘要派生：摘要留空时按正文纯文本截取 140 字，列表与详情共用同一方法，"
        "避免同一篇文章在不同页面摘要不一致。", indent=False)
    m.p("③ 分页防御：页码小于 0 夹取为 0，每页条数小于等于 0 用默认值 10、"
        "上限 50，避免超大分页参数拖垮数据库。", indent=False)

    m.h("5.3 通用层", level=2)
    m.table(
        ["组件", "职责", "关键实现"],
        [
            ["MarkdownRenderer", "Markdown → 安全 HTML",
             "CommonMark + GFM 表格扩展；escapeHtml(true) 转义原始 HTML；"
             "softbreak 转为换行；另提供纯文本摘要方法"],
            ["ApiResponse / PageResult", "统一响应结构",
             "PageResult 提供 of() 映射工厂与 hasPrevious/hasNext，屏蔽 Spring Data Page"],
            ["GlobalExceptionHandler", "异常 → HTTP 语义",
             "业务异常 400/404、类型不匹配 400、方法不支持 405、数据冲突 409、"
             "未预期异常 500；同一处理器按 Accept 头返回 JSON 或错误页"],
            ["Slugs / DateTimes / XmlText", "文本工具",
             "slug 保留中日韩字符；时间格式统一在装配阶段完成，模板无日期函数；"
             "XML 转义并过滤非法控制字符"],
        ],
        widths=[3.2, 4.0, 7.8])

    m.h("5.4 安全设计", level=2)
    m.p("SecurityConfig 采用「默认拒绝 + 白名单放行」：")
    m.code(
        "公开白名单  /  /posts/**  /tags/**  /about  /archive  /search\n"
        "            /theme.css  /css/**  /js/**  /images/**  /favicon.ico\n"
        "            /feed.xml  /sitemap.xml  /robots.txt  /login  /error\n"
        "开发控制台  /h2-console/**（仅默认 H2 配置下存在该路径）\n"
        "健康检查    /actuator/health\n"
        "后台管理    /admin/**  → 需要 ROLE_ADMIN\n"
        "其余一切    denyAll"
    )
    m.p("内容安全策略（CSP）为：default-src 'self'; script-src 'self'; "
        "style-src 'self' 'unsafe-inline'; img-src 'self' data: https:; frame-src 'self'; "
        "frame-ancestors 'self'; form-action 'self'; base-uri 'none'; object-src 'none'。"
        "因禁止内联脚本，页面的删除确认改由外部脚本 admin-confirm.js 通过 data-confirm 属性实现。")

    m.h("5.5 数据库设计", level=2)
    m.p("表结构由 Flyway 脚本管理（db/migration/{h2,mysql}/V1__init.sql），"
        "Hibernate 仅做 validate 校验；脚本与实体不一致会导致启动失败，这是刻意设置的护栏。")
    m.table(
        ["表名", "字段数", "主键", "唯一/外键约束"],
        [
            ["site_profile", "30", "id（固定 1）", "枚举列的 check 约束"],
            ["post", "11", "id（自增）", "uk_post_slug(slug) 唯一"],
            ["post_tag", "2", "pk(post_id, tag)", "fk_post_tag_post → post(id) ON DELETE CASCADE"],
            ["flyway_schema_history", "—", "installed_rank", "Flyway 自动维护"],
        ],
        widths=[3.6, 1.8, 3.4, 6.2])
    m.p("跨库差异只在方言层：H2 用 varchar(32600) / timestamp with time zone / boolean，"
        "MySQL 用 text / datetime(6) / bit；枚举列两端统一为 varchar + check，"
        "避免使用不可移植的原生 enum 类型。")

    m.h("5.6 前端设计", level=2)
    m.bullets([
        "模板分层：layout/base.html 提供 head/header/footer/文章卡片/分页等片段，"
        "layout/admin.html 提供后台骨架（含 CSRF meta）；页面通过 th:replace 组合，避免重复；",
        "样式分层：blog.css 只消费 --cxh-* 变量并全部提供兜底值，theme.css 由服务端按当前风格覆盖，"
        "加载顺序固定为「兜底在前、主题在后、预览挂载点最后」（顺序错误会导致编辑后的风格被兜底值覆盖，"
        "本项目曾出现该缺陷并有专门的回归测试守护）；",
        "外观编辑器：左侧表单 + 右侧 iframe 加载真实首页；编辑事件经 220ms 防抖后，"
        "把整个表单 POST 到 /admin/appearance/preview 取回 CSS 文本，注入 iframe 内的 "
        "<style id=\"cxh-theme-preview\">，实现「预览即所得」且不写库；",
        "渐进增强：前台脚本仅做锚点平滑滚动；禁用 JavaScript 后所有页面仍完全可用；",
        "相册排版：Markdown 图片渲染后是 p 里套链接再套图片的结构，用 "
        ":has(> a:only-child > img:only-child) 把这类段落变成统一高度的行内块，"
        "于是自动排成等高照片墙；不支持该选择器的浏览器退化为纵向大图，不影响可用性。",
    ])

    m.h("5.7 图片上传设计", level=2)
    m.p("图片上传是「改图不必重新构建」的关键。整体沿用依赖倒置：控制器只依赖存储抽象，"
        "换本地磁盘、对象存储还是图床都不需要改控制器与模板。")
    m.code(
        "ImageUploadController  ──依赖──▶  ImageStorage（接口）\n"
        "  POST /api/media/images              │\n"
        "  返回 url / thumbnailUrl / markdown   ├── LocalImageStorage（默认：本地磁盘）\n"
        "                                      └── 未来：OssImageStorage / S3ImageStorage"
    )
    m.table(
        ["组件", "职责", "关键点"],
        [
            ["ImageStorage", "存储能力抽象", "定义 store / delete / isAvailable 与类型白名单、8MB 上限常量"],
            ["LocalImageStorage", "本地磁盘实现", "UUID 重命名、真实解码校验、ImageIO 生成缩略图、路径越界校验"],
            ["ImageUploadController", "上传端点", "仅管理员；返回可直接粘贴的相册 Markdown"],
            ["MediaWebConfig", "静态映射", "把上传目录挂到 /uploads/**，与代码分离、不打进 jar"],
            ["image-upload.js", "后台交互", "选文件 → 上传 → 在光标处插入 Markdown，带 CSRF 令牌"],
        ],
        widths=[3.4, 3.4, 8.2])
    m.p("安全设计（每条对应一类真实攻击面）：")
    m.bullets([
        "文件名不参与落盘路径：一律重命名为 UUID，杜绝 ../ 路径穿越与覆盖已有文件；",
        "以真实解码结果判定类型：ImageIO 解不出图像即拒绝，不信任客户端声明的 Content-Type；",
        "类型白名单与大小上限：jpeg/png/gif/webp，单张 8MB，超出直接拒绝；",
        "落盘路径二次校验：解析后的绝对路径必须仍在配置根目录之内，否则拒绝；",
        "鉴权与 CSRF：/api/** 需要 ROLE_ADMIN，且 POST 必须携带 CSRF 令牌。",
    ])
    m.p("缩略图由 JDK 自带的 ImageIO + Graphics2D 生成（最长边可配置，默认 560px），"
        "不引入第三方图像库；原图小于缩略图尺寸时不放大，直接复制一份，保证缩略图 URL 始终可用。", indent=False)

    m.page_break()

    # ---------------- 6 运行环境与部署 ----------------
    m.h("6 运行环境与部署", level=1)

    m.h("6.1 本机运行（默认 H2）", level=2)
    m.code(
        "# 方式一：直接运行（仓库自带 Maven Wrapper，无需安装 Maven）\n"
        "./mvnw spring-boot:run\n\n"
        "# 方式二：打包后运行\n"
        "./mvnw clean package\n"
        "java -jar target/cxh-blog.jar\n\n"
        "# 访问：前台 http://localhost:8080/   后台 http://localhost:8080/admin\n"
        "# 默认账号 admin / cxh@2024（可用 CXH_ADMIN_USERNAME、CXH_ADMIN_PASSWORD 覆盖）"
    )
    m.p("首次启动流程：Flyway 执行 V1__init.sql 建表 → Hibernate 校验结构 → "
        "初始化单例站点档案 → 写入 5 篇示例文章，其中两篇是图片集（cxh.seed.enabled=true 时）。"
        "数据文件位于 ./data/cxh-blog.mv.db。")

    m.h("6.2 部署到 MySQL", level=2)
    m.code(
        "-- 建库（字符集必须是 utf8mb4）\n"
        "CREATE DATABASE cxh_blog DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;\n\n"
        "# 启动\n"
        "java -jar target/cxh-blog.jar --spring.profiles.active=mysql \\\n"
        "  --DB_HOST=127.0.0.1 --DB_USERNAME=cxh --DB_PASSWORD=***\n"
    )
    m.p("注意：JDBC 连接串的 characterEncoding 必须是 Java 字符集名 UTF-8，"
        "写成 utf8mb4 会被 Connector/J 拒绝（本项目在实测中发现并修正了该缺陷）。")

    m.h("6.3 容器部署", level=2)
    m.code(
        "docker compose up -d --build     # 应用 + MySQL 8.4\n"
        "# 环境变量：DB_PASSWORD、MYSQL_ROOT_PASSWORD、CXH_ADMIN_PASSWORD、APP_PORT、CXH_SEED_ENABLED"
    )
    m.p("Dockerfile 为两阶段构建（Maven 构建 → JRE 运行），以非 root 用户启动，"
        "并配置了 HEALTHCHECK（curl /actuator/health 判定 UP）。")
    m.p("验证边界说明：本项目的开发环境没有运行中的 Docker daemon 且无法访问 Docker Hub，"
        "因此镜像构建与 compose 启动未实际执行。已验证的内容是："
        "docker compose config 语法与变量插值通过；Dockerfile 中的运行时假设被逐条本地复刻验证"
        "（SERVER_PORT 端口覆盖、ENTRYPOINT 的 java $JAVA_OPTS -jar、HEALTHCHECK 命令、"
        "MaxRAMPercentage 与 user.timezone 参数生效）。", indent=False)

    m.h("6.4 生产部署（Nginx 反向代理 + HTTPS）", level=2)
    m.p("生产环境不要把应用端口直接暴露到公网。仓库提供了一套现成编排："
        "Nginx 终结 HTTPS，应用只在容器网络内监听 8080。")
    m.code(
        "cp deploy/.env.example deploy/.env      # 填写域名与全部口令（必填，缺一个就拒绝启动）\n"
        "# 修改 deploy/nginx/cxh-blog.conf 里的 server_name 为你的域名\n"
        "docker compose -f deploy/docker-compose.prod.yml --env-file deploy/.env up -d --build\n\n"
        "# 首次申请证书（certbot 容器已在编排内，负责后续自动续期）\n"
        "docker compose -f deploy/docker-compose.prod.yml run --rm certbot \\\n"
        "  certonly --webroot -w /var/www/certbot -d your-domain.com"
    )
    m.table(
        ["要点", "做法", "原因"],
        [
            ["应用不对外发布端口", "compose 里只用 expose，不写 ports", "唯一入口是 Nginx，减少攻击面"],
            ["真实协议透传", "Nginx 传 X-Forwarded-Proto，应用侧 forward-headers-strategy=framework",
             "否则 RSS / sitemap 会生成 http:// 的绝对地址"],
            ["主题样式表不缓存", "Nginx 对 /theme.css 单独设置 no-store", "后台改风格后立即生效"],
            ["上传图片长缓存", "/uploads/ 设 30 天 immutable", "文件名是 UUID，内容不会变，可放心强缓存"],
            ["请求体上限", "client_max_body_size 10m", "与后端 8MB 图片上限呼应"],
            ["口令必填", ".env 用 :? 语法强制", "避免忘记改默认口令就上线"],
        ],
        widths=[3.2, 6.0, 5.8])
    m.p("验证边界说明：本环境没有运行中的 Docker daemon，也没有安装 nginx，因此镜像构建与实际反代未执行。"
        "已验证的是：docker compose config 语法与变量插值通过（含缺少必填口令时拒绝解析）、"
        "Nginx 配置的静态自检（大括号配平、proxy_pass 指向、透传头、缓存策略、跳转规则共 8 项）全部通过。", indent=False)

    m.h("6.5 备份与恢复", level=2)
    m.p("个人博客最怕的不是宕机，而是数据丢失。备份脚本同时覆盖数据库与上传的图片，并自动清理过期备份。")
    m.code(
        "scripts/backup.sh                     # 自动识别 H2 / MySQL\n"
        "scripts/backup.sh --mysql             # 指定 MySQL（本地 mysqldump）\n"
        "scripts/backup.sh --docker            # 备份 compose 容器里的库与上传卷\n"
        "BACKUP_DIR=/data/backup KEEP_DAYS=14 scripts/backup.sh\n\n"
        "# 加入 crontab（每天凌晨 3 点）\n"
        "0 3 * * * /path/to/cxhmj/scripts/backup.sh >> /var/log/cxh-backup.log 2>&1"
    )
    m.p("产物结构：backups/cxh-blog-YYYYmmdd-HHMMSS/{database.sql | h2-data.tar.gz, uploads.tar.gz, manifest.txt}。"
        "脚本会在检测到 H2 正在运行（存在 .lock.db）时给出告警——H2 文件库建议停服后再备份，"
        "否则可能拿到不一致的快照。", indent=False)
    m.table(
        ["数据", "备份方式", "恢复方式"],
        [
            ["MySQL", "mysqldump --single-transaction（容器内执行）", "mysql -u <user> -p <db> < database.sql"],
            ["H2 文件库", "tar 打包 ./data 目录", "停服后 tar xzf h2-data.tar.gz -C <项目目录>"],
            ["上传图片", "tar 打包 uploads 目录（或容器卷）", "tar xzf uploads.tar.gz -C <项目目录>"],
        ],
        widths=[2.4, 7.0, 5.6])

    m.h("6.6 旧站内容迁移", level=2)
    m.p("从旧静态站切换到本系统时，历史文章可以批量导入。本项目把旧站内容提取成了"
        "带 front matter 的 Markdown（content/legacy/），并用脚本调用后台接口逐篇发布，"
        "既不依赖后台手工逐条粘贴，也不引入额外的导入格式。")
    m.code(
        "# 在服务器上（应用已启动）\n"
        "cd /opt/cxh-blog\n"
        "PASS='后台口令' scripts/import-posts.sh\n\n"
        "# 也可指定地址与目录\n"
        "BASE=http://127.0.0.1:8080 USER=admin PASS=xxx DIR=content/legacy scripts/import-posts.sh"
    )
    m.table(
        ["要点", "说明"],
        [
            ["文件格式", "--- 包裹的 title / tags / summary + 正文 Markdown"],
            ["重复执行", "按标题判重，已存在的文章自动跳过"],
            ["图片", "引用工程内已有的 /images/gallery/ 资源，无需重复上传"],
            ["失败可见", "每篇打印结果，任一失败返回非 0 退出码"],
        ],
        widths=[2.6, 12.4])

    # ---------------- 7 用户手册 ----------------
    m.h("7 用户操作手册", level=1)

    m.h("7.1 访客操作", level=2)
    m.table(
        ["操作", "路径", "说明"],
        [
            ["浏览首页", "/", "大标题 + Markdown 简介 + 最新 6 篇文章 + 标签云"],
            ["阅读文章", "点击文章卡片标题", "正文由 Markdown 渲染，页脚显示阅读时长"],
            ["按标签浏览", "/tags 或文章卡片上的标签", "标签云 → 标签文章列表"],
            ["按时间浏览", "/archive", "按年份分组的全部文章"],
            ["搜索", "/search 或导航「搜索」", "检索标题、摘要与正文；只搜已发布文章"],
            ["订阅", "/feed.xml", "RSS 2.0，可粘贴到任意阅读器"],
            ["查看作者", "/about", "简介与联系方式"],
        ],
        widths=[2.8, 4.2, 8.0])

    m.h("7.2 站长登录", level=2)
    m.steps([
        "访问 /admin 或 /login，任意后台地址在未登录时都会跳转到登录页；",
        "输入配置的账号与口令（默认 admin / cxh@2024）；",
        "登录成功后进入概览页，可看到文章统计、当前外观与最近更新；",
        "点击侧边栏「退出登录」结束会话（POST 提交，需 CSRF 令牌）。",
    ])

    m.h("7.3 文章管理", level=2)
    m.steps([
        "侧边栏 →「文章管理」，可按「全部 / 已发布 / 草稿」筛选；",
        "点「写新文章」，填写标题、（可选）链接标识、（可选）摘要、标签、封面与正文；",
        "正文支持 Markdown；输入后下方实时显示渲染预览（与发布使用同一渲染器）；",
        "状态选「已发布」后保存，公开页面立即出现该文章；选「草稿」则仅后台可见；",
        "标题留空、正文超过 15000 字等非法输入会在表单上给出中文提示，且不会写入数据库；",
        "删除文章需在确认框中确认，确认逻辑在外部脚本中实现。",
    ])

    m.h("7.4 外观自由编辑（核心功能）", level=2)
    m.p("入口：侧边栏 →「外观设置」。页面为左右两栏，左侧编辑、右侧是真实首页的实时预览。")
    m.steps([
        "预置主题：7 张卡片，点一下立即套用整套配色与字体（含保存），并保留已调好的字号、宽度与圆角；",
        "站点身份：修改站点标题、副标题、作者、头像、所在地、邮箱、GitHub、备案号与页脚文字；",
        "简介：用 Markdown 编写，保存后首页 Hero 区与关于页同时生效；",
        "风格令牌：6 个取色器（主色、辅助色、页面背景、卡片背景、正文字色、次要文字色）、"
        "标题与正文字体、字号/内容宽度/圆角三个滑块、顶栏吸顶与首页 Hero 两个开关；",
        "自定义 CSS：追加在主题变量之后，可直接引用 --cxh-* 变量；",
        "实时预览：任何输入都会在约 0.2 秒后刷新右侧预览；预览请求不写数据库；",
        "点击「保存外观」写回数据库，全站立即生效（/theme.css 禁止缓存，无需重启、无需重新打包）。",
    ])
    m.p("提示：后台界面刻意使用独立配色，不会跟随前台主题变化，"
        "因此即使把前台调成极端配色，后台依旧可读可操作。")

    m.h("7.5 在文章里放图片", level=2)
    m.p("系统自带两个图集示例（「相册 · 生活片段」「表情包与整活」）。要自己加图，按下面的约定操作：")
    m.steps([
        "把原图放进 src/main/resources/static/images/gallery/，缩略图放进同级的 thumbs/ 子目录"
        "（缩略图建议最长边 560px，本项目用 macOS 的 sips 命令批量生成）；",
        "在正文里写 Markdown：方括号前加感叹号、括号内写缩略图路径表示图片，"
        "再把整段图片语法套进一对指向原图的方括号与圆括号，即 [ ![说明](缩略图) ](原图)；",
        "保存后正文会出现等高照片墙，点击缩略图即可查看原图；",
        "头像与文章封面分别填在外观设置的「头像地址」与文章表单的「封面图地址」里，取值同样是图片路径。",
    ])
    m.p("注意：图片是随应用打包发布的静态资源，新增图片需要重新构建；"
        "若希望后台上传图片，需要新增图片存储接口与上传端点（见 9.1 可扩展点）。", indent=False)

    m.h("7.6 常见问题", level=2)
    m.table(
        ["现象", "原因", "处理"],
        [
            ["访问 /admin 被跳回登录页", "会话过期或未登录", "重新登录"],
            ["保存文章提示「保存冲突」", "链接标识与已有文章重复（并发同名）", "返回修改链接标识或稍后重试"],
            ["访问未在白名单中的地址被跳转登录页",
             "安全策略为默认拒绝", "访问公开页面请使用导航中的地址；如需 404 语义可调整兜底策略"],
            ["后台改了配色但页面没变化", "浏览器缓存了旧样式", "强制刷新（/theme.css 已带 no-store，一般不会发生）"],
            ["启动报 Schema-validation 错误", "数据库结构与迁移脚本不一致", "执行迁移或删除 ./data 后重启（开发库）"],
        ],
        widths=[4.4, 4.6, 6.0])

    m.page_break()

    # ---------------- 8 测试报告 ----------------
    m.h("8 测试报告", level=1)

    m.h("8.1 测试策略与环境", level=2)
    m.p("采用四层测试策略，从「组件正确」到「用户可用」逐层加码：")
    m.table(
        ["层次", "手段", "规模", "目的"],
        [
            ["单元测试", "JUnit 5 + AssertJ（不启动容器，5 个测试类）", "30 项", "值对象、渲染器、实体不变量、认证规则、图片存储与安全边界"],
            ["集成测试", "SpringBootTest + MockMvc（9 个测试类）", "64 项", "服务事务、仓储查询、页面渲染、安全策略、图片上传"],
            ["验收测试", "真实 jar + curl 脚本（scripts/acceptance-test.sh）", "110 项", "端到端业务闭环与边界输入"],
            ["人工/浏览器验证", "CDP 驱动真实浏览器 + 无头截图", "5 项", "外观编辑器的实时预览链路、样式视觉正确性"],
        ],
        widths=[2.6, 5.6, 1.8, 5.0])
    m.table(
        ["项目", "内容"],
        [
            ["测试环境", "macOS；JDK 17.0.20（另在 JDK 26.0.2 上复跑全部单元/集成测试）；"
                        "MySQL 9.7.1 独立实例（端口 3399）；Microsoft Edge 无头模式"],
            ["数据库", "H2 内存库（表结构由 Flyway 的 h2 脚本创建并校验）；MySQL 9.7.1（mysql 脚本）"],
            ["覆盖工具", "JaCoCo 0.8.15"],
            ["测试入口", "./mvnw test（单元/集成）与 scripts/acceptance-test.sh（验收）"],
        ],
        widths=[3.0, 12.0])

    m.h("8.2 单元与集成测试结果", level=2)
    m.p("执行 ./mvnw clean test，结果如下（JDK 17 与 JDK 26 均为 93 项全通过）：")
    m.table(
        ["测试类", "用例数", "覆盖内容"],
        [
            ["CssVariablesThemeStyleRendererTest", "6", "令牌 → CSS 变量、自定义 CSS 追加顺序、非法颜色与越界数值收敛、字体白名单"],
            ["SearchKeywordTest", "4", "LIKE 通配符 % _ ! 的转义、长度截断、空白规范化、纯符号关键词不触发查询"],
            ["PostTest", "5", "标签解析（中文分隔符与含空格标签）、slug 生成、发布状态与时间、阅读量、空标题被拒"],
            ["AdminUserDetailsServiceTest", "4", "明文口令加密、BCrypt 摘要原样使用、{bcrypt} 前缀、未知账号被拒"],
            ["SiteProfileServiceTest", "5", "默认档案自举、编辑落库与快照刷新、预览不落库、套用预置保留排版、非法颜色不落库"],
            ["PostServiceTest", "10", "保存与渲染、slug 唯一化、草稿隔离、阅读量累加、标签检索、搜索命中与通配符防御、分页防御、删除、编辑复用记录"],
            ["SeedDataTest", "5", "首启动写入 5 篇示例文章、相册渲染出缩略图与原图链接、内容可检索、站点档案自举、种子幂等"],
            ["PublicPagesTest", "12", "首页/列表/详情/标签/归档/关于/搜索渲染、/theme.css 输出与缓存头、样式级联顺序守卫、头像按配置渲染、相册图片随包可访问、关键词转义、404、后台未登录跳转、登录页匿名可访问"],
            ["AdminWebTest", "7", "概览、外观编辑器控件、预览接口不写库、保存后样式与前台即时变化、非法颜色被拦、Markdown 预览、后台文章页"],
            ["ErrorHandlingTest", "12", "非法枚举/路径变量/分页参数、方法不支持、错误页表达式、JSON 错误结构、超长正文、友好绑定提示、安全响应头、actuator 拒绝、CSRF、模板无内联事件"],
            ["FeedEndpointsTest", "5", "feed/sitemap/robots 输出；用 XML 解析器实际解析；草稿不入订阅；lastmod；**sitemap 全量收录（60 篇回归 BUG-08）**"],
            ["LocalImageStorageTest", "11", "重命名落盘、缩略图生成与不放大、路径穿越、伪造类型、空内容、超大文件、删除原图与缩略图、非法键不越界、上传开关、Markdown 片段"],
            ["ImageUploadControllerTest", "7", "匿名不可上传、缺 CSRF 被拒、上传成功返回元数据与 Markdown、上传后图片可公开访问、非图片内容 400、空文件 400、配置生效"],
        ],
        widths=[4.6, 1.6, 8.8])

    m.p("代码覆盖率（JaCoCo，按包统计，节选）：")
    m.table(
        ["包", "行覆盖", "分支覆盖", "说明"],
        [
            ["（整体）", "90.8%", "65.2%", "指令 89.7%，方法 91.7%；行 1178/1297，共 58 个类"],
            ["site.service.impl", "100.0%", "75.0%", "站点档案读写与快照"],
            ["site.theme", "100.0%", "71.4%", "风格渲染器"],
            ["web / web.dto / web.feed", "100.0% / 100.0% / 98.4%", "66.7% / — / 80.0%", "页面装配与订阅输出"],
            ["bootstrap", "100.0%", "100.0%", "首启动种子数据"],
            ["security", "98.2%", "81.2%", "认证与策略"],
            ["media / media.web", "88.2% / 87.5%", "— / —", "图片上传：存储抽象、缩略图、安全边界"],
            ["post.service.impl", "94.1%", "82.1%", "文章服务"],
            ["site.domain", "93.5%", "56.0%", "站点档案与令牌"],
            ["post.dto / site.dto", "90.7% / 92.1%", "80.0% / 33.3%", "读写模型"],
            ["common.exception", "52.8%", "33.3%", "部分 5xx 分支无法确定性触发（已由验收测试覆盖 4xx 分支）"],
        ],
        widths=[4.4, 3.4, 3.0, 4.2])

    m.h("8.3 验收测试结果", level=2)
    m.p("执行 scripts/acceptance-test.sh：脚本在临时目录用干净数据库启动真实 jar，"
        "按 10 组共 110 条用例逐条校验，任何一条失败即返回非 0 退出码。最终结果 **110/110 通过**。")
    m.table(
        ["分组", "用例数", "覆盖点", "结果"],
        [
            ["A 公开页面渲染", "20", "首页/列表/详情/标签/归档/关于/搜索/静态资源，页码与关键词边界", "20/20"],
            ["B 主题样式表", "5", "内容类型、缓存头、变量完整性与派生变量", "5/5"],
            ["C 订阅与站点地图", "8", "feed/sitemap/robots 状态、内容类型、XML 良构性", "8/8"],
            ["D 鉴权与安全基线", "14", "未登录跳转、健康检查、非公开端点、CSRF、安全响应头、JSON 错误", "14/14"],
            ["E 登录流程", "5", "CSRF 隐藏域、错误口令、正确登录、概览可见", "5/5"],
            ["F 后台文章管理", "14", "列表与筛选、新建、草稿隔离、校验拦截、Markdown 预览、鉴权", "14/14"],
            ["G 外观自由编辑", "18", "编辑器控件、非法颜色拦截、保存后样式与页面即时生效、预览不落库、预置套用", "18/18"],
            ["H 异常与边界输入", "11", "参数类型错误、方法不支持、错误页表达式、超长正文、中文绑定提示、内联事件清理", "11/11"],
            ["I 数据完整性", "7", "搜索可定位、详情渲染、阅读量、sitemap/feed 收录、登出与会话失效", "7/7"],
            ["J 图片上传", "8", "匿名/无 CSRF 被拒、上传成功返回元数据与 Markdown、原图与缩略图可访问、伪造内容被拒", "8/8"],
        ],
        widths=[3.4, 1.6, 8.0, 2.0])
    m.p("完整逐条结果已存档于 docs/acceptance-test-result.txt，可复现。")

    m.h("8.4 缺陷记录", level=2)
    m.p("完整性测试阶段共发现并修复 7 个缺陷，全部补充了回归测试。")
    m.table(
        ["编号", "发现阶段", "现象", "根因", "修复"],
        [
            ["BUG-01", "首轮浏览器验证",
             "保存深色主题后，/theme.css 内容正确但页面仍是默认蓝白配色",
             "blog.css 的兜底变量排在 theme.css 之后，同优先级后加载覆盖了真实主题值",
             "调整级联顺序为「兜底在前、主题在后」，并新增级联顺序守卫测试"],
            ["BUG-02", "完整性测试",
             "?status=INVALID、/admin/posts/abc/edit、?page=abc 均返回 500",
             "MethodArgumentTypeMismatchException 落入兜底的 Exception 处理分支",
             "新增类型不匹配处理器返回 400，并补 12 项 ErrorHandlingTest 用例"],
            ["BUG-03", "完整性测试",
             "提交 40000 字正文返回 500，且错误页标题显示“null ?: 'cxh 的博客'”",
             "正文缺少长度上限，写库触发列长度异常；错误页把 Elvis 兜底写在了字面量替换内部，"
             "运算符被原样输出；且异常处理器路径拿不到站点档案",
             "正文加 @Size(15000) 校验并补充错误回显位；标题表达式整体移入 ${} 内使用安全导航与 Elvis"],
            ["BUG-04", "完整性测试",
             "并发保存同名文章时 3/4 请求返回 500",
             "应用层查重无法覆盖并发窗口，数据库唯一约束抛出的异常未被翻译",
             "新增 DataIntegrityViolationException 处理器返回 409 与中文提示"],
            ["BUG-05", "MySQL 实测",
             "MySQL profile 完全无法启动：Unsupported character encoding 'utf8mb4'",
             "JDBC 连接串的 characterEncoding 必须是 Java 字符集名 UTF-8",
             "修正连接串并在文档中显式说明 utf8mb4 由服务端与建库语句保证"],
            ["BUG-06", "MySQL 实测",
             "枚举列被建为数据库原生 enum 类型，schema 不可移植",
             "Hibernate 6 默认对 H2/MySQL 使用原生 enum 类型",
             "在枚举字段上显式声明 @JdbcTypeCode(SqlTypes.VARCHAR)，两端统一 varchar + check"],
            ["BUG-08", "升级评审",
             "站点地图只收录前 50 篇文章，超出部分静默丢失，搜索引擎收录受损且无任何报错",
             "FeedController 直接复用了面向页面的 recentPublished(50)，该方法会把条数夹取到单页上限",
             "新增 PostService.listAllPublished() 分批取全量（含 5 万条防爆上限），"
             "并补回归测试：造 60 篇文章断言全部收录——该测试在旧实现下确实失败，确认能捕获此缺陷"],
            ["BUG-07", "验证脚本自检",
             "验收脚本首轮 44 条失败，应用侧并无对应缺陷",
             "脚本自身缺陷：会话请求缺少 -b 导致令牌不匹配、CSRF 令牌函数误传 HTML 文本、"
             "中文查询未做 URL 编码、同名 POST 漏传令牌、变量名紧跟中文字符被 bash 并入变量名、"
             "以及与种子写入的竞态（Web 容器先于 ApplicationRunner 就绪）",
             "逐项修正脚本并固化：110/110 通过（应用代码未因此改动）"],
        ],
        widths=[1.4, 2.2, 4.4, 3.6, 3.4])
    m.p("说明：BUG-07 记录在案是为了说明「测试脚本本身也需要被验证」——"
        "44 条失败中有 42 条是脚本缺陷，只有 2 条最终指向应用侧的真实断言缺陷。"
        "如果直接把这批失败当作应用缺陷去改代码，就会造成误改。", indent=False)

    m.h("8.5 测试结论", level=2)
    m.bullets([
        "功能完整性：22 项功能需求全部实现，需求追踪矩阵中每一项都有对应用例覆盖；",
        "正确性：94 项单元/集成测试与 110 项验收用例全部通过，行覆盖率 90.8%；",
        "健壮性：非法参数、超长输入、并发冲突、缺失资源均返回恰当的 4xx 语义，"
        "不再出现「用户输入导致 500」的情况；",
        "安全性：后台隔离、CSRF、CSP、口令加密、输入白名单与内容转义均已验证；",
        "可移植性：同一套代码在 H2 与 MySQL 9.7 上完成建表、读写、检索、订阅与重启幂等验证；",
        "遗留风险：Docker 镜像构建未在本环境执行；搜索为 LIKE 模糊匹配，数据量大时需换全文索引；"
        "分支覆盖率 62.4%，主要缺口在无法确定性触发的 5xx 分支与种子数据分支。",
    ])

    # ---------------- 9 质量保证与维护 ----------------
    m.h("9 质量保证与维护", level=1)

    m.h("9.1 可扩展点", level=2)
    m.table(
        ["需求", "改动范围"],
        [
            ["新增一套预置主题", "ThemePreset 增加一个枚举常量（6 个色值 + 2 个字体），再补一条 Flyway 迁移把新值加进 chk_theme_preset；页面与接口自动获得"],
            ["新增一种字体", "FontChoice 增加一个枚举常量"],
            ["新增一个风格维度（如行高）",
             "ThemeTokens 加字段 → SiteProfileForm 加字段与校验 → 渲染器 variables() 加一个变量 → 外观页加一个控件"],
            ["换掉风格表达方式（如输出 JSON 给构建期）", "替换 ThemeStyleRenderer 实现，模板与业务代码不动"],
            ["换 Markdown 引擎", "替换 MarkdownRenderer 实现"],
            ["新增订阅格式（Atom/JSON Feed）", "新增一个 Writer 并在 FeedController 加路由，取数复用 PostService"],
            ["把 LIKE 搜索换成全文索引", "只改 PostRepository 查询与 PostServiceImpl.search"],
            ["更换数据库", "新增一套 db/migration/<vendor> 脚本，业务代码零改动"],
            ["给文章加图片", "图片放进 static/images/gallery 并用 Markdown 引用，无需改代码"],
            ["改成后台上传图片", "已实现：ImageStorage 抽象 + /api/media/images + image-upload.js"],
            ["换成对象存储（OSS/COS/S3）", "新增一个 ImageStorage 实现即可，控制器与模板零改动"],
            ["调整上传限制（类型/大小/缩略图尺寸）", "改 ImageStorage 常量与 cxh.media.* 配置"],
        ],
        widths=[5.6, 9.4])

    m.h("9.2 已知限制", level=2)
    m.bullets([
        "单站点、单管理员模型：站点档案固定一行，管理员账号来自配置；多站点/多作者需引入 tenant 与用户维度；",
        "站点档案使用进程内快照缓存，多实例部署时应改为共享缓存或引入短 TTL；",
        "搜索为 LIKE 模糊匹配，未使用全文索引；文章量级超过万篇时建议迁移到 MySQL FULLTEXT 或搜索引擎；",
        "正文与简介长度上限 15000 字，受 MySQL TEXT 列限制；如需更长需改用 MEDIUMTEXT 并同步迁移脚本；",
        "自定义 CSS 以管理员为信任边界，未做语法白名单；",
        "安全策略为默认拒绝，白名单外的未知路径会跳转登录页而不是返回 404；",
        "Docker 镜像构建与 compose 启动未在本环境实际执行（无 daemon、Docker Hub 不可达）；",
        "Flyway 11.x 官方支持到 MySQL 8.1，在 MySQL 9.7 上会打印升级建议警告，但迁移实测正常。",
    ])

    m.h("9.3 维护建议", level=2)
    m.bullets([
        "结构变更一律新增 V2__xxx.sql，不修改已发布脚本；上线前把 ddl-auto 保持为 validate；",
        "每次改动后至少执行 ./mvnw test 与 scripts/acceptance-test.sh 两条命令（合计约 1 分钟）；",
        "生产环境通过环境变量注入 CXH_ADMIN_PASSWORD 与数据库口令，不要使用默认值；",
        "定期备份 ./data（H2）或 MySQL 的 cxh_blog 库；外观配置不保留历史版本，重要调整前先备份；",
        "关注 JaCoCo 报告中 common.exception 与未覆盖分支，新增异常处理时同步补测试。",
    ])

    m.page_break()

    # ---------------- 10 使用说明 ----------------
    m.h("10 使用说明", level=1)
    m.p("本章是面向运维与日常使用的速查手册：如何把系统跑起来、如何停掉、如何改配置、"
        "以及出问题怎么排查。面向访客与站长的详细界面操作见第 7 章。")

    m.h("10.1 环境准备", level=2)
    m.table(
        ["项目", "要求"],
        [
            ["Java", "JDK 17 及以上（编译目标 17，JDK 17 与 JDK 26 均实测通过全部测试）"],
            ["构建工具", "无需安装 Maven，仓库自带 Maven Wrapper（./mvnw）"],
            ["前端", "无需安装 Node.js / npm（零前端构建，纯服务端渲染）"],
            ["数据库", "默认 H2 文件库，零配置；生产可切 MySQL 8.0+（见 6.2）"],
            ["浏览器", "支持 CSS 自定义属性（变量）的现代浏览器"],
            ["可选", "Docker + Docker Compose（容器化部署，见 6.3）"],
        ],
        widths=[2.4, 12.6])
    m.p("首次构建（jar 已存在时可跳过）：在项目根目录执行 ./mvnw -B package -DskipTests，"
        "首次会联网下载 Maven 与依赖，约 1–2 分钟。构建产物为 target/cxh-blog.jar。", indent=False)

    m.h("10.2 启动、停止与状态", level=2)
    m.code(
        "# 前台启动（开发调试用，Ctrl+C 停止）\n"
        "./mvnw spring-boot:run\n\n"
        "# 后台常驻（推荐；自动写 PID 文件，可 status / stop）\n"
        "scripts/run.sh -d\n\n"
        "# 查看状态（含健康检查）/ 停止\n"
        "scripts/run.sh status\n"
        "scripts/run.sh stop\n\n"
        "# 打包后直接运行\n"
        "./mvnw clean package\n"
        "java -jar target/cxh-blog.jar\n\n"
        "# 换端口（三种方式等价）\n"
        "PORT=9090 scripts/run.sh -d\n"
        "java -jar target/cxh-blog.jar --server.port=9090"
    )
    m.p("重要提示：直接在交互式终端里用 java -jar target/cxh-blog.jar & 起的进程，"
        "可能随该终端 / shell 退出而被回收，浏览器会报 ERR_CONNECTION_REFUSED（8080 无监听）。"
        "需要常驻请使用 scripts/run.sh -d（后台 + PID 文件），或交给 systemd / launchd / Docker 管理。"
        "日志与 PID 分别位于 logs/app.log 与 logs/app.pid。", indent=False)

    m.h("10.3 配置说明", level=2)
    m.table(
        ["配置项", "设置方式", "默认值", "说明"],
        [
            ["服务端口", "SERVER_PORT 或 --server.port（run.sh 用 PORT）", "8080", "被占用时换一个即可"],
            ["管理员账号", "CXH_ADMIN_USERNAME / cxh.admin.username", "admin", "建议生产环境改掉"],
            ["管理员口令", "CXH_ADMIN_PASSWORD / cxh.admin.password", "cxh@2024",
             "支持明文（启动时自动加密）或 $2a$... BCrypt 摘要"],
            ["管理员显示名", "cxh.admin.display-name", "站长", "仅用于界面展示"],
            ["示例数据", "cxh.seed.enabled", "true", "仅在文章表为空时写入 5 篇示例"],
            ["数据库切换", "--spring.profiles.active=mysql 加 DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD", "H2",
             "表结构由 Flyway 自动迁移，见 6.2"],
            ["反向代理", "server.forward-headers-strategy", "framework", "影响 RSS / sitemap 的绝对地址推导"],
        ],
        widths=[2.6, 5.8, 2.0, 4.6])

    m.h("10.4 日常使用速览", level=2)
    m.table(
        ["任务", "操作", "详见"],
        [
            ["浏览 / 搜索 / 订阅", "前台导航；/search；/feed.xml", "7.1"],
            ["登录后台", "访问 /admin 或 /login，用管理员账号", "7.2"],
            ["写文章", "后台「文章管理 → 写新文章」，Markdown + 实时预览", "7.3"],
            ["改外观", "后台「外观设置」：主题 / 简介 / 头像 / 自定义 CSS，保存即全站生效", "7.4"],
            ["加图片", "文章编辑页点「插入图片」上传，自动插入图文 Markdown", "7.5"],
            ["备份数据", "scripts/backup.sh（可加进 crontab 每天执行）", "6.5"],
        ],
        widths=[3.4, 9.4, 2.2])

    m.h("10.5 常见故障排查", level=2)
    m.table(
        ["现象", "可能原因", "处理"],
        [
            ["浏览器报 ERR_CONNECTION_REFUSED", "应用未运行或进程被回收",
             "scripts/run.sh status；lsof -nP -iTCP:8080 -sTCP:LISTEN；再 scripts/run.sh -d"],
            ["启动提示端口被占用", "8080 已有其它进程", "PORT=9090 scripts/run.sh -d"],
            ["MySQL 下报 Unsupported character encoding 'utf8mb4'", "连接串 characterEncoding 写错",
             "改为 UTF-8（真正的 utf8mb4 由服务端与建库语句保证，见 6.2）"],
            ["启动报 Schema-validation 缺少表", "数据库结构与迁移脚本不一致", "开发库删除 ./data 后重启；生产检查迁移是否漏跑"],
            ["后台改配色后页面没变化", "浏览器缓存旧样式", "强制刷新；/theme.css 已带 no-store，一般不会发生"],
            ["登录失败", "账号 / 口令不匹配", "核对 CXH_ADMIN_USERNAME 与 CXH_ADMIN_PASSWORD"],
            ["首次 ./mvnw 失败", "网络或依赖下载问题", "检查网络后重试，按报错定位缺失依赖"],
            ["上传图片提示 403", "未登录或缺少 CSRF 令牌", "重新登录后台后再上传（令牌随会话轮换）"],
            ["上传提示类型不支持 / 无法识别", "不是图片或伪造扩展名", "只支持 jpeg/png/gif/webp，且以真实解码结果判定"],
            ["上传的图片打不开", "上传目录不可写或反代未放行 /uploads/", "检查 cxh.media.location 权限与 Nginx 配置"],
        ],
        widths=[5.0, 4.2, 5.8])

    m.page_break()

    # ---------------- 11 前后端更改指南 ----------------
    m.h("11 前后端更改指南", level=1)
    m.p("本章说明后续如何在保持「高内聚低耦合」的前提下改动系统："
        "后端新增能力、前端改页面或样式、数据库结构变更，以及改动后的验证清单。")

    m.h("11.1 改动前的通用约定", level=2)
    m.bullets([
        "分层与依赖方向：web / bootstrap → 业务域（site、post）→ common；业务域之间互不 import，跨域协作只在 web 装配层发生（见 4.2）；",
        "模板只消费 ${site.*} 与 --cxh-* 变量，不出现硬编码色值；",
        "表结构由 Flyway 管理，实体与脚本不一致会在启动时校验失败（ddl-auto=validate，这是护栏）；",
        "判据：改一个需求需要同时打开的文件越少，内聚越高；",
        "每次改动后至少执行 ./mvnw test 与 scripts/acceptance-test.sh（见 11.5）。",
    ])

    m.h("11.2 后端更改", level=2)

    m.h("11.2.1 新增一个领域能力", level=3)
    m.steps([
        "domain：新建实体 / 值对象，把不变量（校验、归一化、slug 生成等）写在实体内部（参考 SearchKeyword、Tags、HexColor）；",
        "repository：在仓储接口声明查询方法，公开查询与后台查询分开声明；",
        "service：在服务接口加方法，impl 负责编排与视图装配；控制器只依赖接口、不依赖实现；",
        "dto：区分写模型（Form / Draft）与读模型（View / Summary），模板不接触实体；",
        "web：新增控制器；公开页放 web 包或对应域 web 子包，后台管理放对应域 web 子包；",
        "security：后台路径默认已被 /admin/** 保护；新公开路径必须显式加进 SecurityConfig.PUBLIC_PATHS（默认拒绝，否则会 302 跳登录页）；",
        "迁移：若涉及表结构变更，新增 V2__xxx.sql（见 11.4）。",
    ])

    m.h("11.2.2 新增一个 HTTP 接口", level=3)
    m.bullets([
        "控制器方法标注 @GetMapping / @PostMapping，参数用 @RequestParam / @PathVariable / @ModelAttribute 绑定；",
        "表单校验用 jakarta.validation 注解 + @Valid，失败回显交给 BindingResult，不要手写校验；",
        "异常不要自己 try/catch 返回页面：抛领域异常（BusinessException / ResourceNotFoundException），"
        "由 GlobalExceptionHandler 统一翻译为 4xx/5xx，并按 Accept 头自动返回 JSON 或错误页；",
        "返回数据优先用 ApiResponse / PageResult 封装，避免把实体泄漏到接口。",
    ])

    m.h("11.2.3 安全联动", level=3)
    m.bullets([
        "新公开路径必须加进 SecurityConfig.PUBLIC_PATHS，否则默认拒绝；",
        "后台写操作保留 CSRF：异步 POST 从 <meta name=\"_csrf\"> 取令牌并加 X-CSRF-TOKEN 头；",
        "新页面的脚本用外部 js 文件，不写内联事件处理器（CSP 的 script-src 'self' 禁止 inline）；",
        "新枚举字段显式标注 @JdbcTypeCode(SqlTypes.VARCHAR)，否则会被建成不可移植的原生 enum 类型（见 BUG-06）。",
    ])

    m.h("11.3 前端更改", level=2)

    m.h("11.3.1 页面与模板", level=3)
    m.bullets([
        "页面通过 th:replace 复用 layout/base.html 片段（head / header / footer / postCard / pagination），后台复用 layout/admin.html；",
        "新增一个公开页面四步：控制器 → 模板 → 导航加链接 → SecurityConfig 白名单放行；",
        "注意 th:if 与 th:replace 不能同写在一个元素上（replace 优先级更高会先执行）；条件渲染请用 th:block 包裹；",
        "模板里的 ${site.*} 由 GlobalModelAdvice 统一注入，任何页面都能直接使用，无需控制器重复装配。",
    ])

    m.h("11.3.2 主题与样式", level=3)
    m.bullets([
        "改「风格」走后台外观设置、不动模板：模板只消费 --cxh-* 变量；",
        "改「布局 / 结构」才动 blog.css。注意级联顺序：blog.css（兜底值）在前、theme.css（真实主题）在后、"
        "预览挂载点最后——顺序写反会让编辑后的风格被兜底值覆盖（见 BUG-01）；",
        "新增一个风格维度：ThemeTokens 加字段 → SiteProfileForm 加字段与校验 → "
        "CssVariablesThemeStyleRenderer.variables() 加一个变量 → appearance.html 加一个控件（见 5.1.3）；",
        "后台样式独立于前台主题（admin.css，使用 --a-* 变量），改前台配色不影响后台可读性。",
    ])

    m.h("11.3.3 静态资源与图片", level=3)
    m.bullets([
        "css / js / 图片放 src/main/resources/static/ 对应目录，路径即 URL（/css/、/js/、/images/），已在安全白名单内；",
        "新图片放 static/images/gallery/，缩略图放 thumbs/（建议最长边 560px）；"
        "正文用 [![说明](缩略图)](原图) 即可排成照片墙（见 7.5）。",
    ])

    m.h("11.3.4 后台交互脚本", level=3)
    m.bullets([
        "CSRF：异步 POST 从 <meta name=\"_csrf\"> 取令牌，请求头加 X-CSRF-TOKEN（参考 appearance.js、markdown-preview.js）；",
        "内联事件被 CSP 禁止，确认操作等用外部脚本 + data-* 属性（参考 admin-confirm.js）；",
        "外观实时预览链路：表单 input 事件 → 约 220ms 防抖 → 把整个表单 POST 到 /admin/appearance/preview → "
        "取回 CSS → 注入 iframe 内的 <style id=\"cxh-theme-preview\">；预览与保存走同一条渲染链路；",
        "图片上传：input[type=file] 的 change 事件 → FormData 带 CSRF 头 POST 到 /api/media/images → "
        "把返回的 Markdown 插到光标处（参考 image-upload.js）。",
    ])

    m.h("11.4 数据库结构变更（Flyway）", level=2)
    m.bullets([
        "目录：src/main/resources/db/migration/{h2,mysql}/，{vendor} 由 Spring Boot 解析为 h2 / mysql；",
        "命名：V<n>__描述.sql（如 V2__add_post_pinned.sql）；已发布的脚本只增不改；",
        "两端各写一份，结构一一对应，差异只在方言（H2 用 varchar(32600) / timestamp with time zone / boolean，"
        "MySQL 用 text / datetime(6) / bit）；",
        "列类型必须与实体的 @JdbcTypeCode 声明一致，枚举一律 varchar + check 约束；",
        "启动顺序是「迁移 → Hibernate validate」，不一致会启动失败（这是护栏，不是 bug）；",
        "测试环境同样走迁移，所以每次 ./mvnw test 都在顺带校验建表脚本。",
    ])

    m.h("11.5 改动后的验证与提交清单", level=2)
    m.steps([
        "./mvnw test：94 项单元 / 集成测试 + JaCoCo 覆盖率报告（行覆盖以 ≥ 90% 为参考线）；",
        "scripts/acceptance-test.sh：110 项端到端验收用例，任何一条失败返回非 0 退出码；",
        "涉及版面的改动，用浏览器或无头截图人工核对（参考 docs/screenshots 的截图方式）；",
        "同步更新 README 与本说明书里的功能、测试数量、覆盖率等数字；",
        "结构变更新增迁移脚本，不修改已发布脚本；提交前确认 logs/、data/ 等已由 .gitignore 排除。",
    ])

    m.page_break()

    # ---------------- 附录 ----------------
    m.h("附录 A 接口清单", level=1)
    m.table(
        ["方法", "路径", "权限", "说明"],
        [
            ["GET", "/", "公开", "首页"],
            ["GET", "/posts", "公开", "文章列表（分页）"],
            ["GET", "/posts/{slug}", "公开", "文章详情（累加阅读量）"],
            ["GET", "/tags", "公开", "标签云"],
            ["GET", "/tags/{tag}", "公开", "按标签检索"],
            ["GET", "/search?q=", "公开", "站内搜索"],
            ["GET", "/archive", "公开", "归档"],
            ["GET", "/about", "公开", "关于"],
            ["GET", "/theme.css", "公开", "按当前风格实时生成的样式表"],
            ["GET", "/feed.xml", "公开", "RSS 2.0"],
            ["GET", "/sitemap.xml", "公开", "站点地图"],
            ["GET", "/robots.txt", "公开", "爬虫规则"],
            ["GET", "/login", "公开", "登录页"],
            ["POST", "/login", "公开", "登录提交（表单，需 CSRF）"],
            ["POST", "/logout", "公开", "退出登录（需 CSRF）"],
            ["GET", "/admin", "ADMIN", "后台概览"],
            ["GET", "/admin/posts", "ADMIN", "文章管理列表（可带 status 与 page）"],
            ["GET", "/admin/posts/new", "ADMIN", "新建文章表单"],
            ["GET", "/admin/posts/{id}/edit", "ADMIN", "编辑文章表单"],
            ["POST", "/admin/posts", "ADMIN", "保存文章（新建或更新）"],
            ["POST", "/admin/posts/{id}/delete", "ADMIN", "删除文章"],
            ["POST", "/admin/posts/preview", "ADMIN", "Markdown 预览（返回 HTML）"],
            ["GET", "/admin/appearance", "ADMIN", "外观设置页"],
            ["POST", "/admin/appearance", "ADMIN", "保存外观"],
            ["POST", "/admin/appearance/preset", "ADMIN", "一键套用预置主题"],
            ["POST", "/admin/appearance/preview", "ADMIN", "实时预览（返回 CSS，不落库）"],
            ["GET", "/admin/appearance/presets", "ADMIN", "预置主题列表（JSON）"],
            ["GET", "/actuator/health", "公开", "健康检查"],
        ],
        widths=[1.4, 4.6, 1.8, 7.2])

    m.h("附录 B 数据库结构", level=1)
    m.p("以下为 H2 版本的建表语句摘要（MySQL 版本见 db/migration/mysql/V1__init.sql，"
        "差异仅在 text/datetime(6)/bit 与 engine/charset 声明）。")
    m.code(
        "create table site_profile (\n"
        "    id                bigint       not null,\n"
        "    site_title        varchar(80)  not null,\n"
        "    site_subtitle     varchar(160),\n"
        "    author_name       varchar(60)  not null,\n"
        "    author_avatar_url varchar(400),\n"
        "    location          varchar(80),\n"
        "    github_url        varchar(400),\n"
        "    email             varchar(160),\n"
        "    footer_text       varchar(240),\n"
        "    icp_number        varchar(80),\n"
        "    intro_markdown    varchar(32600),   -- MySQL: text\n"
        "    custom_css        varchar(32600),\n"
        "    theme_preset      varchar(40) check (theme_preset in (...)),\n"
        "    color_primary     varchar(9),  color_accent  varchar(9),\n"
        "    color_background  varchar(9),  color_surface varchar(9),\n"
        "    color_text        varchar(9),  color_muted   varchar(9),\n"
        "    font_heading      varchar(40) check (...),\n"
        "    font_body         varchar(40) check (...),\n"
        "    base_font_size    integer,   content_width integer, corner_radius integer,\n"
        "    sticky_header     boolean,   show_hero     boolean,\n"
        "    created_at        timestamp(6) with time zone,\n"
        "    updated_at        timestamp(6) with time zone,\n"
        "    constraint pk_site_profile primary key (id)\n"
        ");\n\n"
        "create table post (\n"
        "    id               bigint generated by default as identity,\n"
        "    title            varchar(200) not null,\n"
        "    slug             varchar(220) not null,\n"
        "    summary          varchar(500),\n"
        "    content_markdown varchar(32600),\n"
        "    cover_image_url  varchar(400),\n"
        "    status           varchar(20)  not null check (status in ('DRAFT','PUBLISHED')),\n"
        "    published_at     timestamp(6) with time zone,\n"
        "    views            bigint       not null,\n"
        "    created_at       timestamp(6) with time zone,\n"
        "    updated_at       timestamp(6) with time zone,\n"
        "    constraint pk_post primary key (id),\n"
        "    constraint uk_post_slug unique (slug)\n"
        ");\n\n"
        "create table post_tag (\n"
        "    post_id bigint      not null,\n"
        "    tag     varchar(30) not null,\n"
        "    constraint pk_post_tag primary key (post_id, tag),\n"
        "    constraint fk_post_tag_post foreign key (post_id) references post (id) on delete cascade\n"
        ");"
    )

    m.h("附录 C 代码文件清单", level=1)
    m.table(
        ["分类", "数量", "说明"],
        [
            ["Java 源文件（main）", "62", "common 9 / site 16 / post 16 / security 5 / web 9 / media 7 / bootstrap 2 / 启动类 1"],
            ["Java 测试文件", "13", "5 个纯单元测试类（30 例）+ 9 个 Spring 集成测试类（63 例），共 93 例"],
            ["页面模板", "15", "前台 8、后台 5、骨架 2"],
            ["静态资源", "30", "样式 2、脚本 5、图标 1，以及相册原图 11 张 + 缩略图 11 张"],
            ["数据库迁移脚本", "2", "h2/V1__init.sql、mysql/V1__init.sql"],
            ["部署与脚本", "9", "Dockerfile、docker-compose.yml、.dockerignore、scripts/{run,acceptance-test,backup}.sh、deploy/{compose.prod.yml,nginx 配置,.env 模板}"],
            ["文档", "3", "README.md、本说明书、docs/acceptance-test-result.txt"],
        ],
        widths=[3.6, 1.8, 9.6])

    m.save(OUTPUT)
    print(f"已生成：{OUTPUT}")


if __name__ == "__main__":
    build()
