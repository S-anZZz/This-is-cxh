package com.cxh.blog.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cxh.blog.common.api.PageResult;
import com.cxh.blog.common.exception.ResourceNotFoundException;
import com.cxh.blog.post.domain.PostStatus;
import com.cxh.blog.post.dto.PostDetail;
import com.cxh.blog.post.dto.PostForm;
import com.cxh.blog.post.dto.PostSummary;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 文章域集成测试。类级事务让每个用例在独立的数据视图中运行，互不干扰。
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PostServiceTest {

    @Autowired
    private PostService postService;

    @Test
    @DisplayName("保存已发布文章：正文按 Markdown 渲染，摘要留空则自动截取")
    void savesAndRenders() {
        PostForm form = form("集成测试文章", PostStatus.PUBLISHED);
        form.setSummary(null);
        form.setContentMarkdown("# 标题\n\n这是对 **渲染** 的验证。");

        PostDetail saved = postService.save(form);

        assertThat(saved.id()).isNotNull();
        assertThat(saved.contentHtml()).contains("<h1>标题</h1>").contains("<strong>渲染</strong>");
        assertThat(saved.summary()).isNotBlank();
        assertThat(saved.publishedAtText()).isNotBlank();
        assertThat(saved.slug()).isEqualTo("集成测试文章");
    }

    @Test
    @DisplayName("同名标题自动获得唯一 slug，无需用户处理唯一键冲突")
    void keepsSlugUnique() {
        String first = postService.save(form("重复标题", PostStatus.PUBLISHED)).slug();
        String second = postService.save(form("重复标题", PostStatus.PUBLISHED)).slug();

        assertThat(first).isEqualTo("重复标题");
        assertThat(second).isEqualTo("重复标题-2");
    }

    @Test
    @DisplayName("草稿不出现在公开列表，也不能通过 slug 访问")
    void draftsArePrivate() {
        PostDetail draft = postService.save(form("未完成的草稿", PostStatus.DRAFT));

        assertThat(draft.draft()).isTrue();
        assertThat(postService.listPublished(0, 50).items())
                .extracting(PostSummary::slug)
                .doesNotContain(draft.slug());
        assertThat(postService.listForAdmin(0, 50, PostStatus.DRAFT).items())
                .extracting(PostSummary::slug)
                .contains(draft.slug());
        assertThatThrownBy(() -> postService.getPublishedBySlug(draft.slug()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("阅读量在详情页访问时累加")
    void countsViews() {
        PostDetail saved = postService.save(form("浏览量测试", PostStatus.PUBLISHED));

        postService.getPublishedBySlug(saved.slug());
        PostDetail second = postService.getPublishedBySlug(saved.slug());

        assertThat(second.views()).isEqualTo(2);
    }

    @Test
    @DisplayName("按标签检索与标签聚合")
    void filtersByTag() {
        PostForm tagged = form("标签检索文章", PostStatus.PUBLISHED);
        tagged.setTagsText("架构、Spring Boot, 架构");
        PostDetail saved = postService.save(tagged);

        assertThat(saved.tags()).containsExactly("架构", "Spring Boot");
        PageResult<PostSummary> byTag = postService.listPublishedByTag("架构", 0, 10);
        assertThat(byTag.items()).extracting(PostSummary::slug).contains(saved.slug());
        assertThat(postService.publishedTags()).contains("架构", "Spring Boot");
    }

    @Test
    @DisplayName("更新时间倒序分页，非法页码被夹取")
    void paginatesDefensively() {
        for (int i = 0; i < 5; i++) {
            postService.save(form("分页文章 " + i, PostStatus.PUBLISHED));
        }

        PageResult<PostSummary> firstPage = postService.listForAdmin(-3, 2, null);

        assertThat(firstPage.page()).isZero();
        assertThat(firstPage.size()).isEqualTo(2);
        assertThat(firstPage.items()).hasSize(2);
        assertThat(firstPage.total()).isGreaterThanOrEqualTo(5);
    }

    @Test
    @DisplayName("删除后不可再编辑")
    void deletesPost() {
        PostDetail saved = postService.save(form("待删除文章", PostStatus.DRAFT));

        postService.delete(saved.id());

        assertThatThrownBy(() -> postService.findForm(saved.id()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("编辑已有文章复用同一条记录并保持 slug 稳定")
    void updatesExisting() {
        PostDetail saved = postService.save(form("可编辑文章", PostStatus.DRAFT));

        PostForm edit = postService.findForm(saved.id());
        edit.setTitle("可编辑文章（改）");
        edit.setStatus(PostStatus.PUBLISHED);
        edit.setContentMarkdown("正文已更新");

        PostDetail updated = postService.save(edit);

        assertThat(updated.id()).isEqualTo(saved.id());
        assertThat(updated.title()).isEqualTo("可编辑文章（改）");
        assertThat(updated.slug()).isEqualTo(saved.slug());
        assertThat(updated.draft()).isFalse();
        assertThat(updated.publishedAtText()).isNotBlank();
    }

    @Test
    @DisplayName("搜索命中标题与正文，且只搜已发布文章")
    void searchesPublishedContent() {
        postService.save(form("搜索目标标题", PostStatus.PUBLISHED));
        PostForm body = form("另一篇普通文章", PostStatus.PUBLISHED);
        body.setContentMarkdown("这段正文里藏着关键字 长颈鹿");
        PostDetail hidden = postService.save(body);
        PostForm draftForm = form("草稿里的 长颈鹿", PostStatus.DRAFT);
        draftForm.setContentMarkdown("长颈鹿");
        PostDetail draft = postService.save(draftForm);

        assertThat(postService.search("搜索目标", 0, 10).items())
                .extracting(PostSummary::title).contains("搜索目标标题");
        assertThat(postService.search("长颈鹿", 0, 10).items())
                .extracting(PostSummary::slug)
                .contains(hidden.slug())
                .doesNotContain(draft.slug());
    }

    @Test
    @DisplayName("搜索关键词里的 LIKE 通配符被转义，不会退化成全表命中")
    void escapesSearchWildcards() {
        postService.save(form("通配符测试文章", PostStatus.PUBLISHED));

        assertThat(postService.search("%", 0, 10).items()).isEmpty();
        assertThat(postService.search("_", 0, 10).items()).isEmpty();
        assertThat(postService.search("   ", 0, 10).items()).isEmpty();
        assertThat(postService.search(null, 0, 10).items()).isEmpty();
    }

    private PostForm form(String title, PostStatus status) {
        PostForm form = new PostForm();
        form.setTitle(title);
        form.setContentMarkdown("正文内容");
        form.setTagsText("测试");
        form.setStatus(status);
        return form;
    }
}
