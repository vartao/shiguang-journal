-- 增量迁移：笔记评论表
-- 适用于已部署的 qinglv_note 数据库

CREATE TABLE IF NOT EXISTS ql_note_comment (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '评论ID',
    note_id BIGINT NOT NULL COMMENT '所属笔记ID',
    user_id BIGINT NOT NULL COMMENT '评论者用户ID',
    content VARCHAR(500) NOT NULL COMMENT '评论内容',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_note_id (note_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记评论表';
