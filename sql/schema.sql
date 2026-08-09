-- ============================================
-- 情侣笔记网站 数据库初始化脚本
-- 数据库：qinglv_note
-- 说明：与 backend Entity / MyBatis-Plus 字段保持一致
-- Docker 首次启动会挂载本文件到 /docker-entrypoint-initdb.d/
-- ============================================

CREATE DATABASE IF NOT EXISTS qinglv_note DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE qinglv_note;

-- ----------------------------
-- 用户表
-- ----------------------------
DROP TABLE IF EXISTS ql_user;
CREATE TABLE ql_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码（BCrypt加密）',
    nickname VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    avatar VARCHAR(500) DEFAULT NULL COMMENT '头像URL',
    gender TINYINT DEFAULT 0 COMMENT '性别：0-保密 1-男 2-女',
    birthday DATETIME DEFAULT NULL COMMENT '生日',
    couple_id BIGINT DEFAULT NULL COMMENT '所属情侣空间ID',
    role VARCHAR(20) DEFAULT 'owner' COMMENT '角色：owner-空间创建者 member-成员',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    KEY idx_couple_id (couple_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ----------------------------
-- 情侣空间表
-- ----------------------------
DROP TABLE IF EXISTS ql_couple;
CREATE TABLE ql_couple (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '空间ID',
    name VARCHAR(100) DEFAULT NULL COMMENT '空间名称',
    avatar VARCHAR(500) DEFAULT NULL COMMENT '空间头像/封面',
    together_date DATE DEFAULT NULL COMMENT '在一起日期',
    user_id1 BIGINT DEFAULT NULL COMMENT '成员1用户ID',
    user_id2 BIGINT DEFAULT NULL COMMENT '成员2用户ID',
    invite_code VARCHAR(20) DEFAULT NULL COMMENT '空间邀请码',
    announcement VARCHAR(500) DEFAULT NULL COMMENT '空间公告/签名',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_invite_code (invite_code),
    KEY idx_user_id1 (user_id1),
    KEY idx_user_id2 (user_id2)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='情侣空间表';

-- ----------------------------
-- 笔记表
-- ----------------------------
DROP TABLE IF EXISTS ql_note;
CREATE TABLE ql_note (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '笔记ID',
    couple_id BIGINT NOT NULL COMMENT '所属情侣空间ID',
    user_id BIGINT NOT NULL COMMENT '作者用户ID',
    title VARCHAR(200) NOT NULL COMMENT '笔记标题',
    content TEXT COMMENT '笔记内容',
    cover_image VARCHAR(500) DEFAULT NULL COMMENT '封面图URL',
    mood VARCHAR(20) DEFAULT NULL COMMENT '心情标签：happy/sad/angry/surprised/tired/loved',
    weather VARCHAR(20) DEFAULT NULL COMMENT '天气：sunny/cloudy/rainy/snowy/windy',
    is_top TINYINT DEFAULT 0 COMMENT '是否置顶：0-否 1-是',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    note_date DATE NOT NULL COMMENT '用户选择的记录日期',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_couple_id (couple_id),
    KEY idx_user_id (user_id),
    KEY idx_note_date (note_date),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记表';

-- ----------------------------
-- 笔记图片表
-- ----------------------------
DROP TABLE IF EXISTS ql_note_image;
CREATE TABLE ql_note_image (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '图片ID',
    note_id BIGINT NOT NULL COMMENT '所属笔记ID',
    image_url VARCHAR(500) NOT NULL COMMENT '图片URL',
    sort_order INT DEFAULT 0 COMMENT '排序序号',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_note_id (note_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记图片表';

-- ----------------------------
-- 笔记点赞表（用户维度去重）
-- ----------------------------
DROP TABLE IF EXISTS ql_note_like;
CREATE TABLE ql_note_like (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
    note_id BIGINT NOT NULL COMMENT '笔记ID',
    user_id BIGINT NOT NULL COMMENT '点赞用户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_note_user (note_id, user_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记点赞表';

-- ----------------------------
-- 相册表
-- ----------------------------
DROP TABLE IF EXISTS ql_album;
CREATE TABLE ql_album (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '相册ID',
    couple_id BIGINT NOT NULL COMMENT '所属情侣空间ID',
    user_id BIGINT NOT NULL COMMENT '创建者用户ID',
    name VARCHAR(100) NOT NULL COMMENT '相册名称',
    description VARCHAR(500) DEFAULT NULL COMMENT '相册描述',
    cover_image VARCHAR(500) DEFAULT NULL COMMENT '封面图URL',
    photo_count INT DEFAULT 0 COMMENT '照片数量',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_couple_id (couple_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='相册表';

-- ----------------------------
-- 照片表
-- ----------------------------
DROP TABLE IF EXISTS ql_photo;
CREATE TABLE ql_photo (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '照片ID',
    album_id BIGINT NOT NULL COMMENT '所属相册ID',
    couple_id BIGINT NOT NULL COMMENT '所属情侣空间ID',
    user_id BIGINT NOT NULL COMMENT '上传者用户ID',
    photo_url VARCHAR(500) NOT NULL COMMENT '照片URL',
    thumbnail_url VARCHAR(500) DEFAULT NULL COMMENT '缩略图URL',
    description VARCHAR(500) DEFAULT NULL COMMENT '照片描述',
    file_size BIGINT DEFAULT NULL COMMENT '文件大小（字节）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_album_id (album_id),
    KEY idx_couple_id (couple_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='照片表';

-- ----------------------------
-- 纪念日表
-- ----------------------------
DROP TABLE IF EXISTS ql_anniversary;
CREATE TABLE ql_anniversary (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '纪念日ID',
    couple_id BIGINT NOT NULL COMMENT '所属情侣空间ID',
    user_id BIGINT NOT NULL COMMENT '创建者用户ID',
    name VARCHAR(100) NOT NULL COMMENT '纪念日名称',
    date DATE NOT NULL COMMENT '纪念日日期',
    type VARCHAR(20) DEFAULT 'custom' COMMENT '类型：love-恋爱 birthday-生日 custom-自定义',
    icon VARCHAR(50) DEFAULT NULL COMMENT '图标',
    repeat_yearly TINYINT DEFAULT 1 COMMENT '是否每年重复：0-否 1-是',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_couple_id (couple_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='纪念日表';

-- ----------------------------
-- 惊喜彩蛋表
-- ----------------------------
DROP TABLE IF EXISTS ql_surprise;
CREATE TABLE ql_surprise (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '彩蛋ID',
    couple_id BIGINT NOT NULL COMMENT '所属情侣空间ID',
    user_id BIGINT NOT NULL COMMENT '创建者用户ID（送出方）',
    title VARCHAR(200) NOT NULL COMMENT '彩蛋标题',
    content TEXT COMMENT '彩蛋内容（解锁后可见）',
    hint VARCHAR(500) DEFAULT NULL COMMENT '封面/提示语（未解锁时展示）',
    cover_image VARCHAR(500) DEFAULT NULL COMMENT '封面图URL',
    unlock_condition VARCHAR(20) NOT NULL COMMENT '解锁条件类型：date/password/click',
    unlock_date DATE DEFAULT NULL COMMENT '解锁日期（date类型）',
    unlock_password VARCHAR(100) DEFAULT NULL COMMENT '解锁密码（password类型，BCrypt加密）',
    unlocked TINYINT DEFAULT 0 COMMENT '是否已解锁：0-未解锁 1-已解锁',
    unlock_time DATETIME DEFAULT NULL COMMENT '解锁时间',
    unlocked_by BIGINT DEFAULT NULL COMMENT '解锁者用户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_couple_id (couple_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='惊喜彩蛋表';

-- ----------------------------
-- 心愿单表
-- ----------------------------
DROP TABLE IF EXISTS ql_wish;
CREATE TABLE ql_wish (
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

-- ----------------------------
-- 文件表
-- ----------------------------
DROP TABLE IF EXISTS ql_file;
CREATE TABLE ql_file (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '文件ID',
    user_id BIGINT DEFAULT NULL COMMENT '上传者用户ID',
    original_name VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
    stored_name VARCHAR(255) DEFAULT NULL COMMENT '存储文件名',
    file_path VARCHAR(500) DEFAULT NULL COMMENT '文件路径',
    file_url VARCHAR(500) DEFAULT NULL COMMENT '访问URL',
    file_type VARCHAR(20) DEFAULT NULL COMMENT '文件类型（扩展名）',
    file_size BIGINT DEFAULT NULL COMMENT '文件大小（字节）',
    content_type VARCHAR(100) DEFAULT NULL COMMENT '文件MIME类型',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件表';

-- ----------------------------
-- 笔记评论表
-- ----------------------------
DROP TABLE IF EXISTS ql_note_comment;
CREATE TABLE ql_note_comment (
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

-- ----------------------------
-- 旅行足迹表
-- ----------------------------
DROP TABLE IF EXISTS ql_travel;
CREATE TABLE ql_travel (
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

-- ----------------------------
-- 旅行照片表
-- ----------------------------
DROP TABLE IF EXISTS ql_travel_photo;
CREATE TABLE ql_travel_photo (
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

-- ----------------------------
-- 行程计划表
-- ----------------------------
DROP TABLE IF EXISTS ql_plan;
CREATE TABLE ql_plan (
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

-- ----------------------------
-- 菜品池表
-- ----------------------------
DROP TABLE IF EXISTS ql_dish;
CREATE TABLE ql_dish (
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

-- ----------------------------
-- 今日菜单表
-- ----------------------------
DROP TABLE IF EXISTS ql_menu_meal;
CREATE TABLE ql_menu_meal (
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

-- ----------------------------
-- 功能开关表
-- ----------------------------
DROP TABLE IF EXISTS ql_couple_feature;
CREATE TABLE ql_couple_feature (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    couple_id BIGINT NOT NULL COMMENT '所属情侣空间ID',
    feature_key VARCHAR(30) NOT NULL COMMENT '功能key：notes/albums/anniversary/surprise/wishes/stats/travels/plans/menu',
    enabled TINYINT DEFAULT 1 COMMENT '是否开启：0-关闭 1-开启',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_couple_feature (couple_id, feature_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='功能开关表';

-- ----------------------------
-- 时间胶囊表
-- ----------------------------
DROP TABLE IF EXISTS ql_capsule;
CREATE TABLE ql_capsule (
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

-- ----------------------------
-- 情侣日志表
-- ----------------------------
DROP TABLE IF EXISTS ql_couple_log;
CREATE TABLE ql_couple_log (
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

-- ----------------------------
-- 留言板表
-- ----------------------------
DROP TABLE IF EXISTS ql_message;
CREATE TABLE ql_message (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '留言ID',
    couple_id BIGINT NOT NULL COMMENT '所属情侣空间ID',
    user_id BIGINT NOT NULL COMMENT '留言者用户ID',
    content VARCHAR(500) NOT NULL COMMENT '留言内容',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    PRIMARY KEY (id),
    KEY idx_couple_id (couple_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='留言板表';
