-- Flyway 迁移：ql_note 增加 note_date（用户选择的记录日期）
-- 对应 sql/schema.sql 与 sql/migration_note_date.sql；已包含该列/索引的环境会自动跳过

SET @col_exists := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ql_note' AND COLUMN_NAME = 'note_date'
);
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE ql_note ADD COLUMN note_date DATE DEFAULT NULL AFTER like_count',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 历史数据回填：默认取创建时间
UPDATE ql_note SET note_date = DATE(create_time) WHERE note_date IS NULL;

ALTER TABLE ql_note MODIFY COLUMN note_date DATE NOT NULL COMMENT '用户选择的记录日期';

SET @idx_exists := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ql_note' AND INDEX_NAME = 'idx_note_date'
);
SET @ddl := IF(@idx_exists = 0,
    'CREATE INDEX idx_note_date ON ql_note (note_date)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
