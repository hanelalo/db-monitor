# 数据库监控端点扩展指南

## 概述

数据库监控组件采用模块化设计，将**核心监控功能**与**HTTP端点暴露功能**分离。用户可以根据需要选择性地启用或禁用不同类型的监控端点。

## 架构设计

### 核心组件
- **DbMonitorAutoConfiguration** - 核心监控功能自动配置
- **DbMonitorEndpointsAutoConfiguration** - 端点扩展功能自动配置

### 功能分层
```
┌─────────────────────────────────────┐
│           HTTP端点层                │
│  (可插拔扩展，可选择性启用/禁用)      │
├─────────────────────────────────────┤
│           核心监控层                │
│    (数据采集、存储、定时任务)        │
└─────────────────────────────────────┘
```

## 端点分类

### 1. 指标端点 (Metrics Endpoints)
- `GET /api/db-monitor/metrics` - Prometheus格式指标
- `GET /api/db-monitor/metrics/json` - JSON格式指标
- `GET /api/db-monitor/health` - 健康状态检查

**控制配置：**
```yaml
db:
  monitor:
    metrics:
      endpoints:
        metrics-enabled: true  # 控制指标端点
        health-enabled: true   # 控制健康检查端点
```

### 2. 统计数据端点 (Statistics Endpoints)
- `GET /api/db-monitor/statistics` - 获取最新统计数据
- `GET /api/db-monitor/statistics/{tableName}` - 获取指定表统计数据

**控制配置：**
```yaml
db:
  monitor:
    metrics:
      endpoints:
        statistics-enabled: true  # 控制统计数据端点
```

### 3. 管理端点 (Management Endpoints)
- `POST /api/db-monitor/trigger` - 手动触发监控任务
- `DELETE /api/db-monitor/cleanup` - 清理过期数据
- `GET /api/db-monitor/status` - 获取监控状态

**控制配置：**
```yaml
db:
  monitor:
    metrics:
      endpoints:
        management-enabled: true  # 控制管理端点
```

### 4. 配置管理端点 (Config Management Endpoints)
- `GET /api/monitor-config` - 获取所有监控配置
- `POST /api/monitor-config` - 创建监控配置
- `PUT /api/monitor-config/{id}` - 更新监控配置
- `DELETE /api/monitor-config/{id}` - 删除监控配置

**控制配置：**
```yaml
db:
  monitor:
    metrics:
      endpoints:
        config-enabled: true  # 控制配置管理端点
```

## 配置示例

### 完整启用所有端点
```yaml
db:
  monitor:
    enabled: true
    metrics:
      endpoints:
        enabled: true              # 启用端点扩展功能
        metrics-enabled: true      # 启用指标端点
        statistics-enabled: true   # 启用统计数据端点
        health-enabled: true       # 启用健康检查端点
        management-enabled: true   # 启用管理端点
        config-enabled: true       # 启用配置管理端点
```

### 仅启用核心监控功能（无HTTP端点）
```yaml
db:
  monitor:
    enabled: true
    metrics:
      endpoints:
        enabled: false  # 完全禁用所有HTTP端点
```

### 选择性启用端点
```yaml
db:
  monitor:
    enabled: true
    metrics:
      endpoints:
        enabled: true              # 启用端点扩展功能
        metrics-enabled: true      # 仅启用指标端点
        statistics-enabled: false  # 禁用统计数据端点
        health-enabled: true       # 启用健康检查端点
        management-enabled: false  # 禁用管理端点
        config-enabled: false      # 禁用配置管理端点
```

## 使用场景

### 场景1：生产环境监控
**需求：** 只需要Prometheus指标采集，不需要其他管理接口
```yaml
db:
  monitor:
    metrics:
      endpoints:
        enabled: true
        metrics-enabled: true      # 仅启用指标端点
        statistics-enabled: false
        health-enabled: false
        management-enabled: false
        config-enabled: false
```

### 场景2：开发环境调试
**需求：** 需要完整的管理和调试接口
```yaml
db:
  monitor:
    metrics:
      endpoints:
        enabled: true              # 启用所有端点
        metrics-enabled: true
        statistics-enabled: true
        health-enabled: true
        management-enabled: true
        config-enabled: true
```

### 场景3：纯数据采集
**需求：** 只需要数据采集和存储，不需要任何HTTP接口
```yaml
db:
  monitor:
    enabled: true
    metrics:
      endpoints:
        enabled: false  # 完全禁用HTTP端点
```

## 技术实现

### 条件注解
- `@ConditionalOnEndpointsEnabled` - 端点功能总开关
- `@ConditionalOnMetricsEndpointsEnabled` - 指标端点开关
- `@ConditionalOnConfigEndpointsEnabled` - 配置管理端点开关

### 自动配置类
- `DbMonitorAutoConfiguration` - 核心功能配置
- `DbMonitorEndpointsAutoConfiguration` - 端点扩展配置

### Controller分离
- `DbMonitorMetricsController` - 指标相关端点
- `DbMonitorStatisticsController` - 统计数据端点
- `DbMonitorManagementController` - 管理端点
- `MonitorConfigController` - 配置管理端点

## 优势

1. **灵活性** - 用户可根据需要选择启用的功能
2. **安全性** - 生产环境可禁用管理端点
3. **性能** - 不需要的功能不会被加载
4. **可维护性** - 功能模块化，职责清晰
5. **向后兼容** - 默认启用所有功能，不影响现有用户
