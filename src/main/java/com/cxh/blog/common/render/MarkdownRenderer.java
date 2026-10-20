package com.cxh.blog.common.render;

/**
 * 文本渲染能力抽象。业务域只依赖该接口，不关心底层是 CommonMark、Markdown-it 还是富文本。
 * 更换渲染实现时只需提供新的 Bean，无需修改任何调用方（依赖倒置）。
 */
public interface MarkdownRenderer {

    /**
     * 将 Markdown 渲染为安全（已转义原始 HTML）的 HTML 片段。
     *
     * @param markdown 原始 Markdown，允许为 null
     * @return HTML 片段，null 输入返回空串
     */
    String toHtml(String markdown);

    /**
     * 将首行纯文本作为列表页摘要，超出长度以省略号截断。
     *
     * @param markdown 原始 Markdown
     * @param maxLength 最大字符数
     */
    String toPlainSummary(String markdown, int maxLength);
}
