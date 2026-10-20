package com.cxh.blog.media;

/**
 * 已存储图片的只读描述。
 *
 * @param key       存储键（相对路径，如 2026/10/a1b2c3d4.jpg），入库与删除都用它
 * @param url       可公开访问的 URL（如 /uploads/2026/10/a1b2c3d4.jpg）
 * @param thumbnailUrl 缩略图 URL（无缩略图时与 {@link #url} 相同）
 * @param width     像素宽
 * @param height    像素高
 * @param bytes     字节数
 * @param contentType 内容类型
 */
public record StoredImage(
        String key,
        String url,
        String thumbnailUrl,
        int width,
        int height,
        long bytes,
        String contentType) {

    /** 供 Markdown 使用的「缩略图套原图」写法：[![alt](thumb)](url) */
    public String toMarkdown(String alt) {
        return "[![" + alt + "](" + thumbnailUrl + ")](" + url + ")";
    }
}
