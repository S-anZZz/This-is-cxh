package com.cxh.blog.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cxh.blog.post.domain.PostStatus;
import com.cxh.blog.post.dto.PostForm;
import com.cxh.blog.post.service.PostService;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * RSS / sitemap / robots 输出测试。
 *
 * <p>关键断言不是字符串包含，而是<b>用 XML 解析器真的去解析一遍</b>：
 * 这类机器可读输出一旦有一处未转义就整体失效，只有解析通过才算合格。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FeedEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostService postService;

    @Test
    @DisplayName("feed.xml 是良构 XML，包含站点信息与文章条目")
    void feedIsWellFormedXml() throws Exception {
        postService.save(post("订阅测试文章 & <特殊字符> \"引号\"", "用于验证 XML 转义"));

        String xml = mockMvc.perform(get("/feed.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/rss+xml"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        Document document = parse(xml);
        assertThat(document.getDocumentElement().getNodeName()).isEqualTo("rss");
        assertThat(document.getElementsByTagName("item").getLength()).isGreaterThanOrEqualTo(1);
        assertThat(document.getElementsByTagName("title").item(0).getTextContent()).isNotBlank();

        // 转义正确性：& < > " 都必须在 XML 里以实体形式出现
        assertThat(xml).contains("&amp;").contains("&lt;特殊字符&gt;").contains("&quot;");
        assertThat(xml).doesNotContain("<特殊字符>");
    }

    @Test
    @DisplayName("sitemap.xml 是良构 XML，且包含文章与标签 URL")
    void sitemapIsWellFormedXml() throws Exception {
        var saved = postService.save(post("站点地图测试", "验证 sitemap 中的 URL"));

        String xml = mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/xml"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        Document document = parse(xml);
        assertThat(document.getDocumentElement().getNodeName()).isEqualTo("urlset");
        NodeList locations = document.getElementsByTagName("loc");
        assertThat(locations.getLength()).isGreaterThan(0);

        String firstUrl = locations.item(0).getTextContent();
        assertThat(firstUrl).startsWith("http").endsWith("/");
        String encodedSlug = org.springframework.web.util.UriUtils.encodePathSegment(
                saved.slug(), StandardCharsets.UTF_8);
        assertThat(xml).contains("/posts/" + encodedSlug);
        assertThat(xml).contains("<lastmod>");
    }

    @Test
    @DisplayName("sitemap 收录全部已发布文章，不受单页上限影响（回归 BUG-08）")
    void sitemapCoversEveryPublishedPost() throws Exception {
        // 造 60 篇（超过公开列表单页上限 50），全部必须在 sitemap 中
        String marker = "SITEMAP" + System.nanoTime();
        List<String> expected = new java.util.ArrayList<>();
        for (int i = 0; i < 60; i++) {
            PostForm form = post(marker + " 第 " + i + " 篇", "sitemap 全量收录回归");
            expected.add(postService.save(form).slug());
        }

        String xml = mockMvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        long matched = expected.stream()
                .filter(slug -> xml.contains(org.springframework.web.util.UriUtils
                        .encodePathSegment(slug, StandardCharsets.UTF_8)))
                .count();

        assertThat(matched)
                .as("60 篇文章应全部出现在 sitemap 中（旧实现只会收录前 50 篇）")
                .isEqualTo(60);
        // 同时确认服务层确实能拿到超过单页上限的全量清单
        assertThat(postService.listAllPublished().size()).isGreaterThanOrEqualTo(60);
    }

    @Test
    @DisplayName("robots.txt 指向 sitemap 并屏蔽后台")
    void robotsPointsToSitemap() throws Exception {
        mockMvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/plain"))
                .andExpect(content().string(containsString("Disallow: /admin/")))
                .andExpect(content().string(containsString("Sitemap: http")))
                .andExpect(content().string(containsString("/sitemap.xml")));
    }

    @Test
    @DisplayName("草稿不会出现在 feed 与 sitemap 中")
    void draftsAreNotSyndicated() throws Exception {
        PostForm draftForm = post("草稿不该被订阅 " + System.nanoTime(), "draft");
        draftForm.setStatus(PostStatus.DRAFT);
        var draft = postService.save(draftForm);

        mockMvc.perform(get("/feed.xml"))
                .andExpect(content().string(not(containsString(draft.title()))));
        mockMvc.perform(get("/sitemap.xml"))
                .andExpect(content().string(not(containsString(draft.slug()))));
    }

    private Document parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }

    private PostForm post(String title, String summary) {
        PostForm form = new PostForm();
        form.setTitle(title);
        form.setSummary(summary);
        form.setContentMarkdown("# 正文\n\n内容。" + title);
        form.setTagsText("订阅测试");
        form.setStatus(PostStatus.PUBLISHED);
        return form;
    }
}
