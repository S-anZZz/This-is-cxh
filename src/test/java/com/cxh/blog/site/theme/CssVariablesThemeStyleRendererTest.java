package com.cxh.blog.site.theme;

import static org.assertj.core.api.Assertions.assertThat;

import com.cxh.blog.site.domain.FontChoice;
import com.cxh.blog.site.domain.ThemePreset;
import com.cxh.blog.site.domain.ThemeTokens;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 风格渲染器单元测试：验证「令牌 → CSS 变量」这一唯一转换链路，
 * 以及非法输入被收敛到安全取值域。
 */
class CssVariablesThemeStyleRendererTest {

    private final CssVariablesThemeStyleRenderer renderer = new CssVariablesThemeStyleRenderer();

    @Test
    @DisplayName("令牌被渲染为 --cxh-* 变量，模板只需要这些变量")
    void rendersNamespacedVariables() {
        Map<String, String> variables = renderer.variables(ThemePreset.WARM_PAPER.newTokens());

        assertThat(variables).containsKeys("primary", "accent", "bg", "surface", "text", "muted",
                "font-heading", "font-body", "font-size", "content-width", "radius", "radius-sm");
        assertThat(variables.get("primary")).isEqualTo("#b45309");
        assertThat(variables.get("font-size")).isEqualTo("17px");
        assertThat(variables.get("content-width")).isEqualTo("780px");
        assertThat(variables.get("font-body")).contains("Georgia");
    }

    @Test
    @DisplayName("渲染结果是合法的 :root 声明块")
    void rendersRootBlock() {
        String css = renderer.renderTokensCss(ThemePreset.MIDNIGHT_DARK.newTokens());

        assertThat(css).startsWith(":root {").contains("--cxh-primary: #60a5fa;").endsWith("}\n");
    }

    @Test
    @DisplayName("自定义 CSS 追加在变量之后，可覆盖默认表现")
    void appendsCustomCss() {
        ThemeTokens tokens = ThemePreset.ELEGANT_LIGHT.newTokens();

        String css = renderer.renderStylesheet(tokens, ".post-card { border: 0; }");

        assertThat(css).contains("--cxh-primary").contains("站点自定义 CSS").contains(".post-card { border: 0; }");
        assertThat(css.indexOf("--cxh-primary")).isLessThan(css.indexOf(".post-card"));
    }

    @Test
    @DisplayName("非法颜色被回退到预置值，杜绝 CSS 注入")
    void sanitizesIllegalColors() {
        ThemeTokens tokens = ThemePreset.OCEAN_BLUE.newTokens();
        tokens.setPrimaryColor("red; } body { display: none; } .x {");
        tokens.setAccentColor(null);

        Map<String, String> variables = renderer.variables(tokens);

        assertThat(variables.get("primary")).isEqualTo("#0ea5e9");
        assertThat(variables.get("accent")).isEqualTo("#22d3ee");
    }

    @Test
    @DisplayName("越界的排版数值被夹取到合法区间，而不是原样落库")
    void clampsNumericTokens() {
        ThemeTokens tokens = ThemeTokens.defaults();
        tokens.setBaseFontSize(200);
        tokens.setContentWidth(10);
        tokens.setCornerRadius(-5);
        tokens.normalize();

        assertThat(tokens.getBaseFontSize()).isEqualTo(26);
        assertThat(tokens.getContentWidth()).isEqualTo(560);
        assertThat(tokens.getCornerRadius()).isEqualTo(ThemeTokens.DEFAULT_RADIUS);
    }

    @Test
    @DisplayName("字体只能来自白名单枚举，非法名称回退默认值")
    void fallsBackToDefaultFont() {
        assertThat(FontChoice.of("NOT_A_FONT", FontChoice.SYSTEM_SANS)).isEqualTo(FontChoice.SYSTEM_SANS);
        assertThat(FontChoice.of("mono", FontChoice.SYSTEM_SANS)).isEqualTo(FontChoice.MONO);
    }
}
