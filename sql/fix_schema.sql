-- ============================================
-- Fix all table schemas to match entity classes
-- ============================================

USE qinglv_note;

-- -------------------------------------------
-- 1. Fix ql_note
-- -------------------------------------------
ALTER TABLE ql_note CHANGE COLUMN author_id user_id BIGINT NOT NULL;
ALTER TABLE ql_note ADD COLUMN cover_image VARCHAR(500) DEFAULT NULL AFTER content;
ALTER TABLE ql_note ADD COLUMN is_top INT NOT NULL DEFAULT 0 AFTER weather;
ALTER TABLE ql_note ADD COLUMN like_count INT NOT NULL DEFAULT 0 AFTER is_top;
ALTER TABLE ql_note ADD COLUMN note_date DATE DEFAULT NULL AFTER like_count;
UPDATE ql_note SET note_date = DATE(create_time) WHERE note_date IS NULL;
ALTER TABLE ql_note MODIFY COLUMN note_date DATE NOT NULL;
CREATE INDEX idx_note_date ON ql_note (note_date);

-- -------------------------------------------
-- 2. Fix ql_album
-- -------------------------------------------
ALTER TABLE ql_album CHANGE COLUMN album_name name VARCHAR(100) NOT NULL;
ALTER TABLE ql_album CHANGE COLUMN cover_url cover_image VARCHAR(500) DEFAULT NULL;
ALTER TABLE ql_album ADD COLUMN user_id BIGINT DEFAULT NULL AFTER couple_id;

-- -------------------------------------------
-- 3. Fix ql_photo
-- -------------------------------------------
ALTER TABLE ql_photo CHANGE COLUMN uploader_id user_id BIGINT NOT NULL;
ALTER TABLE ql_photo CHANGE COLUMN image_url photo_url VARCHAR(500) NOT NULL;
ALTER TABLE ql_photo ADD COLUMN file_size BIGINT NOT NULL DEFAULT 0 AFTER description;

-- -------------------------------------------
-- 4. Fix ql_anniversary
-- -------------------------------------------
ALTER TABLE ql_anniversary CHANGE COLUMN title name VARCHAR(100) NOT NULL;
ALTER TABLE ql_anniversary CHANGE COLUMN anniversary_date date DATE NOT NULL;
ALTER TABLE ql_anniversary CHANGE COLUMN is_yearly repeat_yearly INT NOT NULL DEFAULT 1;
ALTER TABLE ql_anniversary ADD COLUMN user_id BIGINT DEFAULT NULL AFTER couple_id;
ALTER TABLE ql_anniversary ADD COLUMN type VARCHAR(20) DEFAULT 'custom' AFTER icon;
ALTER TABLE ql_anniversary ADD COLUMN remark VARCHAR(500) DEFAULT NULL AFTER repeat_yearly;

-- -------------------------------------------
-- 5. Fix ql_surprise
-- -------------------------------------------
ALTER TABLE ql_surprise CHANGE COLUMN creator_id user_id BIGINT NOT NULL;
ALTER TABLE ql_surprise CHANGE COLUMN is_unlocked unlocked INT NOT NULL DEFAULT 0;
ALTER TABLE ql_surprise CHANGE COLUMN unlocked_at unlock_time DATETIME DEFAULT NULL;
ALTER TABLE ql_surprise MODIFY COLUMN unlock_date DATE DEFAULT NULL;
ALTER TABLE ql_surprise ADD COLUMN hint VARCHAR(500) DEFAULT NULL AFTER content;
ALTER TABLE ql_surprise ADD COLUMN unlocked_by BIGINT DEFAULT NULL AFTER unlock_time;
