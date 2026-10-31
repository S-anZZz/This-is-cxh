package com.cxh.blog.like.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 一条点赞记录。唯一约束 (post_id, user_id) 在数据库层保证「一人一赞」，
 * 因此这里不需要在代码里做「是否已赞」的加锁判断。
 */
@Entity
@Table(name = "post_like")
public class PostLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at")
    private Instant createdAt;

    protected PostLike() {
        // JPA
    }

    public static PostLike of(Long postId, Long userId) {
        PostLike like = new PostLike();
        like.postId = postId;
        like.userId = userId;
        like.createdAt = Instant.now();
        return like;
    }

    public Long getPostId() {
        return postId;
    }

    public Long getUserId() {
        return userId;
    }
}
