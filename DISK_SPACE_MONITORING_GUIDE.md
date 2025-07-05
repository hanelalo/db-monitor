# 磁盘空间监控使用指南

## 概述

数据库监控系统现在支持对增量数据的磁盘空间占用进行预估，帮助您更好地了解数据增长对存储的影响。

## 新增功能

### 1. 磁盘空间估算
- 自动计算每个表增量数据的磁盘空间使用量
- 基于表结构和数据库统计信息进行智能估算
- 提供多种估算方法，确保准确性

### 2. 监控指标
- **增量数据磁盘空间**: 每个表在指定时间间隔内新增数据的预估磁盘空间
- **平均行大小**: 每个表的平均行大小（字节）
- **总磁盘空间使用**: 所有监控表的总磁盘空间使用量

### 3. 缓存机制
- 表结构信息会被缓存，避免重复查询
- 提高性能，减少数据库负载

## 使用方法

### 1. 升级现有部署

如果您已经部署了旧版本的监控系统，需要执行数据库迁移：

```sql
-- 执行迁移脚本
SOURCE src/main/resources/db/migration/V1.1__add_disk_space_columns.sql;
```

### 2. API 接口使用

#### 获取磁盘空间汇总信息
```bash
curl -X GET http://localhost:8080/api/db-monitor/disk-space/summary
```

响应示例：
```json
{
  "total_increment_count": 12500,
  "total_estimated_disk_size_bytes": 2048000,
  "total_estimated_disk_size_formatted": "2.00 MB",
  "monitored_tables_count": 5,
  "timestamp": "2024-01-15T10:30:00"
}
```

#### 获取详细监控指标
```bash
curl -X GET http://localhost:8080/api/db-monitor/metrics/json
```

响应包含磁盘空间详细信息：
```json
{
  "disk_space_summary": {
    "total_estimated_size_bytes": 384000,
    "total_estimated_size_formatted": "375.00 KB",
    "average_row_size_bytes": 256,
    "largest_table_increment": {
      "table_name": "user_activity",
      "increment_count": 800
    },
    "largest_disk_usage": {
      "table_name": "order_info",
      "estimated_disk_size_bytes": 204800,
      "estimated_disk_size_formatted": "200.00 KB"
    }
  },
  "table_metrics": [
    {
      "table_name": "user_activity",
      "increment_count": 800,
      "estimated_disk_size_bytes": 204800,
      "estimated_disk_size_formatted": "200.00 KB",
      "avg_row_size_bytes": 256,
      "avg_row_size_formatted": "256 B"
    }
  ]
}
```

### 3. Prometheus 监控

新增的Prometheus指标：

```promql
# 表增量数据磁盘空间估算
db_monitor_estimated_disk_size_bytes{data_source="primary",table="user_activity"}

# 表平均行大小
db_monitor_avg_row_size_bytes{data_source="primary",table="user_activity"}

# 总磁盘空间估算
db_monitor_total_estimated_disk_size_bytes{data_source="primary"}
```

### 4. Grafana 仪表板配置

#### 磁盘空间使用总览
```promql
# 总磁盘空间使用量
db_monitor_total_estimated_disk_size_bytes

# 各表磁盘空间使用排行
topk(10, db_monitor_estimated_disk_size_bytes)
```

#### 磁盘空间趋势图
```promql
# 磁盘空间使用趋势
increase(db_monitor_estimated_disk_size_bytes[1h])

# 平均行大小变化
avg_over_time(db_monitor_avg_row_size_bytes[1h])
```

#### 表格视图配置
创建表格Panel显示各表的磁盘使用情况：

| 表名 | 增量数据 | 磁盘空间 | 平均行大小 |
|------|----------|----------|------------|
| user_activity | 800 | 200.00 KB | 256 B |
| order_info | 300 | 150.00 KB | 512 B |

## 磁盘空间估算原理

### 1. 估算方法优先级

1. **数据库统计信息**（优先级最高）
   - 从 `INFORMATION_SCHEMA.TABLES` 获取 `AVG_ROW_LENGTH`
   - 从 `SHOW TABLE STATUS` 获取统计信息

2. **表结构分析**（次优先级）
   - 分析表的字段类型和长度
   - 计算理论行大小

3. **默认估算**（最后备选）
   - 使用默认值：每行100字节

### 2. 数据类型大小映射

| 数据类型 | 估算大小 | 说明 |
|----------|----------|------|
| TINYINT | 1 字节 | |
| SMALLINT | 2 字节 | |
| INT | 4 字节 | |
| BIGINT | 8 字节 | |
| FLOAT | 4 字节 | |
| DOUBLE | 8 字节 | |
| DECIMAL | 可变 | 基于精度计算 |
| VARCHAR | 可变 | 按平均长度的50%估算 |
| TEXT | 500 字节 | 平均估算 |
| DATETIME | 8 字节 | |
| DATE | 3 字节 | |

### 3. 缓存机制

- 表结构信息会被缓存30分钟
- 避免重复查询，提高性能
- 可通过重启应用清理缓存

## 性能优化建议

### 1. 监控表数量控制
- 避免监控过多表，建议控制在100个以内
- 使用通配符精确匹配需要监控的表

### 2. 时间间隔设置
- 根据业务需要设置合适的监控间隔
- 频繁监控会增加数据库负载

### 3. 缓存清理
- 定期清理过期的监控数据
- 设置合理的数据保留期限

## 故障排除

### 1. 磁盘空间估算为0
**原因**：
- 表没有合适的时间字段
- 表结构信息获取失败
- 数据库权限不足

**解决方案**：
```sql
-- 检查表是否有时间字段
SHOW COLUMNS FROM your_table_name LIKE '%time%';

-- 检查数据库权限
SHOW GRANTS FOR CURRENT_USER();
```

### 2. 估算值不准确
**原因**：
- 表统计信息过时
- 表结构发生变化
- 数据类型特殊

**解决方案**：
```sql
-- 更新表统计信息
ANALYZE TABLE your_table_name;

-- 重启应用清理缓存
# 或者调用清理缓存接口
```

### 3. 性能问题
**原因**：
- 监控表过多
- 缓存失效频繁
- 数据库负载过高

**解决方案**：
- 减少监控表数量
- 增加监控间隔
- 优化数据库性能

## 最佳实践

### 1. 监控配置
```yaml
db:
  monitor:
    table-names:
      - user_*        # 用户相关表
      - order_*       # 订单相关表
      - log_*         # 日志表
    time-interval:
      type: MINUTES
      value: 30       # 30分钟间隔，平衡准确性和性能
```

### 2. 告警设置
```promql
# 磁盘空间增长过快告警
rate(db_monitor_estimated_disk_size_bytes[1h]) > 100000000  # 100MB/hour

# 单表磁盘使用过大告警
db_monitor_estimated_disk_size_bytes > 1000000000  # 1GB
```

### 3. 定期维护
- 每周检查监控数据准确性
- 每月清理过期数据
- 每季度回顾磁盘空间使用趋势

## 更新日志

### v1.1 (2024-01-15)
- 新增磁盘空间估算功能
- 增加平均行大小计算
- 提供磁盘空间汇总API
- 支持Prometheus磁盘空间指标
- 增加缓存机制提高性能

### v1.0 (2024-01-01)
- 基础监控功能
- 增量数据统计
- Prometheus集成
- XXL-Job支持