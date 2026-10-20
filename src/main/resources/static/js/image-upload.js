/*
 * 图片上传：选中文件 → POST /api/media/images → 把返回的 Markdown 插到正文光标处。
 *
 * 说明：
 *  1) 走 multipart 上传，请求头带 CSRF 令牌（从 meta 标签取）；
 *  2) 插入的是「缩略图套原图链接」的 Markdown，与相册排版规则一致（见 blog.css）；
 *  3) 不依赖任何第三方库，禁用 JS 时仍可手动把图片放到 static/images/gallery 并手写 Markdown。
 */
(function () {
    'use strict';

    var input = document.getElementById('image-upload');
    var textarea = document.getElementById('contentMarkdown');
    var status = document.getElementById('upload-status');
    if (!input || !textarea) {
        return;
    }

    var meta = function (name) {
        var el = document.querySelector('meta[name="' + name + '"]');
        return el ? el.getAttribute('content') : null;
    };
    var csrfToken = meta('_csrf');
    var csrfHeader = meta('_csrf_header') || 'X-CSRF-TOKEN';
    var uploadUrl = input.getAttribute('data-upload-url');

    function setStatus(text, isError) {
        if (!status) {
            return;
        }
        status.textContent = text;
        status.style.color = isError ? '#dc2626' : '';
    }

    /** 在光标处插入文本，并把光标移到插入内容之后 */
    function insertAtCursor(text) {
        var start = textarea.selectionStart || 0;
        var end = textarea.selectionEnd || 0;
        var before = textarea.value.substring(0, start);
        var after = textarea.value.substring(end);
        var prefix = before.length > 0 && !before.endsWith('\n') ? '\n' : '';
        textarea.value = before + prefix + text + '\n' + after;
        var caret = (before + prefix + text + '\n').length;
        textarea.selectionStart = textarea.selectionEnd = caret;
        textarea.dispatchEvent(new Event('input', {bubbles: true}));
    }

    input.addEventListener('change', function () {
        var file = input.files && input.files[0];
        if (!file) {
            return;
        }
        var alt = window.prompt('给这张图写一句说明（会作为图片替代文字）：', file.name.replace(/\.[^.]+$/, ''));
        if (alt === null) {
            input.value = '';
            return;
        }

        var body = new FormData();
        body.append('file', file);
        body.append('alt', alt);

        var headers = {};
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        setStatus('正在上传 ' + file.name + ' …');
        fetch(uploadUrl, {method: 'POST', headers: headers, body: body, credentials: 'same-origin'})
            .then(function (response) {
                return response.json().then(function (json) {
                    if (!response.ok || !json.success) {
                        throw new Error(json && json.message ? json.message : ('上传失败：' + response.status));
                    }
                    return json.data;
                });
            })
            .then(function (data) {
                insertAtCursor(data.markdown);
                setStatus('已插入：' + data.url + '（' + data.width + '×' + data.height + '）');
            })
            .catch(function (error) {
                setStatus(error.message, true);
            })
            .finally(function () {
                input.value = '';
            });
    });
})();
