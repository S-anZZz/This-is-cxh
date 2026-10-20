package com.cxh.blog.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 后台端到端测试：外观编辑器可用、预览走同一条渲染链路、
 * 保存后前台样式表立即变化。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "admin", roles = "ADMIN")
class AdminWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("概览页汇总文章统计")
    void dashboardRenders() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("文章总数")))
                .andExpect(content().string(containsString("当前外观")));
    }

    @Test
    @DisplayName("外观编辑器渲染出全部风格控件与实时预览挂载点")
    void appearanceEditorRendersControls() throws Exception {
        mockMvc.perform(get("/admin/appearance"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("预置主题")))
                .andExpect(content().string(containsString("id=\"primaryColor\"")))
                .andExpect(content().string(containsString("id=\"headingFont\"")))
                .andExpect(content().string(containsString("id=\"baseFontSize\"")))
                .andExpect(content().string(containsString("id=\"customCss\"")))
                .andExpect(content().string(containsString("id=\"introMarkdown\"")))
                .andExpect(content().string(containsString("id=\"preview-frame\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    @DisplayName("预览接口返回 CSS 且不写库")
    void previewReturnsCssWithoutPersisting() throws Exception {
        mockMvc.perform(post("/admin/appearance/preview").with(csrf())
                        .param("preset", "ELEGANT_LIGHT")
                        .param("primaryColor", "#abcdef")
                        .param("accentColor", "#123123")
                        .param("pageBackground", "#ffffff")
                        .param("surfaceBackground", "#ffffff")
                        .param("textColor", "#111111")
                        .param("mutedTextColor", "#888888")
                        .param("headingFont", "MODERN_SERIF")
                        .param("bodyFont", "SYSTEM_SANS")
                        .param("baseFontSize", "18")
                        .param("contentWidth", "820")
                        .param("cornerRadius", "10")
                        .param("stickyHeader", "true")
                        .param("showHero", "true"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/css"))
                .andExpect(content().string(containsString("--cxh-primary: #abcdef;")));

        mockMvc.perform(get("/theme.css"))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("--cxh-primary: #abcdef;"))));
    }

    @Test
    @DisplayName("保存外观后，前台 /theme.css 立即反映新风格")
    void savingAppearanceAffectsPublicTheme() throws Exception {
        mockMvc.perform(post("/admin/appearance").with(csrf())
                        .param("siteTitle", "cxh 的深夜食堂")
                        .param("siteSubtitle", "写代码，也写生活")
                        .param("authorName", "cxh")
                        .param("introMarkdown", "## 你好\n\n这里是新简介。")
                        .param("preset", "MIDNIGHT_DARK")
                        .param("primaryColor", "#22d3ee")
                        .param("accentColor", "#a78bfa")
                        .param("pageBackground", "#0b1220")
                        .param("surfaceBackground", "#131a26")
                        .param("textColor", "#e5e7eb")
                        .param("mutedTextColor", "#94a3b8")
                        .param("headingFont", "MODERN_SERIF")
                        .param("bodyFont", "HUMANIST_SANS")
                        .param("baseFontSize", "18")
                        .param("contentWidth", "860")
                        .param("cornerRadius", "20")
                        .param("customCss", ".post-card { border: 0; }"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/appearance"));

        mockMvc.perform(get("/theme.css"))
                .andExpect(content().string(containsString("--cxh-primary: #22d3ee;")))
                .andExpect(content().string(containsString("--cxh-font-size: 18px;")))
                .andExpect(content().string(containsString("--cxh-radius: 20px;")))
                .andExpect(content().string(containsString(".post-card { border: 0; }")));

        mockMvc.perform(get("/"))
                .andExpect(content().string(containsString("cxh 的深夜食堂")))
                .andExpect(content().string(containsString("这里是新简介。")));
    }

    @Test
    @DisplayName("非法颜色提交被表单校验拦下，不会污染主题")
    void invalidColorIsRejectedByValidation() throws Exception {
        mockMvc.perform(post("/admin/appearance").with(csrf())
                        .param("siteTitle", "标题")
                        .param("authorName", "cxh")
                        .param("preset", "ELEGANT_LIGHT")
                        .param("primaryColor", "red;}")
                        .param("headingFont", "MONO")
                        .param("bodyFont", "MONO")
                        .param("baseFontSize", "17")
                        .param("contentWidth", "780")
                        .param("cornerRadius", "14"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("主色需为")));
    }

    @Test
    @DisplayName("文章编辑器的 Markdown 预览与发布共用同一渲染器")
    void markdownPreviewUsesSameRenderer() throws Exception {
        mockMvc.perform(post("/admin/posts/preview").with(csrf())
                        .param("contentMarkdown", "# 标题\n\n- 一\n- 二"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<h1>标题</h1>")))
                .andExpect(content().string(containsString("<li>一</li>")));
    }

    @Test
    @DisplayName("后台文章列表与新建页可访问")
    void postAdminPagesRender() throws Exception {
        mockMvc.perform(get("/admin/posts")).andExpect(status().isOk())
                .andExpect(content().string(containsString("写新文章")));
        mockMvc.perform(get("/admin/posts/new")).andExpect(status().isOk())
                .andExpect(content().string(containsString("contentMarkdown")));
    }
}
