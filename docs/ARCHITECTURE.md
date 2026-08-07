# 拾光手账（qinglv）架构与模块说明

> 面向维护者：产品边界、分层、核心流程与已知改进点。

## 1. 产品边界

双人私密情侣空间（最多 2 人）：

| 能力 | 说明 |
|------|------|
| 账号 | 注册 / 登录，BCrypt + JWT（7 天） |
| 小窝 | 邀请码绑定、空间名、签名、在一起日期 |
| 时间线 | 笔记 CRUD、心情/天气、配图、置顶、点赞、搜索 |
| 相册 | 相册与照片 |
| 纪念日 | 倒计时（恋爱 / 生日 / 自定义） |
| 惊喜盒子 | date / password / click 解锁 |
| 心愿单 | 分类、优先级、完成状态 |
| 恋爱数据 | 计数、活跃、连续写作、心情分布 |
| 文件 | 本地磁盘上传（扩展名 + Content-Type 校验） |

**不在当前范围：** 多人家庭、公开社区、实时聊天、对象存储 CDN。

## 2. 部署拓扑

```
浏览器
  → Nginx :8090
       ├─ /          前端静态（Vue build）
       ├─ /api/      → Spring Boot :8080
       └─ /uploads/  共享卷 upload_data（只读）
  Spring Boot
       → MySQL 8.x（库 qinglv_note）
```

Docker Compose 服务：`mysql` / `backend` / `frontend`。  
首次 MySQL 会执行 `sql/schema.sql`。

## 3. 后端分层

```
controller  →  解析参数、取 JWT 用户、Result 包装
service     →  业务规则、事务、couple 权限
mapper      →  MyBatis-Plus
entity/dto  →  表实体 / 入参 DTO / 出参 VO
common      →  JWT、限流、全局异常、统一 Result
```

鉴权：`JwtInterceptor` 校验 `Authorization: Bearer`，`userId` 放 ThreadLocal。  
登录/注册额外走 `RateLimiter`（按 IP，约 10 次/分钟）。

几乎所有业务资源都带 **`couple_id` 隔离**：跨空间访问返回 403。

## 4. 核心流程

### 4.1 注册与双人绑定

```
无邀请码 ─► 建 Couple（owner）+ 生成 6 位邀请码
有邀请码 ─► 校验码有效且未满员 ─► 写入 user_id2，role=member
```

也可先注册再在小窝页 `POST /api/couple/bind`。

### 4.2 笔记时间线

- 列表：`couple_id` 过滤，置顶优先，支持 mood、关键词搜索
- 写/改/删：仅 **作者**；点赞：`(note_id, user_id)` 唯一，可取消

### 4.3 惊喜解锁

| 类型 | 行为 |
|------|------|
| `click` | 任意成员点击即解锁 |
| `password` | BCrypt 校验密码 |
| `date` | 到期后自动开放（详情查询时检查）；`unlockedBy` 为空表示系统开放 |

未解锁时 VO **不返回 content**。

### 4.4 恋爱统计

聚合笔记/照片/相册/纪念日/惊喜/点赞，以及近 30 天活跃日、最长连续写作日、心情分布。

## 5. 前端结构

| 目录 | 职责 |
|------|------|
| `views/` | 页面（路由 meta 控制登录态） |
| `components/` | 胶片/拍立得/信封等视觉组件 |
| `api/` | Axios 封装（baseURL `/api`，自动带 token） |
| `stores/` | user / couple / theme |
| `assets/styles/` | 手账风 CSS 变量与动画 |

## 6. 数据表一览

`ql_user` · `ql_couple` · `ql_note` · `ql_note_image` · `ql_note_like` ·  
`ql_album` · `ql_photo` · `ql_anniversary` · `ql_surprise` · `ql_wish` · `ql_file`

逻辑删除字段：`deleted`（MyBatis-Plus 全局配置）。

## 7. 环境与安全清单

| 项 | 说明 |
|----|------|
| `MYSQL_ROOT_PASSWORD` | 生产必须改 |
| `JWT_SECRET` | ≥32 字符，生产必须改 |
| `CORS_ORIGINS` | 生产按域名配置 |
| 上传 | 扩展名白名单 + Content-Type 校验，最大约 20MB |
| HTTPS | `deploy/nginx.conf` 中预留 SSL 注释块 |

## 8. 已知局限 / 后续可做

1. 固定两人模型（`user_id1/2`），无解绑/换绑完整流程  
2. 上传为本地磁盘，无缩略图流水线（表字段 `thumbnail_url` 预留）  
3. 上传资源通过 `/uploads/**` 公开读取，尚未使用鉴权下载或签名 URL  
4. 限流为进程内内存，多实例不共享  
5. 已有核心 service 与控制器契约测试，仍缺基于真实 MySQL 的自动化集成测试  
6. PWA 仅注册了 Service Worker，离线能力有限  

## 9. 本地与 Docker 速查

```bash
# 本地
mysql -uroot -p < sql/schema.sql
cd backend && mvn spring-boot:run          # :8080
cd frontend && npm i && npm run dev        # :5173

# Docker
cp .env.example .env   # 改密码与 JWT
docker compose up -d --build               # :8090
```

测试：`cd backend && mvn test`
