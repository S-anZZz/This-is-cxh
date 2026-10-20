package com.cxh.blog.media;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 图片上传配置。
 *
 * <p>默认把文件写到运行目录下的 uploads/，通过 /uploads/** 对外暴露。
 * 生产环境建议把 location 指到独立的数据盘或挂载卷，便于备份与扩容。
 */
@ConfigurationProperties(prefix = "cxh.media")
public class MediaProperties {

    /** 本地存储根目录。 */
    private String location = "./uploads";

    /** 对外访问前缀（需与 WebMvcConfig 中注册的资源处理器一致）。 */
    private String urlPrefix = "/uploads";

    /** 缩略图最长边（像素）。 */
    private int thumbnailSize = 560;

    /** 是否启用上传功能（关闭后上传接口返回 404 语义的提示）。 */
    private boolean uploadEnabled = true;

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getUrlPrefix() {
        return urlPrefix;
    }

    public void setUrlPrefix(String urlPrefix) {
        this.urlPrefix = urlPrefix;
    }

    public int getThumbnailSize() {
        return thumbnailSize;
    }

    public void setThumbnailSize(int thumbnailSize) {
        this.thumbnailSize = thumbnailSize;
    }

    public boolean isUploadEnabled() {
        return uploadEnabled;
    }

    public void setUploadEnabled(boolean uploadEnabled) {
        this.uploadEnabled = uploadEnabled;
    }
}
