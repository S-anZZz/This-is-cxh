package com.cxh.blog.site.dto;

import com.cxh.blog.site.domain.FontChoice;
import com.cxh.blog.site.domain.HexColor;
import com.cxh.blog.site.domain.SiteProfile;
import com.cxh.blog.site.domain.ThemePreset;
import com.cxh.blog.site.domain.ThemeTokens;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 站点外观编辑表单：后台「外观设置」与站点域之间的唯一写入契约。
 *
 * <p>所有风格输入都在这里被约束：
 * 颜色限定十六进制、字体限定枚举、数值限定区间，
 * 使"自由编辑"不会演变成"任意注入"。
 */
public class SiteProfileForm {

    /* ---------- 站点身份 ---------- */

    @NotBlank(message = "站点标题不能为空")
    @Size(max = 80, message = "站点标题最长 80 字")
    private String siteTitle;

    @Size(max = 160, message = "副标题最长 160 字")
    private String siteSubtitle;

    @NotBlank(message = "作者名不能为空")
    @Size(max = 60, message = "作者名最长 60 字")
    private String authorName;

    @Size(max = 400, message = "头像地址最长 400 字")
    private String authorAvatarUrl;

    @Size(max = 80, message = "所在地最长 80 字")
    private String location;

    @Size(max = 400, message = "GitHub 地址最长 400 字")
    private String githubUrl;

    @Email(message = "邮箱格式不正确")
    @Size(max = 160, message = "邮箱最长 160 字")
    private String email;

    @Size(max = 240, message = "页脚文字最长 240 字")
    private String footerText;

    @Size(max = 80, message = "备案号最长 80 字")
    private String icpNumber;

    /* ---------- 简介 ---------- */

    @Size(max = 8000, message = "简介最长 8000 字")
    private String introMarkdown;

    /* ---------- 主题风格 ---------- */

    @NotNull(message = "请选择预置主题")
    private ThemePreset preset = ThemePreset.ELEGANT_LIGHT;

    @Pattern(regexp = HexColor.PATTERN, message = "主色需为 #RGB 或 #RRGGBB")
    private String primaryColor;

    @Pattern(regexp = HexColor.PATTERN, message = "辅助色需为 #RGB 或 #RRGGBB")
    private String accentColor;

    @Pattern(regexp = HexColor.PATTERN, message = "页面背景色需为 #RGB 或 #RRGGBB")
    private String pageBackground;

    @Pattern(regexp = HexColor.PATTERN, message = "卡片背景色需为 #RGB 或 #RRGGBB")
    private String surfaceBackground;

    @Pattern(regexp = HexColor.PATTERN, message = "正文字色需为 #RGB 或 #RRGGBB")
    private String textColor;

    @Pattern(regexp = HexColor.PATTERN, message = "次要文字色需为 #RGB 或 #RRGGBB")
    private String mutedTextColor;

    @NotNull(message = "请选择标题字体")
    private FontChoice headingFont = FontChoice.SYSTEM_SANS;

    @NotNull(message = "请选择正文字体")
    private FontChoice bodyFont = FontChoice.SYSTEM_SANS;

    @Min(value = 12, message = "字号不得小于 12px")
    @Max(value = 26, message = "字号不得大于 26px")
    private int baseFontSize = ThemeTokens.DEFAULT_FONT_SIZE;

    @Min(value = 560, message = "内容宽度不得小于 560px")
    @Max(value = 1600, message = "内容宽度不得大于 1600px")
    private int contentWidth = ThemeTokens.DEFAULT_CONTENT_WIDTH;

    @Min(value = 0, message = "圆角不得为负")
    @Max(value = 40, message = "圆角不得大于 40px")
    private int cornerRadius = ThemeTokens.DEFAULT_RADIUS;

    private boolean stickyHeader = true;

    private boolean showHero = true;

    @Size(max = 8000, message = "自定义 CSS 最长 8000 字")
    private String customCss;

    /** 由站点档案回填表单。 */
    public static SiteProfileForm from(SiteProfile profile) {
        ThemeTokens theme = profile.getTheme();
        SiteProfileForm form = new SiteProfileForm();
        form.siteTitle = profile.getSiteTitle();
        form.siteSubtitle = profile.getSiteSubtitle();
        form.authorName = profile.getAuthorName();
        form.authorAvatarUrl = profile.getAuthorAvatarUrl();
        form.location = profile.getLocation();
        form.githubUrl = profile.getGithubUrl();
        form.email = profile.getEmail();
        form.footerText = profile.getFooterText();
        form.icpNumber = profile.getIcpNumber();
        form.introMarkdown = profile.getIntroMarkdown();
        form.customCss = profile.getCustomCss();
        form.preset = theme.getPreset();
        form.primaryColor = theme.getPrimaryColor();
        form.accentColor = theme.getAccentColor();
        form.pageBackground = theme.getPageBackground();
        form.surfaceBackground = theme.getSurfaceBackground();
        form.textColor = theme.getTextColor();
        form.mutedTextColor = theme.getMutedTextColor();
        form.headingFont = theme.getHeadingFont();
        form.bodyFont = theme.getBodyFont();
        form.baseFontSize = theme.getBaseFontSize();
        form.contentWidth = theme.getContentWidth();
        form.cornerRadius = theme.getCornerRadius();
        form.stickyHeader = theme.isStickyHeader();
        form.showHero = theme.isShowHero();
        return form;
    }

    /** 表单 → 主题令牌。规范化交给令牌自身，非法值不会落库。 */
    public ThemeTokens toThemeTokens() {
        ThemeTokens tokens = ThemeTokens.defaults();
        tokens.setPreset(preset);
        tokens.setPrimaryColor(primaryColor);
        tokens.setAccentColor(accentColor);
        tokens.setPageBackground(pageBackground);
        tokens.setSurfaceBackground(surfaceBackground);
        tokens.setTextColor(textColor);
        tokens.setMutedTextColor(mutedTextColor);
        tokens.setHeadingFont(headingFont);
        tokens.setBodyFont(bodyFont);
        tokens.setBaseFontSize(baseFontSize);
        tokens.setContentWidth(contentWidth);
        tokens.setCornerRadius(cornerRadius);
        tokens.setStickyHeader(stickyHeader);
        tokens.setShowHero(showHero);
        return tokens;
    }

    /** 表单 → 站点身份值对象。 */
    public SiteProfile.Identity toIdentity() {
        return new SiteProfile.Identity(siteTitle, siteSubtitle, authorName, authorAvatarUrl,
                location, githubUrl, email, footerText, icpNumber);
    }

    public String getSiteTitle() {
        return siteTitle;
    }

    public void setSiteTitle(String siteTitle) {
        this.siteTitle = siteTitle;
    }

    public String getSiteSubtitle() {
        return siteSubtitle;
    }

    public void setSiteSubtitle(String siteSubtitle) {
        this.siteSubtitle = siteSubtitle;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorAvatarUrl() {
        return authorAvatarUrl;
    }

    public void setAuthorAvatarUrl(String authorAvatarUrl) {
        this.authorAvatarUrl = authorAvatarUrl;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public void setGithubUrl(String githubUrl) {
        this.githubUrl = githubUrl;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFooterText() {
        return footerText;
    }

    public void setFooterText(String footerText) {
        this.footerText = footerText;
    }

    public String getIcpNumber() {
        return icpNumber;
    }

    public void setIcpNumber(String icpNumber) {
        this.icpNumber = icpNumber;
    }

    public String getIntroMarkdown() {
        return introMarkdown;
    }

    public void setIntroMarkdown(String introMarkdown) {
        this.introMarkdown = introMarkdown;
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

    public String getCustomCss() {
        return customCss;
    }

    public void setCustomCss(String customCss) {
        this.customCss = customCss;
    }
}
