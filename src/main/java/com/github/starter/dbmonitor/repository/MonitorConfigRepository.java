package com.github.starter.dbmonitor.repository;

import com.github.starter.dbmonitor.entity.MonitorConfig;
import com.github.starter.dbmonitor.mapper.MonitorConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 监控配置数据访问层
 */
@Repository
@Slf4j
public class MonitorConfigRepository {
    
    @Autowired
    private MonitorConfigMapper monitorConfigMapper;
    
    /**
     * 创建监控配置表
     */
    public void createTableIfNotExists() {
        try {
            monitorConfigMapper.createTableIfNotExists();
            log.info("监控配置表创建或已存在");
        } catch (Exception e) {
            log.error("创建监控配置表失败: {}", e.getMessage(), e);
            throw new RuntimeException("创建监控配置表失败", e);
        }
    }
    
    /**
     * 插入监控配置
     */
    public MonitorConfig insert(MonitorConfig config) {
        monitorConfigMapper.insert(config);
        return config;
    }
    
    /**
     * 更新监控配置
     */
    public boolean update(MonitorConfig config) {
        config.setUpdatedTime(LocalDateTime.now());
        int rows = monitorConfigMapper.update(config);
        return rows > 0;
    }
    
    /**
     * 根据ID查找监控配置
     */
    public Optional<MonitorConfig> findById(Long id) {
        try {
            MonitorConfig config = monitorConfigMapper.findById(id);
            return Optional.ofNullable(config);
        } catch (Exception e) {
            log.error("根据ID查找监控配置失败, ID: {}, 错误: {}", id, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * 根据配置名称查找监控配置
     */
    public Optional<MonitorConfig> findByConfigName(String configName) {
        try {
            MonitorConfig config = monitorConfigMapper.findByConfigName(configName);
            return Optional.ofNullable(config);
        } catch (Exception e) {
            log.error("根据配置名称查找监控配置失败, 配置名称: {}, 错误: {}", configName, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * 查找所有监控配置
     */
    public List<MonitorConfig> findAll() {
        return monitorConfigMapper.findAll();
    }
    
    /**
     * 查找启用的监控配置
     */
    public List<MonitorConfig> findAllEnabled() {
        return monitorConfigMapper.findAllEnabled();
    }
    
    /**
     * 根据数据源查找监控配置
     */
    public List<MonitorConfig> findByDataSourceName(String dataSourceName) {
        return monitorConfigMapper.findByDataSourceName(dataSourceName);
    }
    
    /**
     * 根据数据源和表名查找监控配置
     */
    public Optional<MonitorConfig> findByDataSourceNameAndTableName(String dataSourceName, String tableName) {
        try {
            MonitorConfig config = monitorConfigMapper.findByDataSourceNameAndTableName(dataSourceName, tableName);
            return Optional.ofNullable(config);
        } catch (Exception e) {
            log.error("根据数据源和表名查找监控配置失败, 数据源: {}, 表名: {}, 错误: {}",
                    dataSourceName, tableName, e.getMessage(), e);
            return Optional.empty();
        }
    }
    
    /**
     * 根据ID删除监控配置
     */
    public boolean deleteById(Long id) {
        int rows = monitorConfigMapper.deleteById(id);
        return rows > 0;
    }
    
    /**
     * 启用或禁用监控配置
     */
    public boolean updateEnabled(Long id, Boolean enabled, String updatedBy) {
        int rows = monitorConfigMapper.updateEnabled(id, enabled, updatedBy, LocalDateTime.now());
        return rows > 0;
    }
    
    /**
     * 批量启用或禁用监控配置
     */
    public int batchUpdateEnabled(List<Long> ids, Boolean enabled, String updatedBy) {
        return monitorConfigMapper.batchUpdateEnabled(ids, enabled, updatedBy, LocalDateTime.now());
    }
    
}