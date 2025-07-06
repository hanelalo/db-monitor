package com.github.starter.dbmonitor.service;

import com.github.starter.dbmonitor.config.DbMonitorProperties;
import com.github.starter.dbmonitor.entity.DbMonitorStatistics;
import com.github.starter.dbmonitor.entity.MonitorConfig;
import com.github.starter.dbmonitor.repository.JdbcTableOperationRepository;
import com.github.starter.dbmonitor.repository.JdbcDbMonitorStatisticsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据库监控服务
 */
@Service
@Slf4j
public class DbMonitorService {
    
    @Autowired
    private DbMonitorProperties dbMonitorProperties;
    
    @Autowired
    private JdbcDbMonitorStatisticsRepository statisticsRepository;
    
    @Autowired
    private DataSourceService dataSourceService;
    

    
    @Autowired
    private DiskSpaceEstimationService diskSpaceEstimationService;
    
    @Autowired
    private MonitorConfigService monitorConfigService;
    
    @Autowired
    private JdbcTableOperationRepository tableOperationRepository;

    @Autowired
    private DatabaseSecurityService databaseSecurityService;
    
    /**
     * 执行数据库监控任务（非分片模式）
     */
    public void executeMonitoring() {
        executeMonitoring(null);
    }

    /**
     * 执行数据库监控任务（支持分片）
     *
     * @param shardingParam 分片参数，格式："shardIndex/shardTotal" 或 null（非分片模式）
     */
    public void executeMonitoring(String shardingParam) {
        if (shardingParam != null && !shardingParam.trim().isEmpty()) {
            log.info("开始执行数据库监控任务（分片模式），分片参数: {}", shardingParam);
            executeMonitoringWithSharding(shardingParam);
        } else {
            log.info("开始执行数据库监控任务（非分片模式）");
            executeMonitoringWithoutSharding();
        }
    }

    /**
     * 执行数据库监控任务（非分片模式）
     */
    private void executeMonitoringWithoutSharding() {
        try {
            // 获取所有启用的监控配置
            List<MonitorConfig> enabledConfigs = monitorConfigService.getEnabledConfigs();

            if (enabledConfigs.isEmpty()) {
                log.info("没有启用的监控配置，跳过监控任务");
                return;
            }

            int successCount = 0;
            int failureCount = 0;

            // 遍历每个监控配置进行监控，每个配置使用独立事务
            for (MonitorConfig config : enabledConfigs) {
                try {
                    monitorTableWithConfigInTransaction(config);
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    log.error("监控配置 {} 执行失败: {}", config.getConfigName(), e.getMessage(), e);
                }
            }

            log.info("数据库监控任务执行完成，成功: {}, 失败: {}, 总计: {}",
                    successCount, failureCount, enabledConfigs.size());

        } catch (Exception e) {
            log.error("执行数据库监控任务时发生错误: {}", e.getMessage(), e);
            throw new RuntimeException("监控任务执行失败", e);
        }
    }

    /**
     * 执行数据库监控任务（分片模式）
     */
    private void executeMonitoringWithSharding(String shardingParam) {
        try {
            // 直接使用分片参数查询当前分片需要处理的监控配置
            List<MonitorConfig> shardConfigs = monitorConfigService.getEnabledConfigs(shardingParam);

            if (shardConfigs.isEmpty()) {
                log.info("当前分片无需处理的监控配置，跳过执行");
                return;
            }

            // 解析分片参数用于日志输出
            String[] parts = shardingParam.trim().split("/");
            int shardIndex = Integer.parseInt(parts[0]);
            int shardTotal = Integer.parseInt(parts[1]);

            log.info("分片执行 - 当前分片 {}/{} 需处理配置数: {}",
                    shardIndex + 1, shardTotal, shardConfigs.size());

            int successCount = 0;
            int failureCount = 0;

            // 遍历当前分片的监控配置进行监控
            for (MonitorConfig config : shardConfigs) {
                try {
                    monitorTableWithConfigInTransaction(config);
                    successCount++;
                    log.debug("分片执行 - 监控配置 {} 执行成功", config.getConfigName());
                } catch (Exception e) {
                    failureCount++;
                    log.error("分片执行 - 监控配置 {} 执行失败: {}", config.getConfigName(), e.getMessage(), e);
                }
            }

            log.info("分片数据库监控任务执行完成 - 分片 {}/{}, 成功: {}, 失败: {}, 总计: {}",
                    shardIndex + 1, shardTotal, successCount, failureCount, shardConfigs.size());

        } catch (Exception e) {
            log.error("执行分片数据库监控任务时发生错误: {}", e.getMessage(), e);
            throw new RuntimeException("分片监控任务执行失败", e);
        }
    }

    /**
     * 在独立事务中监控单个配置
     */
    @Transactional
    public void monitorTableWithConfigInTransaction(MonitorConfig config) {
        monitorTableWithConfig(config);
    }
    
    /**
     * 使用监控配置监控单个表（支持断点续传）
     */
    private void monitorTableWithConfig(MonitorConfig config) {
        try {
            LocalDateTime currentTime = LocalDateTime.now();

            // 计算需要统计的时间段列表（支持断点续传）
            List<TimeRange> timeRanges = calculateTimeRanges(config, currentTime);

            if (timeRanges.isEmpty()) {
                log.debug("监控配置 {} - 表 {} 无需统计新数据", config.getConfigName(), config.getTableName());
                return;
            }

            log.info("监控配置 {} - 表 {} 需要统计 {} 个时间段",
                    config.getConfigName(), config.getTableName(), timeRanges.size());

            LocalDateTime lastEndTime = null;
            long totalIncrementCount = 0;
            long totalEstimatedSize = 0;

            // 逐个时间段进行统计
            for (TimeRange timeRange : timeRanges) {
                try {
                    // 使用配置的时间字段查询增量数据
                    Long incrementCount = queryTableIncrementWithConfig(config, timeRange.getStartTime(), timeRange.getEndTime());

                    // 估算增量数据的磁盘空间使用量（使用配置的数据源）
                    DiskSpaceEstimationService.DiskSpaceEstimation diskSpaceEstimation =
                        diskSpaceEstimationService.estimateIncrementalDiskSpace(config.getDataSourceName(), config.getTableName(), incrementCount);

                    // 创建统计记录
                    DbMonitorStatistics statistics = new DbMonitorStatistics(
                        config.getDataSourceName(),
                        config.getTableName(),
                        timeRange.getStartTime(),
                        timeRange.getEndTime(),
                        incrementCount,
                        diskSpaceEstimation.getTotalEstimatedSize(),
                        diskSpaceEstimation.getAvgRowSize(),
                        config.getIntervalType(),
                        config.getIntervalValue()
                    );

                    // 设置创建时间
                    statistics.setCreatedTime(LocalDateTime.now());

                    // 保存统计记录
                    statisticsRepository.insert(statistics);

                    totalIncrementCount += incrementCount;
                    totalEstimatedSize += diskSpaceEstimation.getTotalEstimatedSize();
                    lastEndTime = timeRange.getEndTime();

                    log.debug("监控配置 {} - 表 {} 时间段 {} 到 {} 的增量数据: {} 行",
                            config.getConfigName(), config.getTableName(),
                            timeRange.getStartTime(), timeRange.getEndTime(), incrementCount);

                } catch (Exception e) {
                    log.error("监控配置 {} - 表 {} 时间段 {} 到 {} 统计失败: {}",
                            config.getConfigName(), config.getTableName(),
                            timeRange.getStartTime(), timeRange.getEndTime(), e.getMessage(), e);
                    // 继续处理下一个时间段
                }
            }

            // 更新配置的最后统计时间
            if (lastEndTime != null) {
                monitorConfigService.updateLastStatisticTime(config.getId(), lastEndTime);
                log.info("监控配置 {} - 表 {} 完成统计，总计 {} 行，估计磁盘空间: {} ({})",
                        config.getConfigName(), config.getTableName(), totalIncrementCount,
                        formatBytes(totalEstimatedSize), totalEstimatedSize);
            }

        } catch (Exception e) {
            log.error("监控配置 {} 执行失败: {}", config.getConfigName(), e.getMessage(), e);
            throw e;
        }
    }
    

    
    /**
     * 使用监控配置查询表的增量数据
     */
    private Long queryTableIncrementWithConfig(MonitorConfig config,
                                              LocalDateTime startTime, LocalDateTime endTime) {
        try {
            // 使用配置中指定的数据源查询增量数据
            Long count = tableOperationRepository.queryTableIncrement(
                config.getDataSourceName(), config.getTableName(), config.getTimeColumnName(), startTime, endTime);
            log.debug("数据源 {} 中的表 {} 使用时间字段 {} 查询到增量数据: {}",
                    config.getDataSourceName(), config.getTableName(), config.getTimeColumnName(), count);
            return count != null ? count : 0L;

        } catch (Exception e) {
            log.error("查询数据源 {} 中的表 {} 增量数据时发生错误: {}",
                    config.getDataSourceName(), config.getTableName(), e.getMessage(), e);
            return 0L;
        }
    }
    

    
    /**
     * 计算开始时间（基于监控配置）
     */
    private LocalDateTime calculateStartTime(LocalDateTime endTime, MonitorConfig config) {
        String type = config.getIntervalType();
        int value = config.getIntervalValue();
        
        switch (type.toUpperCase()) {
            case "MINUTES":
                return endTime.minusMinutes(value);
            case "HOURS":
                return endTime.minusHours(value);
            case "DAYS":
                return endTime.minusDays(value);
            default:
                return endTime.minusMinutes(value);
        }
    }
    

    

    
    /**
     * 获取最新的监控统计数据
     */
    public List<DbMonitorStatistics> getLatestStatistics() {
        return statisticsRepository.findByDataSourceNameOrderByStatisticTimeDesc(
            dbMonitorProperties.getDataSourceName()
        );
    }
    
    /**
     * 获取指定表的监控统计数据
     */
    public List<DbMonitorStatistics> getTableStatistics(String tableName) {
        return statisticsRepository.findByDataSourceAndTable(
            dbMonitorProperties.getDataSourceName(), tableName
        );
    }
    
    /**
     * 清理过期的监控数据
     */
    @Transactional
    public void cleanupExpiredData() {
        int retentionDays = dbMonitorProperties.getMonitorTable().getRetentionDays();
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(retentionDays);
        
        statisticsRepository.deleteByCreatedTimeBefore(cutoffTime);
        log.info("清理了 {} 天前的监控数据", retentionDays);
    }
    
    /**
     * 获取磁盘空间汇总信息
     */
    public Map<String, Object> getDiskSpaceSummary() {
        List<DbMonitorStatistics> latestStatistics = getLatestStatistics();
        
        // 按表名分组获取最新的统计数据
        Map<String, DbMonitorStatistics> latestByTable = new java.util.HashMap<>();
        for (DbMonitorStatistics stat : latestStatistics) {
            if (!latestByTable.containsKey(stat.getTableName())) {
                latestByTable.put(stat.getTableName(), stat);
            }
        }
        
        // 计算汇总信息
        long totalIncrementCount = 0;
        long totalEstimatedDiskSize = 0;
        
        for (DbMonitorStatistics stat : latestByTable.values()) {
            totalIncrementCount += stat.getIncrementCount();
            if (stat.getEstimatedDiskSizeBytes() != null) {
                totalEstimatedDiskSize += stat.getEstimatedDiskSizeBytes();
            }
        }
        
        Map<String, Object> summary = new java.util.HashMap<>();
        summary.put("total_increment_count", totalIncrementCount);
        summary.put("total_estimated_disk_size_bytes", totalEstimatedDiskSize);
        summary.put("total_estimated_disk_size_formatted", formatBytes(totalEstimatedDiskSize));
        summary.put("monitored_tables_count", latestByTable.size());
        summary.put("timestamp", LocalDateTime.now());
        
        return summary;
    }
    
    /**
     * 格式化字节大小
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * 计算需要统计的时间段列表（支持断点续传）
     */
    private List<TimeRange> calculateTimeRanges(MonitorConfig config, LocalDateTime currentTime) {
        List<TimeRange> timeRanges = new ArrayList<>();

        // 获取上次统计的结束时间
        LocalDateTime lastStatisticTime = config.getLastStatisticTime();

        // 如果是第一次统计，从当前时间往前推一个间隔作为起始时间
        if (lastStatisticTime == null) {
            LocalDateTime startTime = calculateStartTime(currentTime, config);
            timeRanges.add(new TimeRange(startTime, currentTime));
            log.info("监控配置 {} - 表 {} 首次统计，时间范围: {} 到 {}",
                    config.getConfigName(), config.getTableName(), startTime, currentTime);
            return timeRanges;
        }

        // 计算间隔时长（分钟）
        long intervalMinutes = getIntervalMinutes(config);

        // 从上次统计结束时间开始，按间隔切分到当前时间
        LocalDateTime segmentStart = lastStatisticTime;

        while (segmentStart.isBefore(currentTime)) {
            LocalDateTime segmentEnd = segmentStart.plusMinutes(intervalMinutes);

            // 最后一个段不能超过当前时间
            if (segmentEnd.isAfter(currentTime)) {
                segmentEnd = currentTime;
            }

            // 只有当段的开始时间小于结束时间时才添加
            if (segmentStart.isBefore(segmentEnd)) {
                timeRanges.add(new TimeRange(segmentStart, segmentEnd));
            }

            segmentStart = segmentEnd;
        }

        if (!timeRanges.isEmpty()) {
            log.info("监控配置 {} - 表 {} 断点续传，从 {} 开始统计到 {}，共 {} 个时间段",
                    config.getConfigName(), config.getTableName(),
                    lastStatisticTime, currentTime, timeRanges.size());
        }

        return timeRanges;
    }

    /**
     * 获取间隔时长（分钟）
     */
    private long getIntervalMinutes(MonitorConfig config) {
        String type = config.getIntervalType();
        int value = config.getIntervalValue();

        switch (type.toUpperCase()) {
            case "MINUTES":
                return value;
            case "HOURS":
                return value * 60L;
            case "DAYS":
                return value * 24L * 60L;
            default:
                return value; // 默认按分钟
        }
    }



    /**
     * 时间范围内部类
     */
    private static class TimeRange {
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;

        public TimeRange(LocalDateTime startTime, LocalDateTime endTime) {
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }

        public LocalDateTime getEndTime() {
            return endTime;
        }
    }
}