package com.cxh.blog.post.web;

import java.util.Optional;
import com.cxh.blog.like.repository.PostLikeRepository;
import com.cxh.blog.post.dto.PostDetail;
import com.cxh.blog.post.service.PostService;
import com.cxh.blog.user.domain.AppUser;
import com.cxh.blog.user.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 文章公开页面。只依赖 {@link PostService} 接口，
 * 页面所需的一切（含渲染好的 HTML）都由服务层装配完毕。
 */
@Controller
public class PostPublicController {

    private static final int PAGE_SIZE = 8;
    private static final int SEARCH_PAGE_SIZE = 10;

    private final PostService postService;
    private final PostLikeRepository likeRepository;
    private final UserService userService;

    public PostPublicController(PostService postService,
                                PostLikeRepository likeRepository,
                                UserService userService) {
        this.postService = postService;
        this.likeRepository = likeRepository;
        this.userService = userService;
    }

    @GetMapping("/posts")
    public String list(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("pageResult", postService.listPublished(page, PAGE_SIZE));
        model.addAttribute("pageTitle", "全部文章");
        model.addAttribute("activeNav", "posts");
        model.addAttribute("tags", postService.publishedTags());
        return "posts";
    }

    @GetMapping("/posts/{slug}")
    public String detail(@PathVariable String slug, Model model, Authentication authentication) {
        PostDetail detail = postService.getPublishedBySlug(slug);
        model.addAttribute("post", detail);
        model.addAttribute("activeNav", "posts");
        model.addAttribute("likeCount", likeRepository.countByPostId(detail.id()));
        model.addAttribute("liked", currentUser(authentication)
                .map(user -> likeRepository.existsByPostIdAndUserId(detail.id(), user.getId()))
                .orElse(false));
        return "post";
    }

    private Optional<AppUser> currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        return userService.findByLogin(authentication.getName());
    }

    @GetMapping("/tags")
    public String tags(Model model) {
        model.addAttribute("tags", postService.publishedTags());
        model.addAttribute("activeNav", "tags");
        return "tags";
    }

    @GetMapping("/tags/{tag}")
    public String byTag(@PathVariable String tag,
                        @RequestParam(defaultValue = "0") int page,
                        Model model) {
        model.addAttribute("pageResult", postService.listPublishedByTag(tag, page, PAGE_SIZE));
        model.addAttribute("pageTitle", "标签：" + tag);
        model.addAttribute("activeNav", "tags");
        model.addAttribute("currentTag", tag);
        return "posts";
    }

    /**
     * 站内搜索。关键词原样回显给页面（渲染时由 Thymeleaf 转义），
     * 检索范围与转义规则都在文章域内部完成。
     */
    @GetMapping("/search")
    public String search(@RequestParam(required = false) String q,
                         @RequestParam(defaultValue = "0") int page,
                         Model model) {
        model.addAttribute("keyword", q == null ? "" : q.trim());
        model.addAttribute("pageResult", postService.search(q, page, SEARCH_PAGE_SIZE));
        model.addAttribute("activeNav", "search");
        return "search";
    }
}
