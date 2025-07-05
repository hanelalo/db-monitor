package com.example.incrementmonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import com.xxl.job.core.handler.annotation.XxlJob;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service that periodically scans configured tables, calculates row-count increments and stores
 * the statistics in a dedicated monitoring table.
 */
public class DataIncrementMonitorService {

    private static final String MONITOR_TABLE_NAME = "monitor_table_increment";

    private final Logger logger = LoggerFactory.getLogger(DataIncrementMonitorService.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final DataIncrementMonitorProperties properties;

    public DataIncrementMonitorService(DataSource dataSource,
                                       JdbcTemplate jdbcTemplate,
                                       DataIncrementMonitorProperties properties) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    @PostConstruct
    public void init() {
        // Create monitoring table if it doesn't exist
        String ddl = "CREATE TABLE IF NOT EXISTS " + MONITOR_TABLE_NAME + " (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                "table_name VARCHAR(255) NOT NULL, " +
                "record_time TIMESTAMP NOT NULL, " +
                "row_count BIGINT NOT NULL, " +
                "increment BIGINT NOT NULL" +
                ")";
        jdbcTemplate.execute(ddl);
        logger.info("Ensured monitor table exists: {}", MONITOR_TABLE_NAME);
    }

    /**
     * Main task executed based on cron expression from properties.
     */
    @XxlJob("dataIncrementMonitorJob")
    public void monitor() {
        List<String> patterns = parseTablePatterns(properties.getTables());
        if (patterns.isEmpty()) {
            logger.warn("No table patterns configured for monitoring; skipping this run.");
            return;
        }

        Timestamp now = Timestamp.from(Instant.now());

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            for (String rawPattern : patterns) {
                String tablePattern = rawPattern.replace('*', '%');
                try (ResultSet rs = metaData.getTables(conn.getCatalog(), null, tablePattern, new String[]{"TABLE"})) {
                    while (rs.next()) {
                        String tableName = rs.getString("TABLE_NAME");
                        processTable(tableName, now);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Failed during monitor execution", e);
        }
    }

    private void processTable(String tableName, Timestamp recordTime) {
        try {
            Long currentCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
            if (currentCount == null) {
                logger.warn("Count query returned null for table {}", tableName);
                return;
            }

            Long previousCount = getPreviousRowCount(tableName);
            long increment = previousCount == null ? 0L : currentCount - previousCount;

            jdbcTemplate.update(
                    "INSERT INTO " + MONITOR_TABLE_NAME + " (table_name, record_time, row_count, increment) VALUES (?,?,?,?)",
                    tableName, recordTime, currentCount, increment);

            logger.debug("Recorded stats for table {}: currentCount={}, increment={}", tableName, currentCount, increment);
        } catch (Exception e) {
            logger.error("Failed to process table " + tableName, e);
        }
    }

    private Long getPreviousRowCount(String tableName) {
        try {
            String sql = "SELECT row_count FROM " + MONITOR_TABLE_NAME + " WHERE table_name = ? ORDER BY record_time DESC LIMIT 1";
            return jdbcTemplate.queryForObject(sql, new Object[]{tableName}, Long.class);
        } catch (EmptyResultDataAccessException ex) {
            return null;
        }
    }

    private List<String> parseTablePatterns(String tablesProperty) {
        if (tablesProperty == null || tablesProperty.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(tablesProperty.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}