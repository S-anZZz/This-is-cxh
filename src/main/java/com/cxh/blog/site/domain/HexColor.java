package com.cxh.blog.site.domain;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 颜色值对象。风格数据最终会被写入 {@code text/css} 响应，
 * 因此所有颜色都必须经过严格白名单校验，杜绝 CSS 注入。
 */
public final class HexColor {

    /** 供 Bean Validation 复用的正则。 */
    public static final String PATTERN = "^#(?:[0-9a-fA-F]{3}|[0-9a-fA-F]{6})$";

    private static final Pattern COMPILED = Pattern.compile(PATTERN);

    private HexColor() {
    }

    public static boolean isValid(String raw) {
        return raw != null && COMPILED.matcher(raw.trim()).matches();
    }

    /**
     * 规范化：非法值回退到默认值，输出统一为小写 {@code #rrggbb}。
     */
    public static String normalize(String raw, String fallback) {
        String candidate = isValid(raw) ? raw.trim() : fallback;
        if (!isValid(candidate)) {
            return "#000000";
        }
        String value = candidate.toLowerCase(Locale.ROOT);
        if (value.length() == 4) {
            char r = value.charAt(1);
            char g = value.charAt(2);
            char b = value.charAt(3);
            return "#" + r + r + g + g + b + b;
        }
        return value;
    }

    /**
     * 计算给定颜色的相对亮度（0=黑，1=白），用于自动决定文字对比色。
     */
    public static double luminance(String raw) {
        String value = normalize(raw, "#ffffff");
        double r = channel(value, 1);
        double g = channel(value, 3);
        double b = channel(value, 5);
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private static double channel(String hex, int offset) {
        int parsed = Integer.parseInt(hex.substring(offset, offset + 2), 16);
        double scaled = parsed / 255.0;
        return scaled <= 0.03928 ? scaled / 12.92 : Math.pow((scaled + 0.055) / 1.055, 2.4);
    }
}
