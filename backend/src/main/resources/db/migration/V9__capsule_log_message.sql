-- Flyway 迁移：时间胶囊表 + 情侣日志表 + 留言板表
-- 对应 sql/schema.sql；已包含该表的环境会跳过（IF NOT EXISTS）

CREATE TABLE IF NOT EXISTS ql_capsule (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '胶囊ID',
    couple_id BIGINT NOT NULL COMMENT '所属情侣空间ID',
    user_id BIGINT NOT NULL COMMENT '创建者用户ID',
    title VARCHAR(200) NOT NULL COMMENT '胶囊标题',
    message TEXT COMMENT '信件内容',
    photo_urls VARCHAR(2000) DEFAULT NULL COMMENT '照片URL JSON数组',
    open_date DATE NOT NULL COMMENT '开启日期',
    is_opened TINYINT DEFAULT 0 COMMENT '是否已开启：0-未开启 1-已开启',
    opened_by BIGINT DEFAULT NULL COMMENT '开启者用户ID',
    opened_time DATETIME DEFAULT NULL COMMENT '开启时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_couple_id (couple_id),
    KEY idx_open_date (open_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='时间胶囊表';

CREATE TABLE IF NOT EXISTS ql_couple_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID',
    couple_id BIGINT NOT NULL COMMENT '所属情侣空间ID',
    user_id BIGINT NOT NULL COMMENT '创建者用户ID',
    category VARCHAR(32) DEFAULT 'other' COMMENT '分类：movie-电影 food-美食 show-演出 travel-旅行 date-约会 sport-运动 other-其他',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    log_date DATE NOT NULL COMMENT '发生日期',
    note VARCHAR(2000) DEFAULT NULL COMMENT '手记',
    cover VARCHAR(500) DEFAULT NULL COMMENT '封面图URL',
    rating TINYINT DEFAULT 0 COMMENT '评分：0-5',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_couple_id (couple_id),
    KEY idx_log_date (log_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='情侣日志表';

CREATE TABLE IF NOT EXISTS ql_message (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '留言ID',
    couple_id BIGINT NOT NULL COMMENT '所属情侣空间ID',
    user_id BIGINT NOT NULL COMMENT '留言者用户ID',
    content VARCHAR(500) NOT NULL COMMENT '留言内容',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_couple_id (couple_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='留言板表';
