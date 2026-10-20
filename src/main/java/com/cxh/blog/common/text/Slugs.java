package com.cxh.blog.common.text;

import java.text.Normalizer;
import java.util.Locale;

/**
 * URL slug 生成工具。文章域使用它把标题转为可读链接，与 Web 层解耦。
 */
public final class Slugs {

    private Slugs() {
    }

    /**
     * 生成 slug：保留字母数字与连字符，中日韩字符原样保留，其余折叠为连字符。
     *
     * @param source 原始文本
     * @param fallback 兜底前缀（source 全为符号时使用）
     */
    public static String of(String source, String fallback) {
        if (source == null || source.isBlank()) {
            return fallback;
        }
        String normalized = Normalizer.normalize(source.trim(), Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
        String slug = normalized
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\p{IsHan}]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        if (slug.isBlank()) {
            return fallback;
        }
        return slug.length() > 80 ? slug.substring(0, 80).replaceAll("-+$", "") : slug;
    }
}
