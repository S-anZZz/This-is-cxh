package com.cxh.blog.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 安全配置：公开只读页面，后台需要管理员会话。
 *
 * <p>采用「默认拒绝 + 白名单放行」策略：未在 {@link #PUBLIC_PATHS} 中声明的一律拒绝，
 * 新增页面时必须显式表态，避免出现意外裸露的接口。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** 公开访问路径（只读展示）。 */
    private static final String[] PUBLIC_PATHS = {
            "/", "/posts/**", "/tags/**", "/about", "/archive", "/search",
            "/theme.css", "/css/**", "/js/**", "/images/**", "/favicon.ico",
            "/uploads/**",
            "/feed.xml", "/sitemap.xml", "/robots.txt",
            "/login", "/register", "/captcha", "/login/code/**", "/error"
    };

    /** 需要管理员身份的后台/接口路径（上传接口走这里，而不是混进公开白名单）。 */
    private static final String[] ADMIN_PATHS = {"/admin/**", "/api/**"};

    /** 需要「已登录」但不限角色：发文与点赞。 */
    private static final String[] AUTHENTICATED_PATHS = {"/write", "/write/**", "/posts/*/like"};

    /** 开发用的 H2 控制台；生产库不启用该路径，因此不会被暴露。 */
    private static final String[] DEV_CONSOLE_PATHS = {"/h2-console/**"};

    /**
     * 内容安全策略。取舍说明：
     * <ul>
     *   <li>{@code script-src 'self'} —— 不放开 inline script，因此删除确认等交互放在外部 JS；
     *   <li>{@code style-src 'self' 'unsafe-inline'} —— 外观编辑器需要内联样式（取色器预览、
     *       主题变量注入），样式注入的危害远小于脚本；
     *   <li>{@code frame-src 'self'} 之外的 {@code frame-ancestors 'self'} 与
     *       {@code X-Frame-Options} 一起防点击劫持；
     *   <li>{@code img-src} 允许 https，使封面图可以引用外链图片。
     * </ul>
     */
    private static final String CONTENT_SECURITY_POLICY = String.join("; ",
            "default-src 'self'",
            "script-src 'self'",
            "style-src 'self' 'unsafe-inline'",
            "img-src 'self' data: https:",
            "font-src 'self' data:",
            "connect-src 'self'",
            "frame-src 'self'",
            "frame-ancestors 'self'",
            "form-action 'self'",
            "base-uri 'none'",
            "object-src 'none'");

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .requestMatchers(DEV_CONSOLE_PATHS).permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers(AUTHENTICATED_PATHS).authenticated()
                        .requestMatchers(ADMIN_PATHS).hasRole("ADMIN")
                        .anyRequest().denyAll())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/admin", true)
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))
                .csrf(csrf -> csrf.ignoringRequestMatchers(DEV_CONSOLE_PATHS))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.sameOrigin())
                        .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY)));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
