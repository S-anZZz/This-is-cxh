#!/usr/bin/env bash
#
# cxh 博客备份脚本：备份数据库 + 上传的图片，并自动清理过期备份。
#
# 用法：
#   scripts/backup.sh                          # 自动识别 H2 / MySQL（本地部署）
#   scripts/backup.sh --mysql                  # 强制按 MySQL 备份
#   BACKUP_DIR=/data/backup scripts/backup.sh  # 指定备份目录
#   KEEP_DAYS=14 scripts/backup.sh             # 保留天数（默认 7）
#   scripts/backup.sh --docker                 # 备份 docker compose 里的 MySQL 与上传卷
#
# 建议加进 crontab（每天凌晨 3 点）：
#   0 3 * * * /path/to/cxhmj/scripts/backup.sh >> /var/log/cxh-backup.log 2>&1
#
# 备份产物：$BACKUP_DIR/cxh-blog-YYYYmmdd-HHMMSS/{database.sql|h2-data.tar.gz,uploads.tar.gz,manifest.txt}

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

BACKUP_DIR="${BACKUP_DIR:-$ROOT/backups}"
KEEP_DAYS="${KEEP_DAYS:-7}"
STAMP="$(date +%Y%m%d-%H%M%S)"
TARGET="$BACKUP_DIR/cxh-blog-$STAMP"

MODE="auto"
USE_DOCKER=0
for arg in "$@"; do
    case "$arg" in
        --mysql)  MODE="mysql" ;;
        --h2)     MODE="h2" ;;
        --docker) USE_DOCKER=1 ;;
        *) echo "未知参数：$arg" >&2; exit 2 ;;
    esac
done

log() { printf '[%s] %s\n' "$(date '+%F %T')" "$1"; }
mkdir -p "$TARGET"

# ---------------------------------------------------------------- 数据库

backup_mysql_local() {
    log "备份 MySQL（本地 mysqldump）"
    local host="${DB_HOST:-127.0.0.1}" port="${DB_PORT:-3306}"
    local db="${DB_NAME:-cxh_blog}" user="${DB_USERNAME:-root}" pass="${DB_PASSWORD:-}"
    if ! command -v mysqldump >/dev/null 2>&1; then
        log "找不到 mysqldump，跳过数据库备份（请安装 MySQL 客户端）"
        return 1
    fi
    MYSQL_PWD="$pass" mysqldump --single-transaction --quick --routines \
        -h "$host" -P "$port" -u "$user" "$db" > "$TARGET/database.sql"
    log "已导出 $(du -h "$TARGET/database.sql" | cut -f1)"
}

backup_mysql_docker() {
    log "备份 MySQL（docker compose 容器）"
    local container="${MYSQL_CONTAINER:-cxh-blog-mysql}"
    local db="${DB_NAME:-cxh_blog}"
    docker exec "$container" sh -c \
        "exec mysqldump --single-transaction --quick --routines -u root -p\"\$MYSQL_ROOT_PASSWORD\" $db" \
        > "$TARGET/database.sql"
    log "已导出 $(du -h "$TARGET/database.sql" | cut -f1)"
}

backup_h2() {
    local data_dir="$ROOT/data"
    if [ ! -d "$data_dir" ]; then
        log "没有找到 H2 数据目录 $data_dir，跳过"
        return 1
    fi
    if [ -f "$data_dir/cxh-blog.lock.db" ]; then
        log "警告：检测到 H2 正在运行（存在 .lock.db）。"
        log "      H2 文件库建议先停服再备份，否则可能拿到不一致的快照；"
        log "      正在运行的服务可改用后台「导出」或等停服后重跑本脚本。"
    fi
    log "备份 H2 数据目录"
    tar czf "$TARGET/h2-data.tar.gz" -C "$ROOT" data
    log "已打包 $(du -h "$TARGET/h2-data.tar.gz" | cut -f1)"
}

# ---------------------------------------------------------------- 上传图片

backup_uploads() {
    local dir="${CXH_MEDIA_LOCATION:-$ROOT/uploads}"
    if [ "$USE_DOCKER" = "1" ]; then
        log "备份 docker 卷里的上传图片"
        local container="${APP_CONTAINER:-cxh-blog-app}"
        docker exec "$container" tar czf - -C /app uploads > "$TARGET/uploads.tar.gz"
        log "已打包 $(du -h "$TARGET/uploads.tar.gz" | cut -f1)"
        return
    fi
    if [ ! -d "$dir" ]; then
        log "没有上传目录 $dir，跳过"
        return
    fi
    log "备份上传图片"
    tar czf "$TARGET/uploads.tar.gz" -C "$(dirname "$dir")" "$(basename "$dir")"
    log "已打包 $(du -h "$TARGET/uploads.tar.gz" | cut -f1)"
}

# ---------------------------------------------------------------- 执行

case "$MODE" in
    mysql)
        if [ "$USE_DOCKER" = "1" ]; then backup_mysql_docker; else backup_mysql_local; fi
        ;;
    h2)
        backup_h2
        ;;
    auto)
        if [ "$USE_DOCKER" = "1" ]; then
            backup_mysql_docker
        elif [ -d "$ROOT/data" ] && [ -f "$ROOT/data/cxh-blog.mv.db" ]; then
            backup_h2
        elif command -v mysqldump >/dev/null 2>&1; then
            backup_mysql_local
        else
            log "未识别到 H2 数据目录，也没有 mysqldump；可用 --mysql / --h2 / --docker 指定"
            exit 1
        fi
        ;;
esac

backup_uploads || true

# ---------------------------------------------------------------- 清单与清理

{
    echo "备份时间: $(date '+%F %T %Z')"
    echo "主机: $(hostname)"
    echo "模式: $MODE (docker=$USE_DOCKER)"
    echo "项目目录: $ROOT"
    echo "--- 文件 ---"
    ls -lh "$TARGET" | tail -n +2
} > "$TARGET/manifest.txt"

log "清理超过 ${KEEP_DAYS} 天的旧备份"
find "$BACKUP_DIR" -maxdepth 1 -type d -name 'cxh-blog-*' -mtime "+$KEEP_DAYS" -print -exec rm -rf {} + 2>/dev/null || true

log "备份完成：$TARGET"
du -sh "$TARGET" | sed 's/^/  合计 /'

# 恢复提示
cat <<EOF

恢复方法：
  MySQL   : mysql -u <user> -p <db> < database.sql
  H2      : 停服后 tar xzf h2-data.tar.gz -C <项目目录>
  上传图片: tar xzf uploads.tar.gz -C <项目目录>
EOF
