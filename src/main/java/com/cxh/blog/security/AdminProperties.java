package com.cxh.blog.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 管理员账号配置。生产环境请通过环境变量 {@code CXH_ADMIN_PASSWORD} 覆盖默认值。
 *
 * <p>{@code password} 既支持明文（启动时自动 BCrypt 加密），
 * 也支持直接填写 {@code $2a$...} 的 BCrypt 摘要，便于把凭据托管到密钥系统。
 */
@Validated
@ConfigurationProperties(prefix = "cxh.admin")
public class AdminProperties {

    @NotBlank
    private String username = "admin";

    @NotBlank
    private String password = "cxh@2024";

    private String displayName = "站长";

    public boolean passwordIsEncoded() {
        return password != null && (password.startsWith("$2a$") || password.startsWith("$2b$")
                || password.startsWith("$2y$") || password.startsWith("{bcrypt}"));
    }

    public String effectiveEncodedPassword(org.springframework.security.crypto.password.PasswordEncoder encoder) {
        String raw = password.startsWith("{bcrypt}") ? password.substring("{bcrypt}".length()) : password;
        return passwordIsEncoded() ? raw : encoder.encode(password);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
