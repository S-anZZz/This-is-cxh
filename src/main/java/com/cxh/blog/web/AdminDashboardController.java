package com.cxh.blog.web;

import com.cxh.blog.post.service.PostService;
import com.cxh.blog.site.service.SiteProfileService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 后台首页概览。同时汇总文章域统计与站点域信息 —— 跨域聚合只发生在这里，
 * 两个业务域之间依然互不引用。
 */
@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final PostService postService;
    private final SiteProfileService siteProfileService;

    public AdminDashboardController(PostService postService, SiteProfileService siteProfileService) {
        this.postService = postService;
        this.siteProfileService = siteProfileService;
    }

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("stats", postService.stats());
        model.addAttribute("recentPosts", postService.listForAdmin(0, 5, null).items());
        model.addAttribute("currentTheme", siteProfileService.current().theme().getPreset().getLabel());
        model.addAttribute("activeNav", "dashboard");
        return "admin/dashboard";
    }
}
