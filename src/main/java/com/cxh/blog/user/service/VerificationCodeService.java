package com.cxh.blog.user.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 邮箱登录验证码。
 *
 * <p>代码里没有接真实邮件服务：没有 SMTP 配置时，验证码写日志并由页面回显（{@code cxh.auth.dev-show-code}），
 * 接入 JavaMailSender 后只需替换 {@link #deliver} 的实现，其它逻辑不受影响。
 *
 * <p>安全约束：5 分钟过期、最多试 5 次、验证成功立即作废；同一邮箱重复请求会覆盖旧码。
 */
@Service
public class VerificationCodeService {

    private static final Logger log = LoggerFactory.getLogger(VerificationCodeService.class);
    private static final int MAX_ATTEMPTS = 5;

    private final AuthProperties properties;
    private final CodeSender codeSender;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Entry> codes = new ConcurrentHashMap<>();

    public VerificationCodeService(AuthProperties properties, CodeSender codeSender) {
        this.properties = properties;
        this.codeSender = codeSender;
    }

    /** 是否走真实邮件：决定要不要把验证码回显到页面。 */
    public boolean deliversRealMail() {
        return codeSender.deliversRealMail();
    }

    /** 生成并投递验证码，返回明文（仅在开发模式下给页面回显用）。 */
    public String issue(String email) {
        String key = normalize(email);
        String code = String.format("%06d", random.nextInt(1_000_000));
        codes.put(key, new Entry(code, Instant.now().plusSeconds(properties.getCodeTtlSeconds())));
        deliver(email, code);
        return code;
    }

    public boolean verify(String email, String code) {
        String key = normalize(email);
        Entry entry = codes.get(key);
        if (entry == null) {
            return false;
        }
        if (Instant.now().isAfter(entry.expiresAt())) {
            codes.remove(key);
            return false;
        }
        if (entry.attempts() >= MAX_ATTEMPTS) {
            codes.remove(key);
            return false;
        }
        if (!entry.code().equals(code == null ? "" : code.trim())) {
            codes.put(key, new Entry(entry.code(), entry.expiresAt(), entry.attempts() + 1));
            return false;
        }
        codes.remove(key);
        return true;
    }

    private void deliver(String email, String code) {
        codeSender.send(email, code);
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private record Entry(String code, Instant expiresAt, int attempts) {
        Entry(String code, Instant expiresAt) {
            this(code, expiresAt, 0);
        }
    }
}
