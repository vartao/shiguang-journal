# 拾光 Shiguang · 情侣手账

> 拾起时光的碎片，把日子过成胶片里的诗。

情侣私密空间：时间线笔记 · 相册 · 纪念日 · 惊喜盒子。

## 技术栈

| 层 | 技术 |
|----|------|
| 前端 | Vue 3 + Vite 6 + Pinia + Vue Router |
| 后端 | Spring Boot 3.2 + Java 17 + MyBatis-Plus |
| 数据库 | MySQL 8 |
| 部署 | Docker Compose + Nginx |

## 目录结构

```
shiguang-journal/
├── frontend/          # Vue 前端
├── backend/           # Spring Boot 后端
├── sql/               # schema.sql + 迁移脚本
├── deploy/            # Nginx 与部署脚本
├── docs/              # 架构与模块说明
└── docker-compose.yml
```

更细的分层与流程见 [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)，本轮结构与代码审查见
[docs/REFACTORING_REVIEW.md](docs/REFACTORING_REVIEW.md)。

## 本地开发

### 1. 数据库

```bash
# 创建库表（MySQL 8）
mysql -uroot -p < sql/schema.sql
```

默认连接（`backend/src/main/resources/application.yml`）：

- URL: `jdbc:mysql://localhost:3306/qinglv_note`
- 用户/密码: `root` / `root`（可用环境变量覆盖）

### 2. 后端

```bash
cd backend
mvn spring-boot:run
# 服务端口 8080
```

### 3. 前端

```bash
cd frontend
npm install
npm run dev
# 访问 http://localhost:5173
# /api 与 /uploads 已代理到 8080
```

## Docker 一键部署

```bash
cp .env.example .env
# 编辑 .env，改掉 MYSQL_ROOT_PASSWORD 与 JWT_SECRET

docker compose up -d --build
# 默认访问 http://服务器IP:8090
```

说明：

- MySQL 首次启动会执行 `sql/schema.sql`
- 上传文件卷：`upload_data`
- 若改过 schema，需要清库重建：`docker compose down -v` 后再 `up`

## 核心功能

1. **注册 / 登录**：BCrypt + JWT（7 天）
2. **情侣空间 / 小窝**：邀请码绑定、空间签名、头像与昵称
3. **笔记时间线**：心情筛选、图片、点赞、作者编辑/删除
4. **相册**：相册 CRUD、照片上传与删除
5. **纪念日**：倒计时
6. **惊喜盒子**：date / password / click 解锁，创建者可撤回
7. **心愿池**：心愿创建、编辑、状态筛选、伴侣标记达成

## 注册说明

- 不填邀请码：自动创建情侣空间，成为 `owner`，获得邀请码
- 填写邀请码：加入对方空间，成为 `member`

## 常用 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 注册 |
| POST | `/api/auth/login` | 登录 |
| GET | `/api/auth/profile` | 当前用户 |
| GET | `/api/couple/info` | 空间信息 |
| POST | `/api/couple/create` | 创建空间 |
| POST | `/api/couple/bind` | 邀请码绑定 |
| GET | `/api/notes?page=1&size=10` | 笔记分页 |
| POST | `/api/notes/{id}/like` | 点赞 |
| PUT | `/api/auth/profile` | 更新昵称/头像 |
| PUT | `/api/albums/{id}` | 更新相册 |
| DELETE | `/api/surprises/{id}` | 删除惊喜（创建者） |

统一响应：`{ code: 200, message, data }`

## 后端测试

```bash
cd backend
# 需要 JDK 17+
mvn test
```

覆盖：统一 `Result`、JWT 签发校验、笔记创建/权限/点赞切换、惊喜解锁与删除、情侣绑定（无效码/满员）引导文案。

## API 验收（端到端）

后端起来后（默认 `http://localhost:8080`），推荐在 Git Bash 中执行：

```bash
bash scripts/acceptance.sh
# Docker / Nginx：
# bash scripts/acceptance.sh --base-url http://127.0.0.1:8090/api
```

也可使用 PowerShell 版本：

```powershell
powershell -ExecutionPolicy Bypass -File scripts/acceptance.ps1
# powershell -ExecutionPolicy Bypass -File scripts/acceptance.ps1 -BaseUrl http://127.0.0.1:8090/api
```

覆盖：双人注册绑定 → 发笔记 → 时间线 → 点赞切换 → 惊喜 click 解锁（内容隐藏/揭示）→ 统计心情分布 → 非作者删除拒绝。  
说明见 [scripts/start-and-accept.md](scripts/start-and-accept.md)。

### 全功能验收（隔离测试库）

`scripts/acceptance-full.mjs` 覆盖健康检查、鉴权、注册登录、情侣空间、文件上传、笔记、相册、纪念日、惊喜、心愿和统计，同时验证越权、跨空间访问和非法输入。该脚本会对自己创建的测试用户执行数据库解绑，以验证独立创建/绑定空间接口，**只能对一次性隔离数据库运行**。

```bash
cp .env.example .env.e2e
# 将 .env.e2e 中的 CHANGE_ME 替换为本地测试值

docker compose --env-file .env.e2e \
  -f docker-compose.yml -f deploy/docker-compose.e2e.yml \
  -p qinglv-e2e down -v --remove-orphans
docker compose --env-file .env.e2e \
  -f docker-compose.yml -f deploy/docker-compose.e2e.yml \
  -p qinglv-e2e up -d --build --wait --wait-timeout 300

set -a; source .env.e2e; set +a
E2E_MYSQL_CONTAINER=qinglv-e2e-mysql \
E2E_MYSQL_PASSWORD="$MYSQL_ROOT_PASSWORD" \
node scripts/acceptance-full.mjs \
  --base-url http://127.0.0.1:18090/api
```

完整的隔离环境、覆盖矩阵和清理命令见 [scripts/start-and-accept.md](scripts/start-and-accept.md)。

## 注意

1. **生产务必改 `.env` 中的 `MYSQL_ROOT_PASSWORD` 与 `JWT_SECRET`**（`.env.example` 仅为占位）
2. schema 以 **`sql/schema.sql`** 为准（Docker 初始化挂载此文件）；`backend/src/main/resources/schema.sql` 供本地参考，请保持与 Entity 对齐
3. 点赞已做用户维度去重（`ql_note_like`，再次点击可取消）
4. 笔记仅作者可编辑/删除；支持心情筛选、天气与置顶
5. 已有库增量迁移：
   - 点赞表：`mysql -uroot -p qinglv_note < sql/migration_note_like.sql`
   - 心愿单：`mysql -uroot -p qinglv_note < sql/migration_wish.sql`
   - 笔记记录日期：`mysql -uroot -p qinglv_note < sql/migration_note_date.sql`
6. 本地开发请使用 **JDK 17+**（当前 pom 指定 Java 17）
7. 生产 CORS 通过环境变量 `CORS_ORIGINS` 配置；上传会校验扩展名、Content-Type 与文件头魔数
8. 上传资源通过签名 URL 访问（默认 30 天有效，密钥复用 `JWT_SECRET`，可用 `FILE_SIGN_KEY` 独立配置）；旧版直链升级后需要重新进入页面获取新签名地址


## 质量检查

Windows 请在 Git Bash 中执行：

- 环境检查：`bash scripts/check-env.sh`
- 完整验证：`bash scripts/verify.sh`

项目后端要求 JDK 17。若 Maven 报“不支持发行版本 17”，说明当前仍在使用 JDK 11，请将 `JAVA_HOME` 指向 JDK 17 后重试。

生产部署前复制 `.env.example` 为 `.env`，替换所有 `CHANGE_ME` 值，并运行 `bash deploy/validate-env.sh`。
