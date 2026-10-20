package com.cxh.blog.site.dto;

import com.cxh.blog.site.domain.SiteProfile;
import com.cxh.blog.site.domain.ThemeTokens;
import java.time.Instant;

/**
 * 站点档案只读视图：所有页面断言渲染所需的全部站点信息。
 *
 * <p>模板只依赖该读模型，不再回查数据库，也不接触实体，
 * 因此站点域的内部结构可以自由调整。
 */
public record SiteProfileView(
        String siteTitle,
        String siteSubtitle,
        String authorName,
        String authorAvatarUrl,
        String location,
        String githubUrl,
        String email,
        String footerText,
        String icpNumber,
        String introMarkdown,
        String introHtml,
        String customCss,
        ThemeTokens theme,
        Instant updatedAt,
        String updatedAtText) {

    public static SiteProfileView from(SiteProfile profile, String introHtml) {
        return new SiteProfileView(
                profile.getSiteTitle(),
                profile.getSiteSubtitle(),
                profile.getAuthorName(),
                profile.getAuthorAvatarUrl(),
                profile.getLocation(),
                profile.getGithubUrl(),
                profile.getEmail(),
                profile.getFooterText(),
                profile.getIcpNumber(),
                profile.getIntroMarkdown(),
                introHtml,
                profile.getCustomCss(),
                profile.getTheme().copy(),
                profile.getUpdatedAt(),
                com.cxh.blog.common.text.DateTimes.toDateTime(profile.getUpdatedAt()));
    }

    public boolean hasAvatar() {
        return authorAvatarUrl != null && !authorAvatarUrl.isBlank();
    }

    public boolean hasIntro() {
        return introHtml != null && !introHtml.isBlank();
    }

    public boolean hasContact() {
        return notBlank(email) || notBlank(githubUrl) || notBlank(location);
    }

    public String copyright() {
        return "© " + java.time.Year.now().getValue() + " " + authorName;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
