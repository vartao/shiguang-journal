-- Flyway 迁移：行程计划表
-- 对应 sql/schema.sql；已包含该表的环境会跳过（IF NOT EXISTS）

CREATE TABLE IF NOT EXISTS ql_plan (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '行程ID',
    couple_id BIGINT NOT NULL COMMENT '所属情侣空间ID',
    user_id BIGINT NOT NULL COMMENT '创建者用户ID',
    title VARCHAR(200) NOT NULL COMMENT '行程标题',
    destination VARCHAR(200) DEFAULT NULL COMMENT '目的地',
    cover VARCHAR(500) DEFAULT NULL COMMENT '封面图URL',
    start_date DATE DEFAULT NULL COMMENT '开始日期',
    end_date DATE DEFAULT NULL COMMENT '结束日期',
    budget DECIMAL(10,2) DEFAULT NULL COMMENT '预算（元）',
    days TEXT COMMENT '每日安排JSON：[{date,items:[text]}]',
    note VARCHAR(1000) DEFAULT NULL COMMENT '备注',
    status TINYINT DEFAULT 0 COMMENT '状态：0-计划中 1-已出发 2-已完成',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_couple_id (couple_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='行程计划表';
