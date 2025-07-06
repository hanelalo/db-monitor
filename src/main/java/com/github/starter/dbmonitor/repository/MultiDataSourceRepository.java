package com.github.starter.dbmonitor.repository;

import com.github.starter.dbmonitor.service.DataSourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * 多数据源Repository基类
 * 提供根据数据源名称获取对应JdbcTemplate的功能
 */
@Component
@Slf4j
public class MultiDataSourceRepository {
    
    @Autowired
    private DataSourceService dataSourceService;
    
    // 缓存JdbcTemplate实例，避免重复创建
    private final Map<String, JdbcTemplate> jdbcTemplateCache = new ConcurrentHashMap<>();
    
    /**
     * 根据数据源名称获取JdbcTemplate
     */
    protected JdbcTemplate getJdbcTemplate(String dataSourceName) {
        if (dataSourceName == null || dataSourceName.trim().isEmpty()) {
            throw new IllegalArgumentException("数据源名称不能为空");
        }

        return jdbcTemplateCache.computeIfAbsent(dataSourceName, this::createJdbcTemplate);
    }
    
    /**
     * 创建JdbcTemplate实例
     */
    private JdbcTemplate createJdbcTemplate(String dataSourceName) {
        try {
            DataSource dataSource = dataSourceService.getDataSource(dataSourceName);
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            log.info("为数据源 {} 创建了JdbcTemplate实例", dataSourceName);
            return jdbcTemplate;
        } catch (Exception e) {
            log.error("为数据源 {} 创建JdbcTemplate失败: {}", dataSourceName, e.getMessage(), e);
            throw new RuntimeException("无法为数据源 " + dataSourceName + " 创建JdbcTemplate", e);
        }
    }
    

}
