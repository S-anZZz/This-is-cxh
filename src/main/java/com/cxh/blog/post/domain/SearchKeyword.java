package com.cxh.blog.post.domain;

import java.util.Locale;

/**
 * 搜索关键字值对象。
 *
 * <p>把「用户输入 → LIKE 模式」的转换规则收敛在此处：
 * <ul>
 *   <li>长度截断，避免超长输入拖垮查询；</li>
 *   <li>转义 LIKE 通配符（{@code !} 为转义符，与仓储中的 {@code escape '!'} 对应），
 *       否则用户输入 {@code %} 会退化成全表命中；</li>
 *   <li>统一小写，与查询里的 {@code lower(...)} 对齐。</li>
 * </ul>
 * 仓储只接收已经规范化好的模式串，不再自行处理输入。
 */
public record SearchKeyword(String value, String pattern) {

    public static final int MAX_LENGTH = 60;

    /** LIKE 转义字符，必须与查询里的 escape 子句保持一致。 */
    public static final char ESCAPE_CHAR = '!';

    private static final SearchKeyword EMPTY = new SearchKeyword("", "%");

    public static SearchKeyword of(String raw) {
        if (raw == null || raw.isBlank()) {
            return EMPTY;
        }
        String normalized = raw.trim().replaceAll("\\s+", " ");
        if (normalized.length() > MAX_LENGTH) {
            normalized = normalized.substring(0, MAX_LENGTH);
        }
        String escaped = normalized
                .replace(String.valueOf(ESCAPE_CHAR), ESCAPE_CHAR + String.valueOf(ESCAPE_CHAR))
                .replace("%", ESCAPE_CHAR + "%")
                .replace("_", ESCAPE_CHAR + "_");
        return new SearchKeyword(normalized, "%" + escaped.toLowerCase(Locale.ROOT) + "%");
    }

    public boolean isEmpty() {
        return value.isEmpty();
    }

    /** 关键词是否至少包含一个可检索字符（避免只搜符号）。 */
    public boolean isMeaningful() {
        return value.chars().anyMatch(Character::isLetterOrDigit);
    }
}
