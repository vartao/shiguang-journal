-- Flyway 迁移：旅行足迹表 + 旅行照片表
-- 对应 sql/schema.sql；已包含该表的环境会跳过（IF NOT EXISTS）

CREATE TABLE IF NOT EXISTS ql_travel (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '足迹ID',
    couple_id BIGINT NOT NULL COMMENT '所属情侣空间ID',
    user_id BIGINT NOT NULL COMMENT '创建者用户ID',
    province VARCHAR(30) NOT NULL COMMENT '省份（用于地图点亮）',
    city VARCHAR(50) NOT NULL COMMENT '城市',
    latitude DECIMAL(10,6) DEFAULT NULL COMMENT '纬度（可选）',
    longitude DECIMAL(10,6) DEFAULT NULL COMMENT '经度（可选）',
    travel_date DATE DEFAULT NULL COMMENT '出行日期',
    season VARCHAR(10) DEFAULT NULL COMMENT '季节：春/夏/秋/冬',
    weather VARCHAR(20) DEFAULT NULL COMMENT '天气',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    note VARCHAR(2000) DEFAULT NULL COMMENT '旅行手记',
    cover VARCHAR(500) DEFAULT NULL COMMENT '封面图URL',
    status TINYINT DEFAULT 1 COMMENT '状态：0-想去 1-已去过',
    sort_order INT DEFAULT 0 COMMENT '排序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_couple_id (couple_id),
    KEY idx_province (province),
    KEY idx_travel_date (travel_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='旅行足迹表';

CREATE TABLE IF NOT EXISTS ql_travel_photo (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '照片ID',
    travel_id BIGINT NOT NULL COMMENT '所属足迹ID',
    couple_id BIGINT NOT NULL COMMENT '所属情侣空间ID',
    photo_url VARCHAR(500) NOT NULL COMMENT '照片URL',
    sort_order INT DEFAULT 0 COMMENT '排序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_travel_id (travel_id),
    KEY idx_couple_id (couple_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='旅行照片表';
