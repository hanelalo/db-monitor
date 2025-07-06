package com.github.starter.dbmonitor.repository;

import com.github.starter.dbmonitor.entity.MonitorConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * JdbcMonitorConfigRepository 单元测试
 */
@ExtendWith(MockitoExtension.class)
class JdbcMonitorConfigRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private JdbcMonitorConfigRepository repository;

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
    void testCreateTableIfNotExists() {
        // When
        repository.createTableIfNotExists();

        // Then
        verify(jdbcTemplate).execute(anyString());
    }

    @Test
    void testInsert() {
        // Given
        KeyHolder keyHolder = new GeneratedKeyHolder();
        when(jdbcTemplate.update(any(), any(KeyHolder.class))).thenReturn(1);

        // When
        MonitorConfig result = repository.insert(testConfig);

        // Then
        assertNotNull(result);
        verify(jdbcTemplate).update(any(), any(KeyHolder.class));
    }

    @Test
    void testUpdate() {
        // Given
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

        // When
        boolean result = repository.update(testConfig);

        // Then
        assertTrue(result);
        verify(jdbcTemplate).update(anyString(), any(Object[].class));
    }

    @Test
    void testFindById_Found() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(1L)))
                .thenReturn(testConfig);

        // When
        Optional<MonitorConfig> result = repository.findById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testConfig.getId(), result.get().getId());
        verify(jdbcTemplate).queryForObject(anyString(), any(RowMapper.class), eq(1L));
    }

    @Test
    void testFindById_NotFound() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(1L)))
                .thenThrow(new EmptyResultDataAccessException(1));

        // When
        Optional<MonitorConfig> result = repository.findById(1L);

        // Then
        assertFalse(result.isPresent());
        verify(jdbcTemplate).queryForObject(anyString(), any(RowMapper.class), eq(1L));
    }

    @Test
    void testFindByConfigName_Found() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq("test-config")))
                .thenReturn(testConfig);

        // When
        Optional<MonitorConfig> result = repository.findByConfigName("test-config");

        // Then
        assertTrue(result.isPresent());
        assertEquals(testConfig.getConfigName(), result.get().getConfigName());
        verify(jdbcTemplate).queryForObject(anyString(), any(RowMapper.class), eq("test-config"));
    }

    @Test
    void testFindAll() {
        // Given
        List<MonitorConfig> configs = Arrays.asList(testConfig);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(configs);

        // When
        List<MonitorConfig> result = repository.findAll();

        // Then
        assertEquals(1, result.size());
        assertEquals(testConfig.getId(), result.get(0).getId());
        verify(jdbcTemplate).query(anyString(), any(RowMapper.class));
    }

    @Test
    void testFindAllEnabled() {
        // Given
        List<MonitorConfig> configs = Arrays.asList(testConfig);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(configs);

        // When
        List<MonitorConfig> result = repository.findAllEnabled();

        // Then
        assertEquals(1, result.size());
        assertTrue(result.get(0).getEnabled());
        verify(jdbcTemplate).query(anyString(), any(RowMapper.class));
    }

    @Test
    void testFindByDataSourceName() {
        // Given
        List<MonitorConfig> configs = Arrays.asList(testConfig);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("primary")))
                .thenReturn(configs);

        // When
        List<MonitorConfig> result = repository.findByDataSourceName("primary");

        // Then
        assertEquals(1, result.size());
        assertEquals("primary", result.get(0).getDataSourceName());
        verify(jdbcTemplate).query(anyString(), any(RowMapper.class), eq("primary"));
    }

    @Test
    void testFindByDataSourceNameAndTableName_Found() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq("primary"), eq("test_table")))
                .thenReturn(testConfig);

        // When
        Optional<MonitorConfig> result = repository.findByDataSourceNameAndTableName("primary", "test_table");

        // Then
        assertTrue(result.isPresent());
        assertEquals("primary", result.get().getDataSourceName());
        assertEquals("test_table", result.get().getTableName());
        verify(jdbcTemplate).queryForObject(anyString(), any(RowMapper.class), eq("primary"), eq("test_table"));
    }

    @Test
    void testDeleteById() {
        // Given
        when(jdbcTemplate.update(anyString(), eq(1L))).thenReturn(1);

        // When
        boolean result = repository.deleteById(1L);

        // Then
        assertTrue(result);
        verify(jdbcTemplate).update(anyString(), eq(1L));
    }

    @Test
    void testUpdateEnabled() {
        // Given
        when(jdbcTemplate.update(anyString(), eq(true), eq("admin"), any(LocalDateTime.class), eq(1L)))
                .thenReturn(1);

        // When
        boolean result = repository.updateEnabled(1L, true, "admin");

        // Then
        assertTrue(result);
        verify(jdbcTemplate).update(anyString(), eq(true), eq("admin"), any(LocalDateTime.class), eq(1L));
    }

    @Test
    void testBatchUpdateEnabled() {
        // Given
        List<Long> ids = Arrays.asList(1L, 2L, 3L);
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(3);

        // When
        int result = repository.batchUpdateEnabled(ids, true, "admin");

        // Then
        assertEquals(3, result);
        verify(jdbcTemplate).update(anyString(), any(Object[].class));
    }

    @Test
    void testBatchUpdateEnabled_EmptyIds() {
        // When
        int result = repository.batchUpdateEnabled(Arrays.asList(), true, "admin");

        // Then
        assertEquals(0, result);
        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }

    @Test
    void testBatchUpdateEnabled_NullIds() {
        // When
        int result = repository.batchUpdateEnabled(null, true, "admin");

        // Then
        assertEquals(0, result);
        verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
    }
}
