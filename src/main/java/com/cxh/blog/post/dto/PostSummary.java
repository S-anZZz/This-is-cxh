package com.cxh.blog.post.dto;

import com.cxh.blog.common.text.DateTimes;
import com.cxh.blog.post.domain.Post;
import com.cxh.blog.post.domain.PostStatus;
import java.time.Instant;
import java.util.List;

/**
 * 文章列表项视图。列表页需要的字段与详情页不同，
 * 用专门的读模型避免把正文带进列表查询（也避免模板拿到用不到的字段）。
 *
 * <p>同时提供原始 {@link Instant} 与预格式化文本：页面直接用文本，
 * RSS / sitemap 这类机器可读输出需要原始时间自行格式化。
 */
public record PostSummary(
        Long id,
        String title,
        String slug,
        String summary,
        List<String> tags,
        String coverImageUrl,
        PostStatus status,
        String statusLabel,
        Instant publishedAt,
        String publishedAtText,
        String updatedAtText,
        long views,
        int readingMinutes,
        boolean draft) {

    public static PostSummary from(Post post, String summary) {
        return new PostSummary(
                post.getId(),
                post.getTitle(),
                post.getSlug(),
                summary,
                List.copyOf(post.getTags()),
                post.getCoverImageUrl(),
                post.getStatus(),
                post.getStatus().getLabel(),
                post.getPublishedAt(),
                DateTimes.toDate(post.getPublishedAt()),
                DateTimes.toDate(post.getUpdatedAt()),
                post.getViews(),
                ReadingTimes.of(post.getContentMarkdown()),
                post.getStatus() == PostStatus.DRAFT);
    }
}
