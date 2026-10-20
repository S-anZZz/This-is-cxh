package com.cxh.blog.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cxh.blog.post.domain.PostStatus;
import com.cxh.blog.post.dto.PostForm;
import com.cxh.blog.post.service.PostService;
import com.cxh.blog.site.dto.SiteProfileForm;
import com.cxh.blog.site.service.SiteProfileService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 公开页面冒烟测试：所有对外的只读页面必须可渲染、可访问，
 * 而后台必须被拦截。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicPagesTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostService postService;

    @Autowired
    private SiteProfileService siteProfileService;

    private String publishedSlug;

    @BeforeEach
    void prepareContent() {
        if (publishedSlug == null) {
            publishedSlug = postService.save(post("冒烟测试文章", "用于验证详情页渲染", List.of("测试"))).slug();
        }
    }

    @Test
    @DisplayName("首页渲染站点档案与文章列表")
    void homePageRenders() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("最新文章")))
                .andExpect(content().string(containsString("theme.css")));
    }

    @Test
    @DisplayName("主题样式表由服务端生成，且带 no-store 缓存头")
    void themeCssIsGenerated() throws Exception {
        mockMvc.perform(get("/theme.css"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/css"))
                .andExpect(content().string(containsString("--cxh-primary:")))
                .andExpect(content().string(containsString("--cxh-content-width:")))
                .andExpect(result -> org.assertj.core.api.Assertions
                        .assertThat(result.getResponse().getHeader("Cache-Control")).contains("no-store"));
    }

    @Test
    @DisplayName("文章详情页渲染 Markdown 正文")
    void postDetailRendersMarkdown() throws Exception {
        mockMvc.perform(get("/posts/{slug}", publishedSlug))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("冒烟测试文章")))
                .andExpect(content().string(containsString("<h1>正文标题</h1>")));
    }

    @Test
    @DisplayName("列表、标签、归档、关于页均可访问")
    void listPagesRender() throws Exception {
        mockMvc.perform(get("/posts")).andExpect(status().isOk())
                .andExpect(content().string(containsString("全部文章")));
        mockMvc.perform(get("/tags")).andExpect(status().isOk())
                .andExpect(content().string(containsString("全部标签")));
        mockMvc.perform(get("/archive")).andExpect(status().isOk())
                .andExpect(content().string(containsString("归档")));
        mockMvc.perform(get("/about")).andExpect(status().isOk())
                .andExpect(content().string(containsString("联系")));
    }

    @Test
    @DisplayName("搜索页可匿名访问，命中结果与关键词回显都正确")
    void searchPageRenders() throws Exception {
        mockMvc.perform(get("/search"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("搜索范围是已发布文章")));

        mockMvc.perform(get("/search").param("q", "冒烟测试"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("冒烟测试文章")))
                .andExpect(content().string(containsString("找到")));

        mockMvc.perform(get("/search").param("q", "不存在的关键词xyz"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("没有匹配")));
    }

    @Test
    @DisplayName("关键词中的 HTML 被转义，不会形成反射型 XSS")
    void searchKeywordIsEscaped() throws Exception {
        mockMvc.perform(get("/search").param("q", "<script>alert(1)</script>"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("<script>alert(1)</script>"))));
    }

    @Test
    @DisplayName("样式级联顺序：兜底样式在前、主题样式在后，否则编辑后的风格会被兜底值覆盖")
    void stylesheetCascadeOrderIsGuarded() throws Exception {
        String html = mockMvc.perform(get("/")).andReturn().getResponse().getContentAsString();

        int fallbackIndex = html.indexOf("/css/blog.css");
        int themeIndex = html.indexOf("/theme.css");
        org.assertj.core.api.Assertions.assertThat(fallbackIndex)
                .as("blog.css 必须先于 theme.css 出现")
                .isNotNegative()
                .isLessThan(themeIndex);
    }

    @Test
    @DisplayName("配了头像时首页与关于页渲染头像，没配则不渲染空图")
    void rendersAuthorAvatar() throws Exception {
        SiteProfileForm form = siteProfileService.currentForm();
        form.setAuthorAvatarUrl("/images/gallery/cxh03.jpg");
        siteProfileService.update(form);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("class=\"hero__avatar\"")))
                .andExpect(content().string(containsString("src=\"/images/gallery/cxh03.jpg\"")));
        mockMvc.perform(get("/about"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("hero__avatar--lg")));

        SiteProfileForm cleared = siteProfileService.currentForm();
        cleared.setAuthorAvatarUrl("");
        siteProfileService.update(cleared);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("hero__avatar"))));
    }

    @Test
    @DisplayName("相册图片与缩略图随包提供且可直接访问")
    void albumImagesAreServed() throws Exception {
        for (String path : List.of("/images/gallery/bc01.jpg", "/images/gallery/thumbs/bc01.jpg",
                "/images/gallery/ysgn.jpg", "/images/gallery/thumbs/NL1.jpg")) {
            mockMvc.perform(get(path))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith("image/jpeg"));
        }
    }

    @Test
    @DisplayName("不存在的文章返回 404 页面而不是堆栈")
    void missingPostReturns404Page() throws Exception {
        mockMvc.perform(get("/posts/definitely-not-exists"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("404")));
    }

    @Test
    @DisplayName("后台未登录一律跳转登录页")
    void adminRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(get("/admin/appearance"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("登录页可匿名访问并带 CSRF 保护")
    void loginPageIsPublic() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"username\"")))
                .andExpect(content().string(containsString("_csrf")));
    }

    private PostForm post(String title, String summary, List<String> tags) {
        PostForm form = new PostForm();
        form.setTitle(title);
        form.setSummary(summary);
        form.setTagsText(String.join(",", tags));
        form.setContentMarkdown("# 正文标题\n\n这是 **Markdown** 正文。");
        form.setStatus(PostStatus.PUBLISHED);
        return form;
    }
}
