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
