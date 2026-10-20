/*
 * 危险操作的二次确认。
 *
 * 刻意用外部脚本 + data-confirm 属性，而不是 onclick/onsubmit 内联事件：
 * 内联处理器需要 CSP 放开 script-src 'unsafe-inline'，等于把 XSS 防线拆掉。
 */
(function () {
    'use strict';

    document.querySelectorAll('form[data-confirm]').forEach(function (form) {
        form.addEventListener('submit', function (event) {
            if (!window.confirm(form.getAttribute('data-confirm'))) {
                event.preventDefault();
            }
        });
    });
})();
