package com.cxh.blog.user.service;

import com.cxh.blog.security.AdminProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时确保管理员账号存在于数据库。
 *
 * <p>为什么不再用「配置里的单管理员」：既然要支持注册用户，身份就必须统一到一张表里，
 * 否则权限判断要同时照顾两套来源。账号口令仍来自配置，便于用环境变量覆盖。
 */
@Component
public class AdminAccountBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminAccountBootstrap.class);

    private final UserService userService;
    private final AdminProperties adminProperties;

    public AdminAccountBootstrap(UserService userService, AdminProperties adminProperties) {
        this.userService = userService;
        this.adminProperties = adminProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        userService.ensureAdmin(adminProperties.getUsername(), adminProperties.getPassword());
        log.info("管理员账号就绪：{}（密码来自 cxh.admin.password）", adminProperties.getUsername());
    }
}
