# 用 Cloudflare 保留原域名部署（两种编排）

目标：`https://cxh327.dpdns.org/` 继续走 Cloudflare、域名不变，但内容换成 Spring Boot 博客。

Cloudflare 不执行 Java，所以必须有一个**源站**。下面两条路都能保住「Cloudflare + 原域名」：

| 方案 | 源站是什么 | 需要 | 适合 |
| --- | --- | --- | --- |
| **A. 橙云代理**（传统） | VPS / 云主机 | 服务器公网 IP、开放 80/443、证书 | 想 7×24 稳定、有服务器 |
| **B. Cloudflare Tunnel**（推荐给没服务器的） | 任意一台能跑 Java 的机器（Mac / 迷你主机 / NAS / VPS） | 只装一个 `cloudflared`，**不开放任何入站端口** | 不想买服务器、机器本来常开 |

两种方案的共同点：DNS 都留在 Cloudflare，访问者看到的始终是 `https://cxh327.dpdns.org`。

---

## 方案 B：Cloudflare Tunnel（不需要公网 IP）

原理：`cloudflared` 从源站**主动外连** Cloudflare，Cloudflare 把请求沿隧道转发进来。
所以：不用开端口、不用买证书、不用动路由器，TLS 由 Cloudflare 边缘终结。

### B1. 在源站上安装 cloudflared

```bash
# macOS
brew install cloudflared

# Debian / Ubuntu
curl -fsSL https://pkg.cloudflare.com/cloudflare-main.gpg \
  | sudo tee /usr/share/keyrings/cloudflare-main.gpg >/dev/null
echo "deb [signed-by=/usr/share/keyrings/cloudflare-main.gpg] https://pkg.cloudflare.com/cloudflared any main" \
  | sudo tee /etc/apt/sources.list.d/cloudflared.list
sudo apt-get update && sudo apt-get install -y cloudflared
```

### B1b. macOS 源站的额外说明

macOS 没有 systemd，`setup-tunnel.sh` 只适用于 Linux。在 Mac 上当源站时：

```bash
cloudflared tunnel login
cloudflared tunnel create cxh-blog
# 前台运行（关掉终端即停）
cloudflared tunnel --url http://127.0.0.1:8080 run cxh-blog
```

或用 launchd 常驻（适合 Mini 长期开机）。**注意：机器关机/休眠，站点就会 502。**

### B2. 登录并创建隧道（需要浏览器登录你的 Cloudflare 账号）

```bash
cloudflared tunnel login          # 浏览器里授权 cxh327.dpdns.org
cloudflared tunnel create cxh-blog
# 记下输出的 UUID，并把凭据放到配置目录
```

### B3. 生成配置并起服务

```bash
# 本目录提供的脚本会读凭据、写配置、装 systemd（Linux）
sudo bash deploy/cloudflare/setup-tunnel.sh
```

脚本等价于：

```yaml
# /etc/cloudflared/config.yml
tunnel: <UUID>
credentials-file: /etc/cloudflared/<UUID>.json
ingress:
  - hostname: cxh327.dpdns.org
    service: http://127.0.0.1:8080
  - service: http_status:404
```

```bash
cloudflared tunnel route dns cxh-blog cxh327.dpdns.org
sudo systemctl enable --now cxh-blog-tunnel
```

### B4. 在 Cloudflare 面板确认

- DNS 里会出现一条 `cxh327.dpdns.org → <UUID>.cfargotunnel.com` 的 CNAME，**必须保持橙云（已代理）**；
- SSL/TLS 模式设为 **Full** 或 **Full (strict)**（隧道场景下 Cloudflare 到源站由隧道加密，不会报证书错误）；
- 删掉原来指向 GitHub Pages 的记录（不要保留冲突的 A/CNAME）。

### B5. 验证

```bash
curl -sI https://cxh327.dpdns.org/ | head -3          # 200 text/html
curl -s  https://cxh327.dpdns.org/actuator/health     # {"status":"UP"}
```

> 注意：源站机器需要**保持开机**。关掉 Mac/断电，站点就会 502。

---

## 方案 A：橙云代理到 VPS

见 [`../vps/README.md`](../vps/README.md)。要点：

1. VPS 上跑 `deploy/vps/install.sh`（systemd + Nginx + MySQL）；
2. Cloudflare DNS 新增 `cxh327.dpdns.org` 的 **A 记录 → VPS IP**，保持橙云；
3. 证书用 **Cloudflare Origin Certificate**（15 年、免费），把证书路径写进 Nginx 配置，SSL 模式 **Full (strict)**；
4. 删除原指向 GitHub Pages 的记录。

---

## 迁移期间怎么避免宕机

```
① 源站先把应用跑通（本地 curl 127.0.0.1:8080 或临时子域名）
② 再切换 cxh327.dpdns.org 的 DNS（改指向 VPS 或建隧道路由）
③ 验证 HTTPS 与后台登录
④ 最后再替换 GitHub 仓库内容 / 关闭 Pages
```

## 常见问题

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| 502 Bad Gateway | 源站没起或隧道没连上 | `systemctl status cxh-blog`、`journalctl -u cxh-blog-tunnel -f` |
| 打开还是旧静态站 | DNS 仍指向 Pages 或 Cloudflare 缓存 | 看 DNS 记录；Cloudflare → 缓存 → 清除全部 |
| 隧道显示 connected 但 404 | ingress 的 hostname 与访问域名不一致 | 核对 `config.yml` 与 `tunnel route dns` 的域名 |
| 上传图片 413 | Cloudflare/客户端限制 | Cloudflare 免费版请求体上限 100MB，远大于本项目 8MB，检查是否走了别的代理 |
| 后台登录后跳回登录页 | Cookie/协议问题 | 确认 SSL 模式 Full 且 `X-Forwarded-Proto` 为 https |
