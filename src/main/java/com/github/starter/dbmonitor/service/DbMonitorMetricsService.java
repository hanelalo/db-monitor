package com.github.starter.dbmonitor.service;

import com.github.starter.dbmonitor.config.DbMonitorProperties;
import com.github.starter.dbmonitor.entity.DbMonitorStatistics;
import com.github.starter.dbmonitor.repository.JdbcDbMonitorStatisticsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 数据库监控指标服务
 */
@Service
@Slf4j
public class DbMonitorMetricsService {
    
    @Autowired
    private DbMonitorProperties dbMonitorProperties;
    
    @Autowired
    private JdbcDbMonitorStatisticsRepository statisticsRepository;
    
    @Autowired
    private DataSourceService dataSourceService;
    
    /**
     * 生成 Prometheus 格式的监控指标
     */
    public String generatePrometheusMetrics() {
        StringBuilder metrics = new StringBuilder();
        
        try {
            // 获取最新的监控统计数据
            List<DbMonitorStatistics> statistics = statisticsRepository.findByDataSourceNameOrderByStatisticTimeDesc(
                    dbMonitorProperties.getDataSourceName());
            
            // 按表名分组获取最新的统计数据
            Map<String, DbMonitorStatistics> latestStatistics = new HashMap<>();
            for (DbMonitorStatistics stat : statistics) {
                if (!latestStatistics.containsKey(stat.getTableName())) {
                    latestStatistics.put(stat.getTableName(), stat);
                }
            }
            
            // 生成增量数据指标
            metrics.append("# HELP db_monitor_increment_total Total number of records added to table in the specified time interval\n");
            metrics.append("# TYPE db_monitor_increment_total counter\n");
            
            for (DbMonitorStatistics stat : latestStatistics.values()) {
                metrics.append(String.format(
                        "db_monitor_increment_total{data_source=\"%s\",table=\"%s\",interval_type=\"%s\",interval_value=\"%d\"} %d\n",
                        stat.getDataSourceName(),
                        stat.getTableName(),
                        stat.getIntervalType(),
                        stat.getIntervalValue(),
                        stat.getIncrementCount()
                ));
            }
            
            // 生成磁盘空间指标
            metrics.append("\n# HELP db_monitor_estimated_disk_size_bytes Estimated disk space usage for incremental data in bytes\n");
            metrics.append("# TYPE db_monitor_estimated_disk_size_bytes gauge\n");
            
            for (DbMonitorStatistics stat : latestStatistics.values()) {
                long diskSize = stat.getEstimatedDiskSizeBytes() != null ? stat.getEstimatedDiskSizeBytes() : 0L;
                metrics.append(String.format(
                        "db_monitor_estimated_disk_size_bytes{data_source=\"%s\",table=\"%s\",interval_type=\"%s\",interval_value=\"%d\"} %d\n",
                        stat.getDataSourceName(),
                        stat.getTableName(),
                        stat.getIntervalType(),
                        stat.getIntervalValue(),
                        diskSize
                ));
            }
            
            // 生成平均行大小指标
            metrics.append("\n# HELP db_monitor_avg_row_size_bytes Average row size in bytes\n");
            metrics.append("# TYPE db_monitor_avg_row_size_bytes gauge\n");
            
            for (DbMonitorStatistics stat : latestStatistics.values()) {
                long avgRowSize = stat.getAvgRowSizeBytes() != null ? stat.getAvgRowSizeBytes() : 0L;
                metrics.append(String.format(
                        "db_monitor_avg_row_size_bytes{data_source=\"%s\",table=\"%s\"} %d\n",
                        stat.getDataSourceName(),
                        stat.getTableName(),
                        avgRowSize
                ));
            }
            
            // 生成总磁盘空间指标
            long totalDiskSize = latestStatistics.values().stream()
                    .mapToLong(stat -> stat.getEstimatedDiskSizeBytes() != null ? stat.getEstimatedDiskSizeBytes() : 0L)
                    .sum();
            
            metrics.append("\n# HELP db_monitor_total_estimated_disk_size_bytes Total estimated disk space usage for all monitored tables in bytes\n");
            metrics.append("# TYPE db_monitor_total_estimated_disk_size_bytes gauge\n");
            metrics.append(String.format("db_monitor_total_estimated_disk_size_bytes{data_source=\"%s\"} %d\n",
                    dbMonitorProperties.getDataSourceName(), totalDiskSize));
            
            // 生成监控任务状态指标
            metrics.append("\n# HELP db_monitor_last_execution_timestamp_seconds Last execution timestamp\n");
            metrics.append("# TYPE db_monitor_last_execution_timestamp_seconds gauge\n");
            
            for (DbMonitorStatistics stat : latestStatistics.values()) {
                metrics.append(String.format(
                        "db_monitor_last_execution_timestamp_seconds{data_source=\"%s\",table=\"%s\"} %d\n",
                        stat.getDataSourceName(),
                        stat.getTableName(),
                        stat.getStatisticTime().atZone(java.time.ZoneOffset.systemDefault()).toEpochSecond()
                ));
            }
            
            // 生成监控表数量指标
            metrics.append("\n# HELP db_monitor_monitored_tables_total Total number of monitored tables\n");
            metrics.append("# TYPE db_monitor_monitored_tables_total gauge\n");
            metrics.append(String.format("db_monitor_monitored_tables_total{data_source=\"%s\"} %d\n",
                    dbMonitorProperties.getDataSourceName(), latestStatistics.size()));
            
            // 生成数据源健康状态指标
            metrics.append("\n# HELP db_monitor_datasource_health Data source health status (1=healthy, 0=unhealthy)\n");
            metrics.append("# TYPE db_monitor_datasource_health gauge\n");
            
            boolean isHealthy = dataSourceService.isDataSourceAvailable(dbMonitorProperties.getDataSourceName());
            metrics.append(String.format("db_monitor_datasource_health{data_source=\"%s\"} %d\n",
                    dbMonitorProperties.getDataSourceName(), isHealthy ? 1 : 0));
            
        } catch (Exception e) {
            log.error("生成 Prometheus 指标失败: {}", e.getMessage(), e);
            metrics.append("# Error generating metrics: ").append(e.getMessage()).append("\n");
        }
        
        return metrics.toString();
    }
    
    /**
     * 生成 JSON 格式的监控指标
     */
    public Map<String, Object> generateJsonMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            // 获取最新的监控统计数据
            List<DbMonitorStatistics> statistics = statisticsRepository.findByDataSourceNameOrderByStatisticTimeDesc(
                    dbMonitorProperties.getDataSourceName());
            
            // 按表名分组获取最新的统计数据
            Map<String, DbMonitorStatistics> latestStatistics = new HashMap<>();
            for (DbMonitorStatistics stat : statistics) {
                if (!latestStatistics.containsKey(stat.getTableName())) {
                    latestStatistics.put(stat.getTableName(), stat);
                }
            }
            
            // 基本信息
            metrics.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            metrics.put("data_source", dbMonitorProperties.getDataSourceName());
            metrics.put("monitored_tables_count", latestStatistics.size());
            
            // 数据源健康状态
            boolean isHealthy = dataSourceService.isDataSourceAvailable(dbMonitorProperties.getDataSourceName());
            metrics.put("datasource_health", isHealthy ? "healthy" : "unhealthy");
            
            // 表级别的统计数据
            List<Map<String, Object>> tableMetrics = new ArrayList<>();
            for (DbMonitorStatistics stat : latestStatistics.values()) {
                Map<String, Object> tableMetric = new HashMap<>();
                tableMetric.put("table_name", stat.getTableName());
                tableMetric.put("increment_count", stat.getIncrementCount());
                tableMetric.put("estimated_disk_size_bytes", stat.getEstimatedDiskSizeBytes());
                tableMetric.put("estimated_disk_size_formatted", formatBytes(stat.getEstimatedDiskSizeBytes()));
                tableMetric.put("avg_row_size_bytes", stat.getAvgRowSizeBytes());
                tableMetric.put("avg_row_size_formatted", formatBytes(stat.getAvgRowSizeBytes()));
                tableMetric.put("last_execution_time", stat.getStatisticTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                tableMetric.put("start_time", stat.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                tableMetric.put("end_time", stat.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                tableMetric.put("interval_type", stat.getIntervalType());
                tableMetric.put("interval_value", stat.getIntervalValue());
                tableMetrics.add(tableMetric);
            }
            metrics.put("table_metrics", tableMetrics);
            
            // 汇总统计
            long totalIncrementCount = latestStatistics.values().stream()
                    .mapToLong(DbMonitorStatistics::getIncrementCount)
                    .sum();
            metrics.put("total_increment_count", totalIncrementCount);
            
            long totalEstimatedDiskSize = latestStatistics.values().stream()
                    .mapToLong(stat -> stat.getEstimatedDiskSizeBytes() != null ? stat.getEstimatedDiskSizeBytes() : 0L)
                    .sum();
            metrics.put("total_estimated_disk_size_bytes", totalEstimatedDiskSize);
            metrics.put("total_estimated_disk_size_formatted", formatBytes(totalEstimatedDiskSize));
            
            // 磁盘空间详细信息
            Map<String, Object> diskSpaceDetails = new HashMap<>();
            diskSpaceDetails.put("total_estimated_size_bytes", totalEstimatedDiskSize);
            diskSpaceDetails.put("total_estimated_size_formatted", formatBytes(totalEstimatedDiskSize));
            diskSpaceDetails.put("average_row_size_bytes", calculateAverageRowSize(latestStatistics.values()));
            diskSpaceDetails.put("largest_table_increment", findLargestTableIncrement(latestStatistics.values()));
            diskSpaceDetails.put("largest_disk_usage", findLargestDiskUsage(latestStatistics.values()));
            metrics.put("disk_space_summary", diskSpaceDetails);
            
        } catch (Exception e) {
            log.error("生成 JSON 指标失败: {}", e.getMessage(), e);
            metrics.put("error", e.getMessage());
        }
        
        return metrics;
    }
    
    /**
     * 获取健康状态
     */
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> health = new HashMap<>();
        
        try {
            // 数据源健康状态
            boolean isDataSourceHealthy = dataSourceService.isDataSourceAvailable(dbMonitorProperties.getDataSourceName());
            health.put("datasource_health", isDataSourceHealthy ? "UP" : "DOWN");
            
            // 最近的监控任务执行状态
            List<DbMonitorStatistics> recentStatistics = statisticsRepository.findByDataSourceNameOrderByStatisticTimeDesc(
                    dbMonitorProperties.getDataSourceName());
            
            if (!recentStatistics.isEmpty()) {
                DbMonitorStatistics latest = recentStatistics.get(0);
                health.put("last_execution_time", latest.getStatisticTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                health.put("last_execution_status", "SUCCESS");
                
                // 检查最近的执行时间是否超过了预期的间隔
                LocalDateTime expectedNextExecution = latest.getStatisticTime().plusMinutes(
                        dbMonitorProperties.getTimeInterval().getValue());
                boolean isOverdue = LocalDateTime.now().isAfter(expectedNextExecution.plusMinutes(5)); // 允许5分钟的延迟
                health.put("is_overdue", isOverdue);
            } else {
                health.put("last_execution_time", "N/A");
                health.put("last_execution_status", "UNKNOWN");
                health.put("is_overdue", true);
            }
            
            // 监控的表数量
            health.put("monitored_tables_count", recentStatistics.size());
            
            // 整体健康状态
            boolean isHealthy = isDataSourceHealthy && 
                    (recentStatistics.isEmpty() || !(Boolean) health.get("is_overdue"));
            health.put("overall_status", isHealthy ? "HEALTHY" : "UNHEALTHY");
            
        } catch (Exception e) {
            log.error("获取健康状态失败: {}", e.getMessage(), e);
            health.put("overall_status", "ERROR");
            health.put("error_message", e.getMessage());
        }
        
        return health;
    }
    
    /**
     * 计算平均行大小
     */
    private long calculateAverageRowSize(Collection<DbMonitorStatistics> statistics) {
        return statistics.stream()
                .filter(stat -> stat.getAvgRowSizeBytes() != null && stat.getAvgRowSizeBytes() > 0)
                .mapToLong(DbMonitorStatistics::getAvgRowSizeBytes)
                .sum() / Math.max(1, statistics.size());
    }
    
    /**
     * 查找增量数据最多的表
     */
    private Map<String, Object> findLargestTableIncrement(Collection<DbMonitorStatistics> statistics) {
        DbMonitorStatistics largest = statistics.stream()
                .max(Comparator.comparing(DbMonitorStatistics::getIncrementCount))
                .orElse(null);
        
        Map<String, Object> result = new HashMap<>();
        if (largest != null) {
            result.put("table_name", largest.getTableName());
            result.put("increment_count", largest.getIncrementCount());
        }
        return result;
    }
    
    /**
     * 查找磁盘使用量最大的表
     */
    private Map<String, Object> findLargestDiskUsage(Collection<DbMonitorStatistics> statistics) {
        DbMonitorStatistics largest = statistics.stream()
                .filter(stat -> stat.getEstimatedDiskSizeBytes() != null)
                .max(Comparator.comparing(DbMonitorStatistics::getEstimatedDiskSizeBytes))
                .orElse(null);
        
        Map<String, Object> result = new HashMap<>();
        if (largest != null) {
            result.put("table_name", largest.getTableName());
            result.put("estimated_disk_size_bytes", largest.getEstimatedDiskSizeBytes());
            result.put("estimated_disk_size_formatted", formatBytes(largest.getEstimatedDiskSizeBytes()));
        }
        return result;
    }
    
    /**
     * 格式化字节大小
     */
    private String formatBytes(Long bytes) {
        if (bytes == null || bytes < 1024) {
            return (bytes != null ? bytes : 0) + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}