package com.cxh.blog.web;

import com.cxh.blog.post.service.PostService;
import com.cxh.blog.site.service.SiteProfileService;
import com.cxh.blog.web.feed.RssFeedWriter;
import com.cxh.blog.web.feed.SitemapWriter;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * 机器可读输出：RSS 订阅、sitemap、robots。
 *
 * <p>这三者都需要「站点档案 + 文章」的跨域数据，因此按约定的分层规则放在 web 装配层；
 * 序列化本身交给 {@link RssFeedWriter} / {@link SitemapWriter}，控制器只管取数与响应头。
 * 站点地址从当前请求推导，因此换域名无需改配置（反向代理下请开启
 * {@code server.forward-headers-strategy=framework}）。
 */
@Controller
public class FeedController {

    private static final int FEED_SIZE = 20;

    private final PostService postService;
    private final SiteProfileService siteProfileService;
    private final RssFeedWriter rssFeedWriter;
    private final SitemapWriter sitemapWriter;

    public FeedController(PostService postService,
                          SiteProfileService siteProfileService,
                          RssFeedWriter rssFeedWriter,
                          SitemapWriter sitemapWriter) {
        this.postService = postService;
        this.siteProfileService = siteProfileService;
        this.rssFeedWriter = rssFeedWriter;
        this.sitemapWriter = sitemapWriter;
    }

    @GetMapping(value = "/feed.xml", produces = "application/rss+xml;charset=UTF-8")
    @ResponseBody
    public String feed() {
        return rssFeedWriter.write(
                siteProfileService.current(),
                postService.recentPublished(FEED_SIZE),
                baseUrl());
    }

    @GetMapping(value = "/sitemap.xml", produces = "application/xml;charset=UTF-8")
    @ResponseBody
    public String sitemap() {
        // 站点地图必须是完整清单：用 listAllPublished 而不是取前 N 篇，
        // 否则文章数超过单页上限后会被静默漏收录（曾经的缺陷）
        return sitemapWriter.write(
                postService.listAllPublished(),
                postService.publishedTags(),
                baseUrl());
    }

    @GetMapping(value = "/robots.txt", produces = "text/plain;charset=UTF-8")
    @ResponseBody
    public String robots() {
        return """
                User-agent: *
                Allow: /
                Disallow: /admin/
                Disallow: /login
                Disallow: /h2-console/

                Sitemap: %s/sitemap.xml
                """.formatted(baseUrl());
    }

    private String baseUrl() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
    }
}
