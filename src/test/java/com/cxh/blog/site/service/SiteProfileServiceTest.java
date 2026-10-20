package com.cxh.blog.site.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.cxh.blog.site.domain.ThemePreset;
import com.cxh.blog.site.dto.SiteProfileForm;
import com.cxh.blog.site.dto.SiteProfileView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 站点域集成测试：验证「编辑风格与简介」这条主链路的持久化、
 * 快照刷新与样式表输出。
 */
@SpringBootTest
@ActiveProfiles("test")
class SiteProfileServiceTest {

    @Autowired
    private SiteProfileService siteProfileService;

    @Test
    @DisplayName("首次读取会自动得到一份可用的默认档案")
    void providesDefaultProfile() {
        SiteProfileView view = siteProfileService.current();

        assertThat(view.siteTitle()).isNotBlank();
        assertThat(view.authorName()).isNotBlank();
        assertThat(view.theme()).isNotNull();
        assertThat(view.introHtml()).contains("<h2>");
    }

    @Test
    @DisplayName("保存编辑后：读模型立即刷新，简介被渲染为 HTML，样式表同步变化")
    void persistsEditsAndRefreshesStylesheet() {
        SiteProfileForm form = siteProfileService.currentForm();
        form.setSiteTitle("cxh 的技术笔记");
        form.setSiteSubtitle("高内聚，低耦合");
        form.setAuthorName("cxh");
        form.setIntroMarkdown("## 关于我\n\n专注 Java 后端。");
        form.setPrimaryColor("#123456");
        form.setPageBackground("#0b1220");
        form.setTextColor("#e5e7eb");
        form.setBaseFontSize(19);
        form.setContentWidth(900);
        form.setCornerRadius(4);
        form.setCustomCss(".post-card { border-left: 3px solid var(--cxh-primary); }");

        SiteProfileView saved = siteProfileService.update(form);

        assertThat(saved.siteTitle()).isEqualTo("cxh 的技术笔记");
        assertThat(saved.introHtml()).contains("关于我").contains("<h2>");
        assertThat(saved.theme().getPrimaryColor()).isEqualTo("#123456");
        assertThat(saved.theme().getBaseFontSize()).isEqualTo(19);

        // 读路径命中的是刷新后的快照
        assertThat(siteProfileService.current().siteTitle()).isEqualTo("cxh 的技术笔记");

        String css = siteProfileService.stylesheet();
        assertThat(css).contains("--cxh-primary: #123456;")
                .contains("--cxh-font-size: 19px;")
                .contains("--cxh-content-width: 900px;")
                .contains(".post-card { border-left: 3px solid var(--cxh-primary); }");
    }

    @Test
    @DisplayName("预览样式表不落库：只影响本次渲染结果")
    void previewDoesNotPersist() {
        SiteProfileForm form = siteProfileService.currentForm();
        form.setPrimaryColor("#ff00aa");

        assertThat(siteProfileService.previewStylesheet(form)).contains("--cxh-primary: #ff00aa;");
        assertThat(siteProfileService.stylesheet()).doesNotContain("--cxh-primary: #ff00aa;");
    }

    @Test
    @DisplayName("套用预置主题只改配色与字体，保留用户调好的排版")
    void appliesPresetButKeepsTypography() {
        SiteProfileForm form = siteProfileService.currentForm();
        form.setBaseFontSize(15);
        form.setCornerRadius(3);
        siteProfileService.update(form);

        SiteProfileView applied = siteProfileService.applyPreset(ThemePreset.MIDNIGHT_DARK);

        assertThat(applied.theme().getPreset()).isEqualTo(ThemePreset.MIDNIGHT_DARK);
        assertThat(applied.theme().getPrimaryColor()).isEqualTo(ThemePreset.MIDNIGHT_DARK.getPrimaryColor());
        assertThat(applied.theme().getBaseFontSize()).isEqualTo(15);
        assertThat(applied.theme().getCornerRadius()).isEqualTo(3);
    }

    @Test
    @DisplayName("非法颜色值不会写入数据库")
    void rejectsIllegalColor() {
        SiteProfileForm form = siteProfileService.currentForm();
        form.setPrimaryColor("javascript:alert(1)");

        SiteProfileView saved = siteProfileService.update(form);

        assertThat(saved.theme().getPrimaryColor()).startsWith("#");
        assertThat(saved.theme().getPrimaryColor()).doesNotContain("javascript");
    }
}
