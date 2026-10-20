/*
 * 文章编辑器的 Markdown 实时预览。
 * 渲染交给服务端（与最终发布使用同一个 MarkdownRenderer），
 * 因此预览结果与保存后的正文完全一致，前端不引入任何 Markdown 库。
 */
(function () {
    'use strict';

    var form = document.getElementById('post-form');
    var textarea = document.getElementById('contentMarkdown');
    var preview = document.getElementById('markdown-preview');
    var status = document.getElementById('markdown-status');
    if (!form || !textarea || !preview) {
        return;
    }

    var meta = function (name) {
        var el = document.querySelector('meta[name="' + name + '"]');
        return el ? el.getAttribute('content') : null;
    };
    var csrfToken = meta('_csrf');
    var csrfHeader = meta('_csrf_header') || 'X-CSRF-TOKEN';
    var previewUrl = form.getAttribute('data-preview-url');
    var timer = null;

    function render() {
        var body = new URLSearchParams();
        body.append('contentMarkdown', textarea.value);

        var headers = {'Content-Type': 'application/x-www-form-urlencoded;charset=UTF-8'};
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        fetch(previewUrl, {method: 'POST', headers: headers, body: body.toString(), credentials: 'same-origin'})
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('渲染失败：' + response.status);
                }
                return response.text();
            })
            .then(function (html) {
                preview.hidden = false;
                preview.innerHTML = html;
                if (status) {
                    status.textContent = '预览已更新（与发布后的渲染结果一致）。';
                }
            })
            .catch(function (error) {
                if (status) {
                    status.textContent = '预览暂不可用：' + error.message;
                }
            });
    }

    function schedule() {
        window.clearTimeout(timer);
        timer = window.setTimeout(render, 320);
    }

    textarea.addEventListener('input', schedule);

    if (textarea.value.trim().length > 0) {
        render();
    }
})();
