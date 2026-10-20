# 部署到 cxh327.dpdns.org（VPS + systemd + Nginx + MySQL）

> **先说清楚一件事：GitHub Pages 不能运行 Spring Boot。**
> 目前 `cxh327.dpdns.org` 指向的是 `S-anZZz/This-is-cxh` 的 GitHub Pages，
> 只能提供静态文件（`index.html`、`one.js`、图片）。Spring Boot 需要 JVM 进程和数据库，
> 因此必须换一台能跑 Java 的机器（VPS / 云主机 / 支持容器的 PaaS），再把域名指过去。
>
> 这份目录就是为此准备的：照下面做，域名不变、内容换成新的博客系统。

---

## 0. 准备什么

| 需要 | 说明 |
| --- | --- |
| 一台 Linux 服务器 | Ubuntu 22.04 / 24.04 或 Debian 12；1 核 1G 起步（建议 2C2G），能跑 Java 17 与 MySQL |
| 公网 IP | 阿里云/腾讯云/华为云的轻量服务器都行；也可用海外小机器（更省事，免备案） |
| 域名控制权 | 能改 `cxh327.dpdns.org` 的 DNS（dpdns.org 的解析面板） |
| **可选** SSH 访问 | 有的话可以让我直接远程完成部署；没有也能按步骤自己执行 |

> 备案提示：若服务器在中国大陆，域名需完成 ICP 备案才能用 80/443 访问；海外或港澳台机器不需要。

---

## 1. 上传并安装（服务器上执行）

```bash
# 本地：打包（产物约 70MB）
./mvnw clean package -DskipTests
scp target/cxh-blog.jar root@<服务器IP>:/tmp/
scp -r deploy root@<服务器IP>:/root/cxh-deploy/     # 只需要 deploy/vps 这部分

# 服务器：一键安装
ssh root@<服务器IP>
cd /root/cxh-deploy
JAR=/tmp/cxh-blog.jar bash deploy/vps/install.sh
```

安装脚本会自动完成：装 JDK17/Nginx/MySQL/certbot → 建 `cxh` 运行用户与目录 →
建库建号（随机口令，**会打印一次，请记录**）→ 装 systemd 单元 → 装 Nginx 站点 →
启动并轮询 `/actuator/health` 直到就绪。

装完自检：

```bash
curl -s http://127.0.0.1:8080/actuator/health     # 应为 {"status":"UP"}
systemctl status cxh-blog                          # active (running)
journalctl -u cxh-blog -f                          # 看日志
```

---

## 2. 切换域名（关键：按顺序做，避免宕机）

现在的状态是：`cxh327.dpdns.org` → Cloudflare → GitHub Pages。
要变成：`cxh327.dpdns.org` → Cloudflare（可选）→ 你的服务器。

**顺序很重要：先把应用在新机器上跑通并验证，再改 DNS，最后再动 GitHub 仓库。**
这样旧站一直在线，直到新站确认可用。

### 2.1 先用 IP 验证（改 DNS 之前）

在 Cloudflare/解析面板临时加一条记录，例如 `test.cxh327.dpdns.org` → 服务器 IP，
或者直接用服务器 IP 访问 `http://<IP>/`（需要 Nginx 的 default_server 或临时 server_name）。
确认能看到博客首页，再继续。

### 2.2 改 DNS

在 dpdns.org 的解析面板（或 Cloudflare）把 `cxh327.dpdns.org` 的 A 记录指向服务器公网 IP。
如果原来有指向 GitHub Pages 的 CNAME，**删掉它**。

### 2.3 证书（两种方案选一个）

| 方案 | 适用 | 做法 |
| --- | --- | --- |
| **A. Cloudflare 代理（橙云）** | 想继续用 Cloudflare 的 CDN/防护 | 1) DNS 保持橙云；2) Cloudflare → SSL/TLS → Origin Server → 创建 **Origin Certificate**；3) 把证书与私钥放到服务器（如 `/etc/ssl/cxh/`）；4) 修改 `/etc/nginx/sites-available/cxh-blog.conf` 的 `ssl_certificate` 路径；5) SSL 模式选 **Full (strict)**；6) `nginx -t && systemctl reload nginx` |
| **B. 仅 DNS（灰云）** | 不想依赖 Cloudflare | 1) DNS 改灰云；2) `certbot --nginx -d cxh327.dpdns.org`；3) 把完整配置启回：`ln -sf /etc/nginx/sites-available/cxh-blog.conf /etc/nginx/sites-enabled/cxh-blog.conf`；4) `nginx -t && systemctl reload nginx` |

> 为什么安装脚本临时只启用了 HTTP：首次部署时还没有证书，`nginx -t` 会因为找不到证书文件而失败。
> 证书就绪后按上面第 3 步把 HTTPS 段启用即可。

验证：

```bash
curl -sI https://cxh327.dpdns.org/ | head -5      # 200 + text/html
curl -s  https://cxh327.dpdns.org/actuator/health  # {"status":"UP"}
```

### 2.4 处理 GitHub 仓库

确认新站稳定后，二选一：

- **保留仓库做代码仓库（推荐）**：把当前 Spring Boot 工程推到 `S-anZZz/This-is-cxh`，
  并在仓库 Settings → Pages 里 **Disable** Pages（否则仓库根目录没有 `index.html`，Pages 会 404，但域名已不指向它，无影响）。
- **想留旧站备份**：先 `git push origin main:legacy-static` 把旧静态站存成分支，再替换 `main`。

---

## 3. 日常运维

| 操作 | 命令 |
| --- | --- |
| 启动 / 停止 / 重启 | `systemctl {start,stop,restart} cxh-blog` |
| 看日志 | `journalctl -u cxh-blog -f` |
| 改配置 | 改 `/etc/cxh-blog/cxh-blog.env` 后 `systemctl restart cxh-blog` |
| 备份 | `BACKUP_DIR=/var/backups/cxh scripts/backup.sh --mysql`（建议进 crontab） |
| 升级 | `sudo bash deploy/vps/upgrade.sh /tmp/cxh-blog.jar`（失败自动回滚） |
| 上传图片 | 存在 `/opt/cxh-blog/uploads`，已纳入备份 |

建议加一条每日备份：

```bash
sudo mkdir -p /var/backups/cxh
echo '0 3 * * * cd /opt/cxh-blog && BACKUP_DIR=/var/backups/cxh bash /opt/cxh-blog/scripts/backup.sh --mysql' \
  | sudo tee /etc/cron.d/cxh-backup
```

---

## 4. 常见问题

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| `502 Bad Gateway` | 应用没起来 | `systemctl status cxh-blog`、`journalctl -u cxh-blog -n 50` |
| 打开是旧静态站 | DNS 还指向 Pages / 缓存 | `dig +short cxh327.dpdns.org` 看 IP；Cloudflare 清缓存 |
| `ERR_CONNECTION_REFUSED` | 服务未运行或端口不对 | `curl 127.0.0.1:8080/actuator/health`、`ss -lntp \| grep 8080` |
| 上传图片 413 | Nginx 限制太小 | 确认 `client_max_body_size 10m` 已生效并 reload |
| 证书报错 `nginx -t` | 证书路径不对 | 按 2.3 放好证书，或先只启用 HTTP 段 |
| 启动报 `Schema-validation` | 数据库结构与迁移脚本不一致 | 检查是否改过 `db/migration`；新库直接让 Flyway 建表 |
| MySQL 口令忘了 | 存在 `/etc/cxh-blog/cxh-blog.env` | `sudo grep DB_PASSWORD /etc/cxh-blog/cxh-blog.env` |

---

## 5. 备选：不买服务器

如果不想维护 VPS，可以用支持容器的 PaaS（Render / Railway / Fly.io / 阿里云函数计算容器等）：

1. 仓库里已有 `Dockerfile`（两阶段构建，非 root 运行，带健康检查），PaaS 可直接识别；
2. 数据库用平台提供的 MySQL，或改用内置 H2 + 持久磁盘（把 `CXH_MEDIA_LOCATION` 与 `./data` 都放持久盘）；
3. 环境变量按 `deploy/vps/cxh-blog.env.example` 填；
4. 平台绑定自定义域名 `cxh327.dpdns.org`，按平台提示加 CNAME，再回到 Cloudflare 处理证书；
5. 注意：免费实例通常会休眠，首次访问会慢；个人博客建议至少选最低档常驻实例。

---

## 6. 验证清单

- [ ] `curl -s https://cxh327.dpdns.org/actuator/health` 返回 `{"status":"UP"}`
- [ ] 首页、文章列表、详情、标签、归档、搜索都能打开
- [ ] `/feed.xml`、`/sitemap.xml` 返回 200 且是合法 XML
- [ ] `/admin` 能登录，能发文章
- [ ] 后台上传一张图片，前台能显示（说明 `/uploads/` 转发正常）
- [ ] 在「外观设置」改主色并保存，首页立即变色（说明 `/theme.css` 未被缓存）
- [ ] 重启服务器后自动恢复（`systemctl is-enabled cxh-blog` 为 enabled）
