package com.cxh.blog.site.service.impl;

import com.cxh.blog.common.render.MarkdownRenderer;
import com.cxh.blog.site.domain.SiteProfile;
import com.cxh.blog.site.domain.ThemePreset;
import com.cxh.blog.site.domain.ThemeTokens;
import com.cxh.blog.site.dto.SiteProfileForm;
import com.cxh.blog.site.dto.SiteProfileView;
import com.cxh.blog.site.repository.SiteProfileRepository;
import com.cxh.blog.site.service.SiteProfileService;
import com.cxh.blog.site.theme.ThemeStyleRenderer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 站点档案服务实现。
 *
 * <p>两个关键设计：
 * <ol>
 *   <li><b>读模型快照</b>：站点档案是典型的「写一次、全站读」数据，
 *       因此在进程内保留一份不可变视图，写操作后原子替换，读路径零查询；</li>
 *   <li><b>渲染委派</b>：样式表由 {@link ThemeStyleRenderer} 生成，
 *       本类不拼接任何 CSS，风格表达方式的变化不会波及业务逻辑。</li>
 * </ol>
 *
 * <p>读写路径严格分离：读操作只做「取不到就用内存默认值渲染」，绝不写库；
 * 写操作才在事务内落库并刷新快照，避免只读事务静默丢弃写入。
 */
@Service
@Transactional(readOnly = true)
public class SiteProfileServiceImpl implements SiteProfileService {

    private final SiteProfileRepository repository;
    private final MarkdownRenderer markdownRenderer;
    private final ThemeStyleRenderer themeStyleRenderer;
    private final Object snapshotLock = new Object();

    private volatile SiteProfileView snapshot;

    public SiteProfileServiceImpl(SiteProfileRepository repository,
                                  MarkdownRenderer markdownRenderer,
                                  ThemeStyleRenderer themeStyleRenderer) {
        this.repository = repository;
        this.markdownRenderer = markdownRenderer;
        this.themeStyleRenderer = themeStyleRenderer;
    }

    @Override
    public SiteProfileView current() {
        SiteProfileView cached = this.snapshot;
        if (cached != null) {
            return cached;
        }
        synchronized (snapshotLock) {
            if (snapshot == null) {
                snapshot = toView(loadEntityOrDefault());
            }
            return snapshot;
        }
    }

    @Override
    public SiteProfileForm currentForm() {
        return SiteProfileForm.from(loadEntityOrDefault());
    }

    @Override
    @Transactional
    public SiteProfileView update(SiteProfileForm form) {
        SiteProfile profile = loadOrCreateEntity();
        profile.edit(form.toIdentity(), form.getIntroMarkdown(), form.toThemeTokens(), form.getCustomCss());
        return refreshSnapshot(repository.save(profile));
    }

    @Override
    @Transactional
    public SiteProfileView applyPreset(ThemePreset preset) {
        SiteProfile profile = loadOrCreateEntity();
        ThemePreset target = preset == null ? profile.getTheme().getPreset() : preset;
        // 只换配色与字体，保留用户已调好的排版参数
        ThemeTokens tokens = profile.getTheme().copy();
        target.applyColorsTo(tokens);
        tokens.setHeadingFont(target.getHeadingFont());
        tokens.setBodyFont(target.getBodyFont());
        profile.applyPreset(tokens);
        return refreshSnapshot(repository.save(profile));
    }

    @Override
    public String stylesheet() {
        SiteProfileView view = current();
        return themeStyleRenderer.renderStylesheet(view.theme(), view.customCss());
    }

    @Override
    public String previewStylesheet(SiteProfileForm form) {
        return themeStyleRenderer.renderStylesheet(form.toThemeTokens(), form.getCustomCss());
    }

    @Override
    @Transactional
    public void ensureInitialized() {
        if (repository.findById(SiteProfile.SINGLETON_ID).isEmpty()) {
            repository.save(SiteProfile.createDefault());
        }
        synchronized (snapshotLock) {
            snapshot = null;
        }
    }

    private SiteProfileView refreshSnapshot(SiteProfile profile) {
        SiteProfileView view = toView(profile);
        synchronized (snapshotLock) {
            snapshot = view;
        }
        return view;
    }

    /** 读路径：取不到就用内存默认档案渲染，不产生写入。 */
    private SiteProfile loadEntityOrDefault() {
        return repository.findById(SiteProfile.SINGLETON_ID).orElseGet(SiteProfile::createDefault);
    }

    /** 写路径：在写事务内保证单例行存在。 */
    private SiteProfile loadOrCreateEntity() {
        return repository.findById(SiteProfile.SINGLETON_ID)
                .orElseGet(() -> repository.save(SiteProfile.createDefault()));
    }

    private SiteProfileView toView(SiteProfile profile) {
        return SiteProfileView.from(profile, markdownRenderer.toHtml(profile.getIntroMarkdown()));
    }
}
