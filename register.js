/**
 * 用户登录 / 注册页面交互脚本
 * 功能：
 *   1. 选项卡切换 —— "注册" / "登录" 两个面板动态显示与隐藏
 *   2. 表单验证 —— 注册（用户名/邮箱/密码/确认密码/验证码/手机号）、登录（账号/密码）
 *   3. 动态效果 —— 密码强度提示、密码显示/隐藏、手机号输入框动态显隐、错误提示显隐
 *   4. 数据提交 —— 校验通过后提交到 register.jsp / login.jsp，由后端写入 cxh_sql 数据库
 */
(function () {
    "use strict";

    /* ================= 元素引用 ================= */
    // 选项卡
    var tabRegister = document.getElementById("tabRegister");
    var tabLogin = document.getElementById("tabLogin");
    var panelRegister = document.getElementById("panelRegister");
    var panelLogin = document.getElementById("panelLogin");

    // 注册表单
    var registerForm = document.getElementById("registerForm");
    var usernameInput = document.getElementById("username");
    var emailInput = document.getElementById("email");
    var passwordInput = document.getElementById("password");
    var confirmInput = document.getElementById("confirmPwd");
    var captchaInput = document.getElementById("captcha");
    var captchaCanvas = document.getElementById("captchaCanvas");
    var phoneGroup = document.getElementById("phoneGroup");
    var phoneInput = document.getElementById("phone");
    var bindPhone = document.getElementById("bindPhone");
    var agree = document.getElementById("agree");
    var submitBtn = document.getElementById("submitBtn");

    // 登录表单
    var loginForm = document.getElementById("loginForm");
    var accountInput = document.getElementById("account");
    var loginPwdInput = document.getElementById("loginPwd");

    /* ================= 校验规则（正则） ================= */
    var REG = {
        username: /^[a-zA-Z0-9_]{3,16}$/,                    // 3~16位字母、数字、下划线
        email: /^[\w.%+-]+@[\w-]+(\.[\w-]+)+$/,              // 常见邮箱格式
        password: /^(?=.*[a-zA-Z])(?=.*\d)[\w!@#$%^&*.]{8,20}$/, // 8~20位，须含字母和数字
        phone: /^1[3-9]\d{9}$/                               // 11位手机号
    };

    var currentCaptcha = ""; // 当前验证码文本

    /* ================= 工具函数 ================= */

    /**
     * 显示 / 清除 某个字段的错误提示，并切换输入框样式
     * @param {HTMLInputElement} input 输入框
     * @param {string} tipId      错误提示元素 id
     * @param {string} [message]  错误信息；不传或为空则清除错误
     */
    function setError(input, tipId, message) {
        var tip = document.getElementById(tipId);
        if (message) {
            tip.textContent = message;
            tip.classList.add("show");
            input.classList.add("input-error");
            input.classList.remove("input-success");
        } else {
            tip.textContent = "";
            tip.classList.remove("show");
            input.classList.remove("input-error");
            if (input.value.trim() !== "") {
                input.classList.add("input-success");
            }
        }
    }

    /* ================= 选项卡切换 ================= */

    /** 切换"注册/登录"面板，实现表单内容的动态显示与隐藏 */
    function switchTab(showRegister) {
        if (showRegister) {
            tabRegister.classList.add("active");
            tabLogin.classList.remove("active");
            panelRegister.hidden = false;
            panelLogin.hidden = true;
        } else {
            tabLogin.classList.add("active");
            tabRegister.classList.remove("active");
            panelLogin.hidden = false;
            panelRegister.hidden = true;
        }
    }

    /* ================= 注册表单校验 ================= */

    /** 用户名：3~16位字母、数字或下划线 */
    function validateUsername() {
        var value = usernameInput.value.trim();
        if (value === "") {
            setError(usernameInput, "usernameError", "用户名不能为空");
            return false;
        }
        if (!REG.username.test(value)) {
            setError(usernameInput, "usernameError", "用户名为3~16位字母、数字或下划线");
            return false;
        }
        setError(usernameInput, "usernameError");
        return true;
    }

    /** 邮箱格式 */
    function validateEmail() {
        var value = emailInput.value.trim();
        if (value === "") {
            setError(emailInput, "emailError", "邮箱不能为空");
            return false;
        }
        if (!REG.email.test(value)) {
            setError(emailInput, "emailError", "邮箱格式不正确，如 name@example.com");
            return false;
        }
        setError(emailInput, "emailError");
        return true;
    }

    /** 密码：8~20位且同时包含字母和数字 */
    function validatePassword() {
        var value = passwordInput.value;
        if (value === "") {
            setError(passwordInput, "passwordError", "密码不能为空");
            return false;
        }
        if (!REG.password.test(value)) {
            setError(passwordInput, "passwordError", "密码须为8~20位，且同时包含字母和数字");
            return false;
        }
        setError(passwordInput, "passwordError");
        return true;
    }

    /** 确认密码：与密码一致 */
    function validateConfirm() {
        var value = confirmInput.value;
        if (value === "") {
            setError(confirmInput, "confirmError", "请再次输入密码");
            return false;
        }
        if (value !== passwordInput.value) {
            setError(confirmInput, "confirmError", "两次输入的密码不一致");
            return false;
        }
        setError(confirmInput, "confirmError");
        return true;
    }

    /** 验证码：与图片验证码比对（忽略大小写） */
    function validateCaptcha() {
        var value = captchaInput.value.trim();
        if (value === "") {
            setError(captchaInput, "captchaError", "请输入验证码");
            return false;
        }
        if (value.toLowerCase() !== currentCaptcha.toLowerCase()) {
            setError(captchaInput, "captchaError", "验证码错误，请重新输入");
            return false;
        }
        setError(captchaInput, "captchaError");
        return true;
    }

    /** 手机号（仅当勾选绑定时校验） */
    function validatePhone() {
        if (!bindPhone.checked) {
            return true; // 未勾选，跳过
        }
        var value = phoneInput.value.trim();
        if (value === "") {
            setError(phoneInput, "phoneError", "请输入手机号");
            return false;
        }
        if (!REG.phone.test(value)) {
            setError(phoneInput, "phoneError", "手机号格式不正确（11位数字）");
            return false;
        }
        setError(phoneInput, "phoneError");
        return true;
    }

    /** 注册提交时校验全部字段，返回是否通过 */
    function validateRegisterAll() {
        var ok = true;
        ok = validateUsername() && ok;
        ok = validateEmail() && ok;
        ok = validatePassword() && ok;
        ok = validateConfirm() && ok;
        ok = validateCaptcha() && ok;
        ok = validatePhone() && ok;

        // 协议勾选校验
        var agreeTip = document.getElementById("agreeError");
        if (!agree.checked) {
            agreeTip.textContent = "请先阅读并同意《用户注册协议》";
            agreeTip.classList.add("show");
            ok = false;
        } else {
            agreeTip.textContent = "";
            agreeTip.classList.remove("show");
        }
        return ok;
    }

    /* ================= 登录表单校验 ================= */

    /** 登录账号：非空 */
    function validateAccount() {
        var value = accountInput.value.trim();
        if (value === "") {
            setError(accountInput, "accountError", "账号不能为空");
            return false;
        }
        setError(accountInput, "accountError");
        return true;
    }

    /** 登录密码：非空 */
    function validateLoginPwd() {
        var value = loginPwdInput.value;
        if (value === "") {
            setError(loginPwdInput, "loginPwdError", "密码不能为空");
            return false;
        }
        setError(loginPwdInput, "loginPwdError");
        return true;
    }

    /* ================= 动态效果：密码强度提示 ================= */

    /**
     * 计算密码强度：0 无，1 弱，2 中，3 强
     * 评分依据：长度 + 字符种类（小写/大写/数字/特殊字符）
     */
    function calcStrength(pwd) {
        if (pwd === "") {
            return 0;
        }
        var score = 0;
        if (pwd.length >= 8) { score += 1; }
        if (pwd.length >= 12) { score += 1; }
        if (/[a-z]/.test(pwd)) { score += 1; }
        if (/[A-Z]/.test(pwd)) { score += 1; }
        if (/\d/.test(pwd)) { score += 1; }
        if (/[^a-zA-Z0-9]/.test(pwd)) { score += 1; }

        if (score <= 2) { return 1; }      // 弱
        if (score <= 4) { return 2; }      // 中
        return 3;                          // 强
    }

    /** 更新密码强度条与文字提示 */
    function updateStrength() {
        var bar = document.getElementById("strengthBar");
        var text = document.getElementById("strengthText");
        var level = calcStrength(passwordInput.value);

        if (level === 0) {
            bar.hidden = true;               // 密码为空时隐藏强度条
            text.textContent = "";
            text.className = "strength-text";
            return;
        }

        bar.hidden = false;
        bar.className = "strength-bar level-" + level;

        var desc = ["", "弱：建议增加长度与字符种类", "中：可再混合大小写与符号", "强：密码强度很好"];
        text.textContent = desc[level];
        text.className = "strength-text " + ["", "weak", "medium", "strong"][level];
    }

    /* ================= 动态效果：密码显示/隐藏 ================= */

    /** 切换输入框的密码可见性 */
    function togglePassword(input, btn) {
        if (input.type === "password") {
            input.type = "text";
            btn.textContent = "🙈";
        } else {
            input.type = "password";
            btn.textContent = "👁";
        }
    }

    /* ================= 动态效果：图形验证码 ================= */

    /** 生成4位验证码文本（去掉易混淆字符 0/O/1/I/l） */
    function generateCaptchaText() {
        var chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        var code = "";
        for (var i = 0; i < 4; i++) {
            code += chars.charAt(Math.floor(Math.random() * chars.length));
        }
        return code;
    }

    /** 在 canvas 上绘制验证码（随机颜色 + 干扰线 + 旋转字符） */
    function drawCaptcha() {
        var ctx = captchaCanvas.getContext("2d");
        var w = captchaCanvas.width;
        var h = captchaCanvas.height;

        // 背景
        ctx.fillStyle = "#eef2ff";
        ctx.fillRect(0, 0, w, h);

        // 干扰线
        for (var i = 0; i < 5; i++) {
            ctx.strokeStyle = "rgba(" + randInt(100, 200) + "," + randInt(100, 200) + "," + randInt(100, 255) + ",0.6)";
            ctx.beginPath();
            ctx.moveTo(randInt(0, w), randInt(0, h));
            ctx.lineTo(randInt(0, w), randInt(0, h));
            ctx.stroke();
        }

        // 干扰点
        for (var j = 0; j < 40; j++) {
            ctx.fillStyle = "rgba(120,120,120,0.4)";
            ctx.fillRect(randInt(0, w), randInt(0, h), 1.5, 1.5);
        }

        // 绘制字符（逐个随机旋转、着色）
        currentCaptcha = generateCaptchaText();
        for (var k = 0; k < currentCaptcha.length; k++) {
            var deg = randInt(-25, 25) * Math.PI / 180;
            ctx.save();
            ctx.translate(16 + k * 24, h / 2);
            ctx.rotate(deg);
            ctx.font = "bold 22px Arial";
            ctx.fillStyle = "rgb(" + randInt(30, 120) + "," + randInt(30, 120) + "," + randInt(160, 230) + ")";
            ctx.textAlign = "center";
            ctx.textBaseline = "middle";
            ctx.fillText(currentCaptcha.charAt(k), 0, 0);
            ctx.restore();
        }
    }

    function randInt(min, max) {
        return Math.floor(Math.random() * (max - min + 1)) + min;
    }

    /* ================= 动态效果：手机号显隐 ================= */

    /** 勾选"绑定手机号"时动态显示/隐藏手机号输入框 */
    function togglePhoneGroup() {
        if (bindPhone.checked) {
            phoneGroup.hidden = false;
        } else {
            phoneGroup.hidden = true;
            phoneInput.value = "";
            setError(phoneInput, "phoneError");
        }
    }

    /* ================= 数据提交 ================= */

    /** 注册表单提交：校验通过则放行提交到 register.jsp */
    function handleRegisterSubmit(event) {
        if (!validateRegisterAll()) {
            event.preventDefault(); // 校验失败，阻止提交
            return;
        }
        // 校验通过：按钮短暂禁用防止重复提交，随后由浏览器提交到 register.jsp
        submitBtn.disabled = true;
        submitBtn.textContent = "提交中...";
    }

    /** 登录表单提交：校验通过则放行提交到 login.jsp */
    function handleLoginSubmit(event) {
        var ok = validateAccount();
        ok = validateLoginPwd() && ok;
        if (!ok) {
            event.preventDefault(); // 校验失败，阻止提交
        }
    }

    /* ================= 事件绑定 ================= */

    // 选项卡切换
    tabRegister.addEventListener("click", function () { switchTab(true); });
    tabLogin.addEventListener("click", function () { switchTab(false); });

    // ---- 注册表单实时校验（input 事件）+ 失焦校验（blur 事件） ----
    usernameInput.addEventListener("input", validateUsername);
    usernameInput.addEventListener("blur", validateUsername);

    emailInput.addEventListener("input", validateEmail);
    emailInput.addEventListener("blur", validateEmail);

    passwordInput.addEventListener("input", function () {
        updateStrength();
        validatePassword();
        // 密码变化后，若确认密码已填写则同步校验
        if (confirmInput.value !== "") {
            validateConfirm();
        }
    });
    passwordInput.addEventListener("blur", validatePassword);

    confirmInput.addEventListener("input", validateConfirm);
    confirmInput.addEventListener("blur", validateConfirm);

    captchaInput.addEventListener("input", function () {
        // 输入满4位立即校验
        if (captchaInput.value.trim().length === 4) {
            validateCaptcha();
        } else {
            setError(captchaInput, "captchaError");
        }
    });
    captchaInput.addEventListener("blur", validateCaptcha);

    phoneInput.addEventListener("input", validatePhone);
    phoneInput.addEventListener("blur", validatePhone);

    // ---- 登录表单校验 ----
    accountInput.addEventListener("input", validateAccount);
    accountInput.addEventListener("blur", validateAccount);
    loginPwdInput.addEventListener("input", validateLoginPwd);
    loginPwdInput.addEventListener("blur", validateLoginPwd);

    // 密码显示/隐藏
    document.getElementById("togglePwd").addEventListener("click", function () {
        togglePassword(passwordInput, this);
    });
    document.getElementById("toggleConfirmPwd").addEventListener("click", function () {
        togglePassword(confirmInput, this);
    });
    document.getElementById("toggleLoginPwd").addEventListener("click", function () {
        togglePassword(loginPwdInput, this);
    });

    // 验证码：点击刷新
    captchaCanvas.addEventListener("click", function () {
        drawCaptcha();
        captchaInput.value = "";
        setError(captchaInput, "captchaError");
        captchaInput.focus();
    });

    // 手机号动态显隐
    bindPhone.addEventListener("change", togglePhoneGroup);

    // 协议链接：演示弹窗提示
    document.getElementById("agreementLink").addEventListener("click", function () {
        alert("《用户注册协议》演示内容：\n1. 请勿使用他人身份信息注册；\n2. 账号仅限本人使用；\n3. 注册数据将保存到 cxh_sql 数据库。");
    });

    // 表单提交
    registerForm.addEventListener("submit", handleRegisterSubmit);
    loginForm.addEventListener("submit", handleLoginSubmit);

    /* ================= 初始化 ================= */
    drawCaptcha();

    // 从 register.jsp / login.jsp 结果页跳转回来时，根据标记自动切换选项卡
    var autoTab = sessionStorage.getItem("autoTab");
    if (autoTab === "login") {
        switchTab(false);
        sessionStorage.removeItem("autoTab");
    }
})();
