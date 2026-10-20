#!/usr/bin/env bash
#
# cxh 博客 · 一键安装（Ubuntu 22.04 / 24.04，Debian 12 亦可）
#
# 作用：装依赖 → 建运行用户与目录 → 建库建号 → 装 systemd 单元 → 装 Nginx 站点 → 启动
#
# 用法（在服务器上，把整个项目传上去后执行）：
#   sudo bash deploy/vps/install.sh                 # 用 target/cxh-blog.jar
#   sudo JAR=/tmp/cxh-blog.jar bash deploy/vps/install.sh
#   sudo DOMAIN=cxh327.dpdns.org bash deploy/vps/install.sh
#
# 做了什么（可重复执行，已存在的会跳过）：
#   /opt/cxh-blog/{cxh-blog.jar,data,uploads}      应用与数据
#   /etc/cxh-blog/cxh-blog.env                     运行环境变量（含随机数据库口令）
#   /etc/systemd/system/cxh-blog.service           开机自启
#   /etc/nginx/sites-available/cxh-blog.conf       站点配置
#
# 装完还要做两件事（脚本会打印提示）：
#   1) 解析域名到本机；2) 申请证书（见 deploy/vps/README.md）

set -euo pipefail

DOMAIN="${DOMAIN:-cxh327.dpdns.org}"
APP_DIR=/opt/cxh-blog
ENV_DIR=/etc/cxh-blog
ENV_FILE="$ENV_DIR/cxh-blog.env"
SERVICE=cxh-blog

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$HERE/../.." && pwd)"
JAR="${JAR:-$PROJECT_ROOT/target/cxh-blog.jar}"

log()  { printf '\n\033[1;34m==> %s\033[0m\n' "$1"; }
warn() { printf '\033[1;33m[注意] %s\033[0m\n' "$1"; }
die()  { printf '\033[1;31m[失败] %s\033[0m\n' "$1" >&2; exit 1; }

[ "$(id -u)" = "0" ] || die "请用 root 执行：sudo bash deploy/vps/install.sh"
[ -f "$JAR" ] || die "找不到 jar：$JAR（先在本地执行 ./mvnw clean package，再把 jar 传上来）"

# ---------------------------------------------------------------- 1 依赖
log "安装依赖"
export DEBIAN_FRONTEND=noninteractive
apt-get update -qq
apt-get install -y -qq openjdk-17-jre-headless nginx mysql-server \
    certbot python3-certbot-nginx curl >/dev/null
command -v java >/dev/null || die "java 未安装成功"
java -version 2>&1 | head -1 | sed 's/^/    /'

# ---------------------------------------------------------------- 2 用户与目录
log "创建运行用户与目录"
if ! id -u cxh >/dev/null 2>&1; then
    useradd --system --home "$APP_DIR" --shell /usr/sbin/nologin cxh
fi
install -d -o cxh -g cxh -m 755 "$APP_DIR" "$APP_DIR/uploads" "$APP_DIR/data"
install -d -m 750 "$ENV_DIR"

# 停服再换 jar（升级场景避免文件占用）
if systemctl is-active --quiet "$SERVICE"; then
    log "检测到服务在运行，先停止"
    systemctl stop "$SERVICE"
fi
install -o cxh -g cxh -m 640 "$JAR" "$APP_DIR/cxh-blog.jar"
log "已安装 jar（$(du -h "$APP_DIR/cxh-blog.jar" | cut -f1)）"

# ---------------------------------------------------------------- 3 环境变量
log "准备运行环境变量"
DB_PASSWORD=""
if [ -f "$ENV_FILE" ]; then
    log "已存在 $ENV_FILE，保留原配置"
    DB_PASSWORD="$(grep -E '^DB_PASSWORD=' "$ENV_FILE" | head -1 | cut -d= -f2- || true)"
    ADMIN_PASSWORD="$(grep -E '^CXH_ADMIN_PASSWORD=' "$ENV_FILE" | head -1 | cut -d= -f2- || true)"
else
    DB_PASSWORD="$(openssl rand -base64 24 | tr -dc 'A-Za-z0-9' | head -c 24)"
    ADMIN_PASSWORD="$(openssl rand -base64 24 | tr -dc 'A-Za-z0-9' | head -c 16)"
    cp "$HERE/cxh-blog.env.example" "$ENV_FILE"
    sed -i "s|^DB_PASSWORD=.*|DB_PASSWORD=$DB_PASSWORD|" "$ENV_FILE"
    sed -i "s|^CXH_ADMIN_PASSWORD=.*|CXH_ADMIN_PASSWORD=$ADMIN_PASSWORD|" "$ENV_FILE"
    warn "已生成随机口令，请立即记录（稍后可改：$ENV_FILE）"
    printf '    后台账号: admin\n    后台口令: %s\n    数据库口令: %s\n' "$ADMIN_PASSWORD" "$DB_PASSWORD"
fi
chown root:cxh "$ENV_FILE"
chmod 640 "$ENV_FILE"

# ---------------------------------------------------------------- 4 数据库
log "初始化数据库"
systemctl enable --now mysql >/dev/null 2>&1 || true
for _ in $(seq 1 30); do mysqladmin ping --silent >/dev/null 2>&1 && break; sleep 1; done
mysqladmin ping --silent >/dev/null 2>&1 || die "MySQL 未就绪"

mysql <<SQL
CREATE DATABASE IF NOT EXISTS cxh_blog
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE USER IF NOT EXISTS 'cxh'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
CREATE USER IF NOT EXISTS 'cxh'@'127.0.0.1' IDENTIFIED BY '${DB_PASSWORD}';
ALTER USER 'cxh'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
ALTER USER 'cxh'@'127.0.0.1' IDENTIFIED BY '${DB_PASSWORD}';
GRANT ALL PRIVILEGES ON cxh_blog.* TO 'cxh'@'localhost';
GRANT ALL PRIVILEGES ON cxh_blog.* TO 'cxh'@'127.0.0.1';
FLUSH PRIVILEGES;
SQL
log "数据库 cxh_blog 与用户 cxh 就绪（表结构由应用启动时的 Flyway 自动迁移）"

# ---------------------------------------------------------------- 5 systemd
log "安装 systemd 单元"
install -m 644 "$HERE/cxh-blog.service" "/etc/systemd/system/$SERVICE.service"
systemctl daemon-reload
systemctl enable "$SERVICE" >/dev/null 2>&1

# ---------------------------------------------------------------- 6 Nginx
log "安装 Nginx 站点配置"
install -m 644 "$HERE/nginx-cxh327.conf" /etc/nginx/sites-available/cxh-blog.conf
ln -sf /etc/nginx/sites-available/cxh-blog.conf /etc/nginx/sites-enabled/cxh-blog.conf
rm -f /etc/nginx/sites-enabled/default
install -d -m 755 /var/www/certbot

# 没有证书时先只启用 80 端口部分，避免 nginx -t 因缺少证书失败
if [ ! -f "/etc/letsencrypt/live/$DOMAIN/fullchain.pem" ]; then
    warn "未找到证书，临时只启用 HTTP（申请证书后再启用 HTTPS）"
    awk '/^server \{/{n++} n<2 || /^}/{if(n>=2) exit} n<2{print}' \
        /etc/nginx/sites-available/cxh-blog.conf > /etc/nginx/sites-available/cxh-blog.http.conf
    ln -sf /etc/nginx/sites-available/cxh-blog.http.conf /etc/nginx/sites-enabled/cxh-blog.conf
fi

# ---------------------------------------------------------------- 7 启动
log "启动应用"
systemctl restart "$SERVICE"

log "等待应用就绪（最多 90 秒）"
READY=0
for _ in $(seq 1 90); do
    if curl -fsS --max-time 3 http://127.0.0.1:8080/actuator/health >/dev/null 2>&1; then READY=1; break; fi
    sleep 1
done
if [ "$READY" != "1" ]; then
    journalctl -u "$SERVICE" -n 40 --no-pager | sed 's/^/    /'
    die "应用未能就绪，请查看上面的日志：journalctl -u $SERVICE -f"
fi
log "应用健康检查通过"

nginx -t >/dev/null 2>&1 && systemctl reload nginx || warn "nginx 配置检查未通过，请手工执行 nginx -t 查看原因"

# ---------------------------------------------------------------- 8 提示
cat <<EOF

========================================================
安装完成

  应用目录 : $APP_DIR
  环境变量 : $ENV_FILE
  日志     : journalctl -u $SERVICE -f
  本机自检 : curl -s http://127.0.0.1:8080/actuator/health

还需两步才能真正对外访问：

  1) 把域名 $DOMAIN 解析到本机公网 IP
     - 若走 Cloudflare 代理：A 记录指向本机 IP，SSL 模式建议 Full (strict)，
       并改用 Cloudflare Origin Certificate（把证书路径写进 nginx 配置）
     - 若不走代理（灰云）：直接 A 记录，然后用下面命令申请证书

  2) 申请证书（仅灰云方案需要）
     certbot --nginx -d $DOMAIN
     申请成功后执行：
       cp /etc/nginx/sites-available/cxh-blog.conf /etc/nginx/sites-enabled/cxh-blog.conf
       nginx -t && systemctl reload nginx

  后台登录：http://$DOMAIN/admin
========================================================
EOF
