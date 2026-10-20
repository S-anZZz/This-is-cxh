package com.cxh.blog.post.dto;

/**
 * 后台概览用的文章统计。跨实体聚合结果以只读快照暴露，
 * 避免控制器接触仓储或实体。
 */
public record PostStats(long total, long published, long drafts, long views) {
}
