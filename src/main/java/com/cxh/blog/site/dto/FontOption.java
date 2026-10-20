package com.cxh.blog.site.dto;

import com.cxh.blog.site.domain.FontChoice;

/**
 * 字体选项。前端只能提交枚举名，字体栈由服务端决定。
 */
public record FontOption(String value, String label, String cssStack) {

    public static FontOption from(FontChoice choice) {
        return new FontOption(choice.name(), choice.getLabel(), choice.getCssStack());
    }
}
