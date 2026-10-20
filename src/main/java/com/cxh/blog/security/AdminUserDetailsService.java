package com.cxh.blog.security;

import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 单管理员认证。账号来自配置而非数据库，
 * 因此这个博客没有任何「用户管理」的额外复杂度，也就不需要用户表。
 */
@Service
public class AdminUserDetailsService implements UserDetailsService {

    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final AdminProperties properties;
    private final PasswordEncoder passwordEncoder;

    public AdminUserDetailsService(AdminProperties properties, PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (!properties.getUsername().equals(username)) {
            throw new UsernameNotFoundException("账号不存在：" + username);
        }
        return User.withUsername(properties.getUsername())
                .password(properties.effectiveEncodedPassword(passwordEncoder))
                .authorities(List.of(new SimpleGrantedAuthority(ROLE_ADMIN)))
                .build();
    }

    public String displayName() {
        return properties.getDisplayName();
    }
}
