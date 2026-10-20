package com.cxh.blog.post.web;

import com.cxh.blog.common.render.MarkdownRenderer;
import com.cxh.blog.post.domain.PostStatus;
import com.cxh.blog.post.dto.PostDetail;
import com.cxh.blog.post.dto.PostForm;
import com.cxh.blog.post.service.PostService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 文章后台管理页面（新增 / 编辑 / 删除 / 列表）。
 *
 * <p>遵循 Post-Redirect-Get：写入成功一律重定向，避免刷新导致重复提交；
 * 校验失败则原地回显表单与错误信息。
 */
@Controller
@RequestMapping("/admin/posts")
public class PostAdminController {

    private static final int PAGE_SIZE = 12;

    private final PostService postService;
    private final MarkdownRenderer markdownRenderer;

    public PostAdminController(PostService postService, MarkdownRenderer markdownRenderer) {
        this.postService = postService;
        this.markdownRenderer = markdownRenderer;
    }

    /** 下拉选项由枚举直接驱动，新增状态无需改模板。 */
    @ModelAttribute("allStatuses")
    public PostStatus[] allStatuses() {
        return PostStatus.values();
    }

    @GetMapping
    public String list(@RequestParam(required = false) PostStatus status,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        model.addAttribute("pageResult", postService.listForAdmin(page, PAGE_SIZE, status));
        model.addAttribute("currentStatus", status);
        model.addAttribute("stats", postService.stats());
        model.addAttribute("activeNav", "posts");
        return "admin/posts";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("form", PostForm.blank());
        model.addAttribute("activeNav", "posts");
        return "admin/post-form";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", postService.findForm(id));
        model.addAttribute("activeNav", "posts");
        return "admin/post-form";
    }

    @PostMapping
    public String save(@Valid @ModelAttribute("form") PostForm form,
                       BindingResult bindingResult,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("activeNav", "posts");
            return "admin/post-form";
        }
        PostDetail saved = postService.save(form);
        redirectAttributes.addFlashAttribute("flash", "已保存：" + saved.title());
        return "redirect:/admin/posts";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        postService.delete(id);
        redirectAttributes.addFlashAttribute("flash", "文章已删除");
        return "redirect:/admin/posts";
    }

    /**
     * 正文实时预览。复用与发布完全相同的渲染器，
     * 因此「预览的样子」就是「发布后的样子」。
     */
    @PostMapping(value = "/preview", produces = "text/html;charset=UTF-8")
    @ResponseBody
    public String preview(@RequestParam(required = false) String contentMarkdown) {
        return markdownRenderer.toHtml(contentMarkdown);
    }
}
