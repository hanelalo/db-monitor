-- Database Migration Script for Disk Space Monitoring
-- Version: 1.1
-- Description: Add disk space estimation columns to db_monitor_statistics table

-- Check if columns already exist before adding them
SET @sql = '';

-- Add estimated_disk_size_bytes column if it doesn't exist
SELECT COUNT(*) INTO @col_exists FROM information_schema.columns 
WHERE table_schema = DATABASE() 
AND table_name = 'db_monitor_statistics' 
AND column_name = 'estimated_disk_size_bytes';

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE db_monitor_statistics ADD COLUMN estimated_disk_size_bytes bigint(20) DEFAULT NULL COMMENT ''增量数据预估磁盘空间大小（字节）'' AFTER increment_count;',
    'SELECT ''Column estimated_disk_size_bytes already exists'' as msg;');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add avg_row_size_bytes column if it doesn't exist
SELECT COUNT(*) INTO @col_exists FROM information_schema.columns 
WHERE table_schema = DATABASE() 
AND table_name = 'db_monitor_statistics' 
AND column_name = 'avg_row_size_bytes';

SET @sql = IF(@col_exists = 0, 
    'ALTER TABLE db_monitor_statistics ADD COLUMN avg_row_size_bytes bigint(20) DEFAULT NULL COMMENT ''平均每行数据大小（字节）'' AFTER estimated_disk_size_bytes;',
    'SELECT ''Column avg_row_size_bytes already exists'' as msg;');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add indexes for better performance on disk space queries
CREATE INDEX IF NOT EXISTS idx_disk_size ON db_monitor_statistics(estimated_disk_size_bytes);
CREATE INDEX IF NOT EXISTS idx_avg_row_size ON db_monitor_statistics(avg_row_size_bytes);

-- Update table comment to reflect new functionality
ALTER TABLE db_monitor_statistics COMMENT = '数据库监控统计表（包含磁盘空间估算）';

SELECT 'Database migration completed successfully - disk space monitoring columns added' as result;