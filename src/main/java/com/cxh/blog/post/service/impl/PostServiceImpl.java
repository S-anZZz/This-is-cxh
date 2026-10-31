package com.cxh.blog.post.service.impl;

import com.cxh.blog.common.api.PageResult;
import com.cxh.blog.common.exception.ResourceNotFoundException;
import com.cxh.blog.common.render.MarkdownRenderer;
import com.cxh.blog.common.text.Slugs;
import com.cxh.blog.post.domain.Post;
import com.cxh.blog.post.domain.PostDraft;
import com.cxh.blog.post.domain.PostStatus;
import com.cxh.blog.post.domain.SearchKeyword;
import com.cxh.blog.post.dto.PostDetail;
import com.cxh.blog.post.dto.PostForm;
import com.cxh.blog.post.dto.PostStats;
import com.cxh.blog.post.dto.PostSummary;
import com.cxh.blog.post.repository.PostRepository;
import com.cxh.blog.post.service.PostService;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 文章域实现。职责边界清晰：
 * <ul>
 *   <li>仓储只负责查询，实体只负责不变量，本类只负责编排与视图装配；</li>
 *   <li>Markdown 渲染通过 {@link MarkdownRenderer} 抽象注入，替换实现无需改代码。</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int DEFAULT_SUMMARY_LENGTH = 140;
    private static final int MAX_SLUG_ATTEMPTS = 100;

    /** 全量清单（站点地图）的防爆上限，避免异常数据把响应撑爆。 */
    private static final int MAX_BULK_ENTRIES = 50_000;

    /** 全量清单的分批大小，取仓储层允许的单页上限。 */
    private static final int BULK_BATCH_SIZE = MAX_PAGE_SIZE;

    private final PostRepository postRepository;
    private final MarkdownRenderer markdownRenderer;

    public PostServiceImpl(PostRepository postRepository, MarkdownRenderer markdownRenderer) {
        this.postRepository = postRepository;
        this.markdownRenderer = markdownRenderer;
    }

    @Override
    public PageResult<PostSummary> listPublished(int page, int size) {
        Pageable pageable = pageable(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        return toPage(postRepository.findByStatus(PostStatus.PUBLISHED, pageable));
    }

    @Override
    public PageResult<PostSummary> listPublishedByTag(String tag, int page, int size) {
        Pageable pageable = pageable(page, size, Sort.by(Sort.Direction.DESC, "publishedAt"));
        return toPage(postRepository.findByStatusAndTagsContaining(PostStatus.PUBLISHED, tag, pageable));
    }

    @Override
    public List<PostSummary> recentPublished(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, MAX_PAGE_SIZE));
        Pageable pageable = PageRequest.of(0, safeLimit, Sort.by(Sort.Direction.DESC, "publishedAt"));
        return postRepository.findByStatus(PostStatus.PUBLISHED, pageable).getContent().stream()
                .map(this::toSummary)
                .toList();
    }

    /**
     * 分批取全量已发布文章。刻意不直接复用 {@code recentPublished}：
     * 后者会把条数夹取到单页上限，站点地图用它就会静默漏收录（曾经的缺陷）。
     */
    @Override
    public List<PostSummary> listAllPublished() {
        List<PostSummary> all = new ArrayList<>(Math.min(256, MAX_BULK_ENTRIES));
        int page = 0;
        Page<Post> chunk;
        do {
            Pageable pageable = PageRequest.of(page, BULK_BATCH_SIZE,
                    Sort.by(Sort.Direction.DESC, "publishedAt"));
            chunk = postRepository.findByStatus(PostStatus.PUBLISHED, pageable);
            chunk.getContent().forEach(post -> all.add(toSummary(post)));
            page++;
        } while (chunk.hasNext() && all.size() < MAX_BULK_ENTRIES);
        return all.size() > MAX_BULK_ENTRIES ? all.subList(0, MAX_BULK_ENTRIES) : all;
    }

    @Override
    @Transactional
    public PostDetail getPublishedBySlug(String slug) {
        Post post = postRepository.findBySlugAndStatus(slug, PostStatus.PUBLISHED)
                .orElseThrow(() -> new ResourceNotFoundException("文章不存在或尚未发布：" + slug));
        post.incrementViews();
        return toDetail(post);
    }

    @Override
    public PageResult<PostSummary> search(String keyword, int page, int size) {
        SearchKeyword parsed = SearchKeyword.of(keyword);
        if (parsed.isEmpty() || !parsed.isMeaningful()) {
            // 空关键词不查库，直接返回空页，避免 "%%" 全表扫描
            return PageResult.of(List.of(), Math.max(0, page), size <= 0 ? DEFAULT_PAGE_SIZE : size, 0);
        }
        Pageable pageable = pageable(page, size, Sort.unsorted());
        return toPage(postRepository.searchByStatus(PostStatus.PUBLISHED, parsed.pattern(), pageable));
    }

    @Override
    public List<String> publishedTags() {
        return postRepository.findDistinctTags(PostStatus.PUBLISHED);
    }

    @Override
    public PageResult<PostSummary> listForAdmin(int page, int size, PostStatus status) {
        Pageable pageable = pageable(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<Post> result = status == null
                ? postRepository.findAll(pageable)
                : postRepository.findByStatus(status, pageable);
        return toPage(result);
    }

    @Override
    public PostForm findForm(Long id) {
        return postRepository.findById(id)
                .map(PostForm::from)
                .orElseThrow(() -> new ResourceNotFoundException("文章不存在：" + id));
    }

    @Override
    @Transactional
    public PostDetail save(PostForm form) {
        PostDraft draft = form.toDraft();
        Post post;
        if (form.isNew()) {
            post = Post.create(withUniqueSlug(draft, null));
        } else {
            post = postRepository.findById(form.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("文章不存在：" + form.getId()));
            post.applyEdit(withUniqueSlug(draft, post.getId()));
        }
        Post saved = postRepository.save(post);
        return toDetail(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!postRepository.existsById(id)) {
            throw new ResourceNotFoundException("文章不存在：" + id);
        }
        postRepository.deleteById(id);
    }

    @Override
    public PostStats stats() {
        return new PostStats(
                postRepository.count(),
                postRepository.countByStatus(PostStatus.PUBLISHED),
                postRepository.countByStatus(PostStatus.DRAFT),
                postRepository.sumViews());
    }

    /**
     * 保证 slug 全局唯一：冲突时递增后缀，避免用户手动处理唯一键异常。
     */
    private PostDraft withUniqueSlug(PostDraft draft, Long selfId) {
        String base = Slugs.of(hasText(draft.slug()) ? draft.slug() : draft.title(), "post");
        String candidate = base;
        for (int attempt = 2; attempt <= MAX_SLUG_ATTEMPTS; attempt++) {
            Optional<Post> existing = postRepository.findBySlug(candidate);
            if (existing.isEmpty() || existing.get().getId().equals(selfId)) {
                return new PostDraft(draft.title(), candidate, draft.summary(), draft.contentMarkdown(),
                        draft.tags(), draft.coverImageUrl(), draft.status());
            }
            candidate = base + "-" + attempt;
        }
        candidate = base + "-" + System.nanoTime();
        return new PostDraft(draft.title(), candidate, draft.summary(), draft.contentMarkdown(),
                draft.tags(), draft.coverImageUrl(), draft.status());
    }

    private PageResult<PostSummary> toPage(Page<Post> page) {
        return PageResult.of(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
                this::toSummary);
    }

    private PostSummary toSummary(Post post) {
        return PostSummary.from(post, resolveSummary(post));
    }

    private PostDetail toDetail(Post post) {
        return PostDetail.from(post, resolveSummary(post), markdownRenderer.toHtml(post.getContentMarkdown()));
    }

    /**
     * 摘要留空时按正文自动截取。列表与详情共用同一规则，
     * 避免「同一篇文章在不同页面摘要不一致」。
     */
    private String resolveSummary(Post post) {
        return hasText(post.getSummary())
                ? post.getSummary()
                : markdownRenderer.toPlainSummary(post.getContentMarkdown(), DEFAULT_SUMMARY_LENGTH);
    }

    private Pageable pageable(int page, int size, Sort sort) {
        int safeSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(0, page), safeSize, sort);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
