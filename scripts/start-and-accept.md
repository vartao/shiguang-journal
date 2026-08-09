# 验收跑通指南

## 方式 A：本地（推荐开发机）

### 1. 准备

- JDK 17+
- Maven 3.8+
- MySQL 8，库 `qinglv_note`
- Git Bash、`curl`、Node 18+（API 验收脚本使用）

```bash
# 建库
mysql -uroot -proot < sql/schema.sql
# 若密码不是 root，改 application.yml 或环境变量
```

### 2. 起后端

```bash
cd backend
mvn spring-boot:run
# 监听 http://localhost:8080
```

### 3. 跑 API 验收

**Git Bash（项目根目录，推荐）：**

```bash
bash scripts/acceptance.sh
```

也可使用 PowerShell 版本：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/acceptance.ps1
```

### 4. （可选）起前端验证

```bash
cd frontend
npm install
npm run dev
# http://localhost:5173
```

页面验收建议：登录 → 时间线搜索/点赞 → 笔记新建/编辑/删除 → 相册详情 → 纪念日 → 惊喜盒子 → 心愿池 → 恋爱数据 → 小窝资料。

---

## 方式 B：Docker 一键

```bash
cp .env.example .env
# 编辑 .env 改 MYSQL_ROOT_PASSWORD / JWT_SECRET（本地验收可先不改）

docker compose up -d --build
# 等待 mysql healthy + backend 启动（约 1~3 分钟）
# 访问 http://localhost:8090
```

API 验收（走 Nginx）：

```bash
bash scripts/acceptance.sh --base-url http://127.0.0.1:8090/api
```

PowerShell 版本：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/acceptance.ps1 -BaseUrl http://127.0.0.1:8090/api
```

---

## 方式 C：隔离 Docker 全功能验收（发布前推荐）

> 该流程会删除 `qinglv-e2e` 项目的 MySQL 和上传卷，但不会触碰默认 `qinglv` 项目的开发/生产数据。不要将以下脚本指向生产数据库。

### 1. 准备一次性密钥

```bash
cp .env.example .env.e2e
# 编辑 .env.e2e，将 MYSQL_ROOT_PASSWORD / JWT_SECRET 改为本地测试值
# CORS_ORIGINS 会被 E2E Compose 覆盖为 http://127.0.0.1:18090
```

`.env.e2e` 已加入 `.gitignore`，不会进入版本库。

### 2. 重建并启动隔离环境

```bash
docker compose --env-file .env.e2e \
  -f docker-compose.yml -f deploy/docker-compose.e2e.yml \
  -p qinglv-e2e down -v --remove-orphans

docker compose --env-file .env.e2e \
  -f docker-compose.yml -f deploy/docker-compose.e2e.yml \
  -p qinglv-e2e up -d --build --wait --wait-timeout 300

curl -fsS http://127.0.0.1:18090/actuator/health
curl -fsSI http://127.0.0.1:18090/
```

首次启动会在全新 MySQL 卷中执行最新 `sql/schema.sql`，前端、API 和上传静态资源全部经过 Nginx `18090` 端口验证。

### 3. 运行 16 组全量业务链路

```bash
set -a; source .env.e2e; set +a
E2E_MYSQL_CONTAINER=qinglv-e2e-mysql \
E2E_MYSQL_PASSWORD="$MYSQL_ROOT_PASSWORD" \
node scripts/acceptance-full.mjs \
  --base-url http://127.0.0.1:18090/api
```

脚本会生成唯一测试用户，并且只对测试数据执行两类 SQL：对这些用户做解绑（验证通常无法从自动建窝注册流程到达的 `/couple/create` 和 `/couple/bind` 分支），以及把时间胶囊的开启日期拨到当天（验证「到期开启」分支）。成功时会输出检查数、功能组数和供页面验收使用的测试账号。

### 4. 诊断和清理

```bash
docker compose --env-file .env.e2e \
  -f docker-compose.yml -f deploy/docker-compose.e2e.yml \
  -p qinglv-e2e logs --tail=200 mysql backend frontend

docker compose --env-file .env.e2e \
  -f docker-compose.yml -f deploy/docker-compose.e2e.yml \
  -p qinglv-e2e down -v --remove-orphans
```

---

## 验收覆盖点

| 功能组 | 全功能脚本的核心断言 |
|------|------|
| 健康/鉴权/账号 | Actuator、JWT 拦截、双人注册绑定、登录、资料/头像更新、重名拒绝 |
| 情侣空间 | 读取/更新、独立创建、邀请码绑定、无效码、满员拒绝 |
| 文件 | 单传/批量上传、Nginx 静态访问、MIME/扩展名拒绝、匿名拒绝 |
| 笔记 | 创建/详情/更新/删除、页分/心情筛选/搜索、记录日期排序、点赞切换、作者/跨空间权限 |
| 相册 | CRUD、URL/Multipart 照片、两条删照路由、封面同步首张照片、封面替换、跨空间权限 |
| 纪念日 | CRUD、闰日循环倒计时、非法日期、跨空间权限 |
| 惊喜 | click/password/date 解锁、解锁前内容隐藏、错误密码/未到期拒绝、创建者删除 |
| 心愿 | 创建/列表/筛选/编辑/达成/删除、创建者和跨空间权限 |
| 统计 | 笔记、照片、相册、纪念日、惊喜、点赞、活跃/连续天数、心情分布 |
| 旅行足迹 | CRUD、封面同步首张照片、状态筛选、省份地图聚合、照片增删、跨空间权限 |
| 行程计划 | CRUD、每日安排 JSON 校验、伴侣更新、跨空间权限 |
| 今天吃什么 | 菜品池增删/分类筛选、空池/随机选菜、餐次校验、今日菜单、跨空间权限 |
| 时间胶囊 | 开启日期必须未来、未到期内容防泄露、提前开启/跨空间拒绝、到期开启（直改测试库拨日期）、仅创建者删除 |
| 小事记 | 创建、评分 0-5 校验、非法分类落 other、分类/年份筛选、伴侣更新、跨空间权限 |
| 留言板 | 空内容拒绝、双方发布、列表带昵称、limit 限量、只能删自己的留言、跨空间权限 |
| 功能开关 | 默认全开、关闭/恢复、伴侣可见、未知功能项拒绝 |

---

## 常见失败

| 现象 | 处理 |
|------|------|
| 连接被拒绝 | 后端未起 / 端口不是 8080 |
| Access denied for MySQL | 改 `SPRING_DATASOURCE_PASSWORD` 或本地 root 密码 |
| 表不存在 | 执行 `sql/schema.sql`；Docker 需 `down -v` 后重建 |
| 心情分布 FAIL | 确认已部署含 mood select 的 StatsService 修复 |
| 429 | 登录注册限流，等 1 分钟或换 IP |
