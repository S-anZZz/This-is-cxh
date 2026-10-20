package com.cxh.blog.site.theme;

import com.cxh.blog.site.domain.ThemeTokens;

/**
 * 风格渲染器：把主题令牌翻译成浏览器可消费的 CSS。
 *
 * <p>这是本站「风格与内容解耦」的关键接缝：
 * 模板只认 {@code --cxh-*} CSS 变量，令牌的渲染方式可以整体替换
 * （例如换成 Tailwind 变量、多套皮肤叠加、甚至输出 JSON 给前端构建期使用），
 * 而所有模板与业务代码保持不变。
 */
public interface ThemeStyleRenderer {

    /** 命名空间前缀，保证与第三方样式不冲突。 */
    String VARIABLE_PREFIX = "--cxh-";

    /** 仅渲染令牌对应的 CSS 变量声明块。 */
    String renderTokensCss(ThemeTokens tokens);

    /** 渲染完整样式表：令牌变量 + 站点自定义 CSS。 */
    default String renderStylesheet(ThemeTokens tokens, String customCss) {
        StringBuilder css = new StringBuilder(renderTokensCss(tokens));
        if (customCss != null && !customCss.isBlank()) {
            css.append("\n\n/* ===== 站点自定义 CSS（后台外观设置填写） ===== */\n")
                    .append(customCss.trim())
                    .append('\n');
        }
        return css.toString();
    }
}
