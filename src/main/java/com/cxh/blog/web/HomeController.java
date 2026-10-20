package com.cxh.blog.web;

import com.cxh.blog.post.dto.PostSummary;
import com.cxh.blog.post.service.PostService;
import com.cxh.blog.web.dto.ArchiveGroup;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 首页、关于、归档。这三页需要同时消费站点档案（由 {@link GlobalModelAdvice} 注入）
 * 与文章数据，属于典型的跨域装配，因此放在 web 层。
 */
@Controller
public class HomeController {

    private static final int HOME_POST_LIMIT = 6;
    private static final int ARCHIVE_LIMIT = 300;

    private final PostService postService;

    public HomeController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("recentPosts", postService.recentPublished(HOME_POST_LIMIT));
        model.addAttribute("tags", postService.publishedTags());
        model.addAttribute("activeNav", "home");
        return "index";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("activeNav", "about");
        return "about";
    }

    @GetMapping("/archive")
    public String archive(Model model) {
        List<PostSummary> all = postService.listPublished(0, ARCHIVE_LIMIT).items();
        model.addAttribute("groups", groupByYear(all));
        model.addAttribute("total", all.size());
        model.addAttribute("activeNav", "archive");
        return "archive";
    }

    /** 按年份分组，保持发布时间倒序。 */
    private List<ArchiveGroup> groupByYear(List<PostSummary> posts) {
        Map<String, List<PostSummary>> buckets = new LinkedHashMap<>();
        for (PostSummary post : posts) {
            String year = post.publishedAtText().length() >= 4
                    ? post.publishedAtText().substring(0, 4)
                    : "未发布";
            buckets.computeIfAbsent(year, key -> new ArrayList<>()).add(post);
        }
        return buckets.entrySet().stream()
                .map(entry -> new ArchiveGroup(entry.getKey(), List.copyOf(entry.getValue())))
                .toList();
    }
}
