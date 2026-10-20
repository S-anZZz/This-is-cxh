package com.cxh.blog.bootstrap;

import static org.assertj.core.api.Assertions.assertThat;

import com.cxh.blog.post.service.PostService;
import com.cxh.blog.site.service.SiteProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 首次启动体验测试：种子数据必须真的写入，且站点档案必须自举成功。
 *
 * <p>刻意使用独立的数据库（与其他测试类隔离），并打开 {@code cxh.seed.enabled}，
 * 以覆盖「用户第一次 ./mvnw spring-boot:run 之后能看到什么」这条路径。
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:cxh-seed-test;DB_CLOSE_DELAY=-1",
        "cxh.seed.enabled=true"
})
@ActiveProfiles("test")
class SeedDataTest {

    @Autowired
    private PostService postService;

    @Autowired
    private SiteProfileService siteProfileService;

    @Test
    @DisplayName("首次启动写入 5 篇示例文章（含相册），且都处于已发布状态")
    void seedsSamplePosts() {
        var stats = postService.stats();

        assertThat(stats.published()).isEqualTo(5);
        assertThat(stats.drafts()).isZero();
        assertThat(postService.recentPublished(10))
                .extracting(summary -> summary.title())
                .contains("欢迎来到我的博客", "Markdown 写作速查", "相册 · 生活片段", "表情包与整活");
    }

    @Test
    @DisplayName("相册文章渲染出图片，且缩略图链接指向原图")
    void albumPostsRenderImages() {
        var album = postService.recentPublished(10).stream()
                .filter(summary -> summary.title().equals("相册 · 生活片段"))
                .findFirst()
                .orElseThrow();

        String html = postService.getPublishedBySlug(album.slug()).contentHtml();

        assertThat(html)
                .as("每张缩略图都应被一个指向原图的链接包住，这样点击才能看大图")
                .contains("href=\"/images/gallery/bc01.jpg\"")
                .contains("src=\"/images/gallery/thumbs/bc01.jpg\"")
                .contains("alt=\"校园路上的树影与自行车\"")
                .contains("href=\"/images/gallery/cxh05.jpg\"");
        assertThat(html).doesNotContain("src=\"/images/gallery/bc01.jpg\"");
    }

    @Test
    @DisplayName("示例文章可被检索、可生成订阅与站点地图内容")
    void seededContentIsDiscoverable() {
        assertThat(postService.search("Spring", 0, 10).items()).isNotEmpty();
        assertThat(postService.publishedTags()).contains("Spring Boot");
    }

    @Test
    @DisplayName("站点档案自举为默认值，主题与简介都可用")
    void initializesSiteProfile() {
        var site = siteProfileService.current();

        assertThat(site.siteTitle()).isNotBlank();
        assertThat(site.theme()).isNotNull();
        assertThat(site.introHtml()).isNotBlank();
        assertThat(siteProfileService.stylesheet()).contains("--cxh-primary:");
        assertThat(site.hasAvatar()).as("默认头像指向相册里的照片").isTrue();
        assertThat(site.authorAvatarUrl()).startsWith("/images/gallery/");
    }

    @Test
    @DisplayName("种子数据不重复写入（ApplicationRunner 幂等）")
    void seedIsIdempotent() {
        new BlogSeedData(siteProfileService, postService, enabledProperties()).run(null);

        assertThat(postService.stats().published()).isEqualTo(5);
    }

    private SeedProperties enabledProperties() {
        SeedProperties properties = new SeedProperties();
        properties.setEnabled(true);
        return properties;
    }
}
