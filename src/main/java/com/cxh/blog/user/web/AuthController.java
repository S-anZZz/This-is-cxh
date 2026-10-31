package com.cxh.blog.user.web;

import com.cxh.blog.user.domain.AppUser;
import com.cxh.blog.user.service.AuthProperties;
import com.cxh.blog.user.service.CaptchaService;
import com.cxh.blog.user.service.UserService;
import com.cxh.blog.user.service.VerificationCodeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 注册与「邮箱验证码登录」。
 *
 * <p>密码登录仍然交给 Spring Security 的过滤器（表单提交到 /login）；
 * 这里只处理两种走不了过滤器的场景：注册，以及验证码登录后手工建立会话。
 */
@Controller
public class AuthController {

    /** Spring Security 6 默认不再自动把手工认证写入会话，需要显式保存。 */
    private final HttpSessionSecurityContextRepository contextRepository =
            new HttpSessionSecurityContextRepository();

    private final UserService userService;
    private final CaptchaService captchaService;
    private final VerificationCodeService verificationCodeService;
    private final UserDetailsService userDetailsService;
    private final AuthProperties authProperties;

    public AuthController(UserService userService,
                          CaptchaService captchaService,
                          VerificationCodeService verificationCodeService,
                          UserDetailsService userDetailsService,
                          AuthProperties authProperties) {
        this.userService = userService;
        this.captchaService = captchaService;
        this.verificationCodeService = verificationCodeService;
        this.userDetailsService = userDetailsService;
        this.authProperties = authProperties;
    }

    @GetMapping("/captcha")
    public ResponseEntity<byte[]> captcha(HttpSession session) {
        byte[] png = captchaService.issue(session);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                // 验证码不允许任何缓存，否则浏览器会复用旧图
                .cacheControl(CacheControl.noStore().mustRevalidate())
                .body(png);
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new RegisterForm());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegisterForm form,
                           BindingResult binding,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        if (!form.passwordsMatch()) {
            binding.rejectValue("confirmPassword", "mismatch", "两次输入的密码不一致");
        }
        if (!binding.hasFieldErrors("captcha") && !captchaService.verify(session, form.getCaptcha())) {
            binding.rejectValue("captcha", "invalid", "验证码不正确或已过期");
        }
        if (binding.hasErrors()) {
            return "auth/register";
        }
        try {
            AppUser created = userService.register(form.getUsername(), form.getEmail(), form.getPassword());
            redirectAttributes.addFlashAttribute("registered", created.getUsername());
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            binding.rejectValue("username", "duplicate", ex.getMessage());
            return "auth/register";
        }
    }

    /** 请求邮箱验证码。为免暴露「哪些邮箱注册过」，无论是否存在都给出同样的提示。 */
    @PostMapping("/login/code/request")
    public String requestCode(@RequestParam String email, Model model, RedirectAttributes redirectAttributes) {
        String normalized = email == null ? "" : email.trim();
        if (normalized.isEmpty()) {
            redirectAttributes.addFlashAttribute("codeError", "请填写邮箱");
            return "redirect:/login";
        }
        if (userService.findByEmail(normalized).isEmpty()) {
            redirectAttributes.addFlashAttribute("codeSent", normalized);
            return "redirect:/login";
        }
        String code = verificationCodeService.issue(normalized);
        redirectAttributes.addFlashAttribute("codeSent", normalized);
        if (authProperties.isDevShowCode()) {
            // 没有 SMTP 时把验证码回显，方便个人部署自测
            redirectAttributes.addFlashAttribute("devCode", code);
        }
        return "redirect:/login";
    }

    /** 校验验证码并直接建立登录会话。 */
    @PostMapping("/login/code")
    public String loginWithCode(@RequestParam String email,
                                @RequestParam String code,
                                HttpServletRequest request,
                                HttpServletResponse response,
                                RedirectAttributes redirectAttributes) {
        if (!verificationCodeService.verify(email, code)) {
            redirectAttributes.addFlashAttribute("codeError", "验证码不正确或已过期");
            return "redirect:/login";
        }
        AppUser user = userService.findByEmail(email).orElse(null);
        if (user == null || !user.isEnabled()) {
            redirectAttributes.addFlashAttribute("codeError", "该邮箱暂不可登录");
            return "redirect:/login";
        }
        UserDetails details = userDetailsService.loadUserByUsername(user.getUsername());
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                details, null, details.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        // 防会话固定：换一个新的 session id 再保存认证
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }
        contextRepository.saveContext(context, request, response);
        return user.getRole().isAdmin() ? "redirect:/admin" : "redirect:/";
    }
}
