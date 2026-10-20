package com.cxh.blog.common.text;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 时间格式化统一入口。视图对象在装配阶段就完成格式化，
 * 模板不需要任何日期函数，输出格式也因此全局一致。
 */
public final class DateTimes {

    private static final DateTimeFormatter DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.SIMPLIFIED_CHINESE);
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.SIMPLIFIED_CHINESE);

    private DateTimes() {
    }

    public static String toDate(Instant instant) {
        return instant == null ? "" : DATE.format(instant.atZone(ZoneId.systemDefault()));
    }

    public static String toDateTime(Instant instant) {
        return instant == null ? "" : DATE_TIME.format(instant.atZone(ZoneId.systemDefault()));
    }

    /** 机器可读格式，用于 RSS / sitemap 等场景。 */
    public static String toIso(Instant instant) {
        return instant == null ? "" : DateTimeFormatter.ISO_INSTANT.format(instant);
    }

    /** RFC 1123 格式（UTC），RSS 的 pubDate / lastBuildDate 使用。 */
    public static String toRfc1123(Instant instant) {
        return instant == null
                ? ""
                : DateTimeFormatter.RFC_1123_DATE_TIME.format(instant.atZone(java.time.ZoneOffset.UTC));
    }

    /** 仅日期（本地时区），sitemap 的 lastmod 使用。 */
    public static String toIsoDate(Instant instant) {
        return toDate(instant);
    }
}
