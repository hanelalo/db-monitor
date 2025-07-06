package com.github.starter.dbmonitor.service;

import com.github.starter.dbmonitor.repository.JdbcTableOperationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 磁盘空间估计服务
 */
@Service
@Slf4j
public class DiskSpaceEstimationService {
    
    @Autowired
    private JdbcTableOperationRepository tableOperationRepository;
    
    private final Map<String, Long> tableRowSizeCache = new HashMap<>();
    
    /**
     * 估算增量数据的磁盘空间使用量
     * 
     * @param tableName 表名
     * @param incrementCount 增量数据行数
     * @return 磁盘空间估计结果
     */
    public DiskSpaceEstimation estimateIncrementalDiskSpace(String tableName, 
                                                           Long incrementCount) {
        try {
            if (incrementCount == null || incrementCount <= 0) {
                return new DiskSpaceEstimation(0L, 0L);
            }
            
            // 获取表的平均行大小
            Long avgRowSize = getAvgRowSize(tableName);
            
            // 计算总的磁盘空间估计
            Long totalEstimatedSize = avgRowSize * incrementCount;
            
            log.debug("表 {} 的平均行大小: {} 字节, 增量数据 {} 行, 估计磁盘空间: {} 字节", 
                     tableName, avgRowSize, incrementCount, totalEstimatedSize);
            
            return new DiskSpaceEstimation(totalEstimatedSize, avgRowSize);
            
        } catch (Exception e) {
            log.error("估算表 {} 增量数据磁盘空间时发生错误: {}", tableName, e.getMessage(), e);
            return new DiskSpaceEstimation(0L, 0L);
        }
    }
    
    /**
     * 获取表的平均行大小
     */
    private Long getAvgRowSize(String tableName) {
        // 先从缓存中获取
        String cacheKey = tableName.toLowerCase();
        if (tableRowSizeCache.containsKey(cacheKey)) {
            return tableRowSizeCache.get(cacheKey);
        }
        
        Long avgRowSize = calculateAvgRowSize(tableName);
        
        // 缓存结果（缓存30分钟）
        tableRowSizeCache.put(cacheKey, avgRowSize);
        
        return avgRowSize;
    }
    
    /**
     * 计算表的平均行大小
     */
    private Long calculateAvgRowSize(String tableName) {
        try {
            // 方法1: 尝试使用 INFORMATION_SCHEMA 获取表统计信息
            Long avgRowSize = getAvgRowSizeFromInformationSchema(tableName);
            if (avgRowSize != null && avgRowSize > 0) {
                return avgRowSize;
            }
            
            // 方法2: 尝试使用 SHOW TABLE STATUS 获取表统计信息
            avgRowSize = getAvgRowSizeFromShowTableStatus(tableName);
            if (avgRowSize != null && avgRowSize > 0) {
                return avgRowSize;
            }
            
            // 方法3: 基于表结构估算行大小
            avgRowSize = estimateRowSizeFromSchema(tableName);
            if (avgRowSize != null && avgRowSize > 0) {
                return avgRowSize;
            }
            
            // 默认值：假设每行平均100字节
            return 100L;
            
        } catch (Exception e) {
            log.error("计算表 {} 平均行大小时发生错误: {}", tableName, e.getMessage(), e);
            return 100L; // 默认值
        }
    }
    
    /**
     * 从 INFORMATION_SCHEMA 获取平均行大小
     */
    private Long getAvgRowSizeFromInformationSchema(String tableName) {
        try {
            return tableOperationRepository.getAvgRowSizeFromInformationSchema(tableName);
        } catch (Exception e) {
            log.debug("无法从 INFORMATION_SCHEMA 获取表 {} 的平均行大小: {}", tableName, e.getMessage());
            return null;
        }
    }
    
    /**
     * 从 SHOW TABLE STATUS 获取平均行大小
     */
    private Long getAvgRowSizeFromShowTableStatus(String tableName) {
        try {
            Map<String, Object> tableStatus = tableOperationRepository.getTableStatusInfo(tableName);
            if (tableStatus != null && !tableStatus.isEmpty()) {
                Object dataLengthObj = tableStatus.get("Data_length");
                Object rowsObj = tableStatus.get("Rows");
                
                if (dataLengthObj != null && rowsObj != null) {
                    Long dataLength = Long.valueOf(dataLengthObj.toString());
                    Long rows = Long.valueOf(rowsObj.toString());
                    
                    if (rows > 0 && dataLength > 0) {
                        return dataLength / rows;
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.debug("无法从 SHOW TABLE STATUS 获取表 {} 的平均行大小: {}", tableName, e.getMessage());
            return null;
        }
    }
    
    /**
     * 基于表结构估算行大小
     */
    private Long estimateRowSizeFromSchema(String tableName) {
        try {
            List<Map<String, Object>> schemaInfo = tableOperationRepository.getTableColumnDetails(tableName);
            
            long totalSize = 0;
            for (Map<String, Object> columnInfo : schemaInfo) {
                String dataType = columnInfo.get("DATA_TYPE").toString().toLowerCase();
                long charLength = Long.valueOf(columnInfo.get("CHAR_LENGTH").toString());
                int numPrecision = Integer.valueOf(columnInfo.get("NUM_PRECISION").toString());
                
                totalSize += estimateColumnSize(dataType, charLength, numPrecision);
            }
            
            return totalSize > 0 ? totalSize : null;
        } catch (Exception e) {
            log.debug("无法基于表结构估算表 {} 的行大小: {}", tableName, e.getMessage());
            return null;
        }
    }
    
    /**
     * 估算单个字段的大小
     */
    private long estimateColumnSize(String dataType, long charLength, int numPrecision) {
        switch (dataType) {
            // 整数类型
            case "tinyint":
                return 1;
            case "smallint":
                return 2;
            case "mediumint":
                return 3;
            case "int":
            case "integer":
                return 4;
            case "bigint":
                return 8;
            
            // 浮点数类型
            case "float":
                return 4;
            case "double":
                return 8;
            case "decimal":
            case "numeric":
                return Math.max(4, (numPrecision + 2) / 2);
            
            // 字符串类型
            case "char":
                return charLength > 0 ? charLength : 1;
            case "varchar":
                return charLength > 0 ? charLength / 2 : 50; // 假设平均使用一半长度
            case "text":
                return 500; // 假设平均500字节
            case "mediumtext":
                return 5000; // 假设平均5KB
            case "longtext":
                return 50000; // 假设平均50KB
            
            // 日期时间类型
            case "date":
                return 3;
            case "time":
                return 3;
            case "datetime":
            case "timestamp":
                return 8;
            case "year":
                return 1;
            
            // 二进制类型
            case "binary":
                return charLength > 0 ? charLength : 1;
            case "varbinary":
                return charLength > 0 ? charLength / 2 : 50;
            case "blob":
                return 1000; // 假设平均1KB
            case "mediumblob":
                return 10000; // 假设平均10KB
            case "longblob":
                return 100000; // 假设平均100KB
            
            // 其他类型
            default:
                return 10; // 默认10字节
        }
    }
    
    /**
     * 清理缓存
     */
    public void clearCache() {
        tableRowSizeCache.clear();
        log.info("已清理磁盘空间估算缓存");
    }
    
    /**
     * 磁盘空间估计结果
     */
    public static class DiskSpaceEstimation {
        private final Long totalEstimatedSize;
        private final Long avgRowSize;
        
        public DiskSpaceEstimation(Long totalEstimatedSize, Long avgRowSize) {
            this.totalEstimatedSize = totalEstimatedSize;
            this.avgRowSize = avgRowSize;
        }
        
        public Long getTotalEstimatedSize() {
            return totalEstimatedSize;
        }
        
        public Long getAvgRowSize() {
            return avgRowSize;
        }
    }
}