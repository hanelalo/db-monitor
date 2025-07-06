# 数据库监控断点续传功能指南

## 🎯 功能概述

数据库监控组件现在支持**断点续传**功能，确保即使应用重启或中断，也不会丢失任何监控数据。

## ❌ 原有问题

### 问题描述
- 每次统计都基于当前时间计算时间范围
- 应用重启后会丢失中间时段的监控数据
- 无法处理长时间停机后的数据补齐

### 问题示例
```
应用运行: 10:00 - 10:10 (统计了这10分钟的数据)
应用停机: 10:10 - 12:00 (110分钟)
应用重启: 12:00 (只统计12:00前10分钟，丢失了10:10-11:50的数据)
```

## ✅ 解决方案

### 核心机制
1. **记录最后统计时间** - 每个监控配置记录最后统计的结束时间
2. **断点续传** - 下次统计从上次结束时间开始
3. **时间段切分** - 将长时间间隔切分为多个标准时间段
4. **数据补齐** - 自动补齐停机期间的所有数据

### 数据库变更
新增字段：`last_statistic_time TIMESTAMP NULL`

## 🔧 实现细节

### 1. 数据模型扩展

**MonitorConfig实体新增字段：**
```java
/**
 * 最后统计时间（用于断点续传）
 */
private LocalDateTime lastStatisticTime;
```

### 2. 时间段计算逻辑

**首次统计：**
```
startTime = currentTime - interval
endTime = currentTime
```

**断点续传：**
```
从 lastStatisticTime 开始，按 interval 切分到 currentTime
例如：lastStatisticTime=10:10, currentTime=12:00, interval=10分钟
生成时间段：
- 10:10 - 10:20
- 10:20 - 10:30
- 10:30 - 10:40
- ...
- 11:50 - 12:00
```

### 3. 核心算法

```java
private List<TimeRange> calculateTimeRanges(MonitorConfig config, LocalDateTime currentTime) {
    List<TimeRange> timeRanges = new ArrayList<>();
    LocalDateTime lastStatisticTime = config.getLastStatisticTime();
    
    if (lastStatisticTime == null) {
        // 首次统计
        LocalDateTime startTime = calculateStartTime(currentTime, config);
        timeRanges.add(new TimeRange(startTime, currentTime));
    } else {
        // 断点续传：按间隔切分时间段
        long intervalMinutes = getIntervalMinutes(config);
        LocalDateTime segmentStart = lastStatisticTime;
        
        while (segmentStart.isBefore(currentTime)) {
            LocalDateTime segmentEnd = segmentStart.plusMinutes(intervalMinutes);
            if (segmentEnd.isAfter(currentTime)) {
                segmentEnd = currentTime;
            }
            if (segmentStart.isBefore(segmentEnd)) {
                timeRanges.add(new TimeRange(segmentStart, segmentEnd));
            }
            segmentStart = segmentEnd;
        }
    }
    
    return timeRanges;
}
```

## 📊 使用场景

### 场景1：正常运行
```
10:00 首次统计 (09:50-10:00)
10:10 正常统计 (10:00-10:10)
10:20 正常统计 (10:10-10:20)
```

### 场景2：短暂重启
```
10:00 统计完成 (lastStatisticTime = 10:00)
10:05 应用重启
10:10 恢复统计 (10:00-10:10) - 自动补齐5分钟数据
```

### 场景3：长时间停机
```
10:00 统计完成 (lastStatisticTime = 10:00)
10:00-12:00 应用停机 (120分钟)
12:00 应用重启，自动生成12个时间段：
- 10:00-10:10
- 10:10-10:20
- ...
- 11:50-12:00
```

### 场景4：配置间隔变更
```
原配置：10分钟间隔
新配置：5分钟间隔
系统会按新的5分钟间隔进行后续统计
```

## 🔍 监控日志

### 首次统计日志
```
监控配置 user_table_config - 表 user_info 首次统计，时间范围: 2025-01-01 09:50:00 到 2025-01-01 10:00:00
```

### 断点续传日志
```
监控配置 user_table_config - 表 user_info 断点续传，从 2025-01-01 10:00:00 开始统计到 2025-01-01 12:00:00，共 12 个时间段
监控配置 user_table_config - 表 user_info 需要统计 12 个时间段
监控配置 user_table_config - 表 user_info 完成统计，总计 1500 行，估计磁盘空间: 2.5 MB
```

### 详细统计日志
```
监控配置 user_table_config - 表 user_info 时间段 2025-01-01 10:00:00 到 2025-01-01 10:10:00 的增量数据: 125 行
监控配置 user_table_config - 表 user_info 时间段 2025-01-01 10:10:00 到 2025-01-01 10:20:00 的增量数据: 130 行
```

## 🛡️ 容错机制

### 1. 单个时间段失败处理
- 某个时间段统计失败不影响其他时间段
- 记录错误日志并继续处理下一个时间段
- 最后统计时间只在所有成功的时间段中更新

### 2. 数据一致性保证
- 每个时间段的统计在独立事务中执行
- 最后统计时间的更新在单独事务中执行
- 确保部分成功的情况下数据一致性

### 3. 性能优化
- 避免单次处理过多时间段（建议单次不超过100个时间段）
- 大时间跨度会被自动切分为多个标准间隔
- 支持并发执行多个监控配置

## 📈 优势

1. **数据完整性** - 确保不丢失任何监控数据
2. **自动恢复** - 应用重启后自动补齐缺失数据
3. **灵活配置** - 支持不同的监控间隔配置
4. **容错能力** - 单个时间段失败不影响整体统计
5. **性能友好** - 按时间段切分，避免单次查询过大数据量

## 🔧 配置建议

### 监控间隔设置
```yaml
db:
  monitor:
    config-table:
      # 建议监控间隔
      interval-type: MINUTES
      interval-value: 10  # 10分钟间隔，平衡实时性和性能
```

### 数据保留策略
```yaml
db:
  monitor:
    monitor-table:
      retention-days: 30  # 保留30天数据
```

这个断点续传功能确保了监控数据的完整性和可靠性，即使在应用重启或长时间停机的情况下也能保证数据不丢失。
