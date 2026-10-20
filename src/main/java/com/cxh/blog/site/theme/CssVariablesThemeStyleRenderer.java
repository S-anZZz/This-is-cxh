package com.cxh.blog.site.theme;

import com.cxh.blog.site.domain.HexColor;
import com.cxh.blog.site.domain.ThemeTokens;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 默认实现：把令牌渲染为 {@code :root} 上的 CSS 变量。
 *
 * <p>所有派生值（半透明色、次级圆角等）都在这里集中计算，
 * 使模板中不出现任何魔法色值。
 */
@Component
public class CssVariablesThemeStyleRenderer implements ThemeStyleRenderer {

    @Override
    public String renderTokensCss(ThemeTokens tokens) {
        ThemeTokens safe = tokens == null ? ThemeTokens.defaults() : tokens.copy();
        safe.normalize();
        return toCss(variables(safe));
    }

    /** 令牌 → 变量映射。编辑器预览接口直接复用，保证「预览即所见」。 */
    public Map<String, String> variables(ThemeTokens tokens) {
        ThemeTokens safe = tokens == null ? ThemeTokens.defaults() : tokens.copy();
        safe.normalize();

        int radius = safe.getCornerRadius();
        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("primary", safe.getPrimaryColor());
        vars.put("primary-soft", alpha(safe.getPrimaryColor(), 0.12));
        vars.put("accent", safe.getAccentColor());
        vars.put("bg", safe.getPageBackground());
        vars.put("surface", safe.getSurfaceBackground());
        vars.put("text", safe.getTextColor());
        vars.put("muted", safe.getMutedTextColor());
        vars.put("border", alpha(safe.getTextColor(), safe.isDarkSurface() ? 0.22 : 0.12));
        vars.put("shadow", safe.isDarkSurface()
                ? "0 12px 32px rgba(0, 0, 0, 0.45)"
                : "0 12px 32px rgba(15, 23, 42, 0.08)");
        vars.put("font-heading", safe.getHeadingFont().getCssStack());
        vars.put("font-body", safe.getBodyFont().getCssStack());
        vars.put("font-size", safe.getBaseFontSize() + "px");
        vars.put("content-width", safe.getContentWidth() + "px");
        vars.put("radius", radius + "px");
        vars.put("radius-sm", Math.max(0, radius / 2) + "px");
        vars.put("radius-lg", (radius + radius / 2) + "px");
        vars.put("header-position", safe.isStickyHeader() ? "sticky" : "static");
        return vars;
    }

    private String toCss(Map<String, String> variables) {
        String body = variables.entrySet().stream()
                .map(entry -> "  " + VARIABLE_PREFIX + entry.getKey() + ": " + entry.getValue() + ";")
                .collect(Collectors.joining("\n"));
        return ":root {\n" + body + "\n}\n";
    }

    /** 把十六进制色转为带透明度的 rgba()，用于生成柔和的派生色。 */
    private String alpha(String hex, double alpha) {
        String value = HexColor.normalize(hex, "#000000");
        int r = Integer.parseInt(value.substring(1, 3), 16);
        int g = Integer.parseInt(value.substring(3, 5), 16);
        int b = Integer.parseInt(value.substring(5, 7), 16);
        return "rgba(" + r + ", " + g + ", " + b + ", " + alpha + ")";
    }
}
