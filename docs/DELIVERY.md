# 拾光手账（qinglv）· 完整交付文档

> 本文档是项目交付的总纲：覆盖交付物清单、架构、开发全流程、测试验收、部署、运维与整个软件生命周期管理。
> 细节文档见文末「文档索引」。文档与项目约定均使用中文，提交信息沿用中文。

---

## 1. 交付物清单

| 交付物 | 位置 | 说明 |
| --- | --- | --- |
| Web 前端 | `frontend/` | Vue 3.5 + Vite 6 + Pinia 2 + Vue Router 4 + Axios，手账风 UI，深色模式 |
| 后端服务 | `backend/` | Spring Boot 3.2.5 + Java 17 + MyBatis-Plus 3.5.6 + JJWT 0.12.5 + Hutool |
| 微信小程序 | `miniprogram/` | 原生 WXML/WXSS/JS，17 个页面与 Web 端核心功能一一对应 |
| 数据库 Schema | `sql/schema.sql` | 21 张 `ql_` 表，**单一事实来源**（`backend/src/main/resources/schema.sql` 为同步副本） |
| 部署配置 | `docker-compose.yml` + `deploy/` | mysql/backend/frontend 三服务 + Nginx + 部署/校验/SSL 脚本 |
| 验收脚本 | `scripts/` | 静态校验、单环境验收、全功能 E2E（隔离库） |
| 测试 | `frontend/src/**/*.test.js`、`backend/src/test/**` | 前端 vitest（与源码同目录）、后端纯单元/合同测试（不依赖真实 MySQL） |
| 文档 | `docs/`、`README.md`、`AGENTS.md` | 架构、运维、重构审查、交付（本文档） |

## 2. 产品概述与功能矩阵

双人私密情侣空间应用，每个空间最多 2 人（`ql_couple.user_id1/user_id2` 固定两人模型）。

功能模块共 12 个，key 与功能开关一致，三端覆盖情况：

| 模块 | key | Web | 小程序 | 后端接口 |
| --- | :-: | :-: | :-: | --- |
| 笔记时间线（心情/天气/置顶/点赞/评论/搜索/那年今天） | `notes` | ✅ | ✅ | `/api/notes/**` |
| 相册与照片 | `albums` | ✅ | ✅ | `/api/albums/**`、`/api/photos/**` |
| 纪念日倒计时 | `anniversary` | ✅ | ✅ | `/api/anniversaries/**` |
| 惊喜盒子（date/password/click 解锁） | `surprise` | ✅ | ✅ | `/api/surprises/**` |
| 心愿单（伴侣可标记达成） | `wishes` | ✅ | ✅ | `/api/wishes/**` |
| 恋爱统计（年度回顾 + canvas 回忆海报 + PDF 导出） | `stats` | ✅ | ✅ | `/api/stats/**`、`/api/export/**` |
| 旅行足迹（中国地图点亮） | `travels` | ✅ | ✅ | `/api/travels/**` |
| 行程计划（出发前每日安排） | `plans` | ✅ | ✅ | `/api/plans/**` |
| 今天吃什么（菜品池/随机选菜） | `menu` | ✅ | ✅ | `/api/menu/**` |
| 时间胶囊（到期前不可见） | `capsule` | ✅ | ✅ | `/api/capsules/**` |
| 小事记（分类打卡 + 0-5 星评分） | `logs` | ✅ | ✅ | `/api/logs/**` |
| 留言板（双人气泡悄悄话） | `board` | ✅ | ✅ | `/api/messages/**` |

通用能力（不参与开关）：注册登录（BCrypt + JWT 7 天）、情侣小窝（邀请码绑定/自动建空间成为 owner）、功能开关管理（`ql_couple_feature`，无记录默认开启，Web 在「小窝 → 功能管理」配置，三端导航同步生效）、文件上传（签名 URL 访问）。

## 3. 系统架构

```
浏览器 (Vue SPA, :5173 dev / :8090 docker)
微信小程序（wx.request 直连后端 apiBase）
        │  /api/**（JWT Bearer）
        │  /uploads/**（签名 URL，HMAC + 过期时间）
        ▼
Nginx（deploy/nginx.conf：SPA 回退、/api 代理、/uploads 代理、安全头、gzip）
        ▼
Spring Boot 后端（:8080）
  controller（参数解析 + Result 包装）→ service（业务规则 + couple 权限 + 事务）→ mapper（MyBatis-Plus）
  common：JwtInterceptor、CurrentUserContext（请求级用户上下文）、RateLimiter、
          UploadUrlSigner/UploadUrlSerializer（出参统一签名）、GlobalExceptionHandler
        ▼                ▼
MySQL 8（qinglv_note）   文件存储（二选一）
                          - 本地磁盘卷 upload_data（默认，签名 URL 保护）
                          - 阿里云 OSS（配置 ALIYUN_OSS_* 后切换，公开读外链）
```

要点：
- 统一响应体 `{ code: 200, message, data }`；业务错误抛 `BusinessException` 由 `GlobalExceptionHandler` 处理。
- 几乎所有业务资源带 `couple_id` 隔离，跨空间访问返回 403。
- 上传文件出参由 Jackson 全局序列化器附加签名；`UploadsController` 校验签名后流式返回，无效/过期一律 404；写路径入库前剥离查询参数。
- 架构细节：`docs/ARCHITECTURE.md`。

## 4. 运行环境与依赖

| 环境 | 依赖 | 版本要求 |
| --- | --- | --- |
| 后端开发/构建 | JDK、Maven | **JDK 17+**（JDK 11 会报"不支持发行版本 17"）、Maven 3.6+ |
| 前端开发/构建 | Node.js、npm | Node 18+（Vite 6） |
| 数据库 | MySQL | 8.x（Docker 镜像 `mysql:8.4`） |
| 小程序开发 | 微信开发者工具 | 导入仓库根目录（`project.config.json` 已指向 `miniprogram/`） |
| 部署 | Docker + Docker Compose | v2（`docker compose`） |

默认数据源 `jdbc:mysql://localhost:3306/qinglv_note`，用户/密码 `root/root`，可用 `SPRING_DATASOURCE_*` 覆盖。

## 5. 软件开发生命周期（SDLC）

### 5.1 需求与设计

- 新功能先在 `docs/superpowers/specs/` 写设计说明，复杂改动在 `docs/superpowers/plans/` 落实施计划。
- 涉及表结构变更必须同时规划：`sql/schema.sql` 双副本更新 + Flyway 增量脚本（见 7.4）。
- 每个功能模块默认带 `couple_id` 隔离与功能开关 key（`ql_couple_feature` + `FEATURE_META`/`utils/features.js` 两端清单）。

### 5.2 开发规范

- 分支：`main` 为稳定主线；功能分支合并前必须通过质量门禁。
- 提交信息：中文 + conventional 前缀（`feat/fix/refactor/perf/test/chore/docs`），一个提交一个逻辑主题。
- 后端分层固定 `controller → service → mapper`；controller 只做参数解析与 `Result` 包装，业务规则和权限判断放 service。
- 前端 HTTP 一律走 `src/api/request.js` 封装；样式禁止硬编码颜色（用 `variables.css` 的 CSS 变量与 RGB 通道变量）。
- 小程序后端地址只在 `miniprogram/app.js` 的 `globalData.apiBase` 一处修改。
- 项目约定详见 `AGENTS.md`（面向 AI 编码代理，同样适用于人类开发者）。

### 5.3 构建与质量门禁

```bash
# 提交前完整检查（环境检查 + schema 双副本 cmp + 前端 check + 后端测试）
bash scripts/verify.sh

# 分项
cd frontend && npm run check     # ESLint(--max-warnings=0) + Prettier + vitest
cd frontend && npm run build     # 生产构建
cd backend  && mvn test          # 135+ 单元/合同测试
node scripts/check-miniprogram.mjs   # 小程序静态校验（页面文件完整性等）
docker compose config --quiet        # compose 配置合法性
node --check scripts/acceptance-full.mjs
```

已知环境说明：Windows 检出为 CRLF 时 Prettier（默认 LF）会对大量存量文件报格式告警，属机器检出差异；以「本次改动文件必须通过 Prettier」为准。

### 5.4 测试策略与验收

| 层级 | 内容 | 命令 |
| --- | --- | --- |
| 前端单元 | stores/utils/组件/视图（vitest，jsdom，测试与源码同目录） | `npm test -- --run` |
| 后端单元 | JwtUtil、RateLimiter、Result、UploadUrlSigner、各 Service | `mvn test` |
| 后端合同 | Controller 契约（MockMvc standalone，envelope/校验） | 同上 |
| 静态校验 | 小程序页面完整性、schema 一致性、compose 合法性 | `check-miniprogram.mjs` / `verify.sh` |
| 单环境验收 | 后端已在 `localhost:8080` 运行时的核心 API 验收 | `bash scripts/acceptance.sh`（`--base-url` 可指向 Docker 环境） |
| 全功能 E2E | 鉴权、上传、笔记、相册、纪念日、惊喜、心愿、统计、越权与跨空间访问 | 见下 |

**全功能 E2E（必须隔离运行）**：`node scripts/acceptance-full.mjs` 会对自建测试用户执行数据库解绑，**只能对一次性隔离数据库运行，严禁对开发/生产库执行**。

```bash
# 隔离环境（端口 18090，项目名 qinglv-e2e），用法详见 scripts/start-and-accept.md
cp .env.example .env.e2e   # 替换 CHANGE_ME 为测试值
# 按 scripts/start-and-accept.md 启动隔离栈后执行验收
```

## 6. 部署指南

### 6.1 本地开发环境

```bash
# 1. 数据库（MySQL 8，首次建库）
mysql -uroot -p < sql/schema.sql

# 2. 后端（需 JDK 17+，Swagger UI: /swagger-ui.html）
cd backend && mvn spring-boot:run

# 3. 前端（/api 与 /uploads 代理到 8080）
cd frontend && npm install && npm run dev   # http://localhost:5173

# 4. 小程序：微信开发者工具导入仓库根目录，后端地址改 app.js 的 apiBase
```

### 6.2 Docker 一键部署（生产）

```bash
cp .env.example .env           # 必须替换全部 CHANGE_ME 值
bash deploy/validate-env.sh    # 校验 .env（弱密钥/占位值会失败）
docker compose up -d --build   # 访问 http://服务器IP:8090
# 或使用封装脚本
bash deploy/deploy.sh          # 含健康轮询
```

服务拓扑：`mysql`（8.4，首启自动执行 `sql/schema.sql`）、`backend`（写入 `upload_data` 卷，healthcheck 通过后 frontend 才启动）、`frontend`（Nginx，SPA + 代理）。生产部署/回滚/备份详见 `docs/OPERATIONS.md`。

> 改过 schema 需 `docker compose down -v` 后重建（仅当未走 Flyway 增量时）。

### 6.3 环境变量清单（.env）

| 变量 | 必填 | 说明 |
| --- | :-: | --- |
| `MYSQL_ROOT_PASSWORD` | ✅ | 强随机密码 |
| `JWT_SECRET` | ✅ | ≥32 字符；登录令牌密钥，也是上传签名密钥的缺省来源 |
| `CORS_ORIGINS` | ✅ | 生产域名，逗号分隔 |
| `FILE_SIGN_KEY` | 可选 | 上传签名独立密钥（≥32 字符），缺省复用 `JWT_SECRET` |
| `FILE_UPLOAD_PATH` | 可选 | 容器内默认 `/app/uploads` |
| `SPRING_DATASOURCE_*` | 可选 | 覆盖默认数据源 |
| `ALIYUN_OSS_*` | 可选 | ENDPOINT/BUCKET/ACCESS_KEY_ID/ACCESS_KEY_SECRET/PUBLIC_BASE_URL/KEY_PREFIX，配齐后上传走 OSS |

### 6.4 微信小程序发布流程

1. **配置后端地址**：`miniprogram/app.js` 的 `globalData.apiBase` 改为线上地址（需 HTTPS 域名并在微信公众平台配置 request/uploadFile 合法域名）。
2. **本地校验**：`node scripts/check-miniprogram.mjs`。
3. **真机预览**：开发者工具「导入根目录 → 预览」扫码验证核心链路（登录/绑定、时间线、上传图片显示——上传 URL 为签名地址，确认 `<image>` 加载正常）。
4. **上传体验版**：开发者工具「上传」，填版本号与备注；公众平台设为体验版，邀请伙伴体验。
5. **提交审核 → 发布正式版**：类目建议生活记录/工具；如含内容社区描述，按平台要求提供截图。
6. **新页面/新模块同步**：`app.json` 注册页面 + `utils/features.js` 登记 FEATURES 与 PAGE_FEATURES（功能开关同步三端）。

## 7. 运维与生命周期管理

### 7.1 健康检查与监控

- Actuator 仅暴露 `health`、`info`，health 不显示细节；Compose 依赖 backend 健康才启动 frontend。
- 日常巡检：`docker compose ps`、`curl http://127.0.0.1:8090/actuator/health`。
- 日志：`docker compose logs -f backend frontend`。

### 7.2 数据库备份与恢复

```bash
# 备份（建议 cron 每日 + 上传前）
docker compose exec -T mysql mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" qinglv_note > backup-$(date +%F).sql

# 恢复（先停止写入、保留当前备份）
cat backup.sql | docker compose exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" qinglv_note
```

上传文件在 `upload_data` 卷中，迁移/删除前先备份该卷（本地与 OSS 历史文件不互通）。

### 7.3 版本升级与回滚

- 升级：拉取新代码 → `bash deploy/validate-env.sh` → `docker compose up -d --build`；有 Flyway 增量脚本时后端启动自动迁移。
- 回滚：`git checkout <上一发布标签/commit>` 后重新 `docker compose up -d --build`；数据库回滚需用升级前备份恢复（Flyway 不自动降级）。
- 建议每次发布打 git 标签（如 `v1.4.0`），并记录「版本 ↔ schema 基线 ↔ 备份」对照表。

### 7.4 Schema 变更规范

1. **单一事实来源是 `sql/schema.sql`**，同步更新 `backend/src/main/resources/schema.sql` 副本（`scripts/verify.sh` 会 `cmp` 校验一致）。
2. 线上环境由 **Flyway** 管理增量：在 `backend/src/main/resources/db/migration/` 新增 `V<版本>__<描述>.sql`（当前基线版本 1，启动自动 baseline）。已执行过的迁移文件不可修改。
3. 未接入 Flyway 的旧环境手动执行 `sql/migration_*.sql`。
4. 禁止对生产库直接覆盖 schema。

### 7.5 密钥轮换

- 轮换 `JWT_SECRET`：会使所有现有会话失效（用户需重新登录），安排维护窗口；同时影响缺省的上传签名密钥——若单独配置了 `FILE_SIGN_KEY` 则签名 URL 不受影响，否则旧图片签名失效（重新进入页面即可获取新签名）。
- 轮换 `MYSQL_ROOT_PASSWORD`：同步更新 `.env` 与备份脚本中的引用。

### 7.6 故障排查速查

| 症状 | 排查 |
| --- | --- |
| 前端 401 循环跳登录 | JWT_SECRET 是否变更过；令牌过期（7 天）正常重登 |
| 图片不显示（404） | 签名过期/被剥离——确认接口出参带 `?exp=&sig=`；nginx `/uploads` 是否代理到后端 |
| 上传报"文件内容与扩展名不匹配" | 魔数校验拒绝伪造文件，属预期行为 |
| 登录/注册 429 | 按 IP 内存限流（10 次/分钟），等待或重启后端 |
| 邀请码绑定失败 | 空间已满 2 人（固定两人模型） |

## 8. 安全设计与已知局限

**安全机制**：BCrypt 密码存储；JWT 7 天；`/uploads/**` 签名 URL（HMAC-SHA256 + 过期，签名无效一律 404）；上传扩展名白名单 + Content-Type 校验 + 文件头魔数/图片解码校验（`FileSignatureValidator`）+ 单文件 20MB（请求 50MB）+ 文件签名访问；couple_id 资源隔离（跨空间 403）；登录/注册按 IP 限流；Nginx 覆盖客户端伪造的转发头 + 安全响应头；Actuator 最小暴露。

**已知局限**（改动相关代码时注意，详见 `docs/REFACTORING_REVIEW.md`）：
- 固定两人模型；限流为进程内存（多实例不共享）；本地与 OSS 历史文件不互通。
- OSS 模式文件仍为公开 URL（建议后续私有桶 + SDK 预签名）。
- 上传图片未做解码重编码（EXIF 方向问题，剥元数据需配合旋转实现）。

## 9. 生命周期阶段总览

```
需求/设计 ──► 开发 ──► 质量门禁 ──► 验收 ──► 发布 ──► 运维/监控 ──► 迭代 ──► (退役：导出数据+备份后下线)
 specs/plans     AGENTS.md   verify.sh   acceptance   deploy.sh   health/备份      Flyway
                 分支+中文提交  check/build  e2e(隔离)    标签发布      日志排查         schema 演进
```

各阶段准出门槛：
1. **设计**：spec/plan 文档落库；表结构变更含 Flyway 方案。
2. **开发**：遵循分层与样式约定；新资源带 couple_id 隔离与功能开关。
3. **门禁**：`bash scripts/verify.sh` 全绿 + `check-miniprogram.mjs` 通过。
4. **验收**：`acceptance.sh` 通过；涉及全链路时在隔离环境跑 `acceptance-full.mjs`。
5. **发布**：`.env` 校验通过、git 打标签、部署后健康检查通过、冒烟（登录 + 图片显示）。
6. **运维**：每日备份 cron；升级/回滚按 7.3；密钥轮换按 7.5。
7. **退役**：导出数据库备份与 `upload_data` 卷交付用户，再下线服务。

## 10. 常用命令速查

```bash
# 开发
cd backend && mvn spring-boot:run          # 后端 :8080
cd frontend && npm run dev                 # 前端 :5173
node scripts/check-miniprogram.mjs         # 小程序静态校验

# 质量
bash scripts/verify.sh                     # 提交前完整检查
cd frontend && npm run check && npm run build
cd backend && mvn test

# 部署
cp .env.example .env && bash deploy/validate-env.sh
docker compose up -d --build               # 或 bash deploy/deploy.sh
docker compose logs -f backend frontend ; docker compose ps

# 运维
docker compose exec -T mysql mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" qinglv_note > backup-$(date +%F).sql
curl http://127.0.0.1:8090/actuator/health
```

## 11. 文档索引

| 文档 | 内容 |
| --- | --- |
| `README.md` | 功能一览、注册/绑定规则、快速开始 |
| `AGENTS.md` | 面向开发者的项目说明与代码约定（AI 代理同样适用） |
| `docs/ARCHITECTURE.md` | 架构细节与核心流程 |
| `docs/OPERATIONS.md` | 生产运维手册（部署/回滚/备份/密钥） |
| `docs/REFACTORING_REVIEW.md` | 结构审查：已落地改进与后续审查项 |
| `scripts/start-and-accept.md` | 隔离 E2E 环境用法与覆盖矩阵 |
| `miniprogram/README.md` | 小程序开发说明 |
