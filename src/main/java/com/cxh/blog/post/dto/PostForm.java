package com.cxh.blog.post.dto;

import com.cxh.blog.post.domain.Post;
import com.cxh.blog.post.domain.PostDraft;
import com.cxh.blog.post.domain.PostStatus;
import com.cxh.blog.post.domain.Tags;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 文章编辑表单：后台界面与文章域之间的写入契约。
 * 校验注解在此声明，控制器无需手写任何字段校验代码。
 */
public class PostForm {

    private Long id;

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题最长 200 字")
    private String title;

    @Size(max = 220, message = "链接标识最长 220 字")
    private String slug;

    @Size(max = 500, message = "摘要最长 500 字")
    private String summary;

    /**
     * 长度上限按「最小公倍数」取：MySQL 的 {@code TEXT} 只有 64KB，
     * utf8mb4 下最多约 16000 个汉字，因此 15000 字是两端都安全的边界。
     * 不加这个约束的话，超长正文会在写库时抛异常（曾经的 500 缺陷）。
     */
    @Size(max = 15000, message = "正文最长 15000 字")
    private String contentMarkdown;

    @Size(max = 400, message = "标签最多 400 字")
    private String tagsText;

    @Size(max = 400, message = "封面地址最长 400 字")
    private String coverImageUrl;

    @NotNull(message = "请选择文章状态")
    private PostStatus status = PostStatus.DRAFT;

    public static PostForm blank() {
        PostForm form = new PostForm();
        form.status = PostStatus.PUBLISHED;
        return form;
    }

    /** 由实体回填编辑表单（含 Markdown 原文）。 */
    public static PostForm from(Post post) {
        PostForm form = new PostForm();
        form.id = post.getId();
        form.title = post.getTitle();
        form.slug = post.getSlug();
        form.summary = post.getSummary();
        form.contentMarkdown = post.getContentMarkdown();
        form.tagsText = Tags.join(post.getTags());
        form.coverImageUrl = post.getCoverImageUrl();
        form.status = post.getStatus();
        return form;
    }

    /** 表单 → 领域编辑意图。解析规则统一交给 Tags。 */
    public PostDraft toDraft() {
        return new PostDraft(title, slug, summary, contentMarkdown, Tags.parse(tagsText), coverImageUrl, status);
    }

    public boolean isNew() {
        return id == null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getContentMarkdown() {
        return contentMarkdown;
    }

    public void setContentMarkdown(String contentMarkdown) {
        this.contentMarkdown = contentMarkdown;
    }

    public String getTagsText() {
        return tagsText;
    }

    public void setTagsText(String tagsText) {
        this.tagsText = tagsText;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public PostStatus getStatus() {
        return status;
    }

    public void setStatus(PostStatus status) {
        this.status = status;
    }
}
