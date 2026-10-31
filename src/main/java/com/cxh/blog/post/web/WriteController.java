package com.cxh.blog.post.web;

import com.cxh.blog.post.domain.PostStatus;
import com.cxh.blog.post.dto.PostDetail;
import com.cxh.blog.post.dto.PostForm;
import com.cxh.blog.post.service.PostService;
import com.cxh.blog.user.domain.AppUser;
import com.cxh.blog.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 普通用户发文。与后台文章管理复用同一个 {@link PostForm} 与保存逻辑，
 * 区别只有两点：状态固定为「已发布」，并记录当前用户为作者。
 */
@Controller
public class WriteController {

    private final PostService postService;
    private final UserService userService;

    public WriteController(PostService postService, UserService userService) {
        this.postService = postService;
        this.userService = userService;
    }

    @GetMapping("/write")
    public String form(Model model) {
        if (!model.containsAttribute("form")) {
            PostForm form = new PostForm();
            form.setStatus(PostStatus.PUBLISHED);
            model.addAttribute("form", form);
        }
        return "write";
    }

    @PostMapping("/write")
    public String publish(@Valid @ModelAttribute("form") PostForm form,
                          BindingResult binding,
                          Authentication authentication,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            return "write";
        }
        form.setStatus(PostStatus.PUBLISHED);
        AppUser author = userService.findByLogin(authentication.getName()).orElse(null);
        PostDetail saved = postService.save(form, author);
        redirectAttributes.addFlashAttribute("flash", "已发表：" + saved.title());
        return "redirect:/posts/" + saved.slug();
    }
}
