package com.cxh.blog.like.repository;

import com.cxh.blog.like.domain.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    long countByPostId(Long postId);

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    /** 派生删除必须在事务里执行，否则 Spring Data 会抛「No EntityManager with actual transaction」。 */
    @Transactional
    void deleteByPostIdAndUserId(Long postId, Long userId);
}
