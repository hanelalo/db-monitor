package com.github.starter.dbmonitor.integration;

import com.github.starter.dbmonitor.entity.DbMonitorStatistics;
import com.github.starter.dbmonitor.entity.MonitorConfig;
import com.github.starter.dbmonitor.repository.JdbcDbMonitorStatisticsRepository;
import com.github.starter.dbmonitor.repository.JdbcMonitorConfigRepository;
import com.github.starter.dbmonitor.repository.JdbcTableOperationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JDBC Repository 集成测试
 * 使用H2内存数据库进行测试
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "logging.level.org.springframework.jdbc=DEBUG"
})
@ActiveProfiles("test")
@Transactional
class JdbcRepositoryIntegrationTest {

    @Autowired
    private JdbcMonitorConfigRepository monitorConfigRepository;

    @Autowired
    private JdbcDbMonitorStatisticsRepository statisticsRepository;

    @Autowired
    private JdbcTableOperationRepository tableOperationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // 创建测试表
        createTestTables();
        
        // 创建监控相关表
        monitorConfigRepository.createTableIfNotExists();
        statisticsRepository.createTableIfNotExists();
    }

    private void createTestTables() {
        // 创建测试用的业务表
        jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS test_user_table (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "username VARCHAR(100) NOT NULL, " +
                "email VARCHAR(200), " +
                "created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
            ")"
        );

        jdbcTemplate.execute(
            "CREATE TABLE IF NOT EXISTS test_order_table (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id BIGINT NOT NULL, " +
                "order_no VARCHAR(50) NOT NULL, " +
                "amount DECIMAL(10,2), " +
                "status VARCHAR(20), " +
                "created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
            ")"
        );

        // 插入测试数据
        jdbcTemplate.update(
            "INSERT INTO test_user_table (username, email, created_time) VALUES " +
            "('user1', 'user1@test.com', ?), " +
            "('user2', 'user2@test.com', ?), " +
            "('user3', 'user3@test.com', ?)",
            LocalDateTime.now().minusHours(2),
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().minusMinutes(30)
        );

        jdbcTemplate.update(
            "INSERT INTO test_order_table (user_id, order_no, amount, status, created_time) VALUES " +
            "(1, 'ORD001', 100.00, 'PAID', ?), " +
            "(2, 'ORD002', 200.00, 'PENDING', ?), " +
            "(1, 'ORD003', 150.00, 'PAID', ?)",
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().minusMinutes(45),
            LocalDateTime.now().minusMinutes(15)
        );
    }

    @Test
    void testMonitorConfigRepository_CRUD() {
        // Create
        MonitorConfig config = new MonitorConfig();
        config.setConfigName("test-config");
        config.setDataSourceName("primary");
        config.setTableName("test_user_table");
        config.setTimeColumnName("created_time");
        config.setTimeColumnType("TIMESTAMP");
        config.setEnabled(true);
        config.setIntervalType("MINUTES");
        config.setIntervalValue(10);
        config.setDescription("测试配置");
        config.setCreatedTime(LocalDateTime.now());
        config.setUpdatedTime(LocalDateTime.now());
        config.setCreatedBy("admin");

        MonitorConfig savedConfig = monitorConfigRepository.insert(config);
        assertNotNull(savedConfig.getId());
        assertEquals("test-config", savedConfig.getConfigName());

        // Read
        Optional<MonitorConfig> foundConfig = monitorConfigRepository.findById(savedConfig.getId());
        assertTrue(foundConfig.isPresent());
        assertEquals("test-config", foundConfig.get().getConfigName());

        Optional<MonitorConfig> foundByName = monitorConfigRepository.findByConfigName("test-config");
        assertTrue(foundByName.isPresent());
        assertEquals(savedConfig.getId(), foundByName.get().getId());

        // Update
        savedConfig.setDescription("更新后的描述");
        savedConfig.setEnabled(false);
        boolean updated = monitorConfigRepository.update(savedConfig);
        assertTrue(updated);

        Optional<MonitorConfig> updatedConfig = monitorConfigRepository.findById(savedConfig.getId());
        assertTrue(updatedConfig.isPresent());
        assertEquals("更新后的描述", updatedConfig.get().getDescription());
        assertFalse(updatedConfig.get().getEnabled());

        // Delete
        boolean deleted = monitorConfigRepository.deleteById(savedConfig.getId());
        assertTrue(deleted);

        Optional<MonitorConfig> deletedConfig = monitorConfigRepository.findById(savedConfig.getId());
        assertFalse(deletedConfig.isPresent());
    }

    @Test
    void testMonitorConfigRepository_FindMethods() {
        // 创建多个配置
        MonitorConfig config1 = createTestConfig("config1", "primary", "test_user_table");
        MonitorConfig config2 = createTestConfig("config2", "primary", "test_order_table");
        MonitorConfig config3 = createTestConfig("config3", "secondary", "test_user_table");
        
        config1 = monitorConfigRepository.insert(config1);
        config2 = monitorConfigRepository.insert(config2);
        config3 = monitorConfigRepository.insert(config3);

        // 测试 findAll
        List<MonitorConfig> allConfigs = monitorConfigRepository.findAll();
        assertEquals(3, allConfigs.size());

        // 测试 findAllEnabled
        List<MonitorConfig> enabledConfigs = monitorConfigRepository.findAllEnabled();
        assertEquals(3, enabledConfigs.size());

        // 禁用一个配置
        monitorConfigRepository.updateEnabled(config1.getId(), false, "admin");
        enabledConfigs = monitorConfigRepository.findAllEnabled();
        assertEquals(2, enabledConfigs.size());

        // 测试 findByDataSourceName
        List<MonitorConfig> primaryConfigs = monitorConfigRepository.findByDataSourceName("primary");
        assertEquals(2, primaryConfigs.size());

        // 测试 findByDataSourceNameAndTableName
        Optional<MonitorConfig> specificConfig = monitorConfigRepository
                .findByDataSourceNameAndTableName("primary", "test_user_table");
        assertTrue(specificConfig.isPresent());
        assertEquals("config1", specificConfig.get().getConfigName());
    }

    @Test
    void testStatisticsRepository_CRUD() {
        // Create
        DbMonitorStatistics statistics = new DbMonitorStatistics();
        statistics.setDataSourceName("primary");
        statistics.setTableName("test_user_table");
        statistics.setStatisticTime(LocalDateTime.now());
        statistics.setStartTime(LocalDateTime.now().minusHours(1));
        statistics.setEndTime(LocalDateTime.now());
        statistics.setIncrementCount(10L);
        statistics.setEstimatedDiskSizeBytes(2560L);
        statistics.setAvgRowSizeBytes(256L);
        statistics.setIntervalType("MINUTES");
        statistics.setIntervalValue(10);
        statistics.setCreatedTime(LocalDateTime.now());
        statistics.setAdditionalInfo("测试统计");

        DbMonitorStatistics savedStatistics = statisticsRepository.insert(statistics);
        assertNotNull(savedStatistics.getId());
        assertEquals("primary", savedStatistics.getDataSourceName());

        // Read
        Optional<DbMonitorStatistics> foundStatistics = statisticsRepository.findById(savedStatistics.getId());
        assertTrue(foundStatistics.isPresent());
        assertEquals(10L, foundStatistics.get().getIncrementCount());

        // Update
        savedStatistics.setIncrementCount(20L);
        boolean updated = statisticsRepository.update(savedStatistics);
        assertTrue(updated);

        Optional<DbMonitorStatistics> updatedStatistics = statisticsRepository.findById(savedStatistics.getId());
        assertTrue(updatedStatistics.isPresent());
        assertEquals(20L, updatedStatistics.get().getIncrementCount());

        // Delete
        boolean deleted = statisticsRepository.deleteById(savedStatistics.getId());
        assertTrue(deleted);
    }

    @Test
    void testStatisticsRepository_QueryMethods() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneHourAgo = now.minusHours(1);
        LocalDateTime twoHoursAgo = now.minusHours(2);

        // 创建测试数据
        DbMonitorStatistics stats1 = createTestStatistics("primary", "test_user_table", oneHourAgo);
        DbMonitorStatistics stats2 = createTestStatistics("primary", "test_order_table", now);
        DbMonitorStatistics stats3 = createTestStatistics("secondary", "test_user_table", twoHoursAgo);

        statisticsRepository.insert(stats1);
        statisticsRepository.insert(stats2);
        statisticsRepository.insert(stats3);

        // 测试 findByTimeRange
        List<DbMonitorStatistics> recentStats = statisticsRepository.findByTimeRange(oneHourAgo, now);
        assertEquals(2, recentStats.size());

        // 测试 findByDataSourceAndTable
        List<DbMonitorStatistics> userTableStats = statisticsRepository
                .findByDataSourceAndTable("primary", "test_user_table");
        assertEquals(1, userTableStats.size());

        // 测试 findLatestByDataSourceAndTable
        Optional<DbMonitorStatistics> latestStats = statisticsRepository
                .findLatestByDataSourceAndTable("primary", "test_user_table");
        assertTrue(latestStats.isPresent());

        // 测试 count
        long totalCount = statisticsRepository.count();
        assertEquals(3, totalCount);

        long userTableCount = statisticsRepository.countByDataSourceAndTable("primary", "test_user_table");
        assertEquals(1, userTableCount);
    }

    @Test
    void testTableOperationRepository() {
        // 测试 getAllTableNames
        List<String> tableNames = tableOperationRepository.getAllTableNames();
        assertTrue(tableNames.size() >= 2); // 至少包含我们创建的测试表
        assertTrue(tableNames.contains("TEST_USER_TABLE") || tableNames.contains("test_user_table"));

        // 测试 checkTableExists
        assertTrue(tableOperationRepository.checkTableExists("test_user_table"));
        assertFalse(tableOperationRepository.checkTableExists("non_existent_table"));

        // 测试 getTableColumns
        List<String> columns = tableOperationRepository.getTableColumns("test_user_table");
        assertTrue(columns.size() >= 4);
        assertTrue(columns.contains("ID") || columns.contains("id"));
        assertTrue(columns.contains("USERNAME") || columns.contains("username"));

        // 测试 checkColumnExists
        assertTrue(tableOperationRepository.checkColumnExists("test_user_table", "username"));
        assertFalse(tableOperationRepository.checkColumnExists("test_user_table", "non_existent_column"));

        // 测试 detectTimeColumns
        List<String> timeColumns = tableOperationRepository.detectTimeColumns("test_user_table");
        assertTrue(timeColumns.size() >= 1);

        // 测试 queryTableIncrement
        LocalDateTime startTime = LocalDateTime.now().minusHours(3);
        LocalDateTime endTime = LocalDateTime.now();
        Long incrementCount = tableOperationRepository.queryTableIncrement(
                "test_user_table", "created_time", startTime, endTime);
        assertTrue(incrementCount >= 0);

        // 测试 testConnection
        assertTrue(tableOperationRepository.testConnection());

        // 测试 getDatabaseType
        String dbType = tableOperationRepository.getDatabaseType();
        assertEquals("h2", dbType);
    }

    @Test
    void testBatchOperations() {
        // 测试批量插入统计数据
        List<DbMonitorStatistics> statisticsList = Arrays.asList(
                createTestStatistics("primary", "test_user_table", LocalDateTime.now().minusHours(1)),
                createTestStatistics("primary", "test_order_table", LocalDateTime.now().minusMinutes(30)),
                createTestStatistics("secondary", "test_user_table", LocalDateTime.now())
        );

        int insertedCount = statisticsRepository.batchInsert(statisticsList);
        assertEquals(3, insertedCount);

        // 验证数据已插入
        long totalCount = statisticsRepository.count();
        assertEquals(3, totalCount);

        // 测试批量更新配置状态
        MonitorConfig config1 = createTestConfig("batch-config1", "primary", "test_user_table");
        MonitorConfig config2 = createTestConfig("batch-config2", "primary", "test_order_table");
        
        config1 = monitorConfigRepository.insert(config1);
        config2 = monitorConfigRepository.insert(config2);

        List<Long> configIds = Arrays.asList(config1.getId(), config2.getId());
        int updatedCount = monitorConfigRepository.batchUpdateEnabled(configIds, false, "admin");
        assertEquals(2, updatedCount);

        // 验证状态已更新
        List<MonitorConfig> enabledConfigs = monitorConfigRepository.findAllEnabled();
        assertEquals(0, enabledConfigs.size());
    }

    private MonitorConfig createTestConfig(String name, String dataSource, String tableName) {
        MonitorConfig config = new MonitorConfig();
        config.setConfigName(name);
        config.setDataSourceName(dataSource);
        config.setTableName(tableName);
        config.setTimeColumnName("created_time");
        config.setTimeColumnType("TIMESTAMP");
        config.setEnabled(true);
        config.setIntervalType("MINUTES");
        config.setIntervalValue(10);
        config.setDescription("测试配置: " + name);
        config.setCreatedTime(LocalDateTime.now());
        config.setUpdatedTime(LocalDateTime.now());
        config.setCreatedBy("admin");
        return config;
    }

    private DbMonitorStatistics createTestStatistics(String dataSource, String tableName, LocalDateTime statisticTime) {
        DbMonitorStatistics statistics = new DbMonitorStatistics();
        statistics.setDataSourceName(dataSource);
        statistics.setTableName(tableName);
        statistics.setStatisticTime(statisticTime);
        statistics.setStartTime(statisticTime.minusMinutes(10));
        statistics.setEndTime(statisticTime);
        statistics.setIncrementCount(5L);
        statistics.setEstimatedDiskSizeBytes(1280L);
        statistics.setAvgRowSizeBytes(256L);
        statistics.setIntervalType("MINUTES");
        statistics.setIntervalValue(10);
        statistics.setCreatedTime(LocalDateTime.now());
        statistics.setAdditionalInfo("测试统计数据");
        return statistics;
    }
}
