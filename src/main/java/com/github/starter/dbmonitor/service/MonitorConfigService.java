package com.github.starter.dbmonitor.service;

import com.github.starter.dbmonitor.entity.MonitorConfig;
import com.github.starter.dbmonitor.repository.JdbcMonitorConfigRepository;
import com.github.starter.dbmonitor.repository.JdbcTableOperationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 监控配置服务
 */
@Service
@Slf4j
public class MonitorConfigService {
    
    @Autowired
    private JdbcMonitorConfigRepository monitorConfigRepository;

    @Autowired
    private DataSourceService dataSourceService;

    @Autowired
    private JdbcTableOperationRepository tableOperationRepository;

    @Autowired
    private DatabaseSecurityService databaseSecurityService;
    
    /**
     * 初始化监控配置表
     */
    @PostConstruct
    public void init() {
        try {
            monitorConfigRepository.createTableIfNotExists();
            log.info("监控配置服务初始化完成");
        } catch (Exception e) {
            log.error("监控配置服务初始化失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 创建监控配置
     */
    @Transactional
    public MonitorConfig createConfig(MonitorConfig config) {
        validateConfig(config);
        
        // 检查配置名称是否已存在
        if (monitorConfigRepository.findByConfigName(config.getConfigName()).isPresent()) {
            throw new IllegalArgumentException("配置名称已存在: " + config.getConfigName());
        }
        
        // 检查数据源和表名的组合是否已存在
        Optional<MonitorConfig> existingConfig = monitorConfigRepository
                .findByDataSourceNameAndTableName(config.getDataSourceName(), config.getTableName());
        if (existingConfig.isPresent()) {
            throw new IllegalArgumentException("数据源 " + config.getDataSourceName() + 
                    " 中的表 " + config.getTableName() + " 已存在监控配置");
        }
        
        // 验证表和时间字段是否存在
        validateTableAndTimeColumn(config);
        
        // 设置默认值
        config.setCreatedTime(LocalDateTime.now());
        config.setUpdatedTime(LocalDateTime.now());
        if (config.getEnabled() == null) {
            config.setEnabled(true);
        }
        if (!StringUtils.hasText(config.getIntervalType())) {
            config.setIntervalType("MINUTES");
        }
        if (config.getIntervalValue() == null) {
            config.setIntervalValue(10);
        }
        
        return monitorConfigRepository.insert(config);
    }
    
    /**
     * 更新监控配置
     */
    @Transactional
    public boolean updateConfig(MonitorConfig config) {
        validateConfig(config);
        
        // 检查配置是否存在
        Optional<MonitorConfig> existingConfig = monitorConfigRepository.findById(config.getId());
        if (!existingConfig.isPresent()) {
            throw new IllegalArgumentException("监控配置不存在: " + config.getId());
        }
        
        // 检查配置名称是否与其他配置冲突
        Optional<MonitorConfig> configWithSameName = monitorConfigRepository.findByConfigName(config.getConfigName());
        if (configWithSameName.isPresent() && !configWithSameName.get().getId().equals(config.getId())) {
            throw new IllegalArgumentException("配置名称已存在: " + config.getConfigName());
        }
        
        // 验证表和时间字段是否存在
        validateTableAndTimeColumn(config);
        
        // 更新时间
        config.setUpdatedTime(LocalDateTime.now());
        
        return monitorConfigRepository.update(config);
    }
    
    /**
     * 根据ID获取监控配置
     */
    public Optional<MonitorConfig> getConfigById(Long id) {
        return monitorConfigRepository.findById(id);
    }
    
    /**
     * 根据配置名称获取监控配置
     */
    public Optional<MonitorConfig> getConfigByName(String configName) {
        return monitorConfigRepository.findByConfigName(configName);
    }
    
    /**
     * 获取所有监控配置
     */
    public List<MonitorConfig> getAllConfigs() {
        return monitorConfigRepository.findAll();
    }
    
    /**
     * 获取所有启用的监控配置
     */
    public List<MonitorConfig> getEnabledConfigs() {
        return monitorConfigRepository.findAllEnabled();
    }

    /**
     * 获取所有启用的监控配置（支持分片）
     */
    public List<MonitorConfig> getEnabledConfigs(String shardingParam) {
        if (shardingParam == null || shardingParam.trim().isEmpty()) {
            return getEnabledConfigs(); // 非分片模式
        }

        try {
            // 解析分片参数
            String[] parts = shardingParam.trim().split("/");
            if (parts.length != 2) {
                throw new IllegalArgumentException("分片参数格式错误，应为 'shardIndex/shardTotal'");
            }

            int shardIndex = Integer.parseInt(parts[0]);
            int shardTotal = Integer.parseInt(parts[1]);

            if (shardIndex < 0 || shardTotal <= 0 || shardIndex >= shardTotal) {
                throw new IllegalArgumentException("分片参数值错误：shardIndex=" + shardIndex + ", shardTotal=" + shardTotal);
            }

            return monitorConfigRepository.findAllEnabledWithSharding(shardIndex, shardTotal);

        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("分片参数格式错误，无法解析数字: " + shardingParam, e);
        }
    }
    
    /**
     * 根据数据源获取监控配置
     */
    public List<MonitorConfig> getConfigsByDataSource(String dataSourceName) {
        return monitorConfigRepository.findByDataSourceName(dataSourceName);
    }
    
    /**
     * 删除监控配置
     */
    @Transactional
    public boolean deleteConfig(Long id) {
        Optional<MonitorConfig> config = monitorConfigRepository.findById(id);
        if (!config.isPresent()) {
            throw new IllegalArgumentException("监控配置不存在: " + id);
        }
        
        return monitorConfigRepository.deleteById(id);
    }
    
    /**
     * 启用或禁用监控配置
     */
    @Transactional
    public boolean updateConfigEnabled(Long id, Boolean enabled, String updatedBy) {
        Optional<MonitorConfig> config = monitorConfigRepository.findById(id);
        if (!config.isPresent()) {
            throw new IllegalArgumentException("监控配置不存在: " + id);
        }
        
        return monitorConfigRepository.updateEnabled(id, enabled, updatedBy);
    }
    
    /**
     * 批量启用或禁用监控配置
     */
    @Transactional
    public int batchUpdateConfigEnabled(List<Long> ids, Boolean enabled, String updatedBy) {
        return monitorConfigRepository.batchUpdateEnabled(ids, enabled, updatedBy);
    }
    
    /**
     * 测试监控配置是否有效
     */
    public boolean testConfig(MonitorConfig config) {
        try {
            validateTableAndTimeColumn(config);
            return true;
        } catch (Exception e) {
            log.warn("测试监控配置失败: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 获取表的所有列信息
     */
    public List<String> getTableColumns(String dataSourceName, String tableName) {
        // 安全验证
        databaseSecurityService.sanitizeDataSourceName(dataSourceName);
        databaseSecurityService.sanitizeTableName(tableName);

        try {
            return tableOperationRepository.getTableColumns(tableName);
        } catch (Exception e) {
            log.error("获取表 {} 的列信息失败: {}", tableName, e.getMessage(), e);
            throw new RuntimeException("获取表列信息失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 自动检测表的时间字段
     */
    public List<String> detectTimeColumns(String dataSourceName, String tableName) {
        // 安全验证
        databaseSecurityService.sanitizeDataSourceName(dataSourceName);
        databaseSecurityService.sanitizeTableName(tableName);

        try {
            return tableOperationRepository.detectTimeColumns(tableName);
        } catch (Exception e) {
            log.error("自动检测表 {} 的时间字段失败: {}", tableName, e.getMessage(), e);
            throw new RuntimeException("自动检测时间字段失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 根据数据源和表名获取监控配置
     */
    public Optional<MonitorConfig> getConfigByDataSourceAndTable(String dataSourceName, String tableName) {
        return monitorConfigRepository.findByDataSourceNameAndTableName(dataSourceName, tableName);
    }

    /**
     * 更新监控配置的最后统计时间
     */
    @Transactional
    public boolean updateLastStatisticTime(Long configId, LocalDateTime lastStatisticTime) {
        return monitorConfigRepository.updateLastStatisticTime(configId, lastStatisticTime);
    }
    
    /**
     * 验证监控配置
     */
    private void validateConfig(MonitorConfig config) {
        if (!StringUtils.hasText(config.getConfigName())) {
            throw new IllegalArgumentException("配置名称不能为空");
        }
        
        if (!StringUtils.hasText(config.getDataSourceName())) {
            throw new IllegalArgumentException("数据源名称不能为空");
        }
        
        if (!StringUtils.hasText(config.getTableName())) {
            throw new IllegalArgumentException("表名不能为空");
        }
        
        if (!StringUtils.hasText(config.getTimeColumnName())) {
            throw new IllegalArgumentException("时间字段名不能为空");
        }
        
        if (!StringUtils.hasText(config.getTimeColumnType())) {
            throw new IllegalArgumentException("时间字段类型不能为空");
        }
        
        if (config.getIntervalValue() != null && config.getIntervalValue() <= 0) {
            throw new IllegalArgumentException("监控间隔值必须大于0");
        }
        
        // 验证间隔类型
        if (StringUtils.hasText(config.getIntervalType())) {
            String intervalType = config.getIntervalType().toUpperCase();
            if (!intervalType.equals("MINUTES") && !intervalType.equals("HOURS") && !intervalType.equals("DAYS")) {
                throw new IllegalArgumentException("监控间隔类型必须是 MINUTES、HOURS 或 DAYS");
            }
        }
    }
    
    /**
     * 验证表和时间字段是否存在
     */
    private void validateTableAndTimeColumn(MonitorConfig config) {
        try {
            // 安全验证
            databaseSecurityService.sanitizeDataSourceName(config.getDataSourceName());
            databaseSecurityService.sanitizeTableName(config.getTableName());
            databaseSecurityService.sanitizeColumnName(config.getTimeColumnName());

            // 检查数据源是否可用
            if (!dataSourceService.isDataSourceAvailable(config.getDataSourceName())) {
                throw new IllegalArgumentException("数据源不可用: " + config.getDataSourceName());
            }

            // 检查表是否存在（使用指定的数据源）
            boolean tableExists = tableOperationRepository.checkTableExists(config.getDataSourceName(), config.getTableName());
            if (!tableExists) {
                throw new IllegalArgumentException("数据源 " + config.getDataSourceName() + " 中的表不存在: " + config.getTableName());
            }

            // 检查时间字段是否存在（使用指定的数据源）
            boolean columnExists = checkColumnExistsInDataSource(config.getDataSourceName(), config.getTableName(), config.getTimeColumnName());
            if (!columnExists) {
                throw new IllegalArgumentException("数据源 " + config.getDataSourceName() + " 中的表 " + config.getTableName() + " 的时间字段不存在: " + config.getTimeColumnName());
            }

            log.debug("验证表 {} 和时间字段 {} 成功", config.getTableName(), config.getTimeColumnName());

        } catch (IllegalArgumentException e) {
            // 重新抛出参数异常
            throw e;
        } catch (Exception e) {
            log.error("验证表和时间字段失败: {}", e.getMessage(), e);
            throw new RuntimeException("验证表和时间字段失败: " + e.getMessage(), e);
        }
    }

    /**
     * 启用监控配置
     */
    public boolean enableConfig(Long id, String updatedBy) {
        return monitorConfigRepository.updateEnabled(id, true, updatedBy);
    }

    /**
     * 禁用监控配置
     */
    public boolean disableConfig(Long id, String updatedBy) {
        return monitorConfigRepository.updateEnabled(id, false, updatedBy);
    }

    /**
     * 从指定数据源获取表的所有列
     */
    private List<String> getTableColumnsFromDataSource(String dataSourceName, String tableName) {
        // 使用多数据源支持
        return tableOperationRepository.getTableColumns(tableName);
    }

    /**
     * 检查指定数据源中的列是否存在
     */
    private boolean checkColumnExistsInDataSource(String dataSourceName, String tableName, String columnName) {
        // 使用多数据源支持
        return tableOperationRepository.checkColumnExists(tableName, columnName);
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
}