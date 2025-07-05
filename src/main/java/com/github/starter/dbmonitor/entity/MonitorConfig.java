package com.github.starter.dbmonitor.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * 监控配置实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonitorConfig {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 配置名称
     */
    private String configName;
    
    /**
     * 数据源名称
     */
    private String dataSourceName;
    
    /**
     * 表名
     */
    private String tableName;
    
    /**
     * 时间字段名称
     */
    private String timeColumnName;
    
    /**
     * 时间字段类型（DATETIME、TIMESTAMP、BIGINT等）
     */
    private String timeColumnType;
    
    /**
     * 是否启用监控
     */
    private Boolean enabled;
    
    /**
     * 监控间隔类型（MINUTES、HOURS、DAYS）
     */
    private String intervalType;
    
    /**
     * 监控间隔值
     */
    private Integer intervalValue;
    
    /**
     * 描述信息
     */
    private String description;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
    
    /**
     * 创建人
     */
    private String createdBy;
    
    /**
     * 更新人
     */
    private String updatedBy;
    
    /**
     * 扩展配置（JSON格式）
     */
    private String extendConfig;
    
    /**
     * 构造函数
     */
    public MonitorConfig(String configName, String dataSourceName, String tableName, 
                        String timeColumnName, String timeColumnType, Boolean enabled) {
        this.configName = configName;
        this.dataSourceName = dataSourceName;
        this.tableName = tableName;
        this.timeColumnName = timeColumnName;
        this.timeColumnType = timeColumnType;
        this.enabled = enabled;
        this.intervalType = "MINUTES";
        this.intervalValue = 10;
        this.createdTime = LocalDateTime.now();
        this.updatedTime = LocalDateTime.now();
    }
}