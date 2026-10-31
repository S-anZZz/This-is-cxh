package com.cxh.blog.user.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * 配置了 {@code spring.mail.host} 时启用：通过 JavaMailSender 发真实邮件。
 *
 * <p>只在存在 JavaMailSender 时装配，因此没有邮件配置的部署不会因为缺 bean 启动失败。
 */
@Component
public class MailCodeSender implements CodeSender {

    private static final Logger log = LoggerFactory.getLogger(MailCodeSender.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String from;

    public MailCodeSender(ObjectProvider<JavaMailSender> mailSenderProvider,
                          @Value("${spring.mail.username:}") String from) {
        this.mailSenderProvider = mailSenderProvider;
        this.from = from;
    }

    @Override
    public void send(String email, String code) {
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            log.warn("邮件服务不可用，验证码未能发送 email={}", email);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        if (from != null && !from.isBlank()) {
            message.setFrom(from);
        }
        message.setTo(email);
        message.setSubject("登录验证码");
        message.setText("你的登录验证码是：" + code + "\n\n5 分钟内有效，请勿转发给他人。");
        try {
            sender.send(message);
            log.info("验证码邮件已发送 email={}", email);
        } catch (RuntimeException ex) {
            // 发信失败不能让请求 500：降级为日志，用户仍可从页面看到验证码（若开启了回显）
            log.warn("验证码邮件发送失败 email={}：{}", email, ex.getMessage());
        }
    }

    @Override
    public boolean deliversRealMail() {
        return mailSenderProvider.getIfAvailable() != null;
    }
}
