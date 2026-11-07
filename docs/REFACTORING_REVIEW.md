# 项目结构与重构审查

## 1. 当前结构

项目是前后端分离的单仓库应用：

```text
frontend (Vue 3 / Pinia / Router)
    -> /api
deploy/nginx.conf
    -> backend (Spring Boot / MyBatis-Plus)
        -> MySQL 8
        -> uploads volume
```

| 区域 | 当前职责 | 评价 |
| --- | --- | --- |
| `frontend/src/views` | 路由页面与页面状态 | 功能完整，但多个 SFC 同时包含大量样式和业务状态 |
| `frontend/src/components` | 通用视觉与交互组件 | 已有 Modal、上传器、空/错/加载态等良好抽象 |
| `frontend/src/features` | 表单映射和兼容层 | 方向正确，可继续承接页面业务规则 |
| `frontend/src/api` | Axios 与 API 映射 | 响应 envelope 和错误提示已集中处理 |
| `backend/controller` | HTTP 参数和响应包装 | 当前用户与空间解析已集中到请求级上下文 |
| `backend/service` | 权限、事务和业务规则 | 业务边界清晰，部分列表装配仍可继续批量化 |
| `backend/mapper` | MyBatis-Plus 与原子 SQL | 点赞计数已使用原子更新；邀请码绑定已加入行锁 |
| `sql` | 初始化 schema 与 Flyway 增量脚本 | schema 与线上版本化迁移职责已分离 |
| `deploy` | Compose、Nginx 与运维脚本 | 单机部署路径清晰，适合当前双人私密产品规模 |

## 2. 本轮已落地

- 时间线批量加载作者和图片，移除逐条 N+1 查询。
- 抽离前后端日历日期计算，修复 UTC 日期偏移、纪念日当天跳到明年和闰日异常。
- 将笔记 `note_date` 从编辑器贯通到 DTO、Entity、schema 和增量迁移。
- 相册照片写入与汇总刷新统一封装；删除封面后自动选择替代封面。
- 修复删除照片失败时前端仍移除卡片的状态不一致。
- 修复惊喜内容使用 `v-html` 造成的存储型 XSS。
- 本地 JSON 缓存损坏时自动恢复，不再阻断应用启动。
- 邀请码绑定使用 `SELECT ... FOR UPDATE`，避免并发占用同一个成员位置。
- 注册依赖数据库唯一索引处理并发重名，返回稳定业务错误。
- 限流计数定期清理；Nginx 覆盖客户端伪造的转发头。
- 修复 Nginx `/uploads/` 被静态扩展名正则抢占的问题，并补安全响应头。
- 两份 schema 纳入 `scripts/verify.sh` 一致性门禁；新增根目录 `.gitignore`。
- 新增请求级 `CurrentUserContext`，统一 Controller 的用户与空间解析，并移除 JWT 静态 ThreadLocal 状态。
- 上传资源改为签名 URL 访问：出参由 Jackson 全局序列化器（`UploadUrlSerializer`）统一附加 HMAC 签名与过期时间（`UploadUrlSigner`，密钥缺省复用 `JWT_SECRET`），`UploadsController` 校验后流式返回文件（无效签名一律 404，缓存头由后端下发）；nginx 从直接 alias 磁盘改为代理到后端，`/uploads/**` 不再绕过应用层；写路径入库前统一剥离查询参数（`UploadUrls.toStoredPath`），签名只存在于出参边界。
- 上传校验增加文件头魔数比对与图片解码验证（`FileSignatureValidator`），伪造扩展名/Content-Type 的文件在落盘前被拒绝。
- 心愿、惊喜、时间胶囊列表改为批量装配用户信息（`selectBatchIds`），消除逐条查询的 N+1。

## 3. 后续审查项

### 高优先级

1. **OSS 模式的文件仍为公开 URL。** 签名保护目前只覆盖本地磁盘存储；启用 `ALIYUN_OSS_*` 后建议切换私有桶并改用 SDK 预签名 URL 出参。

### 中优先级

1. 上传图片暂未做解码重编码：Java 图像编码不处理 EXIF 方向，直接重编码会让手机竖拍照片横倒；若要剥除元数据（如 GPS）需同步实现方向旋转后重编码。
2. 限流仍为单进程内存状态，多实例部署时不共享；横向扩容后应迁移到 Redis/网关限流。
3. 数据表主要依赖应用层维护关系，缺少外键或离线一致性检查；删除业务可能留下上传文件与历史孤儿记录。
4. 多个页面文件超过 500 行，主要是页面专属 CSS。建议按功能拆出编辑表单、列表卡片和页面样式文件，而不是继续扩大路由组件。

### 兼容性决策

业务异常目前通过 HTTP 200 + 响应体 `code` 表达，前端已兼容，但不符合常规 HTTP 语义。若改为真实 4xx，应作为 API 版本变更处理，并同步第三方调用方与验收脚本。

## 4. 质量门禁

```bash
bash scripts/verify.sh
docker compose config --quiet
node --check scripts/acceptance-full.mjs
```

`verify.sh` 覆盖前端 ESLint、Prettier、Vitest、生产构建、后端测试，以及 schema 双副本一致性。

隔离 Docker 环境中运行 `scripts/acceptance-full.mjs` 可补充所有核心 API 的真实 MySQL/Nginx 业务链路；脚本只允许在一次性测试库上执行。
