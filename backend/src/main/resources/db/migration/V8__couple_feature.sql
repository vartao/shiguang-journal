-- Flyway 迁移：功能开关表
-- 对应 sql/schema.sql；已包含该表的环境会跳过（IF NOT EXISTS）

CREATE TABLE IF NOT EXISTS ql_couple_feature (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    couple_id BIGINT NOT NULL COMMENT '所属情侣空间ID',
    feature_key VARCHAR(30) NOT NULL COMMENT '功能key：notes/albums/anniversary/surprise/wishes/stats/travels/plans/menu',
    enabled TINYINT DEFAULT 1 COMMENT '是否开启：0-关闭 1-开启',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_couple_feature (couple_id, feature_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='功能开关表';
