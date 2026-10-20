package com.cxh.blog.common.text;

/**
 * XML 文本转义。RSS / sitemap 这类机器可读输出必须严格转义，
 * 否则一次标题里的 {@code &} 就能让整个 feed 变成非法 XML。
 */
public final class XmlText {

    private XmlText() {
    }

    /** 转义 XML 文本节点的五个预定义实体。 */
    public static String escape(String raw) {
        if (raw == null || raw.isEmpty()) {
            return "";
        }
        StringBuilder escaped = new StringBuilder(raw.length() + 16);
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            switch (c) {
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                case '"' -> escaped.append("&quot;");
                case '\'' -> escaped.append("&apos;");
                default -> {
                    // 丢弃 XML 1.0 不允许的控制字符，避免产生无法解析的文档
                    if (c >= 0x20 || c == '\t' || c == '\n' || c == '\r') {
                        escaped.append(c);
                    }
                }
            }
        }
        return escaped.toString();
    }
}
