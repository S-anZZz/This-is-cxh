package com.cxh.blog.media.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cxh.blog.media.MediaProperties;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 图片上传接口的集成测试：鉴权、CSRF、真实上传与静态访问闭环。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ImageUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MediaProperties mediaProperties;

    @Test
    @DisplayName("未登录不能上传（重定向到登录页）")
    void anonymousCannotUpload() throws Exception {
        mockMvc.perform(multipart("/api/media/images").file(png("a.png", 120, 90)).with(csrf()))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("已登录但缺少 CSRF 令牌被拒绝")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void missingCsrfIsRejected() throws Exception {
        mockMvc.perform(multipart("/api/media/images").file(png("a.png", 120, 90)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("上传成功：返回 URL、缩略图、尺寸与可直接粘贴的 Markdown")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void uploadsImageAndReturnsMarkdown() throws Exception {
        String body = mockMvc.perform(multipart("/api/media/images")
                        .file(png("封面图.png", 900, 600))
                        .param("alt", "一张测试图")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.url").value(containsString("/uploads/")))
                .andExpect(jsonPath("$.data.thumbnailUrl").value(containsString("_thumb.jpg")))
                .andExpect(jsonPath("$.data.width").value(900))
                .andExpect(jsonPath("$.data.height").value(600))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).contains("[![一张测试图](/uploads/");
    }

    @Test
    @DisplayName("上传后的图片可通过 /uploads/** 公开访问（含缩略图）")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void uploadedImageIsPubliclyAccessible() throws Exception {
        String json = mockMvc.perform(multipart("/api/media/images")
                        .file(png("公开访问.png", 400, 300))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String url = extract(json, "\"url\":\"");
        String thumbUrl = extract(json, "\"thumbnailUrl\":\"");

        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("image/png"));
        mockMvc.perform(get(thumbUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("image/jpeg"));
    }

    @Test
    @DisplayName("非图片内容被拒绝并返回可读消息（JSON 形态）")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void rejectsNonImageContent() throws Exception {
        MockMultipartFile fake = new MockMultipartFile("file", "shell.png", "image/png",
                "#!/bin/sh\necho pwned\n".getBytes());

        mockMvc.perform(multipart("/api/media/images").file(fake).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(containsString("无法识别的图片内容")));
    }

    @Test
    @DisplayName("空文件被拒绝")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void rejectsEmptyFile() throws Exception {
        MockMultipartFile empty = new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        mockMvc.perform(multipart("/api/media/images").file(empty).with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("上传目录与 URL 前缀来自配置")
    void usesConfiguredLocation() {
        assertThat(mediaProperties.getUrlPrefix()).isEqualTo("/uploads");
        assertThat(mediaProperties.getLocation()).isNotBlank();
        assertThat(mediaProperties.getThumbnailSize()).isGreaterThan(0);
    }

    private String extract(String json, String key) {
        int start = json.indexOf(key);
        assertThat(start).as("响应中应包含 " + key).isGreaterThanOrEqualTo(0);
        int from = start + key.length();
        int end = json.indexOf('"', from);
        return json.substring(from, end);
    }

    private MockMultipartFile png(String name, int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(Color.GREEN);
            g.fillRect(0, 0, width, height);
        } finally {
            g.dispose();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return new MockMultipartFile("file", name, "image/png", out.toByteArray());
    }
}
