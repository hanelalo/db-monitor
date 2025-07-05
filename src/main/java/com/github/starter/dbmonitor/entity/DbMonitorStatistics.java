package com.github.starter.dbmonitor.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * 数据库监控统计数据实体
 */
@Entity
@Table(name = "db_monitor_statistics")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DbMonitorStatistics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 数据源名称
     */
    @Column(name = "data_source_name", nullable = false, length = 100)
    private String dataSourceName;
    
    /**
     * 表名
     */
    @Column(name = "table_name", nullable = false, length = 200)
    private String tableName;
    
    /**
     * 统计时间
     */
    @Column(name = "statistic_time", nullable = false)
    private LocalDateTime statisticTime;
    
    /**
     * 统计开始时间
     */
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;
    
    /**
     * 统计结束时间
     */
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;
    
    /**
     * 数据增量
     */
    @Column(name = "increment_count", nullable = false)
    private Long incrementCount;
    
    /**
     * 增量数据预估磁盘空间大小（字节）
     */
    @Column(name = "estimated_disk_size_bytes")
    private Long estimatedDiskSizeBytes;
    
    /**
     * 平均每行数据大小（字节）
     */
    @Column(name = "avg_row_size_bytes")
    private Long avgRowSizeBytes;
    
    /**
     * 时间间隔类型
     */
    @Column(name = "interval_type", nullable = false, length = 20)
    private String intervalType;
    
    /**
     * 时间间隔值
     */
    @Column(name = "interval_value", nullable = false)
    private Integer intervalValue;
    
    /**
     * 创建时间
     */
    @Column(name = "created_time", nullable = false)
    private LocalDateTime createdTime;
    
    /**
     * 附加信息（JSON格式）
     */
    @Column(name = "additional_info", columnDefinition = "TEXT")
    private String additionalInfo;
    
    @PrePersist
    protected void onCreate() {
        createdTime = LocalDateTime.now();
    }
    
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