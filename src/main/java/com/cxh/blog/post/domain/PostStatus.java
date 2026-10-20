package com.cxh.blog.post.domain;

/**
 * 文章状态。草稿只在后台可见，发布后才出现在公开页面。
 */
public enum PostStatus {

    DRAFT("草稿"),
    PUBLISHED("已发布");

    private final String label;

    PostStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static PostStatus of(String name, PostStatus fallback) {
        if (name == null || name.isBlank()) {
            return fallback;
        }
        for (PostStatus status : values()) {
            if (status.name().equalsIgnoreCase(name.trim())) {
                return status;
            }
        }
        return fallback;
    }
}
