package com.cxh.blog.media;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 本地磁盘图片存储实现。
 *
 * <p>安全要点（每条都对应一类真实攻击面）：
 * <ul>
 *   <li><b>文件名不参与落盘路径</b>：一律重命名为 UUID，杜绝 {@code ../} 路径穿越与覆盖已有文件；</li>
 *   <li><b>类型白名单</b>：只接受 jpeg/png/gif/webp，且以<b>真实解码结果</b>为准，
 *       不信任客户端声明的 Content-Type；</li>
 *   <li><b>大小上限</b>：超过 {@link ImageStorage#MAX_BYTES} 直接拒绝，避免写爆磁盘；</li>
 *   <li><b>落盘位置固定</b>：所有文件都在配置的根目录之内，最终路径再做一次规范化校验。</li>
 * </ul>
 *
 * <p>缩略图用 JDK 自带的 ImageIO + Graphics2D 生成，不引入第三方图像库。
 */
@Component
public class LocalImageStorage implements ImageStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalImageStorage.class);

    private static final DateTimeFormatter MONTH_DIR = DateTimeFormatter.ofPattern("yyyy/MM");
    private static final Set<String> ALLOWED = Set.of(ALLOWED_CONTENT_TYPES);

    private final MediaProperties properties;

    public LocalImageStorage(MediaProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean isAvailable() {
        try {
            Path root = root();
            Files.createDirectories(root);
            return Files.isWritable(root);
        } catch (IOException | RuntimeException ex) {
            log.warn("图片存储目录不可用：{}", ex.getMessage());
            return false;
        }
    }

    @Override
    public StoredImage store(InputStream in, String originalName, String contentType, String alt) {
        if (!properties.isUploadEnabled()) {
            throw new IllegalArgumentException("图片上传功能已关闭");
        }
        String declared = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT).trim();
        if (!ALLOWED.contains(declared)) {
            throw new IllegalArgumentException("不支持的图片类型：" + declared + "（仅支持 jpeg / png / gif / webp）");
        }

        try {
            byte[] bytes = in.readNBytes((int) MAX_BYTES + 1);
            if (bytes.length == 0) {
                throw new IllegalArgumentException("上传内容为空");
            }
            if (bytes.length > MAX_BYTES) {
                throw new IllegalArgumentException("图片超过 " + (MAX_BYTES / 1024 / 1024) + "MB 上限");
            }

            // 以真实解码结果为准：能解出图像才算图片，同时拿到宽高
            BufferedImage image = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
            if (image == null) {
                throw new IllegalArgumentException("无法识别的图片内容（可能不是图片或已损坏）");
            }

            String extension = extensionOf(declared);
            String key = LocalDate.now().format(MONTH_DIR) + "/"
                    + UUID.randomUUID().toString().replace("-", "") + "." + extension;
            Path target = resolveInsideRoot(key);
            Files.createDirectories(target.getParent());
            Files.copy(new java.io.ByteArrayInputStream(bytes), target, StandardCopyOption.REPLACE_EXISTING);

            String thumbKey = thumbnailKeyOf(key);
            writeThumbnail(image, resolveInsideRoot(thumbKey));

            return new StoredImage(key, urlOf(key), urlOf(thumbKey),
                    image.getWidth(), image.getHeight(), bytes.length, declared);
        } catch (IOException ex) {
            throw new UncheckedIOException("保存图片失败", ex);
        }
    }

    @Override
    public void delete(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolveInsideRoot(key));
            Files.deleteIfExists(resolveInsideRoot(thumbnailKeyOf(key)));
        } catch (IOException ex) {
            log.warn("删除图片失败：{}", key, ex);
        } catch (IllegalArgumentException ex) {
            // 非法键（路径穿越）在触碰文件系统之前就被拒绝；delete 常用于清理流程，
            // 因此按契约静默忽略，只记一条告警，不打断调用方
            log.warn("忽略非法的图片存储键：{}", key);
        }
    }

    // ---------------------------------------------------------------- 内部

    /** 生成缩略图：按最长边等比缩放，失败时降级为「无缩略图」（仍返回原图 URL）。 */
    private void writeThumbnail(BufferedImage source, Path target) throws IOException {
        int max = Math.max(64, properties.getThumbnailSize());
        int width = source.getWidth();
        int height = source.getHeight();
        double scale = Math.min(1.0, (double) max / Math.max(width, height));
        if (scale >= 1.0) {
            // 原图本来就比缩略图小，直接复制一份，保证 URL 始终可用
            Files.createDirectories(target.getParent());
            ImageIO.write(source, "jpg", target.toFile());
            return;
        }
        int targetWidth = Math.max(1, (int) Math.round(width * scale));
        int targetHeight = Math.max(1, (int) Math.round(height * scale));

        BufferedImage thumbnail = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = thumbnail.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        Files.createDirectories(target.getParent());
        ImageIO.write(thumbnail, "jpg", target.toFile());
    }

    private Path root() {
        return Path.of(properties.getLocation()).toAbsolutePath().normalize();
    }

    /**
     * 把存储键解析为根目录内的绝对路径。
     * 任何越界（路径穿越）都会被拒绝——这是最后一道防线。
     */
    private Path resolveInsideRoot(String key) {
        Path base = root();
        Path resolved = base.resolve(key).normalize();
        if (!resolved.startsWith(base)) {
            throw new IllegalArgumentException("非法的存储键：" + key);
        }
        return resolved;
    }

    private String urlOf(String key) {
        String prefix = properties.getUrlPrefix();
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        if (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return prefix + "/" + key;
    }

    /** 缩略图与原图同目录，文件名加 {@code _thumb} 后缀（统一转 jpg）。 */
    private String thumbnailKeyOf(String key) {
        int dot = key.lastIndexOf('.');
        String base = dot > 0 ? key.substring(0, dot) : key;
        return base + "_thumb.jpg";
    }

    private String extensionOf(String contentType) {
        return switch (contentType) {
            case "image/png" -> "png";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            default -> "jpg";
        };
    }
}
