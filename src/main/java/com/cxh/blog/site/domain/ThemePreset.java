package com.cxh.blog.site.domain;

/**
 * 预置主题。每个预置自带一整套默认风格值，是"一键换风格"的唯一数据源。
 *
 * <p>新增主题：在此追加枚举常量即可，模板与服务代码都不用动（开闭原则）。
 * 但 {@code site_profile.theme_preset} 上有数据库 CHECK 约束，因此还需补一条
 * Flyway 迁移把新值纳入 {@code chk_theme_preset}（见 V2__relax_theme_preset_check.sql 的写法）。
 */
public enum ThemePreset {

    ELEGANT_LIGHT("简约白", "#2563eb", "#7c3aed", "#f7f8fa", "#ffffff", "#1f2937", "#6b7280",
            FontChoice.SYSTEM_SANS, FontChoice.SYSTEM_SANS),
    MIDNIGHT_DARK("午夜黑", "#60a5fa", "#c084fc", "#0e1116", "#171b22", "#e5e7eb", "#9aa4b2",
            FontChoice.SYSTEM_SANS, FontChoice.SYSTEM_SANS),
    WARM_PAPER("暖纸", "#b45309", "#0f766e", "#f6f1e7", "#fffdf8", "#3f3222", "#8a7a63",
            FontChoice.MODERN_SERIF, FontChoice.MODERN_SERIF),
    OCEAN_BLUE("深海蓝", "#0ea5e9", "#22d3ee", "#f0f7fb", "#ffffff", "#0f2b3d", "#5b7a8c",
            FontChoice.HUMANIST_SANS, FontChoice.HUMANIST_SANS),
    FOREST_GREEN("森林绿", "#15803d", "#a16207", "#f3f7f2", "#ffffff", "#1b2e21", "#61705f",
            FontChoice.HUMANIST_SANS, FontChoice.MODERN_SERIF),
    MONO_PRINT("打字稿", "#111827", "#dc2626", "#fafafa", "#ffffff", "#111827", "#6b7280",
            FontChoice.MONO, FontChoice.MONO);

    private final String label;
    private final String primaryColor;
    private final String accentColor;
    private final String pageBackground;
    private final String surfaceBackground;
    private final String textColor;
    private final String mutedTextColor;
    private final FontChoice headingFont;
    private final FontChoice bodyFont;

    ThemePreset(String label, String primaryColor, String accentColor, String pageBackground,
                String surfaceBackground, String textColor, String mutedTextColor,
                FontChoice headingFont, FontChoice bodyFont) {
        this.label = label;
        this.primaryColor = primaryColor;
        this.accentColor = accentColor;
        this.pageBackground = pageBackground;
        this.surfaceBackground = surfaceBackground;
        this.textColor = textColor;
        this.mutedTextColor = mutedTextColor;
        this.headingFont = headingFont;
        this.bodyFont = bodyFont;
    }

    public String getLabel() {
        return label;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public String getPageBackground() {
        return pageBackground;
    }

    public String getSurfaceBackground() {
        return surfaceBackground;
    }

    public String getTextColor() {
        return textColor;
    }

    public String getMutedTextColor() {
        return mutedTextColor;
    }

    public FontChoice getHeadingFont() {
        return headingFont;
    }

    public FontChoice getBodyFont() {
        return bodyFont;
    }

    /** 该预置是否为深色底（用于自动选择代码块等局部配色）。 */
    public boolean isDark() {
        return HexColor.luminance(pageBackground) < 0.4;
    }

    /**
     * 把预置配色写入令牌对象，保留用户的排版类参数（字号、宽度、圆角、开关）。
     */
    public void applyColorsTo(ThemeTokens tokens) {
        tokens.setPreset(this);
        tokens.setPrimaryColor(primaryColor);
        tokens.setAccentColor(accentColor);
        tokens.setPageBackground(pageBackground);
        tokens.setSurfaceBackground(surfaceBackground);
        tokens.setTextColor(textColor);
        tokens.setMutedTextColor(mutedTextColor);
    }

    /** 生成一套完整默认令牌。 */
    public ThemeTokens newTokens() {
        ThemeTokens tokens = ThemeTokens.defaults();
        applyColorsTo(tokens);
        tokens.setHeadingFont(headingFont);
        tokens.setBodyFont(bodyFont);
        return tokens;
    }

    public static ThemePreset of(String name, ThemePreset fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        for (ThemePreset preset : values()) {
            if (preset.name().equalsIgnoreCase(name.trim())) {
                return preset;
            }
        }
        return fallback;
    }
}
