-- 为已有数据库补齐用户选择的笔记记录日期。
USE qinglv_note;

ALTER TABLE ql_note ADD COLUMN note_date DATE DEFAULT NULL AFTER like_count;
UPDATE ql_note SET note_date = DATE(create_time) WHERE note_date IS NULL;
ALTER TABLE ql_note MODIFY COLUMN note_date DATE NOT NULL COMMENT '用户选择的记录日期';
CREATE INDEX idx_note_date ON ql_note (note_date);
