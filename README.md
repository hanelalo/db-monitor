# Database Monitor Spring Boot Starter

一个强大的Spring Boot Starter，用于监控数据库表的增量数据，支持可配置的时间间隔、表名通配符匹配、XXL-Job调度以及Prometheus/Grafana集成。

## 功能特性

- ✅ 定时查询数据库中指定表在近一段时间内的数据增量
- ✅ 支持指定需要查询统计的表，表名支持通配符匹配
- ✅ 查询频率可配置，支持使用XXL-Job来调度
- ✅ 查询的时间区间可配置（近10分钟、近半小时、近1小时等）
- ✅ 统计结果存储到监控数据表中
- ✅ 监控数据暴露接口，用来对接Prometheus、Grafana等监控工具
- ✅ 支持Spring Boot 2.3.12
- ✅ 支持指定数据源
- ✅ **磁盘大小估算**：自动计算增量数据所占磁盘大小的预估值

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

### 磁盘大小估算配置

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `db.monitor.disk-size.enabled` | Boolean | `true` | 是否启用磁盘大小估算 |
| `db.monitor.disk-size.default-row-size-bytes` | Long | `100` | 默认行大小（字节） |
| `db.monitor.disk-size.index-overhead-ratio` | Double | `0.25` | 索引开销比例（相对于数据大小） |
| `db.monitor.disk-size.storage-overhead-ratio` | Double | `0.3` | 存储开销比例（相对于理论大小） |
| `db.monitor.disk-size.cache-enabled` | Boolean | `true` | 是否缓存平均行大小 |
| `db.monitor.disk-size.cache-expiration-minutes` | Integer | `60` | 缓存过期时间（分钟） |

## API接口

### 获取监控统计数据

```http
GET /api/db-monitor/statistics
```

获取最新的监控统计数据。

### 获取指定表的监控统计数据

```http
GET /api/db-monitor/statistics/{tableName}
```

获取指定表的监控统计数据。

### 获取Prometheus格式的监控指标

```http
GET /api/db-monitor/metrics
```

获取Prometheus格式的监控指标，用于对接Prometheus。

### 获取JSON格式的监控指标

```http
GET /api/db-monitor/metrics/json
```

获取JSON格式的监控指标。

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

### 清理磁盘大小估算缓存

```http
DELETE /api/db-monitor/cache/disk-size
```

清理所有表的磁盘大小估算缓存。

### 清理指定表的磁盘大小估算缓存

```http
DELETE /api/db-monitor/cache/disk-size/{tableName}
```

清理指定表的磁盘大小估算缓存。

### 格式化字节大小

```http
GET /api/db-monitor/utils/format-bytes/{bytes}
```

将字节数格式化为可读的大小格式（B、KB、MB、GB、TB）。

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

## 磁盘大小估算

系统会自动估算增量数据所占用的磁盘大小，包括：

### 估算方法

1. **平均行大小计算**：
   - 首先尝试从 `INFORMATION_SCHEMA.TABLES` 获取精确的平均行大小
   - 如果失败，则通过分析表结构计算理论行大小
   - 最后回退到使用配置的默认行大小

2. **磁盘大小计算**：
   - 基础数据大小 = 平均行大小 × 增量行数
   - 索引开销 = 基础数据大小 × 索引开销比例（默认25%）
   - 总磁盘大小 = 基础数据大小 + 索引开销

3. **缓存机制**：
   - 平均行大小会被缓存以提高性能
   - 缓存时间可配置（默认60分钟）
   - 支持手动清理缓存

### 配置参数说明

- **默认行大小**：当无法计算实际行大小时使用的默认值
- **索引开销比例**：索引相对于数据的空间开销比例
- **存储开销比例**：实际存储相对于理论大小的开销比例
- **缓存设置**：控制平均行大小的缓存行为

### 支持的数据库

目前主要支持MySQL数据库的磁盘大小估算，其他数据库会使用默认的估算方法。

## Prometheus集成

启用指标暴露后，可以通过以下端点获取Prometheus格式的指标：

```
http://localhost:8080/api/db-monitor/metrics
```

主要指标包括：

- `db_monitor_increment_total`：表增量数据总数
- `db_monitor_increment_size_bytes`：表增量数据大小（字节）
- `db_monitor_avg_row_size_bytes`：表平均行大小（字节）
- `db_monitor_last_execution_timestamp_seconds`：最后执行时间戳
- `db_monitor_monitored_tables_total`：监控表总数
- `db_monitor_datasource_health`：数据源健康状态

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

系统会自动创建名为`db_monitor_statistics`的监控数据表，结构如下：

```sql
CREATE TABLE `db_monitor_statistics` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `data_source_name` varchar(100) NOT NULL COMMENT '数据源名称',
  `table_name` varchar(200) NOT NULL COMMENT '表名',
  `statistic_time` datetime NOT NULL COMMENT '统计时间',
  `start_time` datetime NOT NULL COMMENT '统计开始时间',
  `end_time` datetime NOT NULL COMMENT '统计结束时间',
  `increment_count` bigint(20) NOT NULL COMMENT '数据增量',
  `increment_size_bytes` bigint(20) COMMENT '增量数据磁盘大小（字节）',
  `avg_row_size_bytes` bigint(20) COMMENT '平均行大小（字节）',
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
    disk-size:
      enabled: true
      default-row-size-bytes: 100
      index-overhead-ratio: 0.25
      storage-overhead-ratio: 0.3
      cache-enabled: true
      cache-expiration-minutes: 60

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
3. 定期清理过期的监控数据，避免数据量过大
4. 使用XXL-Job时，确保XXL-Job管理后台可以正常访问
5. 大量表监控时，注意数据库性能影响

## 许可证

MIT License
