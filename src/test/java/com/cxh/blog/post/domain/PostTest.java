package com.cxh.blog.post.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 文章域的不变量测试：标签解析、slug 生成、发布状态与阅读量。
 */
class PostTest {

    @Test
    @DisplayName("标签支持中文顿号、逗号与井号分隔，并去重限长")
    void parsesTags() {
        assertThat(Tags.parse("Spring Boot、架构, 设计 # 随笔"))
                .containsExactly("Spring Boot", "架构", "设计", "随笔");
        assertThat(Tags.parse("a,a,a")).hasSize(1);
        assertThat(Tags.parse("")).isEmpty();
        assertThat(Tags.parse(null)).isEmpty();
        // 超过上限的标签被截断
        assertThat(Tags.parse("x".repeat(80)).iterator().next()).hasSize(Tags.MAX_TAG_LENGTH);
    }

    @Test
    @DisplayName("保存草稿不会产生发布时间，转为发布时才补记")
    void publishSetsTimestamp() {
        Post post = Post.create(new PostDraft("草稿标题", null, null, "正文", Set.of(), null, PostStatus.DRAFT));
        assertThat(post.isPublished()).isFalse();
        assertThat(post.getPublishedAt()).isNull();

        post.applyEdit(new PostDraft("草稿标题", null, null, "正文", Set.of(), null, PostStatus.PUBLISHED));
        assertThat(post.isPublished()).isTrue();
        assertThat(post.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("slug 由标题生成，中英文都能得到可读链接")
    void generatesSlug() {
        Post chinese = Post.create(new PostDraft("高内聚低耦合的实践", null, null, "", Set.of(), null, PostStatus.DRAFT));
        assertThat(chinese.getSlug()).isEqualTo("高内聚低耦合的实践");

        Post english = Post.create(new PostDraft("Hello Spring Boot 3!", null, null, "", Set.of(), null, PostStatus.DRAFT));
        assertThat(english.getSlug()).isEqualTo("hello-spring-boot-3");
    }

    @Test
    @DisplayName("无标题的文章被拒绝，异常在领域层就抛出")
    void rejectsBlankTitle() {
        assertThatThrownBy(() -> Post.create(new PostDraft("  ", null, null, "", Set.of(), null, PostStatus.DRAFT)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("标题");
    }

    @Test
    @DisplayName("阅读量单调递增")
    void incrementsViews() {
        Post post = Post.create(new PostDraft("标题", null, null, "", Set.of(), null, PostStatus.PUBLISHED));
        post.incrementViews();
        post.incrementViews();
        assertThat(post.getViews()).isEqualTo(2);
    }
}
