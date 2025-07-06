-- 测试数据

-- 插入测试表数据
INSERT INTO test_table (name, email, created_time, updated_time) VALUES
('张三', 'zhangsan@example.com', '2024-01-01 10:00:00', '2024-01-01 10:00:00'),
('李四', 'lisi@example.com', '2024-01-01 11:00:00', '2024-01-01 11:00:00'),
('王五', 'wangwu@example.com', '2024-01-01 12:00:00', '2024-01-01 12:00:00'),
('赵六', 'zhaoliu@example.com', '2024-01-02 10:00:00', '2024-01-02 10:00:00'),
('钱七', 'qianqi@example.com', '2024-01-02 11:00:00', '2024-01-02 11:00:00');

-- 插入用户表数据
INSERT INTO user_table (username, password, email, phone, created_time, updated_time, last_login_time, age, balance) VALUES
('admin', 'password123', 'admin@example.com', '13800138000', '2024-01-01 09:00:00', '2024-01-01 09:00:00', '2024-01-03 09:00:00', 30, 1000.00),
('user1', 'password123', 'user1@example.com', '13800138001', '2024-01-01 10:30:00', '2024-01-01 10:30:00', '2024-01-03 10:30:00', 25, 500.50),
('user2', 'password123', 'user2@example.com', '13800138002', '2024-01-01 14:00:00', '2024-01-01 14:00:00', '2024-01-03 14:00:00', 28, 750.25),
('user3', 'password123', 'user3@example.com', '13800138003', '2024-01-02 09:30:00', '2024-01-02 09:30:00', '2024-01-03 09:30:00', 35, 1200.00),
('user4', 'password123', 'user4@example.com', '13800138004', '2024-01-02 15:00:00', '2024-01-02 15:00:00', '2024-01-03 15:00:00', 22, 300.75);

-- 插入监控配置数据
INSERT INTO monitor_config (config_name, data_source_name, table_name, time_column_name, time_column_type, enabled, interval_type, interval_value, description, created_time, updated_time, created_by) VALUES
('test_table_monitor', 'primary', 'test_table', 'created_time', 'DATETIME', true, 'MINUTES', 10, '测试表监控配置', '2024-01-01 08:00:00', '2024-01-01 08:00:00', 'admin'),
('user_table_monitor', 'primary', 'user_table', 'created_time', 'DATETIME', true, 'MINUTES', 15, '用户表监控配置', '2024-01-01 08:30:00', '2024-01-01 08:30:00', 'admin'),
('disabled_monitor', 'primary', 'test_table', 'updated_time', 'DATETIME', false, 'HOURS', 1, '已禁用的监控配置', '2024-01-01 09:00:00', '2024-01-01 09:00:00', 'admin');

-- 插入监控统计数据
INSERT INTO db_monitor_statistics (data_source_name, table_name, statistic_time, start_time, end_time, increment_count, estimated_disk_size_bytes, avg_row_size_bytes, interval_type, interval_value, created_time) VALUES
('primary', 'test_table', '2024-01-01 10:00:00', '2024-01-01 09:50:00', '2024-01-01 10:00:00', 2, 1024, 512, 'MINUTES', 10, '2024-01-01 10:00:00'),
('primary', 'test_table', '2024-01-01 11:00:00', '2024-01-01 10:50:00', '2024-01-01 11:00:00', 1, 512, 512, 'MINUTES', 10, '2024-01-01 11:00:00'),
('primary', 'user_table', '2024-01-01 11:00:00', '2024-01-01 10:45:00', '2024-01-01 11:00:00', 1, 2048, 1024, 'MINUTES', 15, '2024-01-01 11:00:00'),
('primary', 'user_table', '2024-01-01 15:00:00', '2024-01-01 14:45:00', '2024-01-01 15:00:00', 1, 2048, 1024, 'MINUTES', 15, '2024-01-01 15:00:00');
