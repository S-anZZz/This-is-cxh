package com.cxh.blog.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cxh.blog.post.domain.PostStatus;
import com.cxh.blog.post.dto.PostForm;
import com.cxh.blog.post.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 异常与边界输入的回归测试。
 *
 * <p>用例全部来自「完整性测试」阶段实际探测到的缺陷：非法参数曾经返回 500、
 * 超长正文曾经在写库时炸掉、错误页标题曾经把 {@code ?:} 当字面量打印出来。
 * 把它们固化成测试，防止回归。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ErrorHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostService postService;

    @Test
    @DisplayName("非法枚举参数返回 400 而不是 500")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void invalidEnumParameterIsClientError() throws Exception {
        mockMvc.perform(get("/admin/posts").param("status", "NOT_A_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("格式不正确")));
    }

    @Test
    @DisplayName("路径变量类型错误返回 400 而不是 500")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void invalidPathVariableIsClientError() throws Exception {
        mockMvc.perform(get("/admin/posts/not-a-number/edit"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("格式不正确")));
    }

    @Test
    @DisplayName("非法分页参数返回 400 而不是 500")
    void invalidPagingParameterIsClientError() throws Exception {
        mockMvc.perform(get("/posts").param("page", "abc"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("对只允许 POST 的地址发 GET 返回 405")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void unsupportedMethodReturns405() throws Exception {
        mockMvc.perform(get("/admin/posts/1/delete"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().string(containsString("不支持")));
    }

    @Test
    @DisplayName("错误页在拿不到站点档案时也能正确渲染标题（表达式不被当成字面量）")
    void errorPageRendersWithoutSiteProfile() throws Exception {
        String html = mockMvc.perform(get("/posts/definitely-missing"))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("<title>出错了").doesNotContain("null ·").doesNotContain("?:");
    }

    @Test
    @DisplayName("JSON 请求下的错误返回统一结构")
    void jsonClientGetsStructuredError() throws Exception {
        mockMvc.perform(get("/posts/definitely-missing").accept("application/json"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(content().string(containsString("\"success\":false")));
    }

    @Test
    @DisplayName("超长正文被表单校验拦下，不写库也不 500")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void oversizedContentIsRejectedByValidation() throws Exception {
        long before = postService.stats().total();

        mockMvc.perform(post("/admin/posts").with(csrf())
                        .param("title", "超长正文测试")
                        .param("contentMarkdown", "x".repeat(20_000))
                        .param("status", PostStatus.DRAFT.name()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("正文最长 15000 字")));

        assertThat(postService.stats().total()).isEqualTo(before);
    }

    @Test
    @DisplayName("非法状态值提交后回显友好提示，而非英文转换异常")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void invalidStatusShowsFriendlyMessage() throws Exception {
        mockMvc.perform(post("/admin/posts").with(csrf())
                        .param("title", "非法状态测试")
                        .param("contentMarkdown", "正文")
                        .param("status", "NOT_A_STATUS"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("请选择有效的文章状态")))
                .andExpect(content().string(not(containsString("Failed to convert"))));
    }

    @Test
    @DisplayName("响应头包含安全基线：nosniff、X-Frame-Options 与 CSP")
    void securityHeadersArePresent() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "SAMEORIGIN"))
                .andExpect(header().string("Content-Security-Policy", containsString("default-src 'self'")))
                .andExpect(header().string("Content-Security-Policy", containsString("object-src 'none'")));
    }

    @Test
    @DisplayName("非公开的 actuator 端点被默认拒绝策略挡住，健康检查可用")
    void nonPublicActuatorEndpointsAreDenied() throws Exception {
        mockMvc.perform(get("/actuator/env")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT 等未声明方法不会绕过 CSRF 防护")
    void unsafeMethodWithoutCsrfIsForbidden() throws Exception {
        mockMvc.perform(put("/posts")).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("后台模板不残留内联事件处理器（CSP 的前提）")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void noInlineEventHandlersInAdminTemplates() throws Exception {
        // 列表页只有存在文章时才会渲染删除表单，因此先确保有一篇文章
        PostForm seed = new PostForm();
        seed.setTitle("内联事件检查用文章 " + System.nanoTime());
        seed.setContentMarkdown("正文");
        seed.setStatus(PostStatus.DRAFT);
        postService.save(seed);

        String listPage = mockMvc.perform(get("/admin/posts"))
                .andReturn().getResponse().getContentAsString();
        String formPage = mockMvc.perform(get("/admin/posts/new"))
                .andReturn().getResponse().getContentAsString();

        assertThat(listPage).doesNotContain("onsubmit=").doesNotContain("onclick=")
                .contains("data-confirm=").contains("/js/admin-confirm.js");
        assertThat(formPage).doesNotContain("onsubmit=").doesNotContain("onclick=");
    }
}
