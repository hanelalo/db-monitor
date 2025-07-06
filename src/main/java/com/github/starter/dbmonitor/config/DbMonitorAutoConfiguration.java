package com.github.starter.dbmonitor.config;

import com.github.starter.dbmonitor.job.DbMonitorJobHandler;
import com.github.starter.dbmonitor.service.DbMonitorService;
import com.github.starter.dbmonitor.service.DbMonitorMetricsService;
import com.github.starter.dbmonitor.service.DataSourceService;
import com.github.starter.dbmonitor.service.TablePatternService;
import com.github.starter.dbmonitor.controller.DbMonitorController;
import com.xxl.job.core.executor.XxlJobExecutor;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * 数据库监控自动配置
 */
@Configuration
@EnableConfigurationProperties(DbMonitorProperties.class)
@ComponentScan(basePackages = "com.github.starter.dbmonitor")
@MapperScan("com.github.starter.dbmonitor.mapper")
@EnableScheduling
@ConditionalOnProperty(prefix = "db.monitor", name = "enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class DbMonitorAutoConfiguration {
    
    @Autowired
    private DbMonitorProperties dbMonitorProperties;
    
    @Autowired
    private DbMonitorService dbMonitorService;
    
    private XxlJobExecutor xxlJobExecutor;
    
    /**
     * 配置 XXL-Job 执行器
     */
    @Bean
    @ConditionalOnProperty(prefix = "db.monitor.xxl-job", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean(XxlJobExecutor.class)
    public XxlJobExecutor xxlJobExecutor() {
        log.info("配置 XXL-Job 执行器");
        
        XxlJobExecutor xxlJobExecutor = new XxlJobExecutor();
        DbMonitorProperties.XxlJobConfig config = dbMonitorProperties.getXxlJob();
        
        xxlJobExecutor.setAdminAddresses(config.getAdminAddresses());
        xxlJobExecutor.setAppname(config.getAppName());
        xxlJobExecutor.setAddress(config.getAddress());
        xxlJobExecutor.setIp(config.getIp());
        xxlJobExecutor.setPort(config.getPort());
        xxlJobExecutor.setAccessToken(config.getAccessToken());
        xxlJobExecutor.setLogPath(config.getLogPath());
        xxlJobExecutor.setLogRetentionDays(config.getLogRetentionDays());
        
        return xxlJobExecutor;
    }
    
    /**
     * 启动 XXL-Job 执行器
     */
    @PostConstruct
    public void initXxlJobExecutor() {
        if (dbMonitorProperties.getXxlJob().isEnabled()) {
            try {
                xxlJobExecutor = xxlJobExecutor();
                xxlJobExecutor.start();
                log.info("XXL-Job 执行器启动成功");
            } catch (Exception e) {
                log.error("XXL-Job 执行器启动失败: {}", e.getMessage(), e);
            }
        }
    }
    
    /**
     * 销毁 XXL-Job 执行器
     */
    @PreDestroy
    public void destroyXxlJobExecutor() {
        if (xxlJobExecutor != null) {
            try {
                xxlJobExecutor.destroy();
                log.info("XXL-Job 执行器已销毁");
            } catch (Exception e) {
                log.error("销毁 XXL-Job 执行器时发生错误: {}", e.getMessage(), e);
            }
        }
    }
    
    /**
     * 定时任务 - 数据库监控（当未启用 XXL-Job 时使用）
     */
    @Scheduled(cron = "${db.monitor.xxl-job.cron:0 */10 * * * ?}")
    @ConditionalOnProperty(prefix = "db.monitor.xxl-job", name = "enabled", havingValue = "false", matchIfMissing = true)
    public void scheduledMonitorTask() {
        log.info("执行定时数据库监控任务");
        try {
            dbMonitorService.executeMonitoring();
        } catch (Exception e) {
            log.error("定时数据库监控任务执行失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 定时任务 - 数据清理
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void scheduledCleanupTask() {
        log.info("执行定时数据清理任务");
        try {
            dbMonitorService.cleanupExpiredData();
        } catch (Exception e) {
            log.error("定时数据清理任务执行失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 数据源服务
     */
    @Bean
    @ConditionalOnMissingBean(DataSourceService.class)
    public DataSourceService dataSourceService() {
        return new DataSourceService();
    }
    
    /**
     * 表名模式服务
     */
    @Bean
    @ConditionalOnMissingBean(TablePatternService.class)
    public TablePatternService tablePatternService() {
        return new TablePatternService();
    }
    
    /**
     * 数据库监控服务
     */
    @Bean
    @ConditionalOnMissingBean(DbMonitorService.class)
    public DbMonitorService dbMonitorService() {
        return new DbMonitorService();
    }
    
    /**
     * 数据库监控指标服务
     */
    @Bean
    @ConditionalOnMissingBean(DbMonitorMetricsService.class)
    public DbMonitorMetricsService dbMonitorMetricsService() {
        return new DbMonitorMetricsService();
    }
    
    /**
     * 数据库监控控制器
     */
    @Bean
    @ConditionalOnMissingBean(DbMonitorController.class)
    @ConditionalOnProperty(prefix = "db.monitor.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
    public DbMonitorController dbMonitorController() {
        return new DbMonitorController();
    }
    
    /**
     * XXL-Job 任务处理器
     */
    @Bean
    @ConditionalOnMissingBean(DbMonitorJobHandler.class)
    @ConditionalOnProperty(prefix = "db.monitor.xxl-job", name = "enabled", havingValue = "true")
    public DbMonitorJobHandler dbMonitorJobHandler() {
        return new DbMonitorJobHandler();
    }
    
    @PostConstruct
    public void logConfiguration() {
        log.info("数据库监控功能已启用");
        log.info("数据源: {}", dbMonitorProperties.getDataSourceName());
        log.info("监控表: {}", dbMonitorProperties.getTableNames());
        log.info("时间间隔: {} {}", dbMonitorProperties.getTimeInterval().getValue(), 
                dbMonitorProperties.getTimeInterval().getType());
        log.info("XXL-Job 启用状态: {}", dbMonitorProperties.getXxlJob().isEnabled());
        log.info("指标暴露启用状态: {}", dbMonitorProperties.getMetrics().isEnabled());
    }
}