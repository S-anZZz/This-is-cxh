package com.cxh.blog.site.web;

import com.cxh.blog.site.service.SiteProfileService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 站点公开资源。样式表由服务端按当前主题实时生成，
 * 因此后台改完风格后刷新页面即刻生效，无需重新构建或重启。
 */
@Controller
public class SitePublicController {

    private final SiteProfileService siteProfileService;

    public SitePublicController(SiteProfileService siteProfileService) {
        this.siteProfileService = siteProfileService;
    }

    @GetMapping(value = "/theme.css", produces = "text/css;charset=UTF-8")
    @ResponseBody
    public String themeCss(HttpServletResponse response) {
        // 风格随时可改，禁止浏览器与中间层缓存
        response.setHeader("Cache-Control", "no-store, must-revalidate");
        return siteProfileService.stylesheet();
    }
}
