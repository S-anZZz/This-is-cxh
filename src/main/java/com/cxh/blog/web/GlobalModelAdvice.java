package com.cxh.blog.web;

import com.cxh.blog.site.dto.SiteProfileView;
import com.cxh.blog.site.service.SiteProfileService;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 全站模型装配：每个页面都自动获得当前站点档案（标题、作者、简介、主题）。
 *
 * <p>这是模板与站点域之间<b>唯一</b>的耦合点：
 * 模板统一通过 {@code ${site.*}} 取数据，各控制器完全不必关心站点配置，
 * 站点域也不必知道有哪些页面存在。
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private final SiteProfileService siteProfileService;

    public GlobalModelAdvice(SiteProfileService siteProfileService) {
        this.siteProfileService = siteProfileService;
    }

    @ModelAttribute
    public void injectSiteProfile(Model model) {
        if (!model.containsAttribute("site")) {
            SiteProfileView site = siteProfileService.current();
            model.addAttribute("site", site);
            model.addAttribute("theme", site.theme());
        }
    }
}
