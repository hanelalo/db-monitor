package com.github.starter.dbmonitor.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 配置测试
 */
@SpringBootTest(classes = ConfigurationTest.TestConfig.class)
@TestPropertySource(properties = {
    "db.monitor.enabled=true",
    "db.monitor.data-source-name=primary",
    "db.monitor.config-data-source-name=monitor",
    "db.monitor.config-table.table-name=custom_monitor_config",
    "db.monitor.config-table.auto-create=true",
    "db.monitor.config-table.data-source-name=config_db",
    "db.monitor.monitor-table.table-name=custom_monitor_stats",
    "db.monitor.monitor-table.auto-create=false"
})
class ConfigurationTest {

    @Autowired
    private DbMonitorProperties properties;

    @Test
    void testBasicConfiguration() {
        assertTrue(properties.isEnabled());
        assertEquals("primary", properties.getDataSourceName());
        assertEquals("monitor", properties.getConfigDataSourceName());
    }

    @Test
    void testConfigTableConfiguration() {
        DbMonitorProperties.ConfigTable configTable = properties.getConfigTable();
        assertNotNull(configTable);
        assertEquals("custom_monitor_config", configTable.getTableName());
        assertTrue(configTable.isAutoCreate());
        assertEquals("config_db", configTable.getDataSourceName());
    }

    @Test
    void testMonitorTableConfiguration() {
        DbMonitorProperties.MonitorTable monitorTable = properties.getMonitorTable();
        assertNotNull(monitorTable);
        assertEquals("custom_monitor_stats", monitorTable.getTableName());
        assertFalse(monitorTable.isAutoCreate());
    }

    @EnableConfigurationProperties(DbMonitorProperties.class)
    static class TestConfig {
    }
}
