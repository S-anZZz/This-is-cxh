package com.cxh.blog.post.service;

import com.cxh.blog.common.api.PageResult;
import com.cxh.blog.post.domain.PostStatus;
import com.cxh.blog.post.dto.PostDetail;
import com.cxh.blog.post.dto.PostForm;
import com.cxh.blog.post.dto.PostStats;
import com.cxh.blog.post.dto.PostSummary;
import java.util.List;

/**
 * 文章域对外能力。控制器只依赖本接口，
 * 因此实现可以自由演进（增加缓存、切换渲染器、拆分读写模型），调用方零改动。
 */
public interface PostService {

    /* ---------- 公开侧 ---------- */

    PageResult<PostSummary> listPublished(int page, int size);

    PageResult<PostSummary> listPublishedByTag(String tag, int page, int size);

    List<PostSummary> recentPublished(int limit);

    /**
     * 全部已发布文章（用于站点地图等需要完整清单的场景）。
     *
     * <p>与 {@link #listPublished(int, int)} 的区别：后者面向页面分页，每页条数会被夹取到上限；
     * 本方法按批次循环取全量，只受一个防爆上限保护，因此文章数超过单页上限时不会漏收录。
     */
    List<PostSummary> listAllPublished();

    /** 按 slug 读取已发布文章，并累加一次阅读量。 */
    PostDetail getPublishedBySlug(String slug);

    /**
     * 在已发布文章中检索标题、摘要与正文。
     *
     * @param keyword 原始关键词；为空或只含符号时返回空结果
     */
    PageResult<PostSummary> search(String keyword, int page, int size);

    List<String> publishedTags();

    /* ---------- 后台侧 ---------- */

    PageResult<PostSummary> listForAdmin(int page, int size, PostStatus status);

    PostForm findForm(Long id);

    PostDetail save(PostForm form);

    void delete(Long id);

    PostStats stats();
}
