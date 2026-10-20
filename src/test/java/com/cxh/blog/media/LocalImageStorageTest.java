package com.cxh.blog.media;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 本地图片存储单元测试。
 *
 * <p>重点覆盖安全边界：文件名不参与落盘路径（防路径穿越）、以真实解码结果判定类型
 * （防伪造 Content-Type）、大小上限、以及缩略图生成。
 */
class LocalImageStorageTest {

    @TempDir
    Path tempDir;

    private MediaProperties properties;
    private LocalImageStorage storage;

    @BeforeEach
    void setUp() {
        properties = new MediaProperties();
        properties.setLocation(tempDir.toString());
        properties.setUrlPrefix("/uploads");
        properties.setThumbnailSize(200);
        storage = new LocalImageStorage(properties);
    }

    @Test
    @DisplayName("保存图片：重命名落盘、生成缩略图、返回可用 URL 与尺寸")
    void storesImageAndThumbnail() throws Exception {
        byte[] png = imageBytes(800, 600, "png");

        StoredImage stored = storage.store(new ByteArrayInputStream(png), "校园照片.png", "image/png", "校园");

        assertThat(stored.url()).startsWith("/uploads/").endsWith(".png");
        assertThat(stored.thumbnailUrl()).startsWith("/uploads/").endsWith("_thumb.jpg");
        assertThat(stored.width()).isEqualTo(800);
        assertThat(stored.height()).isEqualTo(600);
        assertThat(stored.bytes()).isEqualTo(png.length);

        // 落盘文件真实存在
        Path original = tempDir.resolve(stored.key());
        assertThat(original).exists();
        assertThat(tempDir.resolve(stored.key().replace(".png", "_thumb.jpg"))).exists();

        // 缩略图最长边被压到配置值
        BufferedImage thumb = ImageIO.read(tempDir.resolve(stored.key().replace(".png", "_thumb.jpg")).toFile());
        assertThat(Math.max(thumb.getWidth(), thumb.getHeight())).isEqualTo(200);
    }

    @Test
    @DisplayName("文件名里的路径穿越被忽略：一律重命名，不落到目录之外")
    void ignoresPathTraversalInFileName() throws Exception {
        byte[] png = imageBytes(120, 120, "png");

        StoredImage stored = storage.store(new ByteArrayInputStream(png),
                "../../../../etc/passwd.png", "image/png", "穿越测试");

        assertThat(stored.key()).doesNotContain("..").doesNotContain("passwd");
        Path resolved = tempDir.resolve(stored.key()).normalize();
        assertThat(resolved.startsWith(tempDir)).isTrue();
        assertThat(resolved).exists();
    }

    @Test
    @DisplayName("伪造的类型被拒绝：声明 image/png 但内容不是图片")
    void rejectsContentThatIsNotAnImage() {
        byte[] notAnImage = "#!/bin/sh\necho pwned\n".getBytes();

        assertThatThrownBy(() -> storage.store(new ByteArrayInputStream(notAnImage),
                "evil.png", "image/png", "恶意文件"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("无法识别的图片内容");
    }

    @Test
    @DisplayName("白名单之外的类型被拒绝")
    void rejectsDisallowedContentType() throws Exception {
        byte[] png = imageBytes(60, 60, "png");

        assertThatThrownBy(() -> storage.store(new ByteArrayInputStream(png),
                "a.svg", "image/svg+xml", "svg"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不支持的图片类型");
    }

    @Test
    @DisplayName("空内容被拒绝")
    void rejectsEmptyContent() {
        assertThatThrownBy(() -> storage.store(new ByteArrayInputStream(new byte[0]),
                "empty.png", "image/png", "空"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("为空");
    }

    @Test
    @DisplayName("超过大小上限被拒绝")
    void rejectsOversizedImage() {
        byte[] huge = new byte[(int) ImageStorage.MAX_BYTES + 1024];

        assertThatThrownBy(() -> storage.store(new ByteArrayInputStream(huge),
                "huge.png", "image/png", "超大"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("上限");
    }

    @Test
    @DisplayName("删除会同时清掉原图与缩略图；非法键不会越界删除")
    void deletesOriginalAndThumbnail() throws Exception {
        StoredImage stored = storage.store(new ByteArrayInputStream(imageBytes(300, 200, "png")),
                "x.png", "image/png", "删除测试");
        Path original = tempDir.resolve(stored.key());
        Path thumb = tempDir.resolve(stored.key().replace(".png", "_thumb.jpg"));
        assertThat(original).exists();
        assertThat(thumb).exists();

        storage.delete(stored.key());

        assertThat(original).doesNotExist();
        assertThat(thumb).doesNotExist();
        // 不抛异常
        storage.delete("../../etc/passwd");
        storage.delete(null);
    }

    @Test
    @DisplayName("小图不放大：缩略图与原图同尺寸仍可用")
    void keepsSmallImageSize() throws Exception {
        StoredImage stored = storage.store(new ByteArrayInputStream(imageBytes(80, 60, "png")),
                "small.png", "image/png", "小图");

        BufferedImage thumb = ImageIO.read(tempDir.resolve(stored.key().replace(".png", "_thumb.jpg")).toFile());
        assertThat(thumb.getWidth()).isEqualTo(80);
        assertThat(thumb.getHeight()).isEqualTo(60);
    }

    @Test
    @DisplayName("上传开关关闭时拒绝写入")
    void respectsUploadSwitch() throws Exception {
        properties.setUploadEnabled(false);

        assertThatThrownBy(() -> storage.store(new ByteArrayInputStream(imageBytes(50, 50, "png")),
                "a.png", "image/png", "关闭"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("已关闭");
    }

    @Test
    @DisplayName("Markdown 片段是「缩略图套原图」写法，与相册排版一致")
    void buildsGalleryMarkdown() throws Exception {
        StoredImage stored = storage.store(new ByteArrayInputStream(imageBytes(400, 300, "png")),
                "g.png", "image/png", "说明文字");

        assertThat(stored.toMarkdown("说明文字"))
                .isEqualTo("[![说明文字](" + stored.thumbnailUrl() + ")](" + stored.url() + ")");
    }

    @Test
    @DisplayName("存储目录可用性检查")
    void reportsAvailability() {
        assertThat(storage.isAvailable()).isTrue();
        assertThat(Files.isDirectory(tempDir)).isTrue();
    }

    private byte[] imageBytes(int width, int height, String format) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(Color.BLUE);
            g.fillRect(0, 0, width, height);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }
}
