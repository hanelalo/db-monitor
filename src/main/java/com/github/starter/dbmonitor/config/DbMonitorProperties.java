package com.github.starter.dbmonitor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 数据库监控配置属性
 */
@Data
@ConfigurationProperties(prefix = "db.monitor")
public class DbMonitorProperties {
    
    /**
     * 是否启用数据库监控
     */
    private boolean enabled = true;
    
    /**
     * 数据源名称，如果不指定则使用默认数据源
     */
    private String dataSourceName = "primary";

    /**
     * 监控配置和结果数据存储的数据源名称
     * 如果不指定，则使用 dataSourceName
     */
    private String configDataSourceName;
    

    
    /**
     * 监控时间间隔配置
     */
    private TimeInterval timeInterval = new TimeInterval();
    
    /**
     * XXL-Job配置
     */
    private XxlJobConfig xxlJob = new XxlJobConfig();
    
    /**
     * 监控数据表配置
     */
    private MonitorTable monitorTable = new MonitorTable();

    /**
     * 监控配置表配置
     */
    private ConfigTable configTable = new ConfigTable();
    
    /**
     * 指标暴露配置
     */
    private Metrics metrics = new Metrics();
    
    @Data
    public static class TimeInterval {
        /**
         * 时间间隔类型：MINUTES, HOURS
         */
        private String type = "MINUTES";
        
        /**
         * 时间间隔值
         */
        private int value = 10;
    }
    
    @Data
    public static class XxlJobConfig {
        /**
         * 是否启用XXL-Job调度
         */
        private boolean enabled = false;
        
        /**
         * XXL-Job管理后台地址
         */
        private String adminAddresses;
        
        /**
         * 应用名称
         */
        private String appName = "db-monitor-executor";
        
        /**
         * 注册地址
         */
        private String address;
        
        /**
         * 执行器IP
         */
        private String ip;
        
        /**
         * 执行器端口号
         */
        private int port = 9999;
        
        /**
         * 执行器通信TOKEN
         */
        private String accessToken;
        
        /**
         * 执行器日志路径
         */
        private String logPath = "/data/applogs/xxl-job/jobhandler";
        
        /**
         * 执行器日志文件保存天数
         */
        private int logRetentionDays = 30;
        
        /**
         * 调度任务Cron表达式
         */
        private String cron = "0 */10 * * * ?";
    }
    
    @Data
    public static class MonitorTable {
        /**
         * 监控数据表名
         */
        private String tableName = "db_monitor_statistics";
        
        /**
         * 是否自动创建表
         */
        private boolean autoCreate = true;
        
        /**
         * 数据保留天数
         */
        private int retentionDays = 30;
    }
    
    @Data
    public static class Metrics {
        /**
         * 是否启用指标暴露
         */
        private boolean enabled = true;

        /**
         * 指标前缀
         */
        private String prefix = "db_monitor";

        /**
         * 暴露端点路径
         */
        private String endpoint = "/metrics";

        /**
         * 端点暴露配置
         */
        private Endpoints endpoints = new Endpoints();
    }

    @Data
    public static class Endpoints {
        /**
         * 是否启用所有监控端点
         */
        private boolean enabled = true;

        /**
         * 是否启用指标端点 (/metrics, /metrics/json)
         */
        private boolean metricsEnabled = true;

        /**
         * 是否启用统计数据端点 (/statistics)
         */
        private boolean statisticsEnabled = true;

        /**
         * 是否启用健康检查端点 (/health)
         */
        private boolean healthEnabled = true;

        /**
         * 是否启用管理端点 (/trigger, /cleanup)
         */
        private boolean managementEnabled = true;

        /**
         * 是否启用配置管理端点 (监控配置相关API)
         */
        private boolean configEnabled = true;
    }

    @Data
    public static class ConfigTable {
        /**
         * 监控配置表名
         */
        private String tableName = "db_monitor_config";

        /**
         * 是否自动创建表
         */
        private boolean autoCreate = true;

        /**
         * 配置数据存储的数据源名称
         * 如果不指定，则使用 configDataSourceName 或 dataSourceName
         */
        private String dataSourceName;
    }
}