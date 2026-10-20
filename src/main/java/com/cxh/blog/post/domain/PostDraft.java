package com.cxh.blog.post.domain;

import java.util.Set;

/**
 * 文章编辑意图：外部（控制器/种子数据）与文章实体之间的唯一写入契约。
 * 引入它之后，实体方法签名稳定，新增字段不会引发连锁改动。
 */
public record PostDraft(
        String title,
        String slug,
        String summary,
        String contentMarkdown,
        Set<String> tags,
        String coverImageUrl,
        PostStatus status) {
}
