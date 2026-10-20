package com.cxh.blog.web.feed;

import com.cxh.blog.common.text.DateTimes;
import com.cxh.blog.common.text.XmlText;
import com.cxh.blog.post.dto.PostSummary;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

/**
 * sitemap.xml 输出。与 {@link RssFeedWriter} 一样只做序列化，
 * 需要哪些 URL 由调用方（装配层）决定。
 */
@Component
public class SitemapWriter {

    private static final List<String> STATIC_PATHS = List.of("/", "/posts", "/archive", "/tags", "/about");

    public String write(List<PostSummary> posts, List<String> tags, String baseUrl) {
        StringBuilder xml = new StringBuilder(2048);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<urlset xmlns=\"http://www.sitemaps.org/schemas/sitemap/0.9\">\n");

        for (String path : STATIC_PATHS) {
            url(xml, baseUrl + path, null);
        }
        for (String tag : tags) {
            url(xml, baseUrl + "/tags/" + UriUtils.encodePathSegment(tag, StandardCharsets.UTF_8), null);
        }
        for (PostSummary post : posts) {
            url(xml,
                    baseUrl + "/posts/" + UriUtils.encodePathSegment(post.slug(), StandardCharsets.UTF_8),
                    DateTimes.toIsoDate(post.publishedAt()));
        }

        xml.append("</urlset>\n");
        return xml.toString();
    }

    private void url(StringBuilder xml, String location, String lastModified) {
        xml.append("  <url>\n");
        xml.append("    <loc>").append(XmlText.escape(location)).append("</loc>\n");
        if (lastModified != null && !lastModified.isBlank()) {
            xml.append("    <lastmod>").append(XmlText.escape(lastModified)).append("</lastmod>\n");
        }
        xml.append("  </url>\n");
    }
}
