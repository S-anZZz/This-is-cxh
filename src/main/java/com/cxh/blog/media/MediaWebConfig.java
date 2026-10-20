package com.cxh.blog.media;

import java.nio.file.Path;
import java.time.Duration;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 把上传目录挂到 HTTP 路径下（默认 /uploads/**）。
 *
 * <p>刻意不用 {@code static/} 目录：上传内容属于运行期数据，不应该打进 jar，
 * 也不应该和代码一起发布。路径前缀与 {@link MediaProperties#getUrlPrefix()} 保持一致。
 */
@Configuration
public class MediaWebConfig implements WebMvcConfigurer {

    private final MediaProperties properties;

    public MediaWebConfig(MediaProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String prefix = properties.getUrlPrefix();
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        if (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        Path root = Path.of(properties.getLocation()).toAbsolutePath().normalize();
        registry.addResourceHandler(prefix + "/**")
                .addResourceLocations(root.toUri().toString())
                .setCachePeriod((int) Duration.ofDays(30).toSeconds());
    }
}
