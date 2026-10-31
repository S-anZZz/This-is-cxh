/*
 * 配色盘：把一张卡片上的整套配色一次填进 6 个取色器，并可围绕主色推导协调配色。
 *
 * 设计约束：
 *  1) 不引入任何构建工具或框架，纯原生 DOM 事件；
 *  2) 只改表单控件的值并派发 input/change 事件，让既有的实时预览逻辑照常工作，
 *     不直接碰样式表 —— 保证「预览」与「保存」永远走同一条渲染链路；
 *  3) 推导配色用 HSL：色相旋转 + 明度/饱和度调整，避免出现刺眼的高饱和底色。
 */
(function () {
    'use strict';

    var FIELDS = {
        primary: 'primaryColor',
        accent: 'accentColor',
        page: 'pageBackground',
        surface: 'surfaceBackground',
        text: 'textColor',
        muted: 'mutedTextColor',
    };

    var grid = document.getElementById('palette-grid');
    var derive = document.getElementById('derive-palette');
    var status = document.getElementById('palette-status');
    if (!grid && !derive) {
        return;
    }

    function setField(key, value) {
        var el = document.getElementById(FIELDS[key]);
        if (!el || !value) {
            return;
        }
        el.value = value;
        // 让 appearance.js 的实时预览监听到这次变化
        el.dispatchEvent(new Event('input', {bubbles: true}));
        el.dispatchEvent(new Event('change', {bubbles: true}));
    }

    function setStatus(text) {
        if (status) {
            status.textContent = text;
        }
    }

    // 配色卡：一次填满 6 个字段
    if (grid) {
        grid.addEventListener('click', function (event) {
            var card = event.target.closest('.palette-card');
            if (!card) {
                return;
            }
            Object.keys(FIELDS).forEach(function (key) {
                setField(key, card.getAttribute('data-' + key));
            });
            setStatus('已套用配色：' + (card.getAttribute('data-label') || '自定义'));
        });
    }

    // 十六进制 → HSL，用于推导配色
    function hexToHsl(hex) {
        var value = String(hex || '').replace('#', '');
        if (value.length === 3) {
            value = value.charAt(0) + value.charAt(0)
                + value.charAt(1) + value.charAt(1)
                + value.charAt(2) + value.charAt(2);
        }
        if (!/^[0-9a-fA-F]{6}$/.test(value)) {
            return null;
        }
        var r = parseInt(value.slice(0, 2), 16) / 255;
        var g = parseInt(value.slice(2, 4), 16) / 255;
        var b = parseInt(value.slice(4, 6), 16) / 255;
        var max = Math.max(r, g, b);
        var min = Math.min(r, g, b);
        var l = (max + min) / 2;
        var d = max - min;
        var h = 0;
        var s = 0;
        if (d !== 0) {
            s = l > 0.5 ? d / (2 - max - min) : d / (max + min);
            if (max === r) {
                h = ((g - b) / d + (g < b ? 6 : 0)) / 6;
            } else if (max === g) {
                h = ((b - r) / d + 2) / 6;
            } else {
                h = ((r - g) / d + 4) / 6;
            }
        }
        return {h: h * 360, s: s * 100, l: l * 100};
    }

    function hslToHex(h, s, l) {
        var hue = ((h % 360) + 360) % 360 / 360;
        var sat = Math.min(100, Math.max(0, s)) / 100;
        var light = Math.min(100, Math.max(0, l)) / 100;

        function channel(p, q, t) {
            if (t < 0) {
                t += 1;
            }
            if (t > 1) {
                t -= 1;
            }
            if (t < 1 / 6) {
                return p + (q - p) * 6 * t;
            }
            if (t < 1 / 2) {
                return q;
            }
            if (t < 2 / 3) {
                return p + (q - p) * (2 / 3 - t) * 6;
            }
            return p;
        }

        var r;
        var g;
        var b;
        if (sat === 0) {
            r = g = b = light;
        } else {
            var q = light < 0.5 ? light * (1 + sat) : light + sat - light * sat;
            var p = 2 * light - q;
            r = channel(p, q, hue + 1 / 3);
            g = channel(p, q, hue);
            b = channel(p, q, hue - 1 / 3);
        }
        var toHex = function (v) {
            var n = Math.round(v * 255).toString(16);
            return n.length === 1 ? '0' + n : n;
        };
        return '#' + toHex(r) + toHex(g) + toHex(b);
    }

    // 围绕主色推导：辅助色转 40°，底色取极浅同色相，正文用低饱和深色
    if (derive) {
        derive.addEventListener('click', function () {
            var primaryInput = document.getElementById(FIELDS.primary);
            var base = hexToHsl(primaryInput ? primaryInput.value : '');
            if (!base) {
                setStatus('主色不是合法的十六进制颜色');
                return;
            }
            setField('accent', hslToHex(base.h + 40, Math.max(base.s, 45), Math.min(Math.max(base.l, 38), 62)));
            setField('page', hslToHex(base.h, Math.min(base.s, 30), 97));
            setField('surface', '#ffffff');
            setField('text', hslToHex(base.h, 12, 16));
            setField('muted', hslToHex(base.h, 10, 45));
            setStatus('已按主色推出一套配色，可继续微调');
        });
    }
})();
