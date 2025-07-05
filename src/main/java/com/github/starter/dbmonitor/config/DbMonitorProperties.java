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
     * 需要监控的表名列表，支持通配符
     */
    private List<String> tableNames;
    
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
     * 指标暴露配置
     */
    private Metrics metrics = new Metrics();
    
    /**
     * 磁盘大小估算配置
     */
    private DiskSize diskSize = new DiskSize();
    
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
    }
    
    @Data
    public static class DiskSize {
        /**
         * 是否启用磁盘大小估算
         */
        private boolean enabled = true;
        
        /**
         * 默认行大小（字节）
         */
        private Long defaultRowSizeBytes = 100L;
        
        /**
         * 索引开销比例（相对于数据大小）
         */
        private double indexOverheadRatio = 0.25;
        
        /**
         * 存储开销比例（相对于理论大小）
         */
        private double storageOverheadRatio = 0.3;
        
        /**
         * 是否缓存平均行大小
         */
        private boolean cacheEnabled = true;
        
        /**
         * 缓存过期时间（分钟）
         */
        private int cacheExpirationMinutes = 60;
    }
}