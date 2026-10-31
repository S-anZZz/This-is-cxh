package com.cxh.blog.post.dto;

import com.cxh.blog.common.text.DateTimes;
import com.cxh.blog.post.domain.Post;
import com.cxh.blog.post.domain.PostStatus;
import java.util.List;

/**
 * 文章详情视图。{@code contentHtml} 由 Markdown 渲染器在装配阶段生成，
 * 模板只负责摆放，不关心渲染技术。
 */
public record PostDetail(
        Long id,
        String title,
        String slug,
        String summary,
        String contentHtml,
        List<String> tags,
        String coverImageUrl,
        PostStatus status,
        String statusLabel,
        String publishedAtText,
        String updatedAtText,
        long views,
        int readingMinutes,
        boolean draft) {

    public static PostDetail from(Post post, String summary, String contentHtml) {
        return new PostDetail(
                post.getId(),
                post.getTitle(),
                post.getSlug(),
                summary,
                contentHtml,
                List.copyOf(post.getTags()),
                post.getCoverImageUrl(),
                post.getStatus(),
                post.getStatus().getLabel(),
                DateTimes.toDate(post.getPublishedAt()),
                DateTimes.toDate(post.getUpdatedAt()),
                post.getViews(),
                ReadingTimes.of(post.getContentMarkdown()),
                post.getStatus() == PostStatus.DRAFT);
    }
}
