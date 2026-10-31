package com.cxh.blog.security;

import com.cxh.blog.user.domain.AppUser;
import com.cxh.blog.user.service.UserService;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 数据库账号认证。登录标识既可以是用户名，也可以是邮箱 ——
 * 表单只有一个输入框，这里统一解析，避免前端再做一个「用邮箱登录」的模式开关。
 */
@Service
public class BlogUserDetailsService implements UserDetailsService {

    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_USER = "ROLE_USER";

    private final UserService userService;

    public BlogUserDetailsService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        AppUser user = userService.findByLogin(login)
                .orElseThrow(() -> new UsernameNotFoundException("账号不存在：" + login));
        String role = user.getRole().isAdmin() ? ROLE_ADMIN : ROLE_USER;
        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(List.of(new SimpleGrantedAuthority(role)))
                .disabled(!user.isEnabled())
                .build();
    }
}
