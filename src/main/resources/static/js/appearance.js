/*
 * 外观编辑器交互。设计要点：
 *  1) 预览走服务端渲染接口（POST 主表单 → CSS 文本），
 *     因此「预览所见」与「保存后所得」由同一段 Java 代码保证一致；
 *  2) 预览注入到 iframe 内真实的首页 <style id="cxh-theme-preview">，
 *     不落库、不刷新页面；
 *  3) 所有请求带上 CSRF 头，保持与表单提交同等的安全级别。
 */
(function () {
    'use strict';

    var form = document.getElementById('appearance-form');
    var frame = document.getElementById('preview-frame');
    if (!form || !frame) {
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

    function previewTarget() {
        try {
            var doc = frame.contentDocument;
            return doc ? doc.getElementById('cxh-theme-preview') : null;
        } catch (error) {
            return null;
        }
    }

    function refreshPreview() {
        var target = previewTarget();
        if (!target) {
            return;
        }
        var headers = {};
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }
        fetch(previewUrl, {method: 'POST', headers: headers, body: new FormData(form), credentials: 'same-origin'})
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('预览请求失败：' + response.status);
                }
                return response.text();
            })
            .then(function (css) {
                target.textContent = css;
            })
            .catch(function (error) {
                console.warn(error);
            });
    }

    function schedule(delay) {
        window.clearTimeout(timer);
        timer = window.setTimeout(refreshPreview, delay);
    }

    /** 让取色器与滑块的数值实时显示在旁边的 output 上 */
    function syncOutput(input) {
        if (!input || !input.id) {
            return;
        }
        var output = document.querySelector('output[for="' + input.id + '"]');
        if (!output) {
            return;
        }
        if (input.type === 'range') {
            output.textContent = input.value + ' px';
        } else if (input.type === 'color') {
            output.textContent = input.value;
        }
    }

    form.querySelectorAll('input, select, textarea').forEach(function (input) {
        syncOutput(input);
    });

    form.addEventListener('input', function (event) {
        syncOutput(event.target);
        schedule(220);
    });

    form.addEventListener('change', function (event) {
        syncOutput(event.target);
        schedule(120);
    });

    // iframe 首次加载完成后再注入一次，确保预览与表单同步
    frame.addEventListener('load', function () {
        window.setTimeout(refreshPreview, 60);
    });

    var refreshButton = document.getElementById('preview-refresh');
    if (refreshButton) {
        refreshButton.addEventListener('click', refreshPreview);
    }
})();
