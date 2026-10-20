package com.cxh.blog.site.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 站点档案聚合根：站点身份（标题/作者/页脚）+ 简介 + 主题风格。
 *
 * <p>本系统是单站点博客，因此固定使用 {@link #SINGLETON_ID} 一行记录，
 * 使「站点配置」成为一个稳定、易缓存的聚合边界。
 *
 * <p>行为内聚：所有字段的合法化逻辑都收敛在 {@link #edit} 与 {@link #applyPreset} 中，
 * 外部无法绕过校验写入非法风格值。
 */
@Entity
@Table(name = "site_profile")
@EntityListeners(AuditingEntityListener.class)
public class SiteProfile {

    public static final Long SINGLETON_ID = 1L;

    @Id
    private Long id = SINGLETON_ID;

    @Column(name = "site_title", nullable = false, length = 80)
    private String siteTitle = "cxh 的博客";

    @Column(name = "site_subtitle", length = 160)
    private String siteSubtitle = "记录、思考与构建";

    @Column(name = "author_name", nullable = false, length = 60)
    private String authorName = "cxh";

    @Column(name = "author_avatar_url", length = 400)
    private String authorAvatarUrl;

    @Column(name = "location", length = 80)
    private String location;

    @Column(name = "github_url", length = 400)
    private String githubUrl;

    @Column(name = "email", length = 160)
    private String email;

    @Column(name = "footer_text", length = 240)
    private String footerText;

    @Column(name = "icp_number", length = 80)
    private String icpNumber;

    /*
     * 大文本字段用 SqlTypes.LONGVARCHAR 声明：
     * 由方言决定实际列类型（MySQL → longtext，H2 → character large object），
     * 既避免硬编码数据库类型，也让 schema 校验（ddl-auto=validate）能正常通过。
     */
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "intro_markdown")
    private String introMarkdown;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "custom_css")
    private String customCss;

    @Embedded
    private ThemeTokens theme = ThemeTokens.defaults();

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    protected SiteProfile() {
        // JPA
    }

    public static SiteProfile createDefault() {
        SiteProfile profile = new SiteProfile();
        profile.id = SINGLETON_ID;
        profile.theme = ThemePreset.ELEGANT_LIGHT.newTokens();
        // 默认头像取自相册里的照片；不想露脸就在后台「外观设置」里清空这个字段
        profile.authorAvatarUrl = "/images/gallery/cxh03.jpg";
        profile.location = "杭州";
        profile.introMarkdown = """
                ## 你好，我是 cxh 👋

                这里是我的个人博客，用来记录技术笔记、项目复盘和一些不成熟的想法。

                - 🔧 主要方向：Java / Spring 生态、后端架构
                - ✍️ 写作原则：能跑起来的代码优先于空谈
                - 📮 联系方式见页脚

                > 风格与简介都可以在后台「外观设置」里自由编辑，改完立刻生效。
                """;
        return profile;
    }

    /**
     * 用一次编辑请求覆盖站点档案。传入的令牌会被规范化后再写入。
     */
    public void edit(Identity identity, String introMarkdown, ThemeTokens theme, String customCss) {
        this.siteTitle = require(identity.siteTitle(), "站点标题不能为空");
        this.siteSubtitle = identity.siteSubtitle();
        this.authorName = require(identity.authorName(), "作者名不能为空");
        this.authorAvatarUrl = blankToNull(identity.authorAvatarUrl());
        this.location = blankToNull(identity.location());
        this.githubUrl = blankToNull(identity.githubUrl());
        this.email = blankToNull(identity.email());
        this.footerText = blankToNull(identity.footerText());
        this.icpNumber = blankToNull(identity.icpNumber());
        this.introMarkdown = introMarkdown;
        this.customCss = customCss;
        applyPresetInternal(theme);
    }

    /** 只切换主题风格，保留身份与简介。 */
    public void applyPreset(ThemeTokens theme) {
        applyPresetInternal(theme);
    }

    private void applyPresetInternal(ThemeTokens incoming) {
        ThemeTokens tokens = incoming == null ? this.theme.copy() : incoming.copy();
        tokens.normalize();
        this.theme = tokens;
    }

    private static String require(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** 站点身份字段的只读快照，避免 edit 方法参数过长。 */
    public record Identity(
            String siteTitle,
            String siteSubtitle,
            String authorName,
            String authorAvatarUrl,
            String location,
            String githubUrl,
            String email,
            String footerText,
            String icpNumber) {
    }

    public Long getId() {
        return id;
    }

    public String getSiteTitle() {
        return siteTitle;
    }

    public String getSiteSubtitle() {
        return siteSubtitle;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getAuthorAvatarUrl() {
        return authorAvatarUrl;
    }

    public String getLocation() {
        return location;
    }

    public String getGithubUrl() {
        return githubUrl;
    }

    public String getEmail() {
        return email;
    }

    public String getFooterText() {
        return footerText;
    }

    public String getIcpNumber() {
        return icpNumber;
    }

    public String getIntroMarkdown() {
        return introMarkdown;
    }

    public String getCustomCss() {
        return customCss;
    }

    public ThemeTokens getTheme() {
        return theme;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
