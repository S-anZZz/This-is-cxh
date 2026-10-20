/*
 * 前台交互增强。渐进增强：禁用 JS 时页面依然完全可用。
 */
(function () {
    'use strict';

    // 让站内锚点在吸顶导航下不被遮挡
    document.querySelectorAll('a[href^="#"]').forEach(function (anchor) {
        anchor.addEventListener('click', function (event) {
            var target = document.querySelector(anchor.getAttribute('href'));
            if (!target) {
                return;
            }
            event.preventDefault();
            target.scrollIntoView({behavior: 'smooth', block: 'start'});
        });
    });
})();
