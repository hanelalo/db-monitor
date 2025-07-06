package com.github.starter.dbmonitor.repository;

import com.github.starter.dbmonitor.config.DbMonitorProperties;
import com.github.starter.dbmonitor.entity.DbMonitorStatistics;
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
 * 基于JdbcTemplate的轻量级数据库监控统计数据访问层
 * 支持指定统计数据存储数据源，避免引入MyBatis等重量级ORM框架
 */
@Repository
@Slf4j
public class JdbcDbMonitorStatisticsRepository extends MultiDataSourceRepository {

    @Autowired
    private DbMonitorProperties dbMonitorProperties;

    /**
     * 获取监控统计表名
     */
    private String getTableName() {
        return dbMonitorProperties.getMonitorTable().getTableName();
    }

    /**
     * 获取配置存储数据源名称
     */
    private String getConfigDataSourceName() {
        // 优先使用专门的配置数据源
        if (dbMonitorProperties.getConfigDataSourceName() != null &&
            !dbMonitorProperties.getConfigDataSourceName().trim().isEmpty()) {
            return dbMonitorProperties.getConfigDataSourceName();
        }

        // 否则使用默认数据源
        return dbMonitorProperties.getDataSourceName();
    }

    /**
     * 获取用于统计数据操作的JdbcTemplate
     */
    private JdbcTemplate getConfigJdbcTemplate() {
        String dataSourceName = getConfigDataSourceName();
        return getJdbcTemplate(dataSourceName);
    }
    
    // RowMapper for DbMonitorStatistics
    private final RowMapper<DbMonitorStatistics> rowMapper = new RowMapper<DbMonitorStatistics>() {
        @Override
        public DbMonitorStatistics mapRow(ResultSet rs, int rowNum) throws SQLException {
            DbMonitorStatistics statistics = new DbMonitorStatistics();
            statistics.setId(rs.getLong("id"));
            statistics.setDataSourceName(rs.getString("data_source_name"));
            statistics.setTableName(rs.getString("table_name"));
            statistics.setStatisticTime(rs.getTimestamp("statistic_time") != null ? 
                rs.getTimestamp("statistic_time").toLocalDateTime() : null);
            statistics.setStartTime(rs.getTimestamp("start_time") != null ? 
                rs.getTimestamp("start_time").toLocalDateTime() : null);
            statistics.setEndTime(rs.getTimestamp("end_time") != null ? 
                rs.getTimestamp("end_time").toLocalDateTime() : null);
            statistics.setIncrementCount(rs.getLong("increment_count"));
            statistics.setEstimatedDiskSizeBytes(rs.getLong("estimated_disk_size_bytes"));
            statistics.setAvgRowSizeBytes(rs.getLong("avg_row_size_bytes"));
            statistics.setIntervalType(rs.getString("interval_type"));
            statistics.setIntervalValue(rs.getInt("interval_value"));
            statistics.setCreatedTime(rs.getTimestamp("created_time") != null ? 
                rs.getTimestamp("created_time").toLocalDateTime() : null);
            statistics.setAdditionalInfo(rs.getString("additional_info"));
            return statistics;
        }
    };
    
    /**
     * 创建统计数据表
     */
    public void createTableIfNotExists() {
        if (!dbMonitorProperties.getMonitorTable().isAutoCreate()) {
            log.info("监控统计表自动创建已禁用，跳过表创建");
            return;
        }

        String tableName = getTableName();
        String sql = "CREATE TABLE IF NOT EXISTS " + tableName + " (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "data_source_name VARCHAR(100) NOT NULL, " +
                "table_name VARCHAR(100) NOT NULL, " +
                "statistic_time TIMESTAMP NOT NULL, " +
                "start_time TIMESTAMP NOT NULL, " +
                "end_time TIMESTAMP NOT NULL, " +
                "increment_count BIGINT DEFAULT 0, " +
                "estimated_disk_size_bytes BIGINT DEFAULT 0, " +
                "avg_row_size_bytes BIGINT DEFAULT 0, " +
                "interval_type VARCHAR(20) NOT NULL, " +
                "interval_value INT NOT NULL, " +
                "created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "additional_info TEXT, " +
                "INDEX idx_data_source_table_time (data_source_name, table_name, statistic_time), " +
                "INDEX idx_statistic_time (statistic_time), " +
                "INDEX idx_created_time (created_time)" +
                ")";

        try {
            getConfigJdbcTemplate().execute(sql);
            log.info("监控统计表 {} 创建或已存在，数据源: {}", tableName, getConfigDataSourceName());
        } catch (Exception e) {
            log.error("创建监控统计表 {} 失败: {}", tableName, e.getMessage(), e);
            throw new RuntimeException("创建监控统计表失败", e);
        }
    }
    
    /**
     * 插入统计记录
     */
    public DbMonitorStatistics insert(DbMonitorStatistics statistics) {
        String tableName = getTableName();
        String sql = "INSERT INTO " + tableName +
                " (data_source_name, table_name, statistic_time, start_time, end_time, " +
                "increment_count, estimated_disk_size_bytes, avg_row_size_bytes, " +
                "interval_type, interval_value, created_time, additional_info) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        
        getConfigJdbcTemplate().update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, statistics.getDataSourceName());
            ps.setString(2, statistics.getTableName());
            ps.setObject(3, statistics.getStatisticTime());
            ps.setObject(4, statistics.getStartTime());
            ps.setObject(5, statistics.getEndTime());
            ps.setLong(6, statistics.getIncrementCount() != null ? statistics.getIncrementCount() : 0L);
            ps.setLong(7, statistics.getEstimatedDiskSizeBytes() != null ? statistics.getEstimatedDiskSizeBytes() : 0L);
            ps.setLong(8, statistics.getAvgRowSizeBytes() != null ? statistics.getAvgRowSizeBytes() : 0L);
            ps.setString(9, statistics.getIntervalType());
            ps.setInt(10, statistics.getIntervalValue());
            ps.setObject(11, statistics.getCreatedTime() != null ? statistics.getCreatedTime() : LocalDateTime.now());
            ps.setString(12, statistics.getAdditionalInfo());
            return ps;
        }, keyHolder);
        
        if (keyHolder.getKey() != null) {
            statistics.setId(keyHolder.getKey().longValue());
        }
        
        return statistics;
    }
    
    /**
     * 批量插入统计记录
     */
    public int batchInsert(List<DbMonitorStatistics> statisticsList) {
        if (statisticsList == null || statisticsList.isEmpty()) {
            return 0;
        }
        
        String tableName = getTableName();
        String sql = "INSERT INTO " + tableName +
                " (data_source_name, table_name, statistic_time, start_time, end_time, " +
                "increment_count, estimated_disk_size_bytes, avg_row_size_bytes, " +
                "interval_type, interval_value, created_time, additional_info) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        return getConfigJdbcTemplate().batchUpdate(sql, statisticsList, statisticsList.size(),
                (ps, statistics) -> {
                    ps.setString(1, statistics.getDataSourceName());
                    ps.setString(2, statistics.getTableName());
                    ps.setObject(3, statistics.getStatisticTime());
                    ps.setObject(4, statistics.getStartTime());
                    ps.setObject(5, statistics.getEndTime());
                    ps.setLong(6, statistics.getIncrementCount() != null ? statistics.getIncrementCount() : 0L);
                    ps.setLong(7, statistics.getEstimatedDiskSizeBytes() != null ? statistics.getEstimatedDiskSizeBytes() : 0L);
                    ps.setLong(8, statistics.getAvgRowSizeBytes() != null ? statistics.getAvgRowSizeBytes() : 0L);
                    ps.setString(9, statistics.getIntervalType());
                    ps.setInt(10, statistics.getIntervalValue());
                    ps.setObject(11, statistics.getCreatedTime() != null ? statistics.getCreatedTime() : LocalDateTime.now());
                    ps.setString(12, statistics.getAdditionalInfo());
                }).length;
    }
    
    /**
     * 根据ID查询统计记录
     */
    public Optional<DbMonitorStatistics> findById(Long id) {
        String tableName = getTableName();
        String sql = "SELECT * FROM " + tableName + " WHERE id = ?";
        try {
            DbMonitorStatistics statistics = getConfigJdbcTemplate().queryForObject(sql, rowMapper, id);
            return Optional.ofNullable(statistics);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 查询指定时间范围内的统计记录
     */
    public List<DbMonitorStatistics> findByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        String tableName = getTableName();
        String sql = "SELECT * FROM " + tableName +
                " WHERE statistic_time >= ? AND statistic_time <= ? ORDER BY statistic_time DESC";
        return getConfigJdbcTemplate().query(sql, rowMapper, startTime, endTime);
    }

    /**
     * 查询指定数据源和表的统计记录
     */
    public List<DbMonitorStatistics> findByDataSourceAndTable(String dataSourceName, String tableName) {
        String configTableName = getTableName();
        String sql = "SELECT * FROM " + configTableName +
                " WHERE data_source_name = ? AND table_name = ? ORDER BY statistic_time DESC";
        return getConfigJdbcTemplate().query(sql, rowMapper, dataSourceName, tableName);
    }

    /**
     * 查询指定数据源和表在指定时间范围内的统计记录
     */
    public List<DbMonitorStatistics> findByDataSourceAndTableAndTimeRange(
            String dataSourceName, String tableName, LocalDateTime startTime, LocalDateTime endTime) {
        String configTableName = getTableName();
        String sql = "SELECT * FROM " + configTableName +
                " WHERE data_source_name = ? AND table_name = ? " +
                "AND statistic_time >= ? AND statistic_time <= ? ORDER BY statistic_time DESC";
        return getConfigJdbcTemplate().query(sql, rowMapper, dataSourceName, tableName, startTime, endTime);
    }

    /**
     * 获取最新的统计记录
     */
    public Optional<DbMonitorStatistics> findLatestByDataSourceAndTable(String dataSourceName, String tableName) {
        String configTableName = getTableName();
        String sql = "SELECT * FROM " + configTableName +
                " WHERE data_source_name = ? AND table_name = ? ORDER BY statistic_time DESC LIMIT 1";
        try {
            DbMonitorStatistics statistics = getConfigJdbcTemplate().queryForObject(sql, rowMapper, dataSourceName, tableName);
            return Optional.ofNullable(statistics);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /**
     * 删除指定时间之前的记录
     */
    public int deleteByCreatedTimeBefore(LocalDateTime cutoffTime) {
        String tableName = getTableName();
        String sql = "DELETE FROM " + tableName + " WHERE created_time < ?";
        return getConfigJdbcTemplate().update(sql, cutoffTime);
    }

    /**
     * 根据ID删除统计记录
     */
    public boolean deleteById(Long id) {
        String tableName = getTableName();
        String sql = "DELETE FROM " + tableName + " WHERE id = ?";
        int rows = getConfigJdbcTemplate().update(sql, id);
        return rows > 0;
    }

    /**
     * 更新统计记录
     */
    public boolean update(DbMonitorStatistics statistics) {
        String tableName = getTableName();
        String sql = "UPDATE " + tableName + " SET " +
                "data_source_name = ?, table_name = ?, statistic_time = ?, start_time = ?, end_time = ?, " +
                "increment_count = ?, estimated_disk_size_bytes = ?, avg_row_size_bytes = ?, " +
                "interval_type = ?, interval_value = ?, additional_info = ? WHERE id = ?";

        int rows = getConfigJdbcTemplate().update(sql,
                statistics.getDataSourceName(),
                statistics.getTableName(),
                statistics.getStatisticTime(),
                statistics.getStartTime(),
                statistics.getEndTime(),
                statistics.getIncrementCount(),
                statistics.getEstimatedDiskSizeBytes(),
                statistics.getAvgRowSizeBytes(),
                statistics.getIntervalType(),
                statistics.getIntervalValue(),
                statistics.getAdditionalInfo(),
                statistics.getId());

        return rows > 0;
    }
    
    /**
     * 统计总记录数
     */
    public long count() {
        String tableName = getTableName();
        String sql = "SELECT COUNT(*) FROM " + tableName;
        return getConfigJdbcTemplate().queryForObject(sql, Long.class);
    }

    /**
     * 统计指定数据源和表的记录数
     */
    public long countByDataSourceAndTable(String dataSourceName, String tableName) {
        String configTableName = getTableName();
        String sql = "SELECT COUNT(*) FROM " + configTableName + " WHERE data_source_name = ? AND table_name = ?";
        return getConfigJdbcTemplate().queryForObject(sql, Long.class, dataSourceName, tableName);
    }

    /**
     * 根据数据源查询统计记录，按统计时间倒序
     */
    public List<DbMonitorStatistics> findByDataSourceNameOrderByStatisticTimeDesc(String dataSourceName) {
        String tableName = getTableName();
        String sql = "SELECT * FROM " + tableName +
                " WHERE data_source_name = ? ORDER BY statistic_time DESC";
        return getConfigJdbcTemplate().query(sql, rowMapper, dataSourceName);
    }
}
