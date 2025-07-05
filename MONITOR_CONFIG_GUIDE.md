# 监控配置管理系统使用指南

## 概述

监控配置管理系统提供了一个完整的解决方案，用于管理数据库表的监控配置。通过这个系统，您可以：

- 动态配置需要监控的表
- 指定每个表的时间字段
- 设置监控间隔
- 启用/禁用监控
- 提供完整的REST API来管理配置

## 系统架构

```
┌─────────────────────────────────────────────────────────────────┐
│                    监控配置管理系统                                │
├─────────────────────────────────────────────────────────────────┤
│  ┌─────────────────┐   ┌─────────────────┐   ┌─────────────────┐  │
│  │   Controller    │   │    Service      │   │   Repository    │  │
│  │                 │   │                 │   │                 │  │
│  │ REST API 接口   │ → │   业务逻辑处理   │ → │   数据访问层    │  │
│  └─────────────────┘   └─────────────────┘   └─────────────────┘  │
├─────────────────────────────────────────────────────────────────┤
│                    monitor_config 表                            │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │ 配置名称 | 数据源 | 表名 | 时间字段 | 间隔 | 启用状态 | ...   │  │
│  └─────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

## 数据库表结构

### monitor_config 表

```sql
CREATE TABLE monitor_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_name VARCHAR(255) NOT NULL UNIQUE,              -- 配置名称
    data_source_name VARCHAR(255) NOT NULL,                -- 数据源名称
    table_name VARCHAR(255) NOT NULL,                      -- 表名
    time_column_name VARCHAR(255) NOT NULL,                -- 时间字段名称
    time_column_type VARCHAR(50) NOT NULL DEFAULT 'DATETIME', -- 时间字段类型
    enabled BOOLEAN NOT NULL DEFAULT TRUE,                 -- 是否启用
    interval_type VARCHAR(50) NOT NULL DEFAULT 'MINUTES',  -- 间隔类型
    interval_value INT NOT NULL DEFAULT 10,                -- 间隔值
    description TEXT,                                       -- 描述
    created_time DATETIME NOT NULL,                        -- 创建时间
    updated_time DATETIME NOT NULL,                        -- 更新时间
    created_by VARCHAR(255),                               -- 创建人
    updated_by VARCHAR(255),                               -- 更新人
    extend_config TEXT,                                     -- 扩展配置（JSON）
    INDEX idx_data_source_table (data_source_name, table_name),
    INDEX idx_enabled (enabled),
    INDEX idx_created_time (created_time)
);
```

## API 接口文档

### 1. 创建监控配置

**POST** `/api/monitor-config`

**请求体：**
```json
{
    "configName": "user_table_monitor",
    "dataSourceName": "primary",
    "tableName": "user",
    "timeColumnName": "created_time",
    "timeColumnType": "DATETIME",
    "enabled": true,
    "intervalType": "MINUTES",
    "intervalValue": 10,
    "description": "用户表增量监控",
    "createdBy": "admin"
}
```

**响应：**
```json
{
    "success": true,
    "message": "监控配置创建成功",
    "data": {
        "id": 1,
        "configName": "user_table_monitor",
        "dataSourceName": "primary",
        "tableName": "user",
        "timeColumnName": "created_time",
        "timeColumnType": "DATETIME",
        "enabled": true,
        "intervalType": "MINUTES",
        "intervalValue": 10,
        "description": "用户表增量监控",
        "createdTime": "2024-01-01T10:00:00",
        "updatedTime": "2024-01-01T10:00:00",
        "createdBy": "admin"
    }
}
```

### 2. 获取所有监控配置

**GET** `/api/monitor-config`

**查询参数：**
- `dataSourceName`（可选）：按数据源筛选
- `enabled`（可选）：按启用状态筛选

**响应：**
```json
{
    "success": true,
    "data": [
        {
            "id": 1,
            "configName": "user_table_monitor",
            "dataSourceName": "primary",
            "tableName": "user",
            "timeColumnName": "created_time",
            "enabled": true,
            "intervalType": "MINUTES",
            "intervalValue": 10
        }
    ],
    "total": 1
}
```

### 3. 根据ID获取监控配置

**GET** `/api/monitor-config/{id}`

**响应：**
```json
{
    "success": true,
    "data": {
        "id": 1,
        "configName": "user_table_monitor",
        "dataSourceName": "primary",
        "tableName": "user",
        "timeColumnName": "created_time",
        "enabled": true
    }
}
```

### 4. 更新监控配置

**PUT** `/api/monitor-config/{id}`

**请求体：**
```json
{
    "configName": "user_table_monitor_updated",
    "dataSourceName": "primary",
    "tableName": "user",
    "timeColumnName": "updated_time",
    "timeColumnType": "DATETIME",
    "enabled": true,
    "intervalType": "HOURS",
    "intervalValue": 1,
    "description": "更新的用户表监控",
    "updatedBy": "admin"
}
```

### 5. 删除监控配置

**DELETE** `/api/monitor-config/{id}`

**响应：**
```json
{
    "success": true,
    "message": "监控配置删除成功"
}
```

### 6. 启用/禁用监控配置

**PUT** `/api/monitor-config/{id}/enabled?enabled=true&updatedBy=admin`

**响应：**
```json
{
    "success": true,
    "message": "监控配置已启用"
}
```

### 7. 批量启用/禁用监控配置

**PUT** `/api/monitor-config/batch/enabled`

**请求体：**
```json
{
    "ids": [1, 2, 3],
    "enabled": true,
    "updatedBy": "admin"
}
```

**响应：**
```json
{
    "success": true,
    "message": "成功更新了 3 个监控配置的状态",
    "updatedCount": 3
}
```

### 8. 测试监控配置

**POST** `/api/monitor-config/test`

**请求体：**
```json
{
    "dataSourceName": "primary",
    "tableName": "user",
    "timeColumnName": "created_time",
    "timeColumnType": "DATETIME"
}
```

**响应：**
```json
{
    "success": true,
    "valid": true,
    "message": "监控配置有效"
}
```

### 9. 获取表的所有列

**GET** `/api/monitor-config/table-columns?dataSourceName=primary&tableName=user`

**响应：**
```json
{
    "success": true,
    "data": [
        "id",
        "username",
        "email",
        "created_time",
        "updated_time"
    ],
    "total": 5
}
```

### 10. 自动检测时间字段

**GET** `/api/monitor-config/detect-time-columns?dataSourceName=primary&tableName=user`

**响应：**
```json
{
    "success": true,
    "data": [
        "created_time",
        "updated_time"
    ],
    "total": 2,
    "message": "检测到 2 个时间字段"
}
```

## 使用示例

### 1. 创建监控配置

```bash
curl -X POST http://localhost:8080/api/monitor-config \
  -H "Content-Type: application/json" \
  -d '{
    "configName": "order_table_monitor",
    "dataSourceName": "primary",
    "tableName": "orders",
    "timeColumnName": "order_time",
    "timeColumnType": "DATETIME",
    "enabled": true,
    "intervalType": "MINUTES",
    "intervalValue": 15,
    "description": "订单表监控",
    "createdBy": "system"
  }'
```

### 2. 获取启用的监控配置

```bash
curl -X GET "http://localhost:8080/api/monitor-config?enabled=true"
```

### 3. 自动检测时间字段

```bash
curl -X GET "http://localhost:8080/api/monitor-config/detect-time-columns?dataSourceName=primary&tableName=orders"
```

### 4. 更新监控配置

```bash
curl -X PUT http://localhost:8080/api/monitor-config/1 \
  -H "Content-Type: application/json" \
  -d '{
    "configName": "order_table_monitor",
    "dataSourceName": "primary",
    "tableName": "orders",
    "timeColumnName": "created_at",
    "timeColumnType": "TIMESTAMP",
    "enabled": true,
    "intervalType": "HOURS",
    "intervalValue": 1,
    "description": "订单表监控 - 每小时",
    "updatedBy": "admin"
  }'
```

### 5. 禁用监控配置

```bash
curl -X PUT "http://localhost:8080/api/monitor-config/1/enabled?enabled=false&updatedBy=admin"
```

## 集成到现有监控系统

监控配置管理系统已经集成到现有的监控服务中。现在监控任务会：

1. 从 `monitor_config` 表读取所有启用的监控配置
2. 对每个配置执行监控任务
3. 使用配置中指定的时间字段进行增量查询
4. 按照配置的间隔设置计算时间范围

### 代码示例

原有的监控服务现在支持两种模式：

1. **配置表模式**（推荐）：从 `monitor_config` 表读取配置
2. **配置文件模式**（兼容）：从 `application.yml` 读取配置

## 最佳实践

### 1. 配置命名规范

- 使用描述性名称：`{业务模块}_{表名}_monitor`
- 例如：`user_service_user_table_monitor`

### 2. 时间字段选择

- 优先使用创建时间字段
- 确保时间字段有索引
- 常见时间字段：`created_time`, `create_time`, `gmt_create`, `created_at`

### 3. 监控间隔设置

- **高频表**：1-5分钟
- **中频表**：10-30分钟
- **低频表**：1-4小时

### 4. 配置管理

- 定期检查配置的有效性
- 为重要表设置备份监控配置
- 使用描述字段记录配置用途

### 5. 监控最佳实践

```bash
# 创建监控配置前先检测时间字段
curl -X GET "http://localhost:8080/api/monitor-config/detect-time-columns?dataSourceName=primary&tableName=user"

# 创建配置前先测试
curl -X POST http://localhost:8080/api/monitor-config/test \
  -H "Content-Type: application/json" \
  -d '{
    "dataSourceName": "primary",
    "tableName": "user",
    "timeColumnName": "created_time",
    "timeColumnType": "DATETIME"
  }'

# 创建配置
curl -X POST http://localhost:8080/api/monitor-config \
  -H "Content-Type: application/json" \
  -d '{
    "configName": "user_table_monitor",
    "dataSourceName": "primary",
    "tableName": "user",
    "timeColumnName": "created_time",
    "timeColumnType": "DATETIME",
    "enabled": true,
    "intervalType": "MINUTES",
    "intervalValue": 10,
    "description": "用户表增量监控"
  }'
```

## 错误处理

常见错误及解决方案：

### 1. 配置名称已存在
```json
{
    "success": false,
    "message": "创建监控配置失败: 配置名称已存在: user_table_monitor"
}
```
**解决方案**：使用唯一的配置名称

### 2. 表不存在
```json
{
    "success": false,
    "message": "创建监控配置失败: 表不存在: user_table"
}
```
**解决方案**：检查表名是否正确

### 3. 时间字段不存在
```json
{
    "success": false,
    "message": "创建监控配置失败: 时间字段不存在: created_time"
}
```
**解决方案**：使用正确的时间字段名称

## 扩展功能

### 1. 扩展配置字段

可以使用 `extend_config` 字段存储JSON格式的扩展配置：

```json
{
    "extendConfig": "{\"alertThreshold\": 1000, \"emailNotification\": true}"
}
```

### 2. 自定义验证

可以在服务层添加自定义验证逻辑：

```java
// 在MonitorConfigService中添加自定义验证
private void customValidation(MonitorConfig config) {
    // 自定义验证逻辑
}
```

### 3. 监控告警

可以基于监控配置实现告警功能：

```java
// 在监控逻辑中添加告警
if (incrementCount > threshold) {
    sendAlert(config, incrementCount);
}
```

## 总结

监控配置管理系统提供了完整的解决方案来管理数据库表的监控配置。通过REST API，您可以：

- 动态添加、修改、删除监控配置
- 灵活设置监控参数
- 自动检测时间字段
- 批量管理配置
- 测试配置有效性

这个系统使监控配置更加灵活和易于管理，避免了硬编码配置的问题。