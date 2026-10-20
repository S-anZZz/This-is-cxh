package com.cxh.blog.site.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 主题令牌：站点风格的完整、受校验的取值集合。
 *
 * <p>它是「风格」这一概念的唯一载体，也是模板与编辑界面之间唯一的数据契约。
 * 模板只消费由令牌渲染出的 CSS 变量，编辑界面只生产令牌，两侧互不知晓对方。
 */
@Embeddable
public class ThemeTokens {

    public static final int DEFAULT_FONT_SIZE = 17;
    public static final int DEFAULT_CONTENT_WIDTH = 780;
    public static final int DEFAULT_RADIUS = 14;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "theme_preset", length = 40)
    private ThemePreset preset = ThemePreset.ELEGANT_LIGHT;

    @Column(name = "color_primary", length = 9)
    private String primaryColor = "#2563eb";

    @Column(name = "color_accent", length = 9)
    private String accentColor = "#7c3aed";

    @Column(name = "color_background", length = 9)
    private String pageBackground = "#f7f8fa";

    @Column(name = "color_surface", length = 9)
    private String surfaceBackground = "#ffffff";

    @Column(name = "color_text", length = 9)
    private String textColor = "#1f2937";

    @Column(name = "color_muted", length = 9)
    private String mutedTextColor = "#6b7280";

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "font_heading", length = 40)
    private FontChoice headingFont = FontChoice.SYSTEM_SANS;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "font_body", length = 40)
    private FontChoice bodyFont = FontChoice.SYSTEM_SANS;

    @Column(name = "base_font_size")
    private int baseFontSize = DEFAULT_FONT_SIZE;

    @Column(name = "content_width")
    private int contentWidth = DEFAULT_CONTENT_WIDTH;

    @Column(name = "corner_radius")
    private int cornerRadius = DEFAULT_RADIUS;

    @Column(name = "sticky_header")
    private boolean stickyHeader = true;

    @Column(name = "show_hero")
    private boolean showHero = true;

    public ThemeTokens() {
        // JPA 需要的无参构造
    }

    public static ThemeTokens defaults() {
        return new ThemeTokens();
    }

    /** 拷贝副本，避免多个实体共享同一个令牌实例。 */
    public ThemeTokens copy() {
        ThemeTokens copy = defaults();
        copy.preset = preset;
        copy.primaryColor = primaryColor;
        copy.accentColor = accentColor;
        copy.pageBackground = pageBackground;
        copy.surfaceBackground = surfaceBackground;
        copy.textColor = textColor;
        copy.mutedTextColor = mutedTextColor;
        copy.headingFont = headingFont;
        copy.bodyFont = bodyFont;
        copy.baseFontSize = baseFontSize;
        copy.contentWidth = contentWidth;
        copy.cornerRadius = cornerRadius;
        copy.stickyHeader = stickyHeader;
        copy.showHero = showHero;
        return copy;
    }

    /**
     * 收敛到合法取值域：颜色非法即回退预置值，数值越界即夹取到区间内。
     * 这是写入数据库前的最后一道防线（防御式校验）。
     */
    public void normalize() {
        ThemePreset safePreset = preset == null ? ThemePreset.ELEGANT_LIGHT : preset;
        this.preset = safePreset;
        this.primaryColor = HexColor.normalize(primaryColor, safePreset.getPrimaryColor());
        this.accentColor = HexColor.normalize(accentColor, safePreset.getAccentColor());
        this.pageBackground = HexColor.normalize(pageBackground, safePreset.getPageBackground());
        this.surfaceBackground = HexColor.normalize(surfaceBackground, safePreset.getSurfaceBackground());
        this.textColor = HexColor.normalize(textColor, safePreset.getTextColor());
        this.mutedTextColor = HexColor.normalize(mutedTextColor, safePreset.getMutedTextColor());
        this.headingFont = headingFont == null ? safePreset.getHeadingFont() : headingFont;
        this.bodyFont = bodyFont == null ? safePreset.getBodyFont() : bodyFont;
        this.baseFontSize = clamp(baseFontSize, 12, 26, DEFAULT_FONT_SIZE);
        this.contentWidth = clamp(contentWidth, 560, 1600, DEFAULT_CONTENT_WIDTH);
        this.cornerRadius = clamp(cornerRadius, 0, 40, DEFAULT_RADIUS);
    }

    /** 深色底判定，模板据此切换局部配色（例如封面遮罩）。 */
    public boolean isDarkSurface() {
        return HexColor.luminance(pageBackground) < 0.4;
    }

    private static int clamp(int value, int min, int max, int fallback) {
        if (value <= 0) {
            return fallback;
        }
        return Math.max(min, Math.min(max, value));
    }

    public ThemePreset getPreset() {
        return preset;
    }

    public void setPreset(ThemePreset preset) {
        this.preset = preset;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(String primaryColor) {
        this.primaryColor = primaryColor;
    }

    public String getAccentColor() {
        return accentColor;
    }

    public void setAccentColor(String accentColor) {
        this.accentColor = accentColor;
    }

    public String getPageBackground() {
        return pageBackground;
    }

    public void setPageBackground(String pageBackground) {
        this.pageBackground = pageBackground;
    }

    public String getSurfaceBackground() {
        return surfaceBackground;
    }

    public void setSurfaceBackground(String surfaceBackground) {
        this.surfaceBackground = surfaceBackground;
    }

    public String getTextColor() {
        return textColor;
    }

    public void setTextColor(String textColor) {
        this.textColor = textColor;
    }

    public String getMutedTextColor() {
        return mutedTextColor;
    }

    public void setMutedTextColor(String mutedTextColor) {
        this.mutedTextColor = mutedTextColor;
    }

    public FontChoice getHeadingFont() {
        return headingFont;
    }

    public void setHeadingFont(FontChoice headingFont) {
        this.headingFont = headingFont;
    }

    public FontChoice getBodyFont() {
        return bodyFont;
    }

    public void setBodyFont(FontChoice bodyFont) {
        this.bodyFont = bodyFont;
    }

    public int getBaseFontSize() {
        return baseFontSize;
    }

    public void setBaseFontSize(int baseFontSize) {
        this.baseFontSize = baseFontSize;
    }

    public int getContentWidth() {
        return contentWidth;
    }

    public void setContentWidth(int contentWidth) {
        this.contentWidth = contentWidth;
    }

    public int getCornerRadius() {
        return cornerRadius;
    }

    public void setCornerRadius(int cornerRadius) {
        this.cornerRadius = cornerRadius;
    }

    public boolean isStickyHeader() {
        return stickyHeader;
    }

    public void setStickyHeader(boolean stickyHeader) {
        this.stickyHeader = stickyHeader;
    }

    public boolean isShowHero() {
        return showHero;
    }

    public void setShowHero(boolean showHero) {
        this.showHero = showHero;
    }
}
