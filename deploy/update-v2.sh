#!/bin/bash
# ============================================
# 拾光手账 v2.0 更新部署脚本
# 在服务器上执行，自动完成数据库迁移+容器重建
# ============================================

set -e
PROJECT_DIR="/opt/qinglv"
ARCHIVE="/tmp/qinglv-update.tar.gz"

echo "=========================================="
echo "  拾光手账 v2.0 Update Script"
echo "=========================================="

# 1. 检查归档文件
if [ ! -f "$ARCHIVE" ]; then
    echo "[ERROR] $ARCHIVE not found"
    echo "Please upload qinglv-update.tar.gz to /tmp/ first:"
    echo "  scp qinglv-update.tar.gz root@103.149.93.129:/tmp/"
    exit 1
fi

# 2. 备份当前版本
echo "[1/6] Backing up current version..."
BACKUP_DIR="/opt/qinglv-backup-$(date +%Y%m%d%H%M%S)"
cp -r "$PROJECT_DIR" "$BACKUP_DIR"
echo "  Backup saved to $BACKUP_DIR"

# 3. 解压更新包（覆盖现有文件）
echo "[2/6] Extracting update archive..."
cd "$PROJECT_DIR"
tar xzf "$ARCHIVE"
echo "  Files extracted successfully"

# 4. 数据库迁移 - 添加心愿单表
echo "[3/6] Running database migration..."

# 从 .env 读取密码
if [ -f "$PROJECT_DIR/.env" ]; then
    source "$PROJECT_DIR/.env"
fi
DB_PASS="${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD is required}"

# 检查表是否存在
TABLE_EXISTS=$(mysql -uroot -p"$DB_PASS" qinglv_note -sN -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='qinglv_note' AND table_name='ql_wish'" 2>/dev/null || echo "0")

if [ "$TABLE_EXISTS" = "0" ]; then
    mysql -uroot -p"$DB_PASS" qinglv_note -e "
CREATE TABLE ql_wish (
    id BIGINT NOT NULL AUTO_INCREMENT,
    couple_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(1000) DEFAULT NULL,
    icon VARCHAR(50) DEFAULT NULL,
    category VARCHAR(30) DEFAULT 'other',
    priority TINYINT DEFAULT 0,
    status TINYINT DEFAULT 0,
    completed_time DATETIME DEFAULT NULL,
    completed_by BIGINT DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_couple_id (couple_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
" 2>/dev/null
    echo "  ql_wish table created"
else
    echo "  ql_wish table already exists, skipping"
fi

# 5. 重建 Docker 容器
echo "[4/6] Rebuilding Docker containers..."
cd "$PROJECT_DIR"
docker compose up -d --build 2>&1

# 6. 等待服务启动
echo "[5/6] Waiting for services to start..."
sleep 15

# 7. 健康检查
echo "[6/6] Health check..."
if curl -sf http://localhost:8090/ > /dev/null 2>&1; then
    echo "  Frontend: OK"
else
    echo "  Frontend: waiting..."
    sleep 10
fi

if curl -sf http://localhost:8090/api/auth/login -X OPTIONS > /dev/null 2>&1; then
    echo "  Backend API: OK"
else
    echo "  Backend API: still starting (check docker compose logs)"
fi

echo ""
echo "=========================================="
echo "  Update complete!"
echo "=========================================="
echo ""
echo "  Access: http://103.149.93.129:8090"
echo "  Swagger: http://103.149.93.129:8090/api/swagger-ui.html"
echo "  Backup: $BACKUP_DIR"
echo ""
echo "  New features:"
echo "  - Search bar on timeline page"
echo "  - Wish pool (wishes)"
echo "  - Stats dashboard"
echo "  - Dark mode toggle (moon icon in navbar)"
echo "  - PWA installable on mobile"
echo ""
echo "  Rollback: cp -r $BACKUP_DIR/* $PROJECT_DIR/ && cd $PROJECT_DIR && docker compose up -d --build"
echo ""

# 清理
rm -f "$ARCHIVE"
echo "Done."
