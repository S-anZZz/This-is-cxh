package com.cxh.blog.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

/**
 * 登录后按角色分流：管理员进后台，普通用户回首页。
 *
 * <p>不用 {@code defaultSuccessUrl("/admin", true)} 的原因：那会把普通用户也强行送去后台，
 * 再被 403 弹回来，体验很差。
 */
@Component
public class RoleBasedAuthSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());
        String target = roles.contains(BlogUserDetailsService.ROLE_ADMIN) ? "/admin" : "/";
        response.sendRedirect(request.getContextPath() + target);
    }
}
