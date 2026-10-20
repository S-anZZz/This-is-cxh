package com.cxh.blog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * cxh 个人博客系统启动类。
 *
 * <p>分层约定（高内聚低耦合）：
 * <ul>
 *   <li>{@code common}    —— 与业务无关的通用能力（统一响应、异常、渲染、文本工具），不依赖任何业务包；</li>
 *   <li>{@code site}      —— 站点域（站点身份、简介、主题风格），自治的垂直切片；</li>
 *   <li>{@code post}      —— 文章域，自治的垂直切片，不依赖 site；</li>
 *   <li>{@code security}  —— 管理员认证与授权；</li>
 *   <li>{@code web}       —— 跨域的页面装配层（全局模型通知、首页、错误页）；</li>
 *   <li>{@code bootstrap} —— 启动期种子数据，是唯一允许同时依赖多个域的包。</li>
 * </ul>
 * 依赖方向始终向内：web/bootstrap → 业务域 → common。业务域之间不互相依赖，需要协作时经 web 层装配。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableJpaAuditing
public class CxhBlogApplication {

    public static void main(String[] args) {
        SpringApplication.run(CxhBlogApplication.class, args);
    }
}
