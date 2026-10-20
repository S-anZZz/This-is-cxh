# 用 Cloudflare Access 保护后台（/admin 与 /login）

博客本身是公开的，但 `/admin` 是写入口。加上 Cloudflare Access 后，访问后台会先要求
**邮箱验证码**，通过后再输入博客自己的账号口令——两层防护，且免费。

> 为什么要额外加一层：Spring Security 的登录没有内置失败次数限制，
> 暴露在公网时理论上可被暴力尝试。Access 直接把未授权请求挡在 Cloudflare 边缘，
> 连应用的登录页都看不到。

## 前置

- 域名 `cxh327.dpdns.org` 已走 Cloudflare 代理（橙云）—— 当前就是；
- Cloudflare 账号可开通 **Zero Trust Free**（50 用户以内免费，个人够用）。

## 方式 A：面板点选（约 3 分钟）

1. **开通 Zero Trust**：Cloudflare 控制台 → 左侧 **Zero Trust** → 首次进入会让你起一个
   Team name（例如 `cxh`），选 **Free** 计划。
2. **确认登录方式**：Zero Trust → **Settings → Authentication** → 确保 **One-time PIN** 是开启的
   （默认开启，用邮箱收验证码，不需要配 IdP）。
3. **新建应用**：**Access → Applications → Add an application → Self-hosted**
   - Application name：`cxh-blog-admin`
   - Session Duration：`24 hours`
   - Public hostname：
     - `cxh327.dpdns.org` + path `admin`
     - 再加一条 `cxh327.dpdns.org` + path `login`
   - 保存
4. **加策略**：在应用的 Policies 里 **Add a policy**
   - Action：`Allow`
   - Include → **Emails** → 填你自己的邮箱
   - 保存
5. **验证**：用隐身窗口打开 `https://cxh327.dpdns.org/admin`，应先出现 Cloudflare 的邮箱验证页；
   输入邮箱 → 收验证码 → 通过后才看到博客的登录页。

## 方式 B：API（我可以代做）

如果你想让我直接配好并验证，需要一个**权限最小**的 API Token：

1. Cloudflare 控制台 → 右上头像 → **My Profile → API Tokens → Create Token**
2. 用 **Custom token**，权限：
   - `Account` → `Access: Apps and Policies` → **Edit**
   - `Account` → `Access: Organizations, Identity Providers, and Groups` → **Edit**
   - `Zone` → `DNS` → **Read**（可选，便于我核对记录）
3. Account Resources 选你的账号；Zone Resources 选 `cxh327.dpdns.org`
4. 生成后把 Token 发我（用完可立即吊销）

我会用 API 完成：创建 Access 组织（若不存在）→ 建两个 Self-hosted 应用（`/admin`、`/login`）
→ 加一条 Allow 策略（邮箱由你指定）→ 然后从公网验证未登录时确实被拦。

## 注意

- Access 只保护你指定的路径。`/`、`/posts/**`、`/uploads/**` 等公开内容不受影响。
- 万一 Access 配错把自己锁在外面：在 Zero Trust → Applications 里临时禁用该应用即可恢复。
- Access 与博客自身登录是两套独立机制，互不影响；退出 Access 需要清浏览器会话或等待 24 小时过期。

---

## 附：本项目已完成的实际配置（2026-10-20）

通过 Cloudflare API 完成，配置如下：

| 项 | 值 |
| --- | --- |
| 组织名 | `cxh-blog` |
| 团队域 | `raspy-pond-db06.cloudflareaccess.com` |
| 身份提供方 | One-time PIN（邮箱验证码） |
| 受保护应用 1 | `cxh327.dpdns.org/admin`（session 24h） |
| 受保护应用 2 | `cxh327.dpdns.org/login`（session 24h） |
| 放行策略 | `allow` + 邮箱 `2027652229@qq.com` |

验证结果（从公网实测）：

```
GET /admin        → 302 https://raspy-pond-db06.cloudflareaccess.com/cdn-cgi/access/login/...
GET /admin/posts  → 302 同上
GET /login        → 302 同上
GET /             → 200（公开内容不受影响）
GET /posts /archive /tags /search /theme.css /feed.xml /sitemap.xml → 全部 200
```

首次登录流程：

1. 打开 `https://cxh327.dpdns.org/admin`
2. 跳转到 Cloudflare Access，输入邮箱 `2027652229@qq.com`
3. 收邮件里的 6 位验证码并填入（24 小时内免重复验证）
4. 再输入博客自己的后台账号口令

> 如果第 3 步没收到邮件，检查垃圾箱；仍不行就在 Zero Trust → Settings → Authentication 里确认
> One-time PIN 处于启用状态。

### 安全提醒

配置时使用的 API Token 请**用完后立即吊销**：
Cloudflare → My Profile → API Tokens → 找到该 Token → Delete。
吊销后不影响已建好的 Access 应用。
