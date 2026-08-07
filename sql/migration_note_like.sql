-- 已有数据库增量迁移：笔记点赞去重表
-- 用法：mysql -uroot -p qinglv_note < sql/migration_note_like.sql

USE qinglv_note;

CREATE TABLE IF NOT EXISTS ql_note_like (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
    note_id BIGINT NOT NULL COMMENT '笔记ID',
    user_id BIGINT NOT NULL COMMENT '点赞用户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_note_user (note_id, user_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记点赞表';
