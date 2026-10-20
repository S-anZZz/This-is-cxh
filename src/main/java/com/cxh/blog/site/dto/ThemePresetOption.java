package com.cxh.blog.site.dto;

import com.cxh.blog.site.domain.ThemePreset;

/**
 * 预置主题选项。字段名与外观表单的输入名严格一致，
 * 前端选中某个预置后直接按字段名回填，无需任何映射逻辑。
 */
public record ThemePresetOption(
        String preset,
        String label,
        String primaryColor,
        String accentColor,
        String pageBackground,
        String surfaceBackground,
        String textColor,
        String mutedTextColor,
        String headingFont,
        String bodyFont,
        boolean dark) {

    public static ThemePresetOption from(ThemePreset preset) {
        return new ThemePresetOption(
                preset.name(),
                preset.getLabel(),
                preset.getPrimaryColor(),
                preset.getAccentColor(),
                preset.getPageBackground(),
                preset.getSurfaceBackground(),
                preset.getTextColor(),
                preset.getMutedTextColor(),
                preset.getHeadingFont().name(),
                preset.getBodyFont().name(),
                preset.isDark());
    }
}
