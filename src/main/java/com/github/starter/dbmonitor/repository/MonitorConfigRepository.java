package com.github.starter.dbmonitor.repository;

import com.github.starter.dbmonitor.entity.MonitorConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
 * 监控配置数据访问层
 */
@Repository
@Slf4j
public class MonitorConfigRepository {
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    private final RowMapper<MonitorConfig> rowMapper = new MonitorConfigRowMapper();
    
    /**
     * 创建监控配置表
     */
    public void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS monitor_config (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
                "config_name VARCHAR(255) NOT NULL UNIQUE," +
                "data_source_name VARCHAR(255) NOT NULL," +
                "table_name VARCHAR(255) NOT NULL," +
                "time_column_name VARCHAR(255) NOT NULL," +
                "time_column_type VARCHAR(50) NOT NULL DEFAULT 'DATETIME'," +
                "enabled BOOLEAN NOT NULL DEFAULT TRUE," +
                "interval_type VARCHAR(50) NOT NULL DEFAULT 'MINUTES'," +
                "interval_value INT NOT NULL DEFAULT 10," +
                "description TEXT," +
                "created_time DATETIME NOT NULL," +
                "updated_time DATETIME NOT NULL," +
                "created_by VARCHAR(255)," +
                "updated_by VARCHAR(255)," +
                "extend_config TEXT," +
                "INDEX idx_data_source_table (data_source_name, table_name)," +
                "INDEX idx_enabled (enabled)," +
                "INDEX idx_created_time (created_time)" +
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
        String sql = "INSERT INTO monitor_config (" +
                "config_name, data_source_name, table_name, time_column_name, " +
                "time_column_type, enabled, interval_type, interval_value, " +
                "description, created_time, updated_time, created_by, updated_by, extend_config" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        KeyHolder keyHolder = new GeneratedKeyHolder();
        
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, config.getConfigName());
            ps.setString(2, config.getDataSourceName());
            ps.setString(3, config.getTableName());
            ps.setString(4, config.getTimeColumnName());
            ps.setString(5, config.getTimeColumnType());
            ps.setBoolean(6, config.getEnabled());
            ps.setString(7, config.getIntervalType());
            ps.setInt(8, config.getIntervalValue());
            ps.setString(9, config.getDescription());
            ps.setObject(10, config.getCreatedTime());
            ps.setObject(11, config.getUpdatedTime());
            ps.setString(12, config.getCreatedBy());
            ps.setString(13, config.getUpdatedBy());
            ps.setString(14, config.getExtendConfig());
            return ps;
        }, keyHolder);
        
        Number key = keyHolder.getKey();
        if (key != null) {
            config.setId(key.longValue());
        }
        
        return config;
    }
    
    /**
     * 更新监控配置
     */
    public boolean update(MonitorConfig config) {
        String sql = "UPDATE monitor_config SET " +
                "config_name = ?, data_source_name = ?, table_name = ?, time_column_name = ?, " +
                "time_column_type = ?, enabled = ?, interval_type = ?, interval_value = ?, " +
                "description = ?, updated_time = ?, updated_by = ?, extend_config = ? " +
                "WHERE id = ?";
        
        int rows = jdbcTemplate.update(sql,
            config.getConfigName(), config.getDataSourceName(), config.getTableName(),
            config.getTimeColumnName(), config.getTimeColumnType(), config.getEnabled(),
            config.getIntervalType(), config.getIntervalValue(), config.getDescription(),
            LocalDateTime.now(), config.getUpdatedBy(), config.getExtendConfig(), config.getId());
        
        return rows > 0;
    }
    
    /**
     * 根据ID查找监控配置
     */
    public Optional<MonitorConfig> findById(Long id) {
        String sql = "SELECT * FROM monitor_config WHERE id = ?";
        try {
            MonitorConfig config = jdbcTemplate.queryForObject(sql, rowMapper, id);
            return Optional.ofNullable(config);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    /**
     * 根据配置名称查找监控配置
     */
    public Optional<MonitorConfig> findByConfigName(String configName) {
        String sql = "SELECT * FROM monitor_config WHERE config_name = ?";
        try {
            MonitorConfig config = jdbcTemplate.queryForObject(sql, rowMapper, configName);
            return Optional.ofNullable(config);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    /**
     * 查找所有监控配置
     */
    public List<MonitorConfig> findAll() {
        String sql = "SELECT * FROM monitor_config ORDER BY created_time DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }
    
    /**
     * 查找启用的监控配置
     */
    public List<MonitorConfig> findAllEnabled() {
        String sql = "SELECT * FROM monitor_config WHERE enabled = TRUE ORDER BY created_time DESC";
        return jdbcTemplate.query(sql, rowMapper);
    }
    
    /**
     * 根据数据源查找监控配置
     */
    public List<MonitorConfig> findByDataSourceName(String dataSourceName) {
        String sql = "SELECT * FROM monitor_config WHERE data_source_name = ? ORDER BY created_time DESC";
        return jdbcTemplate.query(sql, rowMapper, dataSourceName);
    }
    
    /**
     * 根据数据源和表名查找监控配置
     */
    public Optional<MonitorConfig> findByDataSourceNameAndTableName(String dataSourceName, String tableName) {
        String sql = "SELECT * FROM monitor_config WHERE data_source_name = ? AND table_name = ?";
        try {
            MonitorConfig config = jdbcTemplate.queryForObject(sql, rowMapper, dataSourceName, tableName);
            return Optional.ofNullable(config);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    
    /**
     * 根据ID删除监控配置
     */
    public boolean deleteById(Long id) {
        String sql = "DELETE FROM monitor_config WHERE id = ?";
        int rows = jdbcTemplate.update(sql, id);
        return rows > 0;
    }
    
    /**
     * 启用或禁用监控配置
     */
    public boolean updateEnabled(Long id, Boolean enabled, String updatedBy) {
        String sql = "UPDATE monitor_config SET enabled = ?, updated_time = ?, updated_by = ? WHERE id = ?";
        int rows = jdbcTemplate.update(sql, enabled, LocalDateTime.now(), updatedBy, id);
        return rows > 0;
    }
    
    /**
     * 批量启用或禁用监控配置
     */
    public int batchUpdateEnabled(List<Long> ids, Boolean enabled, String updatedBy) {
        String sql = "UPDATE monitor_config SET enabled = ?, updated_time = ?, updated_by = ? WHERE id = ?";
        
        int totalRows = 0;
        for (Long id : ids) {
            totalRows += jdbcTemplate.update(sql, enabled, LocalDateTime.now(), updatedBy, id);
        }
        
        return totalRows;
    }
    
    /**
     * 监控配置行映射器
     */
    private static class MonitorConfigRowMapper implements RowMapper<MonitorConfig> {
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
            config.setCreatedTime(rs.getObject("created_time", LocalDateTime.class));
            config.setUpdatedTime(rs.getObject("updated_time", LocalDateTime.class));
            config.setCreatedBy(rs.getString("created_by"));
            config.setUpdatedBy(rs.getString("updated_by"));
            config.setExtendConfig(rs.getString("extend_config"));
            return config;
        }
    }
}