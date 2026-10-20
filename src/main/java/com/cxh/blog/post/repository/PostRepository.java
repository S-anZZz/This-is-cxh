package com.cxh.blog.post.repository;

import com.cxh.blog.post.domain.Post;
import com.cxh.blog.post.domain.PostStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 文章仓储。公开查询与后台查询分开声明，方法名即契约，
 * 服务层无需拼接查询条件，换库时也只影响这一层。
 */
public interface PostRepository extends JpaRepository<Post, Long> {

    Optional<Post> findBySlug(String slug);

    Optional<Post> findBySlugAndStatus(String slug, PostStatus status);

    Page<Post> findByStatus(PostStatus status, Pageable pageable);

    Page<Post> findByStatusAndTagsContaining(PostStatus status, String tag, Pageable pageable);

    long countByStatus(PostStatus status);

    @Query("select coalesce(sum(p.views), 0) from Post p")
    long sumViews();

    /**
     * 标题 / 摘要 / 正文的模糊检索。模式串由 {@code SearchKeyword} 规范化并转义，
     * 转义字符必须与 escape 子句保持一致。
     */
    @Query("""
            select p from Post p
            where p.status = :status
              and (lower(p.title) like :pattern escape '!'
                   or lower(coalesce(p.summary, '')) like :pattern escape '!'
                   or lower(coalesce(p.contentMarkdown, '')) like :pattern escape '!')
            order by p.publishedAt desc
            """)
    Page<Post> searchByStatus(@Param("status") PostStatus status,
                              @Param("pattern") String pattern,
                              Pageable pageable);

    @Query("select distinct t from Post p join p.tags t where p.status = :status order by t")
    List<String> findDistinctTags(@Param("status") PostStatus status);
}
