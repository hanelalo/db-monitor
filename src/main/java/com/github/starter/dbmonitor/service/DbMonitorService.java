package com.github.starter.dbmonitor.service;

import com.github.starter.dbmonitor.config.DbMonitorProperties;
import com.github.starter.dbmonitor.entity.DbMonitorStatistics;
import com.github.starter.dbmonitor.repository.DbMonitorStatisticsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据库监控服务
 */
@Service
@Slf4j
public class DbMonitorService {
    
    @Autowired
    private DbMonitorProperties dbMonitorProperties;
    
    @Autowired
    private DbMonitorStatisticsRepository statisticsRepository;
    
    @Autowired
    private DataSourceService dataSourceService;
    
    @Autowired
    private TablePatternService tablePatternService;
    
    @Autowired
    private DiskSpaceEstimationService diskSpaceEstimationService;
    
    private final Map<String, JdbcTemplate> jdbcTemplateCache = new ConcurrentHashMap<>();
    
    /**
     * 执行数据库监控任务
     */
    @Transactional
    public void executeMonitoring() {
        log.info("开始执行数据库监控任务");
        
        try {
            // 获取数据源
            DataSource dataSource = dataSourceService.getDataSource(dbMonitorProperties.getDataSourceName());
            JdbcTemplate jdbcTemplate = getJdbcTemplate(dataSource);
            
            // 获取需要监控的表名列表
            List<String> tableNames = getMonitoredTableNames(jdbcTemplate);
            
            // 计算时间范围
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = calculateStartTime(endTime);
            
            // 遍历每个表进行监控
            for (String tableName : tableNames) {
                try {
                    monitorTable(jdbcTemplate, tableName, startTime, endTime);
                } catch (Exception e) {
                    log.error("监控表 {} 时发生错误: {}", tableName, e.getMessage(), e);
                }
            }
            
            log.info("数据库监控任务执行完成，共监控了 {} 个表", tableNames.size());
            
        } catch (Exception e) {
            log.error("执行数据库监控任务时发生错误: {}", e.getMessage(), e);
            throw new RuntimeException("监控任务执行失败", e);
        }
    }
    
    /**
     * 监控单个表
     */
    private void monitorTable(JdbcTemplate jdbcTemplate, String tableName, 
                             LocalDateTime startTime, LocalDateTime endTime) {
        try {
            // 查询表的增量数据
            Long incrementCount = queryTableIncrement(jdbcTemplate, tableName, startTime, endTime);
            
            // 估算增量数据的磁盘空间使用量
            DiskSpaceEstimationService.DiskSpaceEstimation diskSpaceEstimation = 
                diskSpaceEstimationService.estimateIncrementalDiskSpace(jdbcTemplate, tableName, incrementCount);
            
            // 创建统计记录
            DbMonitorStatistics statistics = new DbMonitorStatistics(
                dbMonitorProperties.getDataSourceName(),
                tableName,
                startTime,
                endTime,
                incrementCount,
                diskSpaceEstimation.getTotalEstimatedSize(),
                diskSpaceEstimation.getAvgRowSize(),
                dbMonitorProperties.getTimeInterval().getType(),
                dbMonitorProperties.getTimeInterval().getValue()
            );
            
            // 设置创建时间
            statistics.setCreatedTime(LocalDateTime.now());
            
            // 保存统计记录
            statisticsRepository.insert(statistics);
            
            log.info("表 {} 在时间范围 {} 到 {} 的增量数据为: {} 行，估计磁盘空间: {} 字节 ({})", 
                    tableName, startTime, endTime, incrementCount, 
                    diskSpaceEstimation.getTotalEstimatedSize(), 
                    formatBytes(diskSpaceEstimation.getTotalEstimatedSize()));
            
        } catch (Exception e) {
            log.error("监控表 {} 时发生错误: {}", tableName, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 查询表的增量数据
     */
    private Long queryTableIncrement(JdbcTemplate jdbcTemplate, String tableName, 
                                   LocalDateTime startTime, LocalDateTime endTime) {
        try {
            // 尝试使用创建时间字段查询
            String[] timeColumns = {"created_time", "create_time", "gmt_create", "created_at"};
            
            for (String timeColumn : timeColumns) {
                try {
                    String sql = String.format(
                        "SELECT COUNT(*) FROM %s WHERE %s >= ? AND %s < ?",
                        tableName, timeColumn, timeColumn
                    );
                    
                    Long count = jdbcTemplate.queryForObject(sql, Long.class, startTime, endTime);
                    log.debug("表 {} 使用时间字段 {} 查询到增量数据: {}", tableName, timeColumn, count);
                    return count != null ? count : 0L;
                    
                } catch (Exception e) {
                    log.debug("表 {} 使用时间字段 {} 查询失败: {}", tableName, timeColumn, e.getMessage());
                }
            }
            
            // 如果没有时间字段，返回0
            log.warn("表 {} 没有找到合适的时间字段进行增量查询", tableName);
            return 0L;
            
        } catch (Exception e) {
            log.error("查询表 {} 增量数据时发生错误: {}", tableName, e.getMessage(), e);
            return 0L;
        }
    }
    
    /**
     * 获取需要监控的表名列表
     */
    private List<String> getMonitoredTableNames(JdbcTemplate jdbcTemplate) {
        return tablePatternService.getMatchedTableNames(
            jdbcTemplate, 
            dbMonitorProperties.getTableNames()
        );
    }
    
    /**
     * 计算开始时间
     */
    private LocalDateTime calculateStartTime(LocalDateTime endTime) {
        String type = dbMonitorProperties.getTimeInterval().getType();
        int value = dbMonitorProperties.getTimeInterval().getValue();
        
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
     * 获取JdbcTemplate
     */
    private JdbcTemplate getJdbcTemplate(DataSource dataSource) {
        return jdbcTemplateCache.computeIfAbsent(
            dataSource.toString(), 
            k -> new JdbcTemplate(dataSource)
        );
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
        return statisticsRepository.findByDataSourceNameAndTableNameOrderByStatisticTimeDesc(
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
}