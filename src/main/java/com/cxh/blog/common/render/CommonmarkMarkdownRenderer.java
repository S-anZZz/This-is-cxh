package com.cxh.blog.common.render;

import java.util.ArrayList;
import java.util.List;
import org.commonmark.Extension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Component;

/**
 * 基于 CommonMark 的默认渲染实现。
 *
 * <p>刻意开启 {@code escapeHtml(true)}：简介/正文中的原始 HTML 会被转义，
 * 从而把「样式自由」收敛到受控的 CSS 变量通道，避免通过内容注入脚本。
 */
@Component
public class CommonmarkMarkdownRenderer implements MarkdownRenderer {

    private final Parser parser;
    private final HtmlRenderer htmlRenderer;

    public CommonmarkMarkdownRenderer() {
        List<Extension> extensions = List.of(TablesExtension.create());
        this.parser = Parser.builder().extensions(extensions).build();
        this.htmlRenderer = HtmlRenderer.builder()
                .extensions(extensions)
                .escapeHtml(true)
                .softbreak("<br />\n")
                .build();
    }

    @Override
    public String toHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        Node document = parser.parse(markdown);
        return htmlRenderer.render(document);
    }

    @Override
    public String toPlainSummary(String markdown, int maxLength) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        String plain = stripMarkdown(markdown);
        if (plain.length() <= maxLength) {
            return plain;
        }
        return plain.substring(0, maxLength) + "…";
    }

    /** 轻量去除 Markdown 标记，仅用于生成列表摘要。 */
    private String stripMarkdown(String markdown) {
        StringBuilder sb = new StringBuilder();
        for (String rawLine : markdown.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("```") || line.startsWith("|")) {
                continue;
            }
            line = line.replaceAll("^#{1,6}\\s*", "")
                    .replaceAll("^[>*+-]\\s+", "")
                    .replaceAll("!\\[[^]]*]\\([^)]*\\)", "")
                    .replaceAll("\\[([^]]*)]\\([^)]*\\)", "$1")
                    .replaceAll("[`*_~]", "");
            sb.append(line).append(' ');
        }
        return sb.toString().trim();
    }

    /** 供测试与调试使用：返回所有已注册的扩展名。 */
    public List<String> registeredExtensions() {
        List<String> names = new ArrayList<>();
        names.add(TablesExtension.class.getSimpleName());
        return names;
    }
}
