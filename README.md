# Database Monitor Spring Boot Starter

一个强大的Spring Boot Starter，用于监控数据库表的增量数据，支持可配置的时间间隔、表名通配符匹配、XXL-Job调度以及Prometheus/Grafana集成。

## 功能特性

- ✅ 定时查询数据库中指定表在近一段时间内的数据增量
- ✅ **增量数据磁盘空间占用预估**：自动计算增量数据的磁盘空间使用量
- ✅ 支持指定需要查询统计的表，表名支持通配符匹配
- ✅ 查询频率可配置，支持使用XXL-Job来调度
- ✅ 查询的时间区间可配置（近10分钟、近半小时、近1小时等）
- ✅ 统计结果存储到监控数据表中，包含磁盘空间估算信息
- ✅ 监控数据暴露接口，用来对接Prometheus、Grafana等监控工具
- ✅ 支持Spring Boot 2.3.12
- ✅ 支持指定数据源

## 快速开始

### 1. 添加依赖

在你的Spring Boot项目中添加以下依赖：

```xml
<dependency>
    <groupId>com.github.starter</groupId>
    <artifactId>db-monitor-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 配置参数

在`application.yml`中添加配置：

```yaml
db:
  monitor:
    enabled: true
    data-source-name: primary
    table-names:
      - user_*      # 匹配所有以 user_ 开头的表
      - order_info  # 精确匹配 order_info 表
      - product_*   # 匹配所有以 product_ 开头的表
    time-interval:
      type: MINUTES
      value: 10
    xxl-job:
      enabled: false
      admin-addresses: http://localhost:8080/xxl-job-admin
      app-name: db-monitor-executor
      cron: 0 */10 * * * ?
    monitor-table:
      table-name: db_monitor_statistics
      auto-create: true
      retention-days: 30
    metrics:
      enabled: true
      prefix: db_monitor
      endpoint: /metrics
```

### 3. 启动应用

启动你的Spring Boot应用，数据库监控功能将自动开始工作。

## 磁盘空间监控功能

### 磁盘空间估算方法

系统采用多种方法来估算表的磁盘空间使用量：

1. **优先使用数据库统计信息**：
   - 从 `INFORMATION_SCHEMA.TABLES` 获取平均行长度
   - 从 `SHOW TABLE STATUS` 获取表的统计信息

2. **基于表结构估算**：
   - 分析表的字段类型和长度
   - 根据不同数据类型计算预估大小

3. **缓存机制**：
   - 表结构信息会被缓存，避免重复查询
   - 提高性能，减少数据库负载

### 磁盘空间监控指标

- **每表增量数据磁盘空间**：计算每个表在监控时间段内新增数据的磁盘空间
- **平均行大小**：每个表的平均行大小（字节）
- **总磁盘空间使用**：所有监控表的总磁盘空间使用量
- **磁盘空间排行**：按磁盘使用量排序的表信息

## 配置说明

### 基本配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `db.monitor.enabled` | Boolean | `true` | 是否启用数据库监控功能 |
| `db.monitor.data-source-name` | String | `primary` | 数据源名称 |
| `db.monitor.table-names` | List<String> | - | 需要监控的表名列表，支持通配符 |

### 时间间隔配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `db.monitor.time-interval.type` | String | `MINUTES` | 时间间隔类型：MINUTES, HOURS, DAYS |
| `db.monitor.time-interval.value` | Integer | `10` | 时间间隔值 |

### XXL-Job配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `db.monitor.xxl-job.enabled` | Boolean | `false` | 是否启用XXL-Job调度 |
| `db.monitor.xxl-job.admin-addresses` | String | - | XXL-Job管理后台地址 |
| `db.monitor.xxl-job.app-name` | String | `db-monitor-executor` | 应用名称 |
| `db.monitor.xxl-job.port` | Integer | `9999` | 执行器端口号 |
| `db.monitor.xxl-job.access-token` | String | - | 执行器通信TOKEN |
| `db.monitor.xxl-job.cron` | String | `0 */10 * * * ?` | 调度任务Cron表达式 |

### 监控数据表配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `db.monitor.monitor-table.table-name` | String | `db_monitor_statistics` | 监控数据表名 |
| `db.monitor.monitor-table.auto-create` | Boolean | `true` | 是否自动创建表 |
| `db.monitor.monitor-table.retention-days` | Integer | `30` | 数据保留天数 |

### 指标暴露配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `db.monitor.metrics.enabled` | Boolean | `true` | 是否启用指标暴露 |
| `db.monitor.metrics.prefix` | String | `db_monitor` | 指标前缀 |
| `db.monitor.metrics.endpoint` | String | `/metrics` | 暴露端点路径 |

## API接口

### 获取监控统计数据

```http
GET /api/db-monitor/statistics
```

获取最新的监控统计数据，包含磁盘空间信息。

### 获取指定表的监控统计数据

```http
GET /api/db-monitor/statistics/{tableName}
```

获取指定表的监控统计数据。

### 获取磁盘空间汇总信息

```http
GET /api/db-monitor/disk-space/summary
```

获取磁盘空间使用汇总信息，包括：

**响应示例：**
```json
{
  "total_increment_count": 12500,
  "total_estimated_disk_size_bytes": 2048000,
  "total_estimated_disk_size_formatted": "2.00 MB",
  "monitored_tables_count": 5,
  "timestamp": "2024-01-15T10:30:00"
}
```

### 获取Prometheus格式的监控指标

```http
GET /api/db-monitor/metrics
```

获取Prometheus格式的监控指标，包括磁盘空间相关指标。

**新增的磁盘空间指标：**
- `db_monitor_estimated_disk_size_bytes` - 表增量数据磁盘空间估算
- `db_monitor_avg_row_size_bytes` - 表平均行大小
- `db_monitor_total_estimated_disk_size_bytes` - 总磁盘空间估算

### 获取JSON格式的监控指标

```http
GET /api/db-monitor/metrics/json
```

获取JSON格式的监控指标，包含详细的磁盘空间信息。

**响应示例：**
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "data_source": "primary",
  "monitored_tables_count": 3,
  "datasource_health": "healthy",
  "total_increment_count": 1500,
  "total_estimated_disk_size_bytes": 384000,
  "total_estimated_disk_size_formatted": "375.00 KB",
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
      "avg_row_size_formatted": "256 B",
      "last_execution_time": "2024-01-15T10:25:00",
      "start_time": "2024-01-15T10:15:00",
      "end_time": "2024-01-15T10:25:00",
      "interval_type": "MINUTES",
      "interval_value": 10
    }
  ]
}
```

### 手动触发监控任务

```http
POST /api/db-monitor/trigger
```

手动触发监控任务。

### 获取监控健康状态

```http
GET /api/db-monitor/health
```

获取监控系统的健康状态。

### 清理过期数据

```http
DELETE /api/db-monitor/cleanup
```

清理过期的监控数据。

## 表名通配符匹配

支持以下通配符模式：

- `*`：匹配任意字符
- `?`：匹配单个字符
- 精确匹配：不包含通配符的表名

示例：

```yaml
table-names:
  - user_*        # 匹配 user_info, user_profile, user_settings 等
  - order_?       # 匹配 order_1, order_2, order_a 等
  - product_info  # 精确匹配 product_info 表
```

## 时间字段检测

系统会自动检测表中的时间字段，支持以下字段名：

- `created_time`
- `create_time`
- `gmt_create`
- `created_at`

如果表中没有这些字段，将返回增量数据为0。

## Prometheus集成

启用指标暴露后，可以通过以下端点获取Prometheus格式的指标：

```
http://localhost:8080/api/db-monitor/metrics
```

### 磁盘空间相关指标

- `db_monitor_increment_total`：表增量数据总数
- `db_monitor_estimated_disk_size_bytes`：表增量数据磁盘空间估算（字节）
- `db_monitor_avg_row_size_bytes`：表平均行大小（字节）
- `db_monitor_total_estimated_disk_size_bytes`：总磁盘空间估算（字节）
- `db_monitor_last_execution_timestamp_seconds`：最后执行时间戳
- `db_monitor_monitored_tables_total`：监控表总数
- `db_monitor_datasource_health`：数据源健康状态

### Grafana仪表板示例

可以创建以下查询来监控磁盘空间使用情况：

```promql
# 总磁盘空间使用量
db_monitor_total_estimated_disk_size_bytes

# 各表磁盘空间使用排行
topk(10, db_monitor_estimated_disk_size_bytes)

# 磁盘空间使用率趋势
rate(db_monitor_estimated_disk_size_bytes[5m])

# 平均行大小分布
histogram_quantile(0.95, db_monitor_avg_row_size_bytes)
```

## XXL-Job集成

### 1. 启用XXL-Job

```yaml
db:
  monitor:
    xxl-job:
      enabled: true
      admin-addresses: http://localhost:8080/xxl-job-admin
      app-name: db-monitor-executor
      access-token: your-token
```

### 2. 配置任务

在XXL-Job管理后台配置以下任务：

- **任务名称**：`dbMonitorJob`
- **描述**：数据库监控任务
- **负责人**：admin
- **Cron表达式**：`0 */10 * * * ?`
- **运行模式**：BEAN
- **JobHandler**：`dbMonitorJob`

### 3. 可用的Job Handler

- `dbMonitorJob`：数据库监控任务
- `dbMonitorCleanupJob`：数据清理任务
- `dbMonitorManualJob`：手动触发监控任务

## 监控数据表结构

系统会自动创建名为`db_monitor_statistics`的监控数据表，**已更新**结构如下：

```sql
CREATE TABLE `db_monitor_statistics` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `data_source_name` varchar(100) NOT NULL COMMENT '数据源名称',
  `table_name` varchar(200) NOT NULL COMMENT '表名',
  `statistic_time` datetime NOT NULL COMMENT '统计时间',
  `start_time` datetime NOT NULL COMMENT '统计开始时间',
  `end_time` datetime NOT NULL COMMENT '统计结束时间',
  `increment_count` bigint(20) NOT NULL COMMENT '数据增量',
  `estimated_disk_size_bytes` bigint(20) DEFAULT NULL COMMENT '增量数据预估磁盘空间大小（字节）',
  `avg_row_size_bytes` bigint(20) DEFAULT NULL COMMENT '平均每行数据大小（字节）',
  `interval_type` varchar(20) NOT NULL COMMENT '时间间隔类型',
  `interval_value` int(11) NOT NULL COMMENT '时间间隔值',
  `created_time` datetime NOT NULL COMMENT '创建时间',
  `additional_info` text COMMENT '附加信息',
  PRIMARY KEY (`id`),
  KEY `idx_data_source_table` (`data_source_name`, `table_name`),
  KEY `idx_statistic_time` (`statistic_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据库监控统计表';
```

## 完整配置示例

```yaml
# 数据库监控配置
db:
  monitor:
    enabled: true
    data-source-name: primary
    table-names:
      - user_*
      - order_info
      - product_*
    time-interval:
      type: MINUTES
      value: 10
    xxl-job:
      enabled: true
      admin-addresses: http://localhost:8080/xxl-job-admin
      app-name: db-monitor-executor
      port: 9999
      access-token: your-token
      cron: 0 */10 * * * ?
    monitor-table:
      table-name: db_monitor_statistics
      auto-create: true
      retention-days: 30
    metrics:
      enabled: true
      prefix: db_monitor
      endpoint: /metrics

# Spring Boot Actuator配置
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    prometheus:
      enabled: true
  metrics:
    export:
      prometheus:
        enabled: true

# 数据源配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/test_db
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
```

## 注意事项

1. 确保数据库表中有时间字段（created_time、create_time、gmt_create、created_at）
2. 监控数据表会自动创建，确保数据库用户有创建表的权限
3. **磁盘空间估算基于表结构和统计信息，可能与实际使用存在差异**
4. **表结构信息会被缓存以提高性能，如有表结构变更，建议重启应用**
5. 定期清理过期的监控数据，避免数据量过大
6. 使用XXL-Job时，确保XXL-Job管理后台可以正常访问
7. 大量表监控时，注意数据库性能影响

## 许可证

MIT License
