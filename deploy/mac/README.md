# 部署到本机 Mac（Cloudflare Tunnel，保留原域名）

> 已实测：在**这台 Mac** 上跑通「本机应用 → cloudflared → Cloudflare 边缘 → 公网 HTTPS」整条链路，
> 公网响应头含 `server: cloudflare` 与 `cf-ray`，外部抓取器可取到 `{"status":"UP"}`。

## 为什么可行

`cxh327.dpdns.org` 的 NS 是 `rene.ns.cloudflare.com` / `savanna.ns.cloudflare.com`，
说明这个子域是**独立的 Cloudflare zone**（在你账号下）。这是 Cloudflare Tunnel 的硬前提，已满足。

```
访客 ──HTTPS──▶ Cloudflare 边缘 ──隧道(出站)──▶ 你的 Mac: cloudflared ──▶ Spring Boot :8080 ──▶ H2
```

- Mac **不需要公网 IP**、不需要路由器端口映射（出口 IP 变化也不影响）；
- 不需要备案；
- TLS 由 Cloudflare 边缘终结，**本机不用装证书**；
- 域名保持 `cxh327.dpdns.org` 不变。

## 前提

| 项 | 检查命令 |
| --- | --- |
| JDK 17+ | `java -version`（脚本会用 `java_home -v 17` 优先固定到 17） |
| cloudflared | `brew install cloudflared`，或把二进制放到项目 `.tools/cloudflared` |
| 已构建 jar | `./mvnw clean package -DskipTests` |
| 域名 zone 在 Cloudflare | 已确认 |

## 一次性准备（需要你在浏览器点一下授权）

```bash
cd /Users/san/IJidea/CXHMJ-proweb/cxhmj

# 1) 登录（会打开浏览器，选择 cxh327.dpdns.org 授权）
cloudflared tunnel login

# 2) 创建隧道（只需一次）
cloudflared tunnel create cxh-blog
```

完成后 `~/.cloudflared/` 下会有 `<UUID>.json` 凭据。

## 安装（一条命令）

```bash
bash deploy/mac/install.sh
```

脚本会：

1. 检查 java / cloudflared / 隧道凭据 / 端口占用；
2. 建 `runtime/`，把 `target/cxh-blog.jar` 复制为 `runtime/cxh-blog.jar`；
3. 生成 `runtime/env`（权限 600），内含**随机 16 位后台口令**并打印一次；
4. 写 `~/.cloudflared/config.yml`（`cxh327.dpdns.org → http://127.0.0.1:8080`）；
5. 执行 `cloudflared tunnel route dns`，在 Cloudflare 自动创建 CNAME；
6. 生成并加载两个 launchd 服务：`com.cxh.blog`（应用）、`com.cxh.tunnel`（隧道）；
7. 等应用健康检查通过、隧道注册成功，最后从公网 `https://cxh327.dpdns.org/actuator/health` 复核。

> 第 6 步会写 `~/Library/LaunchAgents`，必须在**本机终端**里执行（沙箱/远程会话下可能没有 GUI 域）。

## 验证

```bash
bash deploy/mac/status.sh

curl -s https://cxh327.dpdns.org/actuator/health    # {"status":"UP"}
curl -sI https://cxh327.dpdns.org/ | head -3        # 200 + server: cloudflare
open https://cxh327.dpdns.org/admin                 # 用打印出来的口令登录
```

首次切换 DNS 后可能需要 1–2 分钟生效（Cloudflare 的 CNAME 传播）。

## 运维

| 操作 | 命令 |
| --- | --- |
| 状态 | `bash deploy/mac/status.sh` |
| 看应用日志 | `tail -f logs/mac-app.log` |
| 看隧道日志 | `tail -f logs/mac-tunnel.log` |
| 重启应用 | `launchctl kickstart -k gui/$(id -u)/com.cxh.blog` |
| 重启隧道 | `launchctl kickstart -k gui/$(id -u)/com.cxh.tunnel` |
| 改口令/内存 | 编辑 `runtime/env` 后重启应用 |
| 升级 | 重新 `package` 后 `bash deploy/mac/install.sh`（会保留口令与数据） |
| 备份 | `scripts/backup.sh --h2`（`data/` 与 `uploads/` 都在项目内） |
| 卸载 | `bash deploy/mac/uninstall.sh`（保留数据） |

## 注意事项

1. **Mac 必须保持开机、联网、不睡眠**。脚本已用 `caffeinate -s` 在**接电源**时阻止系统睡眠；
   合盖、断电、Wi-Fi 断开会 502。想彻底稳定就把「系统设置 → 锁定屏幕/电池」里的休眠关掉。
2. **安全**：站点现在对公网开放。后台口令是随机生成的 16 位；若要更严，可在 Cloudflare 给
   `/admin*` 加 **Access** 策略（免费版支持少量用户），相当于再套一层登录。
3. **数据在本机**：`data/`（H2 库）与 `uploads/`（图片）都在项目目录里，已 gitignore。
   重装系统前记得备份。
4. **不要提交 `runtime/env`**：里面是后台口令，已在 `.gitignore` 中排除。
5. **网络变化**：换 Wi-Fi / 断网重连后隧道会自动重连（launchd 的 `KeepAlive` 会兜底重启）。

## 与 VPS 方案对比

| | 本机 Mac + Tunnel | 香港 VPS |
| --- | --- | --- |
| 成本 | 0 | ¥100–300/年 |
| 稳定性 | 依赖你的机器与家庭网络 | 机房 7×24 |
| 公网 IP | 不需要 | 需要 |
| 备案 | 不需要 | 香港不需要 / 大陆需要 |
| 延迟 | 家庭上行带宽决定 | 30–80ms |
| 适合 | 个人博客、折腾用 | 长期稳定对外 |

两者可以随时互换：应用、数据、域名都不变，只是把 Cloudflare 的指向从隧道换成 A 记录（或反之）。
