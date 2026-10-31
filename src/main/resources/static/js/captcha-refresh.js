/*
 * 点击「换一张」重新拉取验证码。
 * 用外部脚本而不是内联 onclick：CSP 只允许 script-src 'self'。
 * 加时间戳参数强制绕过浏览器缓存（服务端也返回了 no-store）。
 */
(function () {
    'use strict';
    var button = document.getElementById('captcha-refresh');
    var image = document.getElementById('captcha-image');
    if (!button || !image) {
        return;
    }
    button.addEventListener('click', function () {
        image.src = '/captcha?t=' + Date.now();
    });
})();
