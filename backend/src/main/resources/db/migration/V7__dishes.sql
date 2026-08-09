-- Flyway 迁移：菜品池表 + 今日菜单表
-- 对应 sql/schema.sql；已包含该表的环境会跳过（IF NOT EXISTS）

CREATE TABLE IF NOT EXISTS ql_dish (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '菜品ID',
    couple_id BIGINT NOT NULL COMMENT '所属情侣空间ID',
    user_id BIGINT NOT NULL COMMENT '创建者用户ID',
    name VARCHAR(100) NOT NULL COMMENT '菜名',
    category VARCHAR(20) DEFAULT 'lunch' COMMENT '分类：breakfast-早餐 lunch-午餐 dinner-晚餐 night-宵夜 snack-甜点 drink-饮品',
    note VARCHAR(500) DEFAULT NULL COMMENT '备注（做法/店名等）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_couple_id (couple_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品池表';

CREATE TABLE IF NOT EXISTS ql_menu_meal (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    couple_id BIGINT NOT NULL COMMENT '所属情侣空间ID',
    user_id BIGINT NOT NULL COMMENT '点菜者用户ID',
    meal_date DATE NOT NULL COMMENT '日期',
    meal_type VARCHAR(10) NOT NULL COMMENT '餐次：breakfast-早餐 lunch-午餐 dinner-晚餐',
    dish_name VARCHAR(100) NOT NULL COMMENT '菜名',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_couple_date_meal_dish (couple_id, meal_date, meal_type, dish_name),
    KEY idx_couple_date (couple_id, meal_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='今日菜单表';
