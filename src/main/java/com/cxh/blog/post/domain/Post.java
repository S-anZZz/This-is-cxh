package com.cxh.blog.post.domain;

import com.cxh.blog.common.text.Slugs;
import com.cxh.blog.user.domain.AppUser;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 文章聚合根。
 *
 * <p>只保存 Markdown 原文，不保存渲染结果：渲染器可随时替换、升级后立即生效，
 * 数据层与展示层因此彻底解耦。
 */
@Entity
@Table(name = "post")
@EntityListeners(AuditingEntityListener.class)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, unique = true, length = 220)
    private String slug;

    @Column(length = 500)
    private String summary;

    /** 正文大文本：列类型交给方言决定（MySQL → longtext，H2 → character large object）。 */
    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "content_markdown")
    private String contentMarkdown;

    @Column(name = "cover_image_url", length = 400)
    private String coverImageUrl;

    /*
     * 显式声明为 VARCHAR：Hibernate 6 默认会让 H2/MySQL 建原生 enum 列，
     * 那样 schema 不可移植、新增枚举常量还得 ALTER TABLE。
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private PostStatus status = PostStatus.DRAFT;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "post_tag", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "tag", length = Tags.MAX_TAG_LENGTH)
    private Set<String> tags = new LinkedHashSet<>();

    /** 作者：注册用户发表的文章会记录作者；后台与种子数据可为空。 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private AppUser author;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(nullable = false)
    private long views;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    protected Post() {
        // JPA
    }

    public static Post create(PostDraft draft) {
        Post post = new Post();
        post.applyEdit(draft);
        return post;
    }

    /**
     * 应用一次编辑。slug、标签、状态与首发时间都在这里被规范化，
     * 保证任何入口写入的数据都满足同一套不变量。
     */
    public void applyEdit(PostDraft draft) {
        this.title = require(draft.title(), "标题不能为空");
        this.slug = Slugs.of(hasText(draft.slug()) ? draft.slug() : draft.title(), "post-" + fallbackId());
        this.summary = hasText(draft.summary()) ? draft.summary().trim() : null;
        this.contentMarkdown = draft.contentMarkdown();
        this.coverImageUrl = hasText(draft.coverImageUrl()) ? draft.coverImageUrl().trim() : null;
        this.tags = draft.tags() == null ? new LinkedHashSet<>() : new LinkedHashSet<>(draft.tags());
        PostStatus target = draft.status() == null ? PostStatus.DRAFT : draft.status();
        if (target == PostStatus.PUBLISHED && this.publishedAt == null) {
            this.publishedAt = Instant.now();
        }
        this.status = target;
    }

    /** 首次访问时补记发布时间，避免草稿转发布后排序错乱。 */
    public void ensurePublishedAt(Instant when) {
        if (this.publishedAt == null) {
            this.publishedAt = when;
        }
    }

    public void incrementViews() {
        this.views++;
    }

    public boolean isPublished() {
        return this.status == PostStatus.PUBLISHED;
    }

    private String fallbackId() {
        return this.id == null ? Long.toString(System.nanoTime()) : Long.toString(this.id);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String require(String value, String message) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSlug() {
        return slug;
    }

    public String getSummary() {
        return summary;
    }

    public String getContentMarkdown() {
        return contentMarkdown;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public PostStatus getStatus() {
        return status;
    }

    public Set<String> getTags() {
        return tags;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public long getViews() {
        return views;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public AppUser getAuthor() {
        return author;
    }

    /** 只允许在新建时指定作者，避免编辑时被改写。 */
    public void assignAuthor(AppUser author) {
        if (this.author == null) {
            this.author = author;
        }
    }
}
