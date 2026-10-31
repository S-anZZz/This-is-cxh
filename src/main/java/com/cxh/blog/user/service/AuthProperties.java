package com.cxh.blog.user.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 登录相关配置。
 *
 * <p>{@code devShowCode}：没有配置 SMTP 时，邮箱验证码无处可发，
 * 开发/个人部署下把验证码直接回显在页面上以便自测；
 * 一旦接入真实邮件服务，应把它设为 false。
 */
@ConfigurationProperties(prefix = "cxh.auth")
public class AuthProperties {

    /** 验证码有效期（秒）。 */
    private int codeTtlSeconds = 300;

    /** 是否在页面上回显邮箱验证码（仅限没有邮件服务的场景）。 */
    private boolean devShowCode = true;

    public int getCodeTtlSeconds() {
        return codeTtlSeconds;
    }

    public void setCodeTtlSeconds(int codeTtlSeconds) {
        this.codeTtlSeconds = codeTtlSeconds;
    }

    public boolean isDevShowCode() {
        return devShowCode;
    }

    public void setDevShowCode(boolean devShowCode) {
        this.devShowCode = devShowCode;
    }
}
