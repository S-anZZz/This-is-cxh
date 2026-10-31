package com.cxh.blog.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 站点账号（注册用户与管理员共用一张表）。
 *
 * <p>为什么管理员也在这张表：认证只走一条 {@code UserDetailsService} 链路，
 * 避免「配置里的管理员 + 数据库里的用户」两套身份并存导致的权限判断分裂。
 *
 * <p>密码只存 BCrypt 摘要，任何地方都不保存明文。
 */
@Entity
@Table(name = "app_user")
@EntityListeners(AuditingEntityListener.class)
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String username;

    @Column(nullable = false, unique = true, length = 160)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "display_name", length = 60)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    @Column(nullable = false)
    private boolean enabled = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    protected AppUser() {
        // JPA
    }

    public static AppUser create(String username, String email, String passwordHash, UserRole role) {
        AppUser user = new AppUser();
        user.username = username;
        user.email = email;
        user.passwordHash = passwordHash;
        user.role = role == null ? UserRole.USER : role;
        user.displayName = username;
        user.enabled = true;
        return user;
    }

    /** 对外展示用的名字：优先 displayName，其次用户名。 */
    public String displayName() {
        return displayName == null || displayName.isBlank() ? username : displayName;
    }

    public void changePassword(String newHash) {
        this.passwordHash = newHash;
    }

    public void disable() {
        this.enabled = false;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
