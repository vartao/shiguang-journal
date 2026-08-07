#!/bin/bash
# ============================================
# 情侣笔记网站 - 一键部署脚本
# 在你的香港服务器上执行此脚本
# ============================================

set -e

echo "🌹 开始部署情侣笔记网站..."

# 1. 创建项目目录
PROJECT_DIR="/opt/qinglv"
mkdir -p $PROJECT_DIR
cd $PROJECT_DIR

# 2. 检查 Docker 和 Docker Compose
if ! command -v docker &> /dev/null; then
    echo "❌ 未检测到 Docker，请先安装 Docker"
    exit 1
fi

if ! docker compose version &> /dev/null; then
    echo "❌ 未检测到 Docker Compose，请先安装"
    exit 1
fi

echo "✅ Docker 环境检测通过"

# 3. 创建必要目录
mkdir -p deploy/ssl

# 4. 创建 .env 文件（如果不存在）
if [ ! -f .env ]; then
    cp .env.example .env
    echo "ERROR: 已创建 .env，请填写安全值后重新部署"
    exit 1
fi
bash deploy/validate-env.sh


# 5. 构建并启动
echo "🔨 开始构建项目（首次构建约需5-10分钟）..."
docker compose up -d --build

# 6. 等待服务启动
echo "⏳ 等待服务启动..."
for attempt in $(seq 1 30); do
  if curl -fsS http://localhost:8090/actuator/health >/dev/null 2>&1; then break; fi
  if [ "$attempt" -eq 30 ]; then echo "❌ 服务健康检查超时"; docker compose logs --tail=100 backend; exit 1; fi
  sleep 2
done

# 7. 检查服务状态
echo ""
echo "📋 服务状态："
docker compose ps

echo ""
echo "============================================"
echo "🎉 部署完成！"
echo "============================================"
echo ""
echo "访问地址: http://你的服务器IP"
echo ""
echo "后续操作："
echo "1. 修改 deploy/nginx.conf 中的 server_name 为你的域名"
echo "2. 如需HTTPS，将证书放入 deploy/ssl/ 目录并取消 nginx.conf 中SSL注释"
echo "3. 执行 docker compose restart frontend 重启前端"
echo ""
echo "常用命令："
echo "  查看日志: docker compose logs -f"
echo "  重启服务: docker compose restart"
echo "  停止服务: docker compose down"
echo "  更新部署: git pull && docker compose up -d --build"
