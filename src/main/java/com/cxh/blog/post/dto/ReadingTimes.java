package com.cxh.blog.post.dto;

/**
 * 预估阅读时长。规则是文章域的内部知识，放在读模型旁边，
 * 便于单独调整与测试。
 */
final class ReadingTimes {

    private static final int CHARS_PER_MINUTE = 400;

    private ReadingTimes() {
    }

    static int of(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return 1;
        }
        int meaningful = markdown.replaceAll("\\s", "").length();
        return Math.max(1, (int) Math.ceil((double) meaningful / CHARS_PER_MINUTE));
    }
}
