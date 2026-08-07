-- 心愿单表
CREATE TABLE IF NOT EXISTS ql_wish (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '心愿ID',
    couple_id BIGINT NOT NULL COMMENT '所属情侣空间ID',
    user_id BIGINT NOT NULL COMMENT '创建者用户ID',
    title VARCHAR(200) NOT NULL COMMENT '心愿标题',
    description VARCHAR(1000) DEFAULT NULL COMMENT '心愿描述',
    icon VARCHAR(50) DEFAULT NULL COMMENT '图标emoji',
    category VARCHAR(30) DEFAULT 'other' COMMENT '分类：travel-旅行 food-美食 movie-电影 gift-礼物 other-其他',
    priority TINYINT DEFAULT 0 COMMENT '优先级：0-普通 1-重要 2-紧急',
    status TINYINT DEFAULT 0 COMMENT '状态：0-许愿中 1-已完成 2-已放弃',
    completed_time DATETIME DEFAULT NULL COMMENT '完成时间',
    completed_by BIGINT DEFAULT NULL COMMENT '完成者用户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_couple_id (couple_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='心愿单表';
