package com.cxh.blog.user.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 没有配置邮件服务时的降级实现：只写日志，不假装发送成功。
 */
@Component
@ConditionalOnMissingBean(CodeSender.class)
public class LoggingCodeSender implements CodeSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingCodeSender.class);

    @Override
    public void send(String email, String code) {
        log.info("邮箱登录验证码 email={} code={}（未配置 SMTP，仅写日志）", email, code);
    }

    @Override
    public boolean deliversRealMail() {
        return false;
    }
}
