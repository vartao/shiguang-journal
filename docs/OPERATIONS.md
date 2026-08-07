# 生产运维手册

## 部署前

1. 复制 `.env.example` 为 `.env`，设置强随机 `MYSQL_ROOT_PASSWORD`、至少 32 字符的 `JWT_SECRET` 和生产域名 `CORS_ORIGINS`。
2. 执行 `bash deploy/validate-env.sh`。
3. 确认证书、域名和备份策略。

## 日常部署与回滚

- 部署：`bash deploy/deploy.sh`
- 查看状态：`docker compose ps`
- 查看日志：`docker compose logs -f backend frontend`
- 回滚：切换到上一版本代码后执行 `docker compose up -d --build`。

## 数据库备份/恢复

```bash
docker compose exec -T mysql mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" qinglv_note > backup-$(date +%F).sql
cat backup.sql | docker compose exec -T mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" qinglv_note
```

恢复前先停止写入并保留当前备份。`sql/` 中的迁移脚本需按版本顺序执行，生产环境不要直接覆盖 schema。

## 文件与密钥

上传文件由 `upload_data` 卷持久化，删除或迁移前先备份该卷。轮换 JWT 会使现有会话失效，应安排维护窗口并通知用户重新登录。

## 健康检查

后端只暴露 Actuator `health`、`info`，Compose 会等待后端健康后再启动前端；部署脚本使用有上限的轮询。
