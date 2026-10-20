package com.cxh.blog.post.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 标签集合的解析与规范化规则。标签是文章域的受约束取值集合，
 * 规则集中在此处，避免在控制器、模板、服务之间重复。
 */
public final class Tags {

    public static final int MAX_TAG_LENGTH = 30;
    public static final int MAX_TAG_COUNT = 8;

    /**
     * 支持逗号（中英文）、顿号、井号、分号与换行分隔。
     * 刻意不按空格切分，这样「Spring Boot」这类含空格的标签才能保持完整。
     */
    private static final String SEPARATOR = "[,，、#;；\\n\\r]+";

    private Tags() {
    }

    public static Set<String> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split(SEPARATOR))
                .map(Tags::normalize)
                .filter(tag -> !tag.isEmpty())
                .distinct()
                .limit(MAX_TAG_COUNT)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** 表单回填用：把集合还原为可编辑文本。 */
    public static String join(Iterable<String> tags) {
        if (tags == null) {
            return "";
        }
        List<String> list = new ArrayList<>();
        tags.forEach(list::add);
        return String.join("、", list);
    }

    public static String normalize(String tag) {
        if (tag == null) {
            return "";
        }
        String trimmed = tag.trim().replaceAll("\\s+", " ");
        return trimmed.length() > MAX_TAG_LENGTH ? trimmed.substring(0, MAX_TAG_LENGTH) : trimmed;
    }
}
