package com.cxh.blog.media.web;

import com.cxh.blog.common.api.ApiResponse;
import com.cxh.blog.media.ImageStorage;
import com.cxh.blog.media.MediaProperties;
import com.cxh.blog.media.StoredImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 图片上传接口（仅管理员）。
 *
 * <p>放在 media 域内，只依赖 {@link ImageStorage} 抽象；因此换成本地磁盘、
 * 对象存储还是图床，本类都不需要改。路径以 /api 开头，异常会自动返回 JSON（见 GlobalExceptionHandler）。
 */
@RestController
@RequestMapping("/api/media")
public class ImageUploadController {

    private final ImageStorage imageStorage;
    private final MediaProperties properties;

    public ImageUploadController(ImageStorage imageStorage, MediaProperties properties) {
        this.imageStorage = imageStorage;
        this.properties = properties;
    }

    /**
     * 上传一张图片，返回可直接粘贴到文章里的 Markdown 片段。
     *
     * @param file 图片文件
     * @param alt  图片说明（可选，默认用文件名）
     */
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                                   @RequestParam(value = "alt", required = false) String alt) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请选择要上传的图片");
        }
        String description = (alt == null || alt.isBlank()) ? stripExtension(file.getOriginalFilename()) : alt.trim();

        StoredImage stored;
        try (var in = file.getInputStream()) {
            stored = imageStorage.store(in, file.getOriginalFilename(), file.getContentType(), description);
        } catch (IOException ex) {
            throw new UncheckedIOException("读取上传内容失败", ex);
        }

        return ApiResponse.ok("上传成功", Map.of(
                "url", stored.url(),
                "thumbnailUrl", stored.thumbnailUrl(),
                "width", stored.width(),
                "height", stored.height(),
                "bytes", stored.bytes(),
                "markdown", stored.toMarkdown(description)));
    }

    private String stripExtension(String name) {
        if (name == null || name.isBlank()) {
            return "图片";
        }
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        return base.isBlank() ? "图片" : base;
    }
}
