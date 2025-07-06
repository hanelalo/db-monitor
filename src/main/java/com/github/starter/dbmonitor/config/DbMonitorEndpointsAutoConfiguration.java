package com.github.starter.dbmonitor.config;

import com.github.starter.dbmonitor.config.condition.ConditionalOnConfigEndpointsEnabled;
import com.github.starter.dbmonitor.config.condition.ConditionalOnEndpointsEnabled;
import com.github.starter.dbmonitor.config.condition.ConditionalOnMetricsEndpointsEnabled;
import com.github.starter.dbmonitor.controller.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;

/**
 * 数据库监控端点扩展自动配置
 * 
 * 这个配置类专门负责管理监控端点的暴露功能，
 * 用户可以通过配置选择性地启用或禁用不同类型的端点。
 */
@Configuration
@ConditionalOnEndpointsEnabled
@AutoConfigureAfter(DbMonitorAutoConfiguration.class)
@Slf4j
public class DbMonitorEndpointsAutoConfiguration {
    
    /**
     * 指标端点控制器
     * 提供 Prometheus 和 JSON 格式的监控指标
     */
    @Bean
    @ConditionalOnMissingBean(DbMonitorMetricsController.class)
    @ConditionalOnMetricsEndpointsEnabled
    public DbMonitorMetricsController dbMonitorMetricsController() {
        log.info("启用数据库监控指标端点");
        return new DbMonitorMetricsController();
    }
    
    /**
     * 统计数据端点控制器
     * 提供监控统计数据查询接口
     */
    @Bean
    @ConditionalOnMissingBean(DbMonitorStatisticsController.class)
    @ConditionalOnProperty(prefix = "db.monitor.metrics.endpoints", name = "statistics-enabled", havingValue = "true", matchIfMissing = true)
    public DbMonitorStatisticsController dbMonitorStatisticsController() {
        log.info("启用数据库监控统计数据端点");
        return new DbMonitorStatisticsController();
    }
    
    /**
     * 管理端点控制器
     * 提供监控任务管理接口
     */
    @Bean
    @ConditionalOnMissingBean(DbMonitorManagementController.class)
    @ConditionalOnProperty(prefix = "db.monitor.metrics.endpoints", name = "management-enabled", havingValue = "true", matchIfMissing = true)
    public DbMonitorManagementController dbMonitorManagementController() {
        log.info("启用数据库监控管理端点");
        return new DbMonitorManagementController();
    }
    
    /**
     * 配置管理端点控制器
     * 提供监控配置管理接口
     */
    @Bean
    @ConditionalOnMissingBean(MonitorConfigController.class)
    @ConditionalOnConfigEndpointsEnabled
    public MonitorConfigController monitorConfigController() {
        log.info("启用数据库监控配置管理端点");
        return new MonitorConfigController();
    }
    
    @PostConstruct
    public void logEndpointsConfiguration() {
        log.info("数据库监控端点扩展功能已启用");
        log.info("可通过 db.monitor.metrics.endpoints.* 配置控制各类端点的启用状态");
    }
}
