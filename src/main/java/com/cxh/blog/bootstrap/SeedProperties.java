package com.cxh.blog.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 演示数据开关。首次启动写入示例文章，方便直接看到效果；
 * 置为 {@code false} 即可得到一个干净的博客。
 */
@ConfigurationProperties(prefix = "cxh.seed")
public class SeedProperties {

    private boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
