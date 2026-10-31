package com.cxh.blog.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 管理员认证单元测试（不启动 Spring 容器）。
 *
 * <p>覆盖三种配置形态：明文密码、BCrypt 摘要、{bcrypt} 前缀摘要，
 * 以及未知账号必须被拒绝。
 */
class AdminUserDetailsServiceTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Test
    @DisplayName("明文密码在运行时被加密，且能通过校验")
    void encodesPlainPassword() {
        AdminProperties properties = properties("admin", "plain-secret");
        AdminUserDetailsService service = new AdminUserDetailsService(properties, encoder);

        String encoded = service.loadUserByUsername("admin").getPassword();

        assertThat(encoded).startsWith("$2");
        assertThat(encoder.matches("plain-secret", encoded)).isTrue();
        assertThat(service.displayName()).isEqualTo("站长");
    }

    @Test
    @DisplayName("直接配置 BCrypt 摘要时原样使用")
    void acceptsPreEncodedPassword() {
        String hash = encoder.encode("already-hashed");
        AdminProperties properties = properties("cxh", hash);
        AdminUserDetailsService service = new AdminUserDetailsService(properties, encoder);

        String password = service.loadUserByUsername("cxh").getPassword();

        assertThat(password).isEqualTo(hash);
        assertThat(service.loadUserByUsername("cxh").getAuthorities())
                .extracting(Object::toString)
                .containsExactly(AdminUserDetailsService.ROLE_ADMIN);
    }

    @Test
    @DisplayName("带 {bcrypt} 前缀的摘要也能识别")
    void acceptsBcryptPrefixedPassword() {
        AdminProperties properties = properties("admin", "{bcrypt}" + encoder.encode("prefixed"));
        assertThat(properties.passwordIsEncoded()).isTrue();
        assertThat(properties.effectiveEncodedPassword(encoder)).startsWith("$2");
    }

    @Test
    @DisplayName("账号不匹配时抛出 UsernameNotFoundException")
    void rejectsUnknownUser() {
        AdminUserDetailsService service = new AdminUserDetailsService(properties("admin", "x"), encoder);

        assertThatThrownBy(() -> service.loadUserByUsername("someone-else"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("someone-else");
    }

    private AdminProperties properties(String username, String password) {
        AdminProperties properties = new AdminProperties();
        properties.setUsername(username);
        properties.setPassword(password);
        return properties;
    }
}
