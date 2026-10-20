package com.cxh.blog.web.feed;

import com.cxh.blog.common.text.DateTimes;
import com.cxh.blog.common.text.XmlText;
import com.cxh.blog.post.dto.PostSummary;
import com.cxh.blog.site.dto.SiteProfileView;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

/**
 * RSS 2.0 输出。
 *
 * <p>职责单一：只负责把「站点档案 + 文章列表」序列化成合法的 RSS XML。
 * 它不查库、不依赖控制器，因此可以独立测试与复用（例如将来加 JSON Feed）。
 * 所有文本都经过 {@link XmlText} 转义 —— 标题里有一个 {@code &} 就足以毁掉整份 feed。
 */
@Component
public class RssFeedWriter {

    private static final int INDENT = 4;

    public String write(SiteProfileView site, List<PostSummary> posts, String baseUrl) {
        StringBuilder xml = new StringBuilder(4096);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<rss version=\"2.0\" xmlns:atom=\"http://www.w3.org/2005/Atom\">\n");
        xml.append("  <channel>\n");
        element(xml, INDENT, "title", site.siteTitle());
        element(xml, INDENT, "link", baseUrl + "/");
        element(xml, INDENT, "description", channelDescription(site));
        element(xml, INDENT, "language", "zh-CN");
        element(xml, INDENT, "lastBuildDate", DateTimes.toRfc1123(Instant.now()));
        xml.append("    <atom:link href=\"").append(XmlText.escape(baseUrl)).append("/feed.xml\"")
                .append(" rel=\"self\" type=\"application/rss+xml\"/>\n");

        for (PostSummary post : posts) {
            appendItem(xml, post, baseUrl);
        }

        xml.append("  </channel>\n</rss>\n");
        return xml.toString();
    }

    private void appendItem(StringBuilder xml, PostSummary post, String baseUrl) {
        String link = baseUrl + "/posts/" + UriUtils.encodePathSegment(post.slug(), java.nio.charset.StandardCharsets.UTF_8);
        xml.append("    <item>\n");
        element(xml, 6, "title", post.title());
        element(xml, 6, "link", link);
        xml.append("      <guid isPermaLink=\"true\">").append(XmlText.escape(link)).append("</guid>\n");
        if (post.publishedAt() != null) {
            element(xml, 6, "pubDate", DateTimes.toRfc1123(post.publishedAt()));
        }
        element(xml, 6, "description", post.summary());
        for (String tag : post.tags()) {
            element(xml, 6, "category", tag);
        }
        xml.append("    </item>\n");
    }

    private String channelDescription(SiteProfileView site) {
        if (site.siteSubtitle() != null && !site.siteSubtitle().isBlank()) {
            return site.siteSubtitle();
        }
        return site.siteTitle() + " 的文章订阅";
    }

    private void element(StringBuilder xml, int indent, String name, String value) {
        xml.append(" ".repeat(indent))
                .append('<').append(name).append('>')
                .append(XmlText.escape(value))
                .append("</").append(name).append(">\n");
    }
}
