package com.github.starter.dbmonitor.service;

import com.github.starter.dbmonitor.entity.MonitorConfig;
import com.github.starter.dbmonitor.repository.JdbcMonitorConfigRepository;
import com.github.starter.dbmonitor.repository.JdbcTableOperationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * MonitorConfigService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class MonitorConfigServiceTest {

    @Mock
    private JdbcMonitorConfigRepository monitorConfigRepository;

    @Mock
    private DataSourceService dataSourceService;

    @Mock
    private JdbcTableOperationRepository tableOperationRepository;

    @Mock
    private DatabaseSecurityService databaseSecurityService;

    @InjectMocks
    private MonitorConfigService monitorConfigService;

    private MonitorConfig testConfig;

    @BeforeEach
    void setUp() {
        testConfig = new MonitorConfig();
        testConfig.setId(1L);
        testConfig.setConfigName("test-config");
        testConfig.setDataSourceName("primary");
        testConfig.setTableName("test_table");
        testConfig.setTimeColumnName("created_time");
        testConfig.setTimeColumnType("DATETIME");
        testConfig.setEnabled(true);
        testConfig.setIntervalType("MINUTES");
        testConfig.setIntervalValue(10);
        testConfig.setDescription("测试配置");
        testConfig.setCreatedTime(LocalDateTime.now());
        testConfig.setUpdatedTime(LocalDateTime.now());
        testConfig.setCreatedBy("admin");
        testConfig.setUpdatedBy("admin");
    }

    @Test
    void testCreateConfig_Success() {
        // Given
        when(monitorConfigRepository.findByConfigName("test-config")).thenReturn(Optional.empty());
        when(monitorConfigRepository.findByDataSourceNameAndTableName("primary", "test_table"))
                .thenReturn(Optional.empty());
        when(tableOperationRepository.checkTableExists("test_table")).thenReturn(true);
        when(tableOperationRepository.checkColumnExists("test_table", "created_time")).thenReturn(true);
        when(monitorConfigRepository.insert(any(MonitorConfig.class))).thenReturn(testConfig);

        // When
        MonitorConfig result = monitorConfigService.createConfig(testConfig);

        // Then
        assertNotNull(result);
        assertEquals("test-config", result.getConfigName());
        verify(monitorConfigRepository).insert(any(MonitorConfig.class));
        verify(databaseSecurityService).sanitizeDataSourceName("primary");
        verify(databaseSecurityService).sanitizeTableName("test_table");
        verify(databaseSecurityService).sanitizeColumnName("created_time");
    }

    @Test
    void testCreateConfig_DuplicateConfigName() {
        // Given
        when(monitorConfigRepository.findByConfigName("test-config")).thenReturn(Optional.of(testConfig));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> monitorConfigService.createConfig(testConfig));
        assertEquals("配置名称已存在: test-config", exception.getMessage());
        verify(monitorConfigRepository, never()).insert(any(MonitorConfig.class));
    }

    @Test
    void testCreateConfig_DuplicateDataSourceAndTable() {
        // Given
        when(monitorConfigRepository.findByConfigName("test-config")).thenReturn(Optional.empty());
        when(monitorConfigRepository.findByDataSourceNameAndTableName("primary", "test_table"))
                .thenReturn(Optional.of(testConfig));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> monitorConfigService.createConfig(testConfig));
        assertEquals("数据源 primary 的表 test_table 已存在监控配置", exception.getMessage());
        verify(monitorConfigRepository, never()).insert(any(MonitorConfig.class));
    }

    @Test
    void testCreateConfig_TableNotExists() {
        // Given
        when(monitorConfigRepository.findByConfigName("test-config")).thenReturn(Optional.empty());
        when(monitorConfigRepository.findByDataSourceNameAndTableName("primary", "test_table"))
                .thenReturn(Optional.empty());
        when(tableOperationRepository.checkTableExists("test_table")).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> monitorConfigService.createConfig(testConfig));
        assertEquals("表不存在: test_table", exception.getMessage());
        verify(monitorConfigRepository, never()).insert(any(MonitorConfig.class));
    }

    @Test
    void testCreateConfig_ColumnNotExists() {
        // Given
        when(monitorConfigRepository.findByConfigName("test-config")).thenReturn(Optional.empty());
        when(monitorConfigRepository.findByDataSourceNameAndTableName("primary", "test_table"))
                .thenReturn(Optional.empty());
        when(tableOperationRepository.checkTableExists("test_table")).thenReturn(true);
        when(tableOperationRepository.checkColumnExists("test_table", "created_time")).thenReturn(false);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> monitorConfigService.createConfig(testConfig));
        assertEquals("时间字段不存在: created_time", exception.getMessage());
        verify(monitorConfigRepository, never()).insert(any(MonitorConfig.class));
    }

    @Test
    void testUpdateConfig_Success() {
        // Given
        when(monitorConfigRepository.findById(1L)).thenReturn(Optional.of(testConfig));
        when(monitorConfigRepository.findByConfigName("test-config")).thenReturn(Optional.of(testConfig));
        when(tableOperationRepository.checkTableExists("test_table")).thenReturn(true);
        when(tableOperationRepository.checkColumnExists("test_table", "created_time")).thenReturn(true);
        when(monitorConfigRepository.update(any(MonitorConfig.class))).thenReturn(true);

        // When
        boolean result = monitorConfigService.updateConfig(testConfig);

        // Then
        assertTrue(result);
        verify(monitorConfigRepository).update(any(MonitorConfig.class));
    }

    @Test
    void testUpdateConfig_NotExists() {
        // Given
        when(monitorConfigRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> monitorConfigService.updateConfig(testConfig));
        assertEquals("监控配置不存在: 1", exception.getMessage());
        verify(monitorConfigRepository, never()).update(any(MonitorConfig.class));
    }

    @Test
    void testGetConfigById() {
        // Given
        when(monitorConfigRepository.findById(1L)).thenReturn(Optional.of(testConfig));

        // When
        Optional<MonitorConfig> result = monitorConfigService.getConfigById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        verify(monitorConfigRepository).findById(1L);
    }

    @Test
    void testGetConfigByName() {
        // Given
        when(monitorConfigRepository.findByConfigName("test-config")).thenReturn(Optional.of(testConfig));

        // When
        Optional<MonitorConfig> result = monitorConfigService.getConfigByName("test-config");

        // Then
        assertTrue(result.isPresent());
        assertEquals("test-config", result.get().getConfigName());
        verify(monitorConfigRepository).findByConfigName("test-config");
    }

    @Test
    void testGetAllConfigs() {
        // Given
        List<MonitorConfig> configs = Arrays.asList(testConfig);
        when(monitorConfigRepository.findAll()).thenReturn(configs);

        // When
        List<MonitorConfig> result = monitorConfigService.getAllConfigs();

        // Then
        assertEquals(1, result.size());
        assertEquals("test-config", result.get(0).getConfigName());
        verify(monitorConfigRepository).findAll();
    }

    @Test
    void testGetEnabledConfigs() {
        // Given
        List<MonitorConfig> configs = Arrays.asList(testConfig);
        when(monitorConfigRepository.findAllEnabled()).thenReturn(configs);

        // When
        List<MonitorConfig> result = monitorConfigService.getEnabledConfigs();

        // Then
        assertEquals(1, result.size());
        assertTrue(result.get(0).getEnabled());
        verify(monitorConfigRepository).findAllEnabled();
    }

    @Test
    void testDeleteConfig() {
        // Given
        when(monitorConfigRepository.deleteById(1L)).thenReturn(true);

        // When
        boolean result = monitorConfigService.deleteConfig(1L);

        // Then
        assertTrue(result);
        verify(monitorConfigRepository).deleteById(1L);
    }

    @Test
    void testEnableConfig() {
        // Given
        when(monitorConfigRepository.updateEnabled(1L, true, "admin")).thenReturn(true);

        // When
        boolean result = monitorConfigService.enableConfig(1L, "admin");

        // Then
        assertTrue(result);
        verify(monitorConfigRepository).updateEnabled(1L, true, "admin");
    }

    @Test
    void testDisableConfig() {
        // Given
        when(monitorConfigRepository.updateEnabled(1L, false, "admin")).thenReturn(true);

        // When
        boolean result = monitorConfigService.disableConfig(1L, "admin");

        // Then
        assertTrue(result);
        verify(monitorConfigRepository).updateEnabled(1L, false, "admin");
    }

    @Test
    void testGetTableColumns() {
        // Given
        List<String> columns = Arrays.asList("id", "name", "created_time");
        when(tableOperationRepository.getTableColumns("test_table")).thenReturn(columns);

        // When
        List<String> result = monitorConfigService.getTableColumns("primary", "test_table");

        // Then
        assertEquals(3, result.size());
        assertEquals("id", result.get(0));
        verify(tableOperationRepository).getTableColumns("test_table");
        verify(databaseSecurityService).sanitizeDataSourceName("primary");
        verify(databaseSecurityService).sanitizeTableName("test_table");
    }

    @Test
    void testDetectTimeColumns() {
        // Given
        List<String> timeColumns = Arrays.asList("created_time", "updated_time");
        when(tableOperationRepository.detectTimeColumns("test_table")).thenReturn(timeColumns);

        // When
        List<String> result = monitorConfigService.detectTimeColumns("primary", "test_table");

        // Then
        assertEquals(2, result.size());
        assertEquals("created_time", result.get(0));
        verify(tableOperationRepository).detectTimeColumns("test_table");
        verify(databaseSecurityService).sanitizeDataSourceName("primary");
        verify(databaseSecurityService).sanitizeTableName("test_table");
    }

    @Test
    void testGetConfigByDataSourceAndTable() {
        // Given
        when(monitorConfigRepository.findByDataSourceNameAndTableName("primary", "test_table"))
                .thenReturn(Optional.of(testConfig));

        // When
        Optional<MonitorConfig> result = monitorConfigService.getConfigByDataSourceAndTable("primary", "test_table");

        // Then
        assertTrue(result.isPresent());
        assertEquals("primary", result.get().getDataSourceName());
        assertEquals("test_table", result.get().getTableName());
        verify(monitorConfigRepository).findByDataSourceNameAndTableName("primary", "test_table");
    }

    @Test
    void testValidateConfig_EmptyConfigName() {
        // Given
        testConfig.setConfigName("");

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> monitorConfigService.createConfig(testConfig));
        assertEquals("配置名称不能为空", exception.getMessage());
    }

    @Test
    void testValidateConfig_EmptyDataSourceName() {
        // Given
        testConfig.setDataSourceName("");

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> monitorConfigService.createConfig(testConfig));
        assertEquals("数据源名称不能为空", exception.getMessage());
    }

    @Test
    void testValidateConfig_EmptyTableName() {
        // Given
        testConfig.setTableName("");

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> monitorConfigService.createConfig(testConfig));
        assertEquals("表名不能为空", exception.getMessage());
    }

    @Test
    void testValidateConfig_EmptyTimeColumnName() {
        // Given
        testConfig.setTimeColumnName("");

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
                () -> monitorConfigService.createConfig(testConfig));
        assertEquals("时间字段名称不能为空", exception.getMessage());
    }
}
