package com.github.starter.dbmonitor.service;

import com.github.starter.dbmonitor.config.DbMonitorProperties;
import com.github.starter.dbmonitor.repository.JdbcDbMonitorStatisticsRepository;
import com.github.starter.dbmonitor.repository.JdbcMonitorConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * 多数据源配置服务
 * 负责管理监控配置和统计数据的存储数据源
 */
@Service
@Slf4j
public class MultiDataSourceConfigService {
    
    @Autowired
    private DbMonitorProperties dbMonitorProperties;
    
    @Autowired
    private JdbcMonitorConfigRepository monitorConfigRepository;
    
    @Autowired
    private JdbcDbMonitorStatisticsRepository statisticsRepository;
    
    @Autowired
    private DataSourceService dataSourceService;
    
    /**
     * 初始化多数据源配置
     */
    @PostConstruct
    public void initializeMultiDataSourceConfig() {
        try {
            // 确定配置存储数据源
            String configDataSourceName = getConfigDataSourceName();
            
            // 设置Repository的配置数据源
            setConfigDataSource(configDataSourceName);
            
            // 验证配置数据源可用性
            validateConfigDataSource(configDataSourceName);
            
            log.info("多数据源配置初始化完成，配置存储数据源: {}", configDataSourceName);
            
        } catch (Exception e) {
            log.error("多数据源配置初始化失败: {}", e.getMessage(), e);
            throw new RuntimeException("多数据源配置初始化失败", e);
        }
    }
    
    /**
     * 获取配置存储数据源名称
     */
    public String getConfigDataSourceName() {
        // 优先使用专门的配置数据源
        if (dbMonitorProperties.getConfigDataSourceName() != null && 
            !dbMonitorProperties.getConfigDataSourceName().trim().isEmpty()) {
            return dbMonitorProperties.getConfigDataSourceName();
        }
        
        // 否则使用默认数据源
        return dbMonitorProperties.getDataSourceName();
    }
    
    /**
     * 设置Repository的配置数据源
     */
    private void setConfigDataSource(String configDataSourceName) {
        try {
            // 使用反射设置配置数据源名称
            java.lang.reflect.Field configField1 = monitorConfigRepository.getClass().getDeclaredField("configDataSourceName");
            configField1.setAccessible(true);
            configField1.set(monitorConfigRepository, configDataSourceName);

            java.lang.reflect.Field configField2 = statisticsRepository.getClass().getDeclaredField("configDataSourceName");
            configField2.setAccessible(true);
            configField2.set(statisticsRepository, configDataSourceName);

            log.info("已设置Repository配置数据源为: {}", configDataSourceName);

        } catch (Exception e) {
            log.error("设置Repository配置数据源失败: {}", e.getMessage(), e);
            throw new RuntimeException("设置Repository配置数据源失败", e);
        }
    }
    
    /**
     * 验证配置数据源可用性
     */
    private void validateConfigDataSource(String configDataSourceName) {
        if (!dataSourceService.isDataSourceAvailable(configDataSourceName)) {
            throw new RuntimeException("配置数据源 " + configDataSourceName + " 不可用");
        }
        log.info("配置数据源 {} 验证通过", configDataSourceName);
    }
    
    /**
     * 获取所有可用的数据源名称
     */
    public String[] getAvailableDataSourceNames() {
        return dataSourceService.getAvailableDataSourceNames();
    }
    
    /**
     * 检查数据源是否可用
     */
    public boolean isDataSourceAvailable(String dataSourceName) {
        return dataSourceService.isDataSourceAvailable(dataSourceName);
    }
    
    /**
     * 获取数据源健康状态
     */
    public String getDataSourceHealth(String dataSourceName) {
        try {
            if (dataSourceService.isDataSourceAvailable(dataSourceName)) {
                return "healthy";
            } else {
                return "unhealthy";
            }
        } catch (Exception e) {
            log.error("检查数据源 {} 健康状态失败: {}", dataSourceName, e.getMessage());
            return "error";
        }
    }
    
    /**
     * 获取多数据源配置信息
     */
    public java.util.Map<String, Object> getMultiDataSourceInfo() {
        java.util.Map<String, Object> info = new java.util.HashMap<>();
        
        // 配置数据源信息
        String configDataSourceName = getConfigDataSourceName();
        info.put("config_data_source", configDataSourceName);
        info.put("config_data_source_health", getDataSourceHealth(configDataSourceName));
        
        // 所有可用数据源
        String[] availableDataSources = getAvailableDataSourceNames();
        info.put("available_data_sources", availableDataSources);
        
        // 各数据源健康状态
        java.util.Map<String, String> healthStatus = new java.util.HashMap<>();
        for (String dataSourceName : availableDataSources) {
            healthStatus.put(dataSourceName, getDataSourceHealth(dataSourceName));
        }
        info.put("data_source_health_status", healthStatus);
        
        return info;
    }
}
