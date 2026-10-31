package com.cxh.blog.user.service;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.SecureRandom;
import java.util.Locale;
import javax.imageio.ImageIO;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

/**
 * 图形验证码。
 *
 * <p>只做「拦住脚本批量注册」这一件事：
 * 答案存 session、用一次即失效、比较时忽略大小写。
 * 不引入第三方验证码服务，避免多一个外部依赖与隐私出口。
 */
@Service
public class CaptchaService {

    /** 去掉 0/O、1/I 这类容易看错的字符。 */
    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final String SESSION_KEY = "cxh.captcha.text";
    private static final int LENGTH = 4;
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;

    private final SecureRandom random = new SecureRandom();

    /** 生成新验证码图片，并把答案写进 session。 */
    public byte[] issue(HttpSession session) {
        StringBuilder text = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            text.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        session.setAttribute(SESSION_KEY, text.toString());
        return render(text.toString());
    }

    /**
     * 校验并立即作废（无论对错），防止同一张图被反复试。
     */
    public boolean verify(HttpSession session, String input) {
        Object expected = session.getAttribute(SESSION_KEY);
        session.removeAttribute(SESSION_KEY);
        if (expected == null || input == null) {
            return false;
        }
        return expected.toString().equals(input.trim().toUpperCase(Locale.ROOT));
    }

    private byte[] render(String text) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(0xF7F8FA));
            g.fillRect(0, 0, WIDTH, HEIGHT);

            // 干扰线：数量很少，够让脚本识别变难，又不影响人眼
            g.setStroke(new BasicStroke(1.2f));
            for (int i = 0; i < 5; i++) {
                g.setColor(new Color(random.nextInt(0x9F) + 0x60, random.nextInt(0x9F) + 0x60,
                        random.nextInt(0x9F) + 0x60));
                g.drawLine(random.nextInt(WIDTH), random.nextInt(HEIGHT),
                        random.nextInt(WIDTH), random.nextInt(HEIGHT));
            }

            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 26));
            for (int i = 0; i < text.length(); i++) {
                g.setColor(new Color(random.nextInt(0x66) + 0x33, random.nextInt(0x66) + 0x33,
                        random.nextInt(0x66) + 0x33));
                double angle = (random.nextDouble() - 0.5) * 0.5;
                int x = 14 + i * 26;
                int y = 30 + random.nextInt(4);
                g.rotate(angle, x, y);
                g.drawString(String.valueOf(text.charAt(i)), x, y);
                g.rotate(-angle, x, y);
            }
        } finally {
            g.dispose();
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("验证码图片生成失败", ex);
        }
    }
}
