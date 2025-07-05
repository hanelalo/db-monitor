package com.github.starter.dbmonitor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.Map;

/**
 * 数据源服务
 */
@Service
@Slf4j
public class DataSourceService {
    
    @Autowired
    private ApplicationContext applicationContext;
    
    /**
     * 根据名称获取数据源
     */
    public DataSource getDataSource(String dataSourceName) {
        try {
            // 尝试根据名称获取数据源
            if (dataSourceName != null && !dataSourceName.isEmpty()) {
                try {
                    return applicationContext.getBean(dataSourceName, DataSource.class);
                } catch (Exception e) {
                    log.warn("未找到名称为 {} 的数据源，尝试获取默认数据源", dataSourceName);
                }
            }
            
            // 获取默认数据源
            return getDefaultDataSource();
            
        } catch (Exception e) {
            log.error("获取数据源失败: {}", e.getMessage(), e);
            throw new RuntimeException("无法获取数据源", e);
        }
    }
    
    /**
     * 获取默认数据源
     */
    private DataSource getDefaultDataSource() {
        try {
            // 尝试获取名为 dataSource 的bean
            return applicationContext.getBean("dataSource", DataSource.class);
        } catch (Exception e) {
            try {
                // 尝试获取类型为 DataSource 的bean
                return applicationContext.getBean(DataSource.class);
            } catch (Exception ex) {
                // 尝试获取所有 DataSource 类型的bean，选择第一个
                Map<String, DataSource> dataSourceMap = applicationContext.getBeansOfType(DataSource.class);
                if (!dataSourceMap.isEmpty()) {
                    String firstKey = dataSourceMap.keySet().iterator().next();
                    DataSource dataSource = dataSourceMap.get(firstKey);
                    log.info("使用数据源: {}", firstKey);
                    return dataSource;
                }
                throw new RuntimeException("没有找到可用的数据源", ex);
            }
        }
    }
    
    /**
     * 检查数据源是否可用
     */
    public boolean isDataSourceAvailable(String dataSourceName) {
        try {
            DataSource dataSource = getDataSource(dataSourceName);
            return dataSource != null;
        } catch (Exception e) {
            log.warn("数据源 {} 不可用: {}", dataSourceName, e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取所有可用的数据源名称
     */
    public String[] getAvailableDataSourceNames() {
        try {
            Map<String, DataSource> dataSourceMap = applicationContext.getBeansOfType(DataSource.class);
            return dataSourceMap.keySet().toArray(new String[0]);
        } catch (Exception e) {
            log.error("获取数据源名称列表失败: {}", e.getMessage(), e);
            return new String[0];
        }
    }
}