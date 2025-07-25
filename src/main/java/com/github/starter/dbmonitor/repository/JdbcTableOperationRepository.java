package com.github.starter.dbmonitor.repository;

import com.github.starter.dbmonitor.config.DbMonitorProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于JdbcTemplate的轻量级表操作数据访问层
 * 支持多数据源操作，避免引入MyBatis等重量级ORM框架
 */
@Repository
@Slf4j
public class JdbcTableOperationRepository extends MultiDataSourceRepository {

    @Autowired
    private DbMonitorProperties dbMonitorProperties;

    /**
     * 获取默认数据源名称
     */
    private String getDefaultDataSourceName() {
        return dbMonitorProperties.getDataSourceName();
    }

    /**
     * 获取默认数据源的JdbcTemplate
     */
    private JdbcTemplate getDefaultJdbcTemplate() {
        return getJdbcTemplate(getDefaultDataSourceName());
    }

    /**
     * 获取数据库中所有表名（使用默认数据源）
     */
    public List<String> getAllTableNames() {
        return getAllTableNames(null);
    }

    /**
     * 获取指定数据源中的所有表名
     */
    public List<String> getAllTableNames(String dataSourceName) {
        try {
            JdbcTemplate template = (dataSourceName != null) ? getJdbcTemplate(dataSourceName) : getDefaultJdbcTemplate();

            // 尝试使用标准的 INFORMATION_SCHEMA 查询
            try {
                String sql = "SELECT table_name FROM information_schema.tables " +
                           "WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'";
                return template.queryForList(sql, String.class);
            } catch (Exception e) {
                log.debug("使用 INFORMATION_SCHEMA 查询失败，尝试使用 SHOW TABLES: {}", e.getMessage());
            }

            // 如果上面的查询失败，尝试使用 SHOW TABLES
            return template.queryForList("SHOW TABLES", String.class);

        } catch (Exception e) {
            log.error("获取数据源 {} 的数据库表名失败: {}", dataSourceName, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 检查表是否存在（使用默认数据源）
     */
    public boolean checkTableExists(String tableName) {
        return checkTableExists(null, tableName);
    }

    /**
     * 检查指定数据源中的表是否存在
     */
    public boolean checkTableExists(String dataSourceName, String tableName) {
        try {
            JdbcTemplate template = (dataSourceName != null) ? getJdbcTemplate(dataSourceName) : getDefaultJdbcTemplate();

            String sql = "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = DATABASE() AND table_name = ?";
            Integer count = template.queryForObject(sql, Integer.class, tableName);
            return count != null && count > 0;
        } catch (Exception e) {
            log.debug("使用 INFORMATION_SCHEMA 检查表存在性失败，尝试直接查询表: {}", e.getMessage());
            try {
                JdbcTemplate template = (dataSourceName != null) ? getJdbcTemplate(dataSourceName) : getDefaultJdbcTemplate();
                // 尝试直接查询表
                template.queryForObject("SELECT 1 FROM " + tableName + " LIMIT 1", Integer.class);
                return true;
            } catch (Exception ex) {
                log.debug("数据源 {} 中的表 {} 不存在或无法访问", dataSourceName, tableName);
                return false;
            }
        }
    }
    
    /**
     * 获取表的列信息
     */
    public List<String> getTableColumns(String tableName) {
        try {
            String sql = "SELECT column_name FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = ? " +
                        "ORDER BY ordinal_position";
            return getDefaultJdbcTemplate().queryForList(sql, String.class, tableName);
        } catch (Exception e) {
            log.error("获取表 {} 的列信息失败: {}", tableName, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取表的详细列信息（包括数据类型）
     */
    public List<Map<String, Object>> getTableColumnDetails(String tableName) {
        return getTableColumnDetails(null, tableName);
    }

    /**
     * 获取表的详细列信息（包括数据类型，支持指定数据源）
     */
    public List<Map<String, Object>> getTableColumnDetails(String dataSourceName, String tableName) {
        try {
            JdbcTemplate template = (dataSourceName != null) ? getJdbcTemplate(dataSourceName) : getDefaultJdbcTemplate();
            String sql = "SELECT column_name, data_type, is_nullable, column_default, column_comment " +
                        "FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = ? " +
                        "ORDER BY ordinal_position";
            return template.queryForList(sql, tableName);
        } catch (Exception e) {
            log.error("获取数据源 {} 表 {} 的详细列信息失败: {}", dataSourceName, tableName, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 自动检测表的时间字段
     */
    public List<String> detectTimeColumns(String tableName) {
        try {
            String sql = "SELECT column_name FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = ? " +
                        "AND (data_type IN ('datetime', 'timestamp', 'date', 'time') " +
                        "OR (data_type = 'bigint' AND (column_name LIKE '%time%' OR column_name LIKE '%Time%')) " +
                        "OR column_name IN ('created_at', 'updated_at', 'create_time', 'update_time', " +
                        "'created_time', 'updated_time', 'gmt_create', 'gmt_modified')) " +
                        "ORDER BY ordinal_position";
            return getDefaultJdbcTemplate().queryForList(sql, String.class, tableName);
        } catch (Exception e) {
            log.error("检测表 {} 的时间字段失败: {}", tableName, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 检查列是否存在
     */
    public boolean checkColumnExists(String tableName, String columnName) {
        try {
            String sql = "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?";
            Integer count = getDefaultJdbcTemplate().queryForObject(sql, Integer.class, tableName, columnName);
            return count != null && count > 0;
        } catch (Exception e) {
            log.error("检查表 {} 的列 {} 是否存在失败: {}", tableName, columnName, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 查询表的增量数据（使用默认数据源）
     */
    public Long queryTableIncrement(String tableName, String timeColumn,
                                   LocalDateTime startTime, LocalDateTime endTime) {
        return queryTableIncrement(null, tableName, timeColumn, startTime, endTime);
    }

    /**
     * 查询指定数据源中表的增量数据
     */
    public Long queryTableIncrement(String dataSourceName, String tableName, String timeColumn,
                                   LocalDateTime startTime, LocalDateTime endTime) {
        try {
            JdbcTemplate template = (dataSourceName != null) ? getJdbcTemplate(dataSourceName) : getDefaultJdbcTemplate();
            String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE " + timeColumn + " >= ? AND " + timeColumn + " < ?";
            return template.queryForObject(sql, Long.class, startTime, endTime);
        } catch (Exception e) {
            log.error("查询数据源 {} 中表 {} 的增量数据失败: {}", dataSourceName, tableName, e.getMessage(), e);
            return 0L;
        }
    }
    
    /**
     * 获取表的平均行大小（从 INFORMATION_SCHEMA）
     */
    public Long getAvgRowSizeFromInformationSchema(String tableName) {
        return getAvgRowSizeFromInformationSchema(null, tableName);
    }

    /**
     * 获取表的平均行大小（从 INFORMATION_SCHEMA，支持指定数据源）
     */
    public Long getAvgRowSizeFromInformationSchema(String dataSourceName, String tableName) {
        try {
            JdbcTemplate template = (dataSourceName != null) ? getJdbcTemplate(dataSourceName) : getDefaultJdbcTemplate();
            String sql = "SELECT ROUND(data_length / table_rows) as avg_row_length " +
                        "FROM information_schema.tables " +
                        "WHERE table_schema = DATABASE() AND table_name = ? AND table_rows > 0";
            return template.queryForObject(sql, Long.class, tableName);
        } catch (Exception e) {
            log.debug("从数据源 {} 的 INFORMATION_SCHEMA 获取表 {} 的平均行大小失败: {}", dataSourceName, tableName, e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取表状态信息
     */
    public Map<String, Object> getTableStatusInfo(String tableName) {
        return getTableStatusInfo(null, tableName);
    }

    /**
     * 获取表状态信息（支持指定数据源）
     */
    public Map<String, Object> getTableStatusInfo(String dataSourceName, String tableName) {
        try {
            JdbcTemplate template = (dataSourceName != null) ? getJdbcTemplate(dataSourceName) : getDefaultJdbcTemplate();
            String sql = "SELECT table_rows, data_length, index_length, avg_row_length " +
                        "FROM information_schema.tables " +
                        "WHERE table_schema = DATABASE() AND table_name = ?";
            List<Map<String, Object>> results = template.queryForList(sql, tableName);
            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            log.error("获取数据源 {} 表 {} 的状态信息失败: {}", dataSourceName, tableName, e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 执行自定义查询（用于灵活的数据查询）
     */
    public List<Map<String, Object>> executeQuery(String sql, Object... params) {
        try {
            return getDefaultJdbcTemplate().queryForList(sql, params);
        } catch (DataAccessException e) {
            log.error("执行自定义查询失败，SQL: {}, 参数: {}, 错误: {}", sql, params, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 执行自定义更新操作
     */
    public int executeUpdate(String sql, Object... params) {
        try {
            return getDefaultJdbcTemplate().update(sql, params);
        } catch (DataAccessException e) {
            log.error("执行自定义更新失败，SQL: {}, 参数: {}, 错误: {}", sql, params, e.getMessage(), e);
            throw e;
        }
    }
    
    /**
     * 获取数据库类型
     */
    public String getDatabaseType() {
        try {
            String url = getDefaultJdbcTemplate().getDataSource().getConnection().getMetaData().getURL();
            if (url.contains("mysql")) {
                return "mysql";
            } else if (url.contains("postgresql")) {
                return "postgresql";
            } else if (url.contains("oracle")) {
                return "oracle";
            } else if (url.contains("sqlserver")) {
                return "sqlserver";
            } else if (url.contains("h2")) {
                return "h2";
            } else {
                return "unknown";
            }
        } catch (Exception e) {
            log.error("获取数据库类型失败: {}", e.getMessage(), e);
            return "unknown";
        }
    }
    
    /**
     * 测试数据库连接
     */
    public boolean testConnection() {
        try {
            getDefaultJdbcTemplate().queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            log.error("数据库连接测试失败: {}", e.getMessage(), e);
            return false;
        }
    }
}
