# Database Monitor Spring Boot Starter

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-8%2B-blue.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.3%2B-green.svg)](https://spring.io/projects/spring-boot)

一个轻量级的数据库表增量监控 Spring Boot Starter，支持多数据源、动态配置管理和实时监控。

## ✨ 特性

- 🚀 **开箱即用** - Spring Boot Starter，零配置启动
- 🔄 **动态配置** - 通过 REST API 实时管理监控配置
- 🗄️ **多数据源支持** - 原生支持监控多个数据源的表
- 📊 **增量监控** - 基于时间字段的高效增量数据监控
- 🎯 **灵活配置** - 可自定义表名、数据源、监控间隔
- 📈 **指标暴露** - 支持 Prometheus 指标导出
- ⚡ **轻量级架构** - 基于 JdbcTemplate，无重量级 ORM 依赖
- 🔒 **安全防护** - 内置 SQL 注入防护和参数验证

## 🚀 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.github.starter</groupId>
    <artifactId>db-monitor-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 基础配置

在 `application.yml` 中添加配置：

```yaml
db:
  monitor:
    enabled: true
    data-source-name: primary
```

### 3. 启动应用

启动 Spring Boot 应用，组件会自动创建监控相关表并启动监控服务。

### 4. 添加监控配置

通过 REST API 添加要监控的表：

```bash
curl -X POST http://localhost:8080/api/monitor-config \
  -H "Content-Type: application/json" \
  -d '{
    "configName": "user_table_monitor",
    "dataSourceName": "primary", 
    "tableName": "user_info",
    "timeColumnName": "created_time",
    "enabled": true
  }'
```

### 5. 查看监控数据

```bash
# 查看所有监控配置
curl http://localhost:8080/api/monitor-config

# 查看监控统计数据
curl http://localhost:8080/api/monitor-statistics
```

## 📖 配置指南

### 基础配置

```yaml
db:
  monitor:
    enabled: true                    # 启用监控
    data-source-name: primary        # 默认数据源名称
    config-data-source-name: monitor # 配置存储数据源（可选）
    
    # 监控配置表设置
    config-table:
      table-name: db_monitor_config  # 配置表名
      auto-create: true              # 自动创建表
    
    # 监控统计表设置  
    monitor-table:
      table-name: db_monitor_statistics # 统计表名
      auto-create: true                 # 自动创建表
      retention-days: 30                # 数据保留天数
    
    # 定时任务配置
    time-interval:
      type: MINUTES                  # 时间间隔类型
      value: 10                      # 时间间隔值
```

### 多数据源配置

```yaml
spring:
  datasource:
    # 业务数据源
    primary:
      url: jdbc:mysql://localhost:3306/business_db
      username: user
      password: pass
    
    # 监控数据源
    monitor:
      url: jdbc:mysql://localhost:3306/monitor_db
      username: monitor_user
      password: monitor_pass

db:
  monitor:
    enabled: true
    data-source-name: primary
    config-data-source-name: monitor
```

## 🔧 API 接口

### 监控配置管理

#### 创建监控配置

```bash
POST /api/monitor-config
Content-Type: application/json

{
  "configName": "user_table_monitor",
  "dataSourceName": "primary",
  "tableName": "user_info", 
  "timeColumnName": "created_time",
  "timeColumnType": "DATETIME",
  "enabled": true,
  "intervalType": "MINUTES",
  "intervalValue": 10,
  "description": "用户表监控"
}
```

#### 查询监控配置

```bash
# 查询所有配置
GET /api/monitor-config

# 查询指定配置
GET /api/monitor-config/{id}

# 查询启用的配置
GET /api/monitor-config?enabled=true
```

#### 更新监控配置

```bash
PUT /api/monitor-config/{id}
Content-Type: application/json

{
  "configName": "user_table_monitor_updated",
  "enabled": false
}
```

#### 删除监控配置

```bash
DELETE /api/monitor-config/{id}
```

### 监控数据查询

#### 查询统计数据

```bash
# 查询所有统计数据
GET /api/monitor-statistics

# 查询指定时间范围的数据
GET /api/monitor-statistics?startTime=2023-01-01T00:00:00&endTime=2023-01-02T00:00:00

# 查询指定表的数据
GET /api/monitor-statistics?dataSourceName=primary&tableName=user_info
```

## 🏗️ 架构设计

### 核心组件

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   REST API      │    │  Monitor        │    │  Data Access    │
│   Controller    │───▶│  Service        │───▶│  Repository     │
└─────────────────┘    └─────────────────┘    └─────────────────┘
                                │
                                ▼
                       ┌─────────────────┐
                       │  Scheduler      │
                       │  (XXL-Job)      │
                       └─────────────────┘
```

### 数据流

1. **配置管理** - 通过 REST API 管理监控配置
2. **定时执行** - 定时任务读取配置并执行监控
3. **数据采集** - 查询目标表的增量数据
4. **结果存储** - 将监控结果存储到统计表
5. **指标暴露** - 通过 API 和 Prometheus 暴露监控指标

## 🔌 集成

### XXL-Job 集成

```yaml
db:
  monitor:
    xxl-job:
      enabled: true
      admin-addresses: http://localhost:8080/xxl-job-admin
      app-name: db-monitor-executor
      access-token: your-token
```

### 端点扩展功能（可插拔）

组件采用模块化设计，HTTP端点暴露功能是**可插拔的扩展能力**。用户可以选择性地启用或禁用不同类型的监控端点。

#### 完整配置示例
```yaml
db:
  monitor:
    metrics:
      enabled: true
      prefix: db_monitor
      endpoint: /metrics

      # 端点扩展配置（可插拔功能）
      endpoints:
        enabled: true              # 是否启用所有监控端点
        metrics-enabled: true      # 指标端点 (/metrics, /metrics/json)
        statistics-enabled: true   # 统计数据端点 (/statistics)
        health-enabled: true       # 健康检查端点 (/health)
        management-enabled: true   # 管理端点 (/trigger, /cleanup)
        config-enabled: true       # 配置管理端点
```

#### 使用场景

**仅使用核心监控功能（无HTTP端点）：**
```yaml
db:
  monitor:
    enabled: true
    metrics:
      endpoints:
        enabled: false  # 禁用所有HTTP端点
```

**仅启用Prometheus指标：**
```yaml
db:
  monitor:
    metrics:
      endpoints:
        enabled: true
        metrics-enabled: true      # 仅启用指标端点
        statistics-enabled: false
        management-enabled: false
        config-enabled: false
```

详细说明请参考：[端点扩展指南](ENDPOINTS_EXTENSION_GUIDE.md)

访问 `http://localhost:8080/metrics` 获取 Prometheus 格式的监控指标。

## 🛡️ 安全特性

- **SQL 注入防护** - 严格的参数验证和 SQL 转义
- **输入验证** - 完整的输入参数校验
- **权限控制** - 支持 Spring Security 集成
- **数据隔离** - 多数据源间的数据完全隔离

## 📊 监控指标

组件提供以下监控指标：

- `db_monitor_table_increment_total` - 表增量数据总数
- `db_monitor_execution_duration_seconds` - 监控执行耗时
- `db_monitor_execution_total` - 监控执行次数
- `db_monitor_error_total` - 监控执行错误次数

## 💡 使用场景

### 数据增长监控
监控业务表的数据增长趋势，及时发现异常增长或停滞：

```bash
# 监控用户注册表
curl -X POST http://localhost:8080/api/monitor-config \
  -d '{"configName": "user_registration", "tableName": "users", "timeColumnName": "created_at"}'

# 监控订单表
curl -X POST http://localhost:8080/api/monitor-config \
  -d '{"configName": "order_growth", "tableName": "orders", "timeColumnName": "order_time"}'
```

### 多环境监控
在不同环境中监控相同的业务指标：

```yaml
# 生产环境
db:
  monitor:
    data-source-name: prod-db
    config-data-source-name: monitor-db

# 测试环境
db:
  monitor:
    data-source-name: test-db
    config-data-source-name: monitor-db
```

### 跨数据库监控
同时监控多个数据库的表：

```bash
# MySQL 数据库
curl -X POST http://localhost:8080/api/monitor-config \
  -d '{"configName": "mysql_users", "dataSourceName": "mysql-db", "tableName": "users"}'

# PostgreSQL 数据库
curl -X POST http://localhost:8080/api/monitor-config \
  -d '{"configName": "pg_orders", "dataSourceName": "postgres-db", "tableName": "orders"}'
```

## 🔧 高级配置

### 自定义表名

```yaml
db:
  monitor:
    config-table:
      table-name: my_monitor_config     # 自定义配置表名
      data-source-name: config-db       # 配置表专用数据源

    monitor-table:
      table-name: my_monitor_statistics # 自定义统计表名
      retention-days: 90                # 数据保留90天
```

### 性能优化

```yaml
db:
  monitor:
    time-interval:
      type: HOURS        # 降低监控频率
      value: 1           # 每小时执行一次

    monitor-table:
      retention-days: 7  # 减少数据保留时间
```

### 错误处理

```yaml
logging:
  level:
    com.github.starter.dbmonitor: DEBUG  # 开启调试日志

db:
  monitor:
    # 监控失败时的重试机制由 Spring 的 @Retryable 处理
    enabled: true
```

## 🚨 注意事项

### 性能影响
- 监控查询会对数据库产生额外负载，建议在业务低峰期执行
- 对于大表，建议在时间字段上创建索引以提高查询性能
- 合理设置监控间隔，避免过于频繁的查询

### 数据库兼容性
- 支持 MySQL 5.7+、PostgreSQL 9.6+、Oracle 11g+
- 不同数据库的时间函数可能有差异，组件会自动适配
- 建议在生产环境使用前进行充分测试

### 安全建议
- 监控数据源建议使用只读权限的数据库用户
- 在生产环境中启用 HTTPS 保护 API 接口
- 定期清理过期的监控数据

## 🐛 故障排除

### 常见问题

**Q: 监控配置表创建失败**
```
A: 检查数据库连接和权限，确保应用有创建表的权限
```

**Q: 监控数据不准确**
```
A: 确认时间字段类型正确，检查时区设置
```

**Q: 内存使用过高**
```
A: 减少监控频率，清理历史数据，检查是否有内存泄漏
```

### 调试模式

启用调试日志：

```yaml
logging:
  level:
    com.github.starter.dbmonitor: DEBUG
    org.springframework.jdbc: DEBUG
```

查看详细的 SQL 执行日志和监控过程。

## 🤝 贡献

我们欢迎所有形式的贡献！

### 如何贡献

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启 Pull Request

### 开发环境

```bash
# 克隆项目
git clone https://github.com/your-username/db-monitor-spring-boot-starter.git

# 进入项目目录
cd db-monitor-spring-boot-starter

# 运行测试
mvn test

# 构建项目
mvn clean package
```

## 📄 许可证

本项目采用 [MIT 许可证](LICENSE)。
