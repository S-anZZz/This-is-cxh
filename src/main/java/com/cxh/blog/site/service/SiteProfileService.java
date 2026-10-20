package com.cxh.blog.site.service;

import com.cxh.blog.site.domain.ThemePreset;
import com.cxh.blog.site.dto.SiteProfileForm;
import com.cxh.blog.site.dto.SiteProfileView;

/**
 * 站点域对外能力：读取站点档案、编辑风格与简介、输出样式表。
 *
 * <p>对外只暴露读模型（{@link SiteProfileView}）与写模型（{@link SiteProfileForm}），
 * 实体与仓储不外泄，因此持久化与渲染实现都能独立替换。
 */
public interface SiteProfileService {

    /** 当前生效的站点档案（带进程内快照，读多写少）。 */
    SiteProfileView current();

    /** 编辑页初值。 */
    SiteProfileForm currentForm();

    /** 保存一次编辑，返回最新视图。 */
    SiteProfileView update(SiteProfileForm form);

    /** 一键套用预置主题（保留字号/宽度等排版参数）。 */
    SiteProfileView applyPreset(ThemePreset preset);

    /** 当前生效的完整样式表（CSS 变量 + 自定义 CSS）。 */
    String stylesheet();

    /** 编辑期的实时预览：不落库，直接返回该组令牌渲染出的 CSS。 */
    String previewStylesheet(SiteProfileForm form);

    /** 启动期保证单例档案存在。 */
    void ensureInitialized();
}
