package com.cxh.blog.user.service;

/**
 * 验证码投递方式。
 *
 * <p>两种实现按配置自动选择：配了 {@code spring.mail.host} 就发真实邮件，
 * 否则降级为写日志（并把验证码回显到页面，便于个人部署自测）。
 * 业务代码只依赖这个接口，换短信、换服务商都不用改调用方。
 */
public interface CodeSender {

    void send(String email, String code);

    /** 是否真的发出去了。用于决定要不要把验证码回显在页面上。 */
    boolean deliversRealMail();
}
