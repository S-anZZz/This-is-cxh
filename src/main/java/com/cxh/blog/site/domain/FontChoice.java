package com.cxh.blog.site.domain;

/**
 * 字体选择白名单。前端只能从这些预置字体栈中选择，
 * 避免把任意字符串拼进 CSS（既是安全边界，也让"风格自由"有确定的取值域）。
 */
public enum FontChoice {

    SYSTEM_SANS("系统无衬线", "-apple-system, BlinkMacSystemFont, \"Segoe UI\", \"PingFang SC\", \"Hiragino Sans GB\", \"Microsoft YaHei\", sans-serif"),
    HUMANIST_SANS("人文无衬线", "\"Optima\", \"Segoe UI\", \"PingFang SC\", \"Microsoft YaHei\", Avenir, sans-serif"),
    MODERN_SERIF("现代衬线", "\"Iowan Old Style\", Georgia, \"Songti SC\", \"Source Han Serif SC\", \"Noto Serif SC\", serif"),
    CLASSIC_SERIF("经典衬线", "Georgia, \"Times New Roman\", \"Kaiti SC\", \"STKaiti\", serif"),
    MONO("等宽", "ui-monospace, SFMono-Regular, Menlo, Consolas, \"Liberation Mono\", monospace");

    private final String label;
    private final String cssStack;

    FontChoice(String label, String cssStack) {
        this.label = label;
        this.cssStack = cssStack;
    }

    public String getLabel() {
        return label;
    }

    public String getCssStack() {
        return cssStack;
    }

    public static FontChoice of(String name, FontChoice fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        for (FontChoice choice : values()) {
            if (choice.name().equalsIgnoreCase(name.trim())) {
                return choice;
            }
        }
        return fallback;
    }
}
