package com.cxh.blog.user.service;

import com.cxh.blog.user.domain.AppUser;
import com.cxh.blog.user.domain.UserRole;
import com.cxh.blog.user.repository.UserRepository;
import java.util.Locale;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 账号服务：注册、按「用户名或邮箱」查询、管理员账号初始化。
 *
 * <p>唯一性校验放在这里给出可读提示，数据库的唯一约束仍是最后一道防线
 * （并发注册同名时由约束兜底，异常在控制器里转成友好错误）。
 */
@Service
@Transactional
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    public AppUser register(String username, String email, String rawPassword) {
        String name = username == null ? "" : username.trim();
        String mail = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (repository.existsByUsernameIgnoreCase(name)) {
            throw new IllegalArgumentException("这个用户名已经被占用了");
        }
        if (repository.existsByEmailIgnoreCase(mail)) {
            throw new IllegalArgumentException("这个邮箱已经注册过了");
        }
        return repository.save(AppUser.create(name, mail, passwordEncoder.encode(rawPassword), UserRole.USER));
    }

    /** 登录标识既可以是用户名，也可以是邮箱。 */
    @Transactional(readOnly = true)
    public Optional<AppUser> findByLogin(String login) {
        if (login == null || login.isBlank()) {
            return Optional.empty();
        }
        String value = login.trim();
        Optional<AppUser> byName = repository.findByUsernameIgnoreCase(value);
        return byName.isPresent() ? byName : repository.findByEmailIgnoreCase(value);
    }

    @Transactional(readOnly = true)
    public Optional<AppUser> findByEmail(String email) {
        return email == null ? Optional.empty() : repository.findByEmailIgnoreCase(email.trim());
    }

    @Transactional(readOnly = true)
    public Optional<AppUser> findById(Long id) {
        return id == null ? Optional.empty() : repository.findById(id);
    }

    /**
     * 确保管理员账号存在。只在缺失时创建，不覆盖已有密码 ——
     * 否则每次重启都会把用户改过的密码重置回配置值。
     */
    public void ensureAdmin(String username, String rawOrEncodedPassword) {
        if (repository.existsByUsernameIgnoreCase(username)) {
            return;
        }
        String hash = rawOrEncodedPassword != null && rawOrEncodedPassword.startsWith("$2")
                ? rawOrEncodedPassword
                : passwordEncoder.encode(rawOrEncodedPassword);
        AppUser admin = AppUser.create(username, username + "@local", hash, UserRole.ADMIN);
        admin.setDisplayName("站长");
        repository.save(admin);
    }
}
