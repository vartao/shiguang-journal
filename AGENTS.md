# AGENTS.md

> 面向 AI 编码代理的项目说明。本文档基于仓库实际内容整理，修改代码前请先阅读。
> 项目文档与代码注释主要使用中文，提交信息也请沿用中文。

## 1. 项目概览

**拾光手账（qinglv）**：双人私密情侣空间应用（每个空间最多 2 人）。

- 前端：Vue 3.5 + Vite 6 + Pinia 2 + Vue Router 4 + Axios（`frontend/`）
- 后端：Spring Boot 3.2.5 + Java 17 + MyBatis-Plus 3.5.6 + JJWT 0.12.5 + Hutool 5.8 + Lombok + SpringDoc OpenAPI（`backend/`）
- 数据库：MySQL 8（库名 `qinglv_note`，schema 见 `sql/schema.sql`；Docker 镜像用 `mysql:8.4`）
- 部署：Docker Compose（`mysql` / `backend` / `frontend` 三服务）+ Nginx

功能模块：注册登录（BCrypt + JWT，7 天有效期）、情侣小窝（邀请码绑定，不填邀请码自动创建空间成为 owner）、笔记时间线（心情/天气/置顶/点赞/搜索）、相册与照片、纪念日倒计时、惊喜盒子（date/password/click 解锁，创建者可撤回/删除）、心愿单（伴侣可标记达成）、恋爱统计、文件上传。

## 2. 目录结构

```
qinglv/
├── frontend/          # Vue 前端（Vite，开发端口 5173）
│   └── src/
│       ├── api/           # Axios 封装（request.js，baseURL=/api，自动带 token）+ 各模块 API
│       ├── views/         # 页面组件（路由 meta 控制登录态）
│       ├── components/    # 胶片/拍立得/信封/弹窗等视觉组件
│       ├── stores/        # Pinia：user / couple / theme
│       ├── features/      # 表单逻辑（anniversary、wish）
│       ├── composables/、utils/、router/、assets/styles/（手账风 CSS 变量）
│       └── test/setup.js  # vitest 全局配置
├── backend/           # Spring Boot 后端（端口 8080）
│   └── src/main/java/com/qinglv/
│       ├── controller/    # 解析参数、取 JWT 用户、Result 包装
│       ├── service/       # 业务规则、事务、couple 权限（含 support/ 工具类）
│       ├── mapper/        # MyBatis-Plus
│       ├── entity/ dto/   # 表实体 / 入参 DTO / 出参 VO
│       └── common/        # JwtUtil、JwtInterceptor、RateLimiter、全局异常、统一 Result、WebConfig
├── sql/               # schema.sql（以这份为准）+ 旧环境手动迁移脚本（migration_*.sql）
├── deploy/            # nginx.conf、deploy.sh / deploy-windows.ps1、validate-env.sh、docker-compose.e2e.yml、ssl/
├── scripts/           # 验收脚本（acceptance.sh / .ps1 / acceptance-full.mjs）、check-env.sh、verify.sh
├── docs/              # ARCHITECTURE.md、OPERATIONS.md、REFACTORING_REVIEW.md、superpowers/（plans+specs）
└── docker-compose.yml
```

数据表（前缀 `ql_`，共 11 张）：`ql_user`、`ql_couple`、`ql_note`、`ql_note_image`、`ql_note_like`、`ql_album`、`ql_photo`、`ql_anniversary`、`ql_surprise`、`ql_wish`、`ql_file`。

## 3. 构建与运行

### 本地开发（Windows 请用 Git Bash）

```bash
# 1. 数据库（MySQL 8）
mysql -uroot -p < sql/schema.sql

# 2. 后端（需要 JDK 17+；Maven 报“不支持发行版本 17”说明 JAVA_HOME 指向了 JDK 11）
cd backend && mvn spring-boot:run        # :8080，Swagger UI 在 /swagger-ui.html

# 3. 前端
cd frontend && npm install && npm run dev  # :5173，/api 与 /uploads 代理到 8080
```

后端默认数据源：`jdbc:mysql://localhost:3306/qinglv_note`，用户/密码 `root`/`root`，均可由环境变量 `SPRING_DATASOURCE_*` 覆盖。

### Docker 一键部署

```bash
cp .env.example .env          # 必须替换所有 CHANGE_ME 值
bash deploy/validate-env.sh   # 校验 .env
docker compose up -d --build  # 访问 http://服务器IP:8090
```

- MySQL 首次启动自动执行 `sql/schema.sql`（挂载到 `/docker-entrypoint-initdb.d/`）；改过 schema 需 `docker compose down -v` 后重建。
- 前端镜像内置 `frontend/nginx.conf`，compose 会用 `deploy/nginx.conf` 挂载覆盖；上传卷 `upload_data` 同时挂到 backend（写）和 frontend nginx（只读）。
- 生产部署/回滚/备份见 `docs/OPERATIONS.md`（`bash deploy/deploy.sh`）。

## 4. 测试与质量检查

### 前端（`frontend/`）

```bash
npm run lint          # ESLint 10 flat config + eslint-plugin-vue，--max-warnings=0（警告也算失败）
npm run format:check  # Prettier 3 检查
npm test -- --run     # vitest 4（jsdom 环境，测试文件与源码同目录，*.test.js）
npm run check         # lint + format:check + test 一次跑完
npm run build         # vite build
```

### 后端（`backend/`）

```bash
mvn test              # 需要 JDK 17+
```

测试为纯单元/契约测试（JwtUtil、RateLimiter、Result、各 Service、ControllerContractTest），**不依赖真实 MySQL**；目前缺少真实数据库的自动化集成测试。

### 端到端验收

- `bash scripts/acceptance.sh`：需后端已在 `localhost:8080` 运行；`--base-url` 可指向 Docker 环境（`http://127.0.0.1:8090/api`）。另有 PowerShell 版 `scripts/acceptance.ps1`。
- `node scripts/acceptance-full.mjs`：全功能验收（鉴权、上传、笔记、相册、纪念日、惊喜、心愿、统计、越权与跨空间访问），会对自己创建的测试用户执行数据库解绑，**只能对一次性隔离数据库运行**，严禁对开发/生产库执行。隔离环境用 `deploy/docker-compose.e2e.yml`（端口 18090，项目名 `qinglv-e2e`），用法见 `scripts/start-and-accept.md`。
- 提交前完整检查：`bash scripts/verify.sh`（环境检查 + `cmp` 校验两份 schema 一致 + 前端 check/build + 后端 mvn test）。

## 5. 代码约定

### 后端

- 分层固定：`controller → service → mapper`，controller 只做参数解析与 `Result` 包装，业务规则和 couple 权限判断放 service。
- 统一响应体：`{ code: 200, message, data }`（`common/Result.java`）；业务错误抛 `BusinessException`，由 `GlobalExceptionHandler` 统一处理。
- 鉴权：`JwtInterceptor` 校验 `Authorization: Bearer`，`userId` 存 ThreadLocal；登录/注册有按 IP 的内存限流（10 次/分钟，`common/RateLimiter.java`）。
- 几乎所有业务资源带 **`couple_id` 隔离**，跨空间访问返回 403——新增资源时必须沿用此模式。
- 表前缀 `ql_`；逻辑删除字段 `deleted`（MyBatis-Plus 全局配置，删除值 1/未删 0）；实体/字段用驼峰，DB 用下划线（已开启 map-underscore-to-camel-case）；主键自增。
- 使用 Lombok（打包时已 exclude）。依赖版本集中在 `backend/pom.xml` 的 `<properties>`。
- **schema 单一事实来源是 `sql/schema.sql`**；`backend/src/main/resources/schema.sql` 是副本，`scripts/verify.sh` 会用 `cmp` 校验两者一致——改表结构必须两份同步更新。线上环境由 Flyway 管理增量迁移：在 `backend/src/main/resources/db/migration/` 新增 `V<版本>__<描述>.sql`（当前基线版本为 1，启动时自动 baseline）。`sql/` 下的 `migration_*.sql` 保留给未接入 Flyway 的旧环境手动执行。

### 前端

- Prettier：无分号、单引号、行宽 120、无尾逗号（`.prettierrc.json`）；ESLint flat config（`eslint.config.js`）+ eslint-plugin-vue flat/recommended + eslint-config-prettier。
- 路径别名 `@` → `src/`。未使用的参数/异常变量以 `_` 开头可豁免 no-unused-vars。
- HTTP 一律走 `src/api/request.js` 封装（自动带 token、统一错误处理），不要在组件里直接 new axios。
- 视觉风格：手账风，CSS 变量集中在 `src/assets/styles/variables.css`。**样式中禁止硬编码颜色**：纯色用 `var(--xxx)`，带透明度的用 RGB 通道变量（如 `rgba(var(--brown-rgb), 0.15)`，暗色模式统一覆盖通道值）；深色固定面板上的浅色文字/描边用 `--cream-static-rgb`（不随主题变化），卡片/纸面渐变用 `--gradient-card` / `--gradient-paper` / `--gradient-nav`。

## 6. 安全注意事项

- 生产必须改 `.env` 中的 `MYSQL_ROOT_PASSWORD` 与 `JWT_SECRET`（≥32 字符）；`.env` 不入库，`.env.example` 仅占位。
- CORS 通过环境变量 `CORS_ORIGINS` 配置（逗号分隔，本地默认允许 5173/8090）。
- 上传限制：扩展名白名单（jpg/jpeg/png/gif/webp/bmp/mp4/mp3/pdf/txt）+ Content-Type 校验，单文件 20MB（请求 50MB）。存储二选一：默认本地磁盘经 `/uploads/**` 公开读取；配置 `ALIYUN_OSS_*` 后走阿里云 OSS（对象 key 前缀 `qinglv/`，返回 `public-base-url` 拼接的 URL，静态图片同时生成 `_thumb.jpg` 缩略图）。
- Actuator 只暴露 `health`、`info`，health 不显示细节。
- 已知局限（改动相关代码时注意）：固定两人模型（`user_id1/user_id2`）、限流为进程内存（多实例不共享）、上传默认存本地磁盘卷 `upload_data`（可通过 `ALIYUN_OSS_*` 切换到 OSS，本地与 OSS 的历史文件不互通）。

## 7. 常用参考

- 架构细节与核心流程：`docs/ARCHITECTURE.md`
- 运维手册：`docs/OPERATIONS.md`
- API 一览与注册/绑定规则：`README.md`
- 验收脚本用法与覆盖矩阵：`scripts/start-and-accept.md`
