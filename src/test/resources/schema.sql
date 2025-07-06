-- 测试数据库表结构

-- 创建测试表
CREATE TABLE IF NOT EXISTS test_table (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    status INT DEFAULT 1
);

-- 创建用户表
CREATE TABLE IF NOT EXISTS user_table (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login_time DATETIME,
    status TINYINT DEFAULT 1,
    age INT,
    balance DECIMAL(10,2) DEFAULT 0.00
);

-- 创建监控配置表
CREATE TABLE IF NOT EXISTS monitor_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_name VARCHAR(255) NOT NULL UNIQUE,
    data_source_name VARCHAR(255) NOT NULL,
    table_name VARCHAR(255) NOT NULL,
    time_column_name VARCHAR(255) NOT NULL,
    time_column_type VARCHAR(50) NOT NULL DEFAULT 'DATETIME',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    interval_type VARCHAR(50) NOT NULL DEFAULT 'MINUTES',
    interval_value INT NOT NULL DEFAULT 10,
    description TEXT,
    created_time DATETIME NOT NULL,
    updated_time DATETIME NOT NULL,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    extend_config TEXT
);

-- 创建监控统计表
CREATE TABLE IF NOT EXISTS db_monitor_statistics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    data_source_name VARCHAR(255) NOT NULL,
    table_name VARCHAR(255) NOT NULL,
    statistic_time DATETIME NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    increment_count BIGINT NOT NULL DEFAULT 0,
    estimated_disk_size_bytes BIGINT,
    avg_row_size_bytes BIGINT,
    interval_type VARCHAR(50) NOT NULL,
    interval_value INT NOT NULL,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    additional_info TEXT
);

-- 创建索引
CREATE INDEX IF NOT EXISTS idx_monitor_config_data_source_table ON monitor_config(data_source_name, table_name);
CREATE INDEX IF NOT EXISTS idx_monitor_config_enabled ON monitor_config(enabled);
CREATE INDEX IF NOT EXISTS idx_monitor_statistics_table_time ON db_monitor_statistics(table_name, statistic_time);
CREATE INDEX IF NOT EXISTS idx_monitor_statistics_created_time ON db_monitor_statistics(created_time);
