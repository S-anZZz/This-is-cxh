package com.cxh.blog.user.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 注册表单。校验写在 DTO 上，控制器只负责编排流程。
 */
public class RegisterForm {

    @NotBlank(message = "请填写用户名")
    @Size(min = 3, max = 40, message = "用户名长度需在 3–40 之间")
    @Pattern(regexp = "^[A-Za-z0-9_\\-\\u4e00-\\u9fa5]+$", message = "用户名只能包含字母、数字、下划线、短横线或中文")
    private String username;

    @NotBlank(message = "请填写邮箱")
    @Email(message = "邮箱格式不正确")
    @Size(max = 160, message = "邮箱过长")
    private String email;

    @NotBlank(message = "请填写密码")
    @Size(min = 6, max = 60, message = "密码长度需在 6–60 之间")
    private String password;

    @NotBlank(message = "请再次输入密码")
    private String confirmPassword;

    @NotBlank(message = "请填写验证码")
    private String captcha;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public String getCaptcha() {
        return captcha;
    }

    public void setCaptcha(String captcha) {
        this.captcha = captcha;
    }

    public boolean passwordsMatch() {
        return password != null && password.equals(confirmPassword);
    }
}
