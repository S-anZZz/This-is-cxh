package com.cxh.blog.media;

import java.io.InputStream;

/**
 * 图片存储能力抽象。
 *
 * <p>把「图片存到哪里」与「博客怎么用图片」解耦：默认实现写本地磁盘，
 * 未来换成对象存储（OSS / COS / S3）只需提供另一个实现类，控制器与模板零改动。
 */
public interface ImageStorage {

    /** 允许的图片类型白名单（同时用于校验与扩展名推断）。 */
    String[] ALLOWED_CONTENT_TYPES = {"image/jpeg", "image/png", "image/gif", "image/webp"};

    /** 单张图片的大小上限（字节）。 */
    long MAX_BYTES = 8L * 1024 * 1024;

    /**
     * 保存一张图片，并生成缩略图。
     *
     * @param in          图片内容流
     * @param originalName 原始文件名（仅用于推断扩展名与展示，不参与落盘路径）
     * @param contentType 声明的内容类型
     * @param alt         图片说明（用于生成可直接粘贴的 Markdown）
     * @throws IllegalArgumentException 类型不在白名单、为空、或超过大小上限
     */
    StoredImage store(InputStream in, String originalName, String contentType, String alt);

    /**
     * 删除一张图片（同时删除其缩略图）。键不存在时静默返回。
     */
    void delete(String key);

    /** 当前存储是否可用（例如本地目录不可写时返回 false）。 */
    boolean isAvailable();
}
