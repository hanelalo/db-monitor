package com.github.starter.dbmonitor.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 数据库监控统计数据实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DbMonitorStatistics {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 数据源名称
     */
    private String dataSourceName;
    
    /**
     * 表名
     */
    private String tableName;
    
    /**
     * 统计时间
     */
    private LocalDateTime statisticTime;
    
    /**
     * 统计开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 统计结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 数据增量
     */
    private Long incrementCount;
    
    /**
     * 增量数据预估磁盘空间大小（字节）
     */
    private Long estimatedDiskSizeBytes;
    
    /**
     * 平均每行数据大小（字节）
     */
    private Long avgRowSizeBytes;
    
    /**
     * 时间间隔类型
     */
    private String intervalType;
    
    /**
     * 时间间隔值
     */
    private Integer intervalValue;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
    
    /**
     * 附加信息（JSON格式）
     */
    private String additionalInfo;
    
    /**
     * 构造函数
     */
    public DbMonitorStatistics(String dataSourceName, String tableName, 
                               LocalDateTime startTime, LocalDateTime endTime, 
                               Long incrementCount, String intervalType, Integer intervalValue) {
        this.dataSourceName = dataSourceName;
        this.tableName = tableName;
        this.statisticTime = LocalDateTime.now();
        this.startTime = startTime;
        this.endTime = endTime;
        this.incrementCount = incrementCount;
        this.intervalType = intervalType;
        this.intervalValue = intervalValue;
        this.createdTime = LocalDateTime.now();
    }
    
    /**
     * 构造函数（包含磁盘空间估计）
     */
    public DbMonitorStatistics(String dataSourceName, String tableName, 
                               LocalDateTime startTime, LocalDateTime endTime, 
                               Long incrementCount, Long estimatedDiskSizeBytes, Long avgRowSizeBytes,
                               String intervalType, Integer intervalValue) {
        this.dataSourceName = dataSourceName;
        this.tableName = tableName;
        this.statisticTime = LocalDateTime.now();
        this.startTime = startTime;
        this.endTime = endTime;
        this.incrementCount = incrementCount;
        this.estimatedDiskSizeBytes = estimatedDiskSizeBytes;
        this.avgRowSizeBytes = avgRowSizeBytes;
        this.intervalType = intervalType;
        this.intervalValue = intervalValue;
        this.createdTime = LocalDateTime.now();
    }
}