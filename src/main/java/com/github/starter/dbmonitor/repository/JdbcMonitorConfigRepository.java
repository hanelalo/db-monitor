package com.github.starter.dbmonitor.repository;

import com.github.starter.dbmonitor.entity.MonitorConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 基于JdbcTemplate的轻量级监控配置数据访问层
 * 避免引入MyBatis等重量级ORM框架
 */
@Repository
@Slf4j
public class JdbcMonitorConfigRepository {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private static final String TABLE_NAME = "db_monitor_config";
    
    // RowMapper for MonitorConfig
    private final RowMapper<MonitorConfig> rowMapper = new RowMapper<MonitorConfig>() {
        @Override
        public MonitorConfig mapRow(ResultSet rs, int rowNum) throws SQLException {
            MonitorConfig config = new MonitorConfig();
            config.setId(rs.getLong("id"));
            config.setConfigName(rs.getString("config_name"));
            config.setDataSourceName(rs.getString("data_source_name"));
            config.setTableName(rs.getString("table_name"));
            config.setTimeColumnName(rs.getString("time_column_name"));
            config.setTimeColumnType(rs.getString("time_column_type"));
            config.setEnabled(rs.getBoolean("enabled"));
            config.setIntervalType(rs.getString("interval_type"));
            config.setIntervalValue(rs.getInt("interval_value"));
            config.setDescription(rs.getString("description"));
            config.setCreatedTime(rs.getTimestamp("created_time") != null ? 
                rs.getTimestamp("created_time").toLocalDateTime() : null);
            config.setUpdatedTime(rs.getTimestamp("updated_time") != null ? 
                rs.getTimestamp("updated_time").toLocalDateTime() : null);
            config.setCreatedBy(rs.getString("created_by"));
            config.setUpdatedBy(rs.getString("updated_by"));
            config.setExtendConfig(rs.getString("extend_config"));
            return config;
        }
    };
    
    /**
     * 创建监控配置表
     */
    public void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "config_name VARCHAR(100) NOT NULL UNIQUE, " +
                "data_source_name VARCHAR(100) NOT NULL, " +
                "table_name VARCHAR(100) NOT NULL, " +
                "time_column_name VARCHAR(100) NOT NULL, " +
                "time_column_type VARCHAR(50) NOT NULL, " +
                "enabled BOOLEAN DEFAULT TRUE, " +
                "interval_type VARCHAR(20) DEFAULT 'MINUTES', " +
                "interval_value INT DEFAULT 10, " +
                "description TEXT, " +
                "created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                "created_by VARCHAR(100), " +
                "updated_by VARCHAR(100), " +
                "extend_config TEXT, " +
                "INDEX idx_data_source_table (data_source_name, table_name), " +
                "INDEX idx_enabled (enabled)" +
                ")";
        
        try {
            jdbcTemplate.execute(sql);
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
        String sql = "INSERT INTO " + TABLE_NAME + 
                " (config_name, data_source_name, table_name, time_column_name, time_column_type, " +
                "enabled, interval_type, interval_value, description, created_time, updated_time, " +
                "created_by, updated_by, extend_config) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, config.getConfigName());
            ps.setString(2, config.getDataSourceName());
            ps.setString(3, config.getTableName());
            ps.setString(4, config.getTimeColumnName());
            ps.setString(5, config.getTimeColumnType());
            ps.setBoolean(6, config.getEnabled() != null ? config.getEnabled() : true);
            ps.setString(7, config.getIntervalType() != null ? config.getIntervalType() : "MINUTES");
            ps.setInt(8, config.getIntervalValue() != null ? config.getIntervalValue() : 10);
            ps.setString(9, config.getDescription());
            ps.setObject(10, config.getCreatedTime() != null ? config.getCreatedTime() : LocalDateTime.now());
            ps.setObject(11, config.getUpdatedTime() != null ? config.getUpdatedTime() : LocalDateTime.now());
            ps.setString(12, config.getCreatedBy());
            ps.setString(13, config.getUpdatedBy());
            ps.setString(14, config.getExtendConfig());
            return ps;
        }, keyHolder);
        
        if (keyHolder.getKey() != null) {
            config.setId(keyHolder.getKey().longValue());
        }
        
        return config;
    }
    
    /**
     * 更新监控配置
     */
    public boolean update(MonitorConfig config) {
        String sql = "UPDATE " + TABLE_NAME + " SET " +
                "config_name = ?, data_source_name = ?, table_name = ?, time_column_name = ?, " +
                "time_column_type = ?, enabled = ?, interval_type = ?, interval_value = ?, " +
                "description = ?, updated_time = ?, updated_by = ?, extend_config = ? " +
                "WHERE id = ?";
        
        config.setUpdatedTime(LocalDateTime.now());
        
        int rows = jdbcTemplate.update(sql,
                config.getConfigName(),
                config.getDataSourceName(),
                config.getTableName(),
                config.getTimeColumnName(),
                config.getTimeColumnType(),
                config.getEnabled(),
                config.getIntervalType(),
                config.getIntervalValue(),
                config.getDescription(),
                config.getUpdatedTime(),
                config.getUpdatedBy(),
                config.getExtendConfig(),
                config.getId());
        
        return rows > 0;
    }
    
    /**
     * 根据ID查找监控配置
     */
    public Optional<MonitorConfig> findById(Long id) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE id = ?";
        try {
            MonitorConfig config = jdbcTemplate.queryForObject(sql, rowMapper, id);
            return Optional.ofNullable(config);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
    
    /**
     * 根据配置名称查找监控配置
     */
    public Optional<MonitorConfig> findByConfigName(String configName) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE config_name = ?";
        try {
            MonitorConfig config = jdbcTemplate.queryForObject(sql, rowMapper, configName);
            return Optional.ofNullable(config);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
    
    /**
     * 查找所有监控配置
     */
    public List<MonitorConfig> findAll() {
        String sql = "SELECT * FROM " + TABLE_NAME + " ORDER BY created_time DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }
    
    /**
     * 查找启用的监控配置
     */
    public List<MonitorConfig> findAllEnabled() {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE enabled = TRUE ORDER BY created_time DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }
    
    /**
     * 根据数据源查找监控配置
     */
    public List<MonitorConfig> findByDataSourceName(String dataSourceName) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE data_source_name = ? ORDER BY created_time DESC";
        return jdbcTemplate.query(sql, rowMapper, dataSourceName);
    }
    
    /**
     * 根据数据源和表名查找监控配置
     */
    public Optional<MonitorConfig> findByDataSourceNameAndTableName(String dataSourceName, String tableName) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE data_source_name = ? AND table_name = ?";
        try {
            MonitorConfig config = jdbcTemplate.queryForObject(sql, rowMapper, dataSourceName, tableName);
            return Optional.ofNullable(config);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
    
    /**
     * 根据ID删除监控配置
     */
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        int rows = jdbcTemplate.update(sql, id);
        return rows > 0;
    }
    
    /**
     * 启用或禁用监控配置
     */
    public boolean updateEnabled(Long id, Boolean enabled, String updatedBy) {
        String sql = "UPDATE " + TABLE_NAME + " SET enabled = ?, updated_by = ?, updated_time = ? WHERE id = ?";
        int rows = jdbcTemplate.update(sql, enabled, updatedBy, LocalDateTime.now(), id);
        return rows > 0;
    }
    
    /**
     * 批量启用或禁用监控配置
     */
    public int batchUpdateEnabled(List<Long> ids, Boolean enabled, String updatedBy) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        
        String sql = "UPDATE " + TABLE_NAME + " SET enabled = ?, updated_by = ?, updated_time = ? WHERE id IN (" +
                String.join(",", ids.stream().map(id -> "?").toArray(String[]::new)) + ")";
        
        Object[] params = new Object[ids.size() + 3];
        params[0] = enabled;
        params[1] = updatedBy;
        params[2] = LocalDateTime.now();
        for (int i = 0; i < ids.size(); i++) {
            params[i + 3] = ids.get(i);
        }
        
        return jdbcTemplate.update(sql, params);
    }
}
