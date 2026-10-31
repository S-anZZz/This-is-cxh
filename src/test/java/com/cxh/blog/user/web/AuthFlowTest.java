package com.cxh.blog.user.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cxh.blog.like.repository.PostLikeRepository;
import com.cxh.blog.post.service.PostService;
import com.cxh.blog.user.repository.UserRepository;
import com.cxh.blog.user.service.VerificationCodeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 注册 / 登录 / 点赞 的端到端测试。
 *
 * <p>验证码是「答案存在会话里」的，所以测试先从 /captcha 取一次图，再读会话里的答案，
 * 而不是去猜 —— 这样测的是真实链路，而不是给测试开的后门。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowTest {

    private static final String CAPTCHA_KEY = "cxh.captcha.text";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostLikeRepository likeRepository;

    @Autowired
    private PostService postService;

    @Autowired
    private VerificationCodeService verificationCodeService;

    @AfterEach
    void cleanUp() {
        userRepository.findByUsernameIgnoreCase("newcomer").ifPresent(userRepository::delete);
    }

    @Test
    @DisplayName("注册页与验证码图片可匿名访问")
    void registerPageIsPublic() throws Exception {
        mockMvc.perform(get("/register")).andExpect(status().isOk());
        mockMvc.perform(get("/captcha")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("验证码正确时可以完成注册")
    void registersWithValidCaptcha() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(get("/captcha").session(session));
        String answer = (String) session.getAttribute(CAPTCHA_KEY);
        assertThat(answer).isNotBlank();

        mockMvc.perform(post("/register").session(session).with(csrf())
                        .param("username", "newcomer")
                        .param("email", "newcomer@example.com")
                        .param("password", "secret123")
                        .param("confirmPassword", "secret123")
                        .param("captcha", answer))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        assertThat(userRepository.findByUsernameIgnoreCase("newcomer")).isPresent();
    }

    @Test
    @DisplayName("验证码错误时注册被拒绝，且不落库")
    void rejectsWrongCaptcha() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(get("/captcha").session(session));

        mockMvc.perform(post("/register").session(session).with(csrf())
                        .param("username", "newcomer")
                        .param("email", "newcomer@example.com")
                        .param("password", "secret123")
                        .param("confirmPassword", "secret123")
                        .param("captcha", "ZZZZ"))
                .andExpect(status().isOk());

        assertThat(userRepository.findByUsernameIgnoreCase("newcomer")).isEmpty();
    }

    @Test
    @DisplayName("可以用邮箱登录（别名解析）")
    void logsInWithEmail() throws Exception {
        mockMvc.perform(post("/login").with(csrf())
                        .param("username", "admin@local")
                        .param("password", "test-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    @DisplayName("邮箱验证码可以直接登录")
    void logsInWithCode() throws Exception {
        String code = verificationCodeService.issue("admin@local");

        mockMvc.perform(post("/login/code").with(csrf())
                        .param("email", "admin@local")
                        .param("code", code))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin"));
    }

    @Test
    @DisplayName("点赞可切换：一次点赞、再次取消")
    void togglesLike() throws Exception {
        // 测试库不灌种子数据，这里先自建一篇已发布文章
        com.cxh.blog.post.dto.PostForm form = new com.cxh.blog.post.dto.PostForm();
        form.setTitle("点赞测试");
        form.setContentMarkdown("正文");
        form.setStatus(com.cxh.blog.post.domain.PostStatus.PUBLISHED);
        com.cxh.blog.post.dto.PostDetail post = postService.save(form);
        var admin = userRepository.findByUsernameIgnoreCase("admin").orElseThrow();

        mockMvc.perform(post("/posts/{slug}/like", post.slug())
                        .with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(likeRepository.countByPostId(post.id())).isEqualTo(1);

        mockMvc.perform(post("/posts/{slug}/like", post.slug())
                        .with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().is3xxRedirection());
        assertThat(likeRepository.countByPostId(post.id())).isZero();
        assertThat(admin.getId()).isNotNull();
    }

    @Test
    @DisplayName("未登录不能发文，会被引导到登录页")
    void anonymousCannotWrite() throws Exception {
        mockMvc.perform(post("/write").with(csrf())
                        .param("title", "匿名文章")
                        .param("contentMarkdown", "正文"))
                .andExpect(status().is3xxRedirection());
    }
}
