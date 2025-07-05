package com.github.starter.dbmonitor.service;

import com.github.starter.dbmonitor.config.DbMonitorProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 磁盘大小估算服务
 */
@Service
@Slf4j
public class DiskSizeEstimationService {
    
    @Autowired
    private DbMonitorProperties dbMonitorProperties;
    
    // 缓存表的平均行大小，避免重复计算
    private final ConcurrentMap<String, Long> avgRowSizeCache = new ConcurrentHashMap<>();
    
    /**
     * 估算增量数据的磁盘大小
     */
    public TableSizeInfo estimateIncrementSize(JdbcTemplate jdbcTemplate, String tableName, Long incrementCount) {
        if (incrementCount == null || incrementCount <= 0) {
            return new TableSizeInfo(0L, 0L);
        }
        
        try {
            // 获取平均行大小
            Long avgRowSize = getAverageRowSize(jdbcTemplate, tableName);
            
            // 计算增量数据大小
            Long incrementSizeBytes = avgRowSize * incrementCount;
            
            // 考虑索引开销（通常为数据大小的20-30%）
            double indexOverheadRatio = dbMonitorProperties.getDiskSize().getIndexOverheadRatio();
            Long totalSizeBytes = Math.round(incrementSizeBytes * (1 + indexOverheadRatio));
            
            log.debug("表 {} 增量数据大小估算: 行数={}, 平均行大小={}B, 数据大小={}B, 总大小={}B", 
                     tableName, incrementCount, avgRowSize, incrementSizeBytes, totalSizeBytes);
            
            return new TableSizeInfo(totalSizeBytes, avgRowSize);
            
        } catch (Exception e) {
            log.error("估算表 {} 增量数据大小时发生错误: {}", tableName, e.getMessage(), e);
            return new TableSizeInfo(0L, 0L);
        }
    }
    
    /**
     * 获取表的平均行大小
     */
    private Long getAverageRowSize(JdbcTemplate jdbcTemplate, String tableName) {
        // 先从缓存中获取
        String cacheKey = tableName;
        Long cachedSize = avgRowSizeCache.get(cacheKey);
        if (cachedSize != null) {
            return cachedSize;
        }
        
        try {
            Long avgRowSize = calculateAverageRowSize(jdbcTemplate, tableName);
            
            // 如果计算失败，使用默认值
            if (avgRowSize == null || avgRowSize <= 0) {
                avgRowSize = dbMonitorProperties.getDiskSize().getDefaultRowSizeBytes();
                log.warn("无法计算表 {} 的平均行大小，使用默认值: {}B", tableName, avgRowSize);
            }
            
            // 缓存结果
            avgRowSizeCache.put(cacheKey, avgRowSize);
            
            return avgRowSize;
            
        } catch (Exception e) {
            log.error("获取表 {} 平均行大小时发生错误: {}", tableName, e.getMessage(), e);
            Long defaultSize = dbMonitorProperties.getDiskSize().getDefaultRowSizeBytes();
            avgRowSizeCache.put(cacheKey, defaultSize);
            return defaultSize;
        }
    }
    
    /**
     * 计算表的平均行大小
     */
    private Long calculateAverageRowSize(JdbcTemplate jdbcTemplate, String tableName) {
        try {
            // 尝试使用 INFORMATION_SCHEMA 查询（适用于MySQL）
            String sql = "SELECT " +
                        "ROUND(((data_length + index_length) / table_rows), 0) AS avg_row_size " +
                        "FROM information_schema.tables " +
                        "WHERE table_schema = DATABASE() AND table_name = ? AND table_rows > 0";
            
            try {
                Long avgRowSize = jdbcTemplate.queryForObject(sql, Long.class, tableName);
                if (avgRowSize != null && avgRowSize > 0) {
                    return avgRowSize;
                }
            } catch (Exception e) {
                log.debug("使用 INFORMATION_SCHEMA 查询平均行大小失败: {}", e.getMessage());
            }
            
            // 如果上面的方法失败，尝试抽样计算
            return calculateAverageRowSizeBySampling(jdbcTemplate, tableName);
            
        } catch (Exception e) {
            log.error("计算表 {} 平均行大小时发生错误: {}", tableName, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 通过抽样计算平均行大小
     */
    private Long calculateAverageRowSizeBySampling(JdbcTemplate jdbcTemplate, String tableName) {
        try {
            // 获取表的列信息
            String columnSql = "SELECT column_name, data_type, " +
                              "CASE " +
                              "  WHEN data_type IN ('varchar', 'char', 'text') THEN IFNULL(character_maximum_length, 100) " +
                              "  WHEN data_type IN ('int', 'integer') THEN 4 " +
                              "  WHEN data_type = 'bigint' THEN 8 " +
                              "  WHEN data_type IN ('decimal', 'numeric') THEN 8 " +
                              "  WHEN data_type IN ('datetime', 'timestamp') THEN 8 " +
                              "  WHEN data_type = 'date' THEN 3 " +
                              "  WHEN data_type IN ('float', 'double') THEN 8 " +
                              "  ELSE 10 " +
                              "END AS estimated_size " +
                              "FROM information_schema.columns " +
                              "WHERE table_schema = DATABASE() AND table_name = ?";
            
            try {
                Long totalEstimatedSize = jdbcTemplate.query(columnSql, rs -> {
                    long total = 0;
                    while (rs.next()) {
                        total += rs.getLong("estimated_size");
                    }
                    return total;
                }, tableName);
                
                if (totalEstimatedSize != null && totalEstimatedSize > 0) {
                    // 考虑实际存储的开销（通常比理论值大20-40%）
                    double storageOverhead = dbMonitorProperties.getDiskSize().getStorageOverheadRatio();
                    return Math.round(totalEstimatedSize * (1 + storageOverhead));
                }
            } catch (Exception e) {
                log.debug("通过列信息计算平均行大小失败: {}", e.getMessage());
            }
            
            // 如果都失败了，返回null，让上层使用默认值
            return null;
            
        } catch (Exception e) {
            log.error("抽样计算表 {} 平均行大小时发生错误: {}", tableName, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 清理缓存
     */
    public void clearCache() {
        avgRowSizeCache.clear();
        log.info("已清理平均行大小缓存");
    }
    
    /**
     * 清理指定表的缓存
     */
    public void clearCache(String tableName) {
        avgRowSizeCache.remove(tableName);
        log.debug("已清理表 {} 的平均行大小缓存", tableName);
    }
    
    /**
     * 格式化字节大小为可读格式
     */
    public String formatBytes(Long bytes) {
        if (bytes == null || bytes <= 0) {
            return "0 B";
        }
        
        final String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        BigDecimal size = new BigDecimal(bytes);
        
        while (size.compareTo(new BigDecimal(1024)) >= 0 && unitIndex < units.length - 1) {
            size = size.divide(new BigDecimal(1024), 2, RoundingMode.HALF_UP);
            unitIndex++;
        }
        
        return size.toString() + " " + units[unitIndex];
    }
    
    /**
     * 表大小信息
     */
    public static class TableSizeInfo {
        private final Long totalSizeBytes;
        private final Long avgRowSizeBytes;
        
        public TableSizeInfo(Long totalSizeBytes, Long avgRowSizeBytes) {
            this.totalSizeBytes = totalSizeBytes;
            this.avgRowSizeBytes = avgRowSizeBytes;
        }
        
        public Long getTotalSizeBytes() {
            return totalSizeBytes;
        }
        
        public Long getAvgRowSizeBytes() {
            return avgRowSizeBytes;
        }
    }
}