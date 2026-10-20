package com.cxh.blog.site.web;

import com.cxh.blog.common.api.ApiResponse;
import com.cxh.blog.site.domain.FontChoice;
import com.cxh.blog.site.domain.ThemePreset;
import com.cxh.blog.site.dto.FontOption;
import com.cxh.blog.site.dto.SiteProfileForm;
import com.cxh.blog.site.dto.SiteProfileView;
import com.cxh.blog.site.dto.ThemePresetOption;
import com.cxh.blog.site.service.SiteProfileService;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 站点外观后台：风格、简介、站点身份的自由编辑。
 *
 * <p>页面本身不包含任何风格逻辑，只负责把表单提交给站点域，
 * 并把可选值（预置主题、字体）从枚举直接投影成下拉选项。
 * 新增一个预置主题或字体，页面与接口自动获得，无需改动此处代码。
 */
@Controller
@RequestMapping("/admin/appearance")
public class SiteAdminController {

    private final SiteProfileService siteProfileService;

    public SiteAdminController(SiteProfileService siteProfileService) {
        this.siteProfileService = siteProfileService;
    }

    @ModelAttribute("presetOptions")
    public List<ThemePresetOption> presetOptions() {
        return Arrays.stream(ThemePreset.values()).map(ThemePresetOption::from).toList();
    }

    @ModelAttribute("fontOptions")
    public List<FontOption> fontOptions() {
        return Arrays.stream(FontChoice.values()).map(FontOption::from).toList();
    }

    @GetMapping
    public String page(Model model) {
        model.addAttribute("form", siteProfileService.currentForm());
        model.addAttribute("activeNav", "appearance");
        return "admin/appearance";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("form") SiteProfileForm form,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeNav", "appearance");
            return "admin/appearance";
        }
        SiteProfileView saved = siteProfileService.update(form);
        redirectAttributes.addFlashAttribute("flash", "外观已保存：" + saved.siteTitle());
        return "redirect:/admin/appearance";
    }

    /** 一键套用预置配色（保留排版参数）。 */
    @PostMapping("/preset")
    public String applyPreset(@RequestParam ThemePreset preset, RedirectAttributes redirectAttributes) {
        SiteProfileView view = siteProfileService.applyPreset(preset);
        redirectAttributes.addFlashAttribute("flash", "已套用预置主题：" + view.theme().getPreset().getLabel());
        return "redirect:/admin/appearance";
    }

    /**
     * 实时预览接口：接收与保存完全相同的表单，返回渲染后的 CSS。
     * 复用同一条渲染链路，保证「预览 = 保存后的效果」。
     */
    @PostMapping(value = "/preview", produces = "text/css;charset=UTF-8")
    @ResponseBody
    public String preview(@ModelAttribute SiteProfileForm form) {
        return siteProfileService.previewStylesheet(form);
    }

    /** 供前端「选中预置即回填取色器」使用。 */
    @GetMapping(value = "/presets", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ApiResponse<List<ThemePresetOption>> presets() {
        return ApiResponse.ok(presetOptions());
    }
}
