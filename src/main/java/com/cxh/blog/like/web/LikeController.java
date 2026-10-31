package com.cxh.blog.like.web;

import com.cxh.blog.like.repository.PostLikeRepository;
import com.cxh.blog.like.domain.PostLike;
import com.cxh.blog.post.domain.Post;
import com.cxh.blog.post.repository.PostRepository;
import com.cxh.blog.user.domain.AppUser;
import com.cxh.blog.user.service.UserService;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;

/**
 * 点赞：再点一次取消。
 *
 * <p>用 POST + 重定向（PRG 模式），避免刷新页面时重复提交；
 * 需要登录由安全配置统一拦截，这里只处理「谁点的」。
 */
@Controller
public class LikeController {

    private final PostRepository postRepository;
    private final PostLikeRepository likeRepository;
    private final UserService userService;

    public LikeController(PostRepository postRepository, PostLikeRepository likeRepository, UserService userService) {
        this.postRepository = postRepository;
        this.likeRepository = likeRepository;
        this.userService = userService;
    }

    @PostMapping("/posts/{slug}/like")
    public String toggle(@PathVariable String slug, Authentication authentication) {
        Post post = postRepository.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "文章不存在"));
        AppUser user = userService.findByLogin(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录"));
        if (likeRepository.existsByPostIdAndUserId(post.getId(), user.getId())) {
            likeRepository.deleteByPostIdAndUserId(post.getId(), user.getId());
        } else {
            likeRepository.save(PostLike.of(post.getId(), user.getId()));
        }
        return "redirect:/posts/" + slug;
    }
}
