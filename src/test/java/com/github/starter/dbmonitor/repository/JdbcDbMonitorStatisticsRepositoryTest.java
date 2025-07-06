package com.github.starter.dbmonitor.repository;

import com.github.starter.dbmonitor.entity.DbMonitorStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;
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
 * JdbcDbMonitorStatisticsRepository 单元测试
 */
@ExtendWith(MockitoExtension.class)
class JdbcDbMonitorStatisticsRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private JdbcDbMonitorStatisticsRepository repository;

    private DbMonitorStatistics testStatistics;

    @BeforeEach
    void setUp() {
        testStatistics = new DbMonitorStatistics();
        testStatistics.setId(1L);
        testStatistics.setDataSourceName("primary");
        testStatistics.setTableName("test_table");
        testStatistics.setStatisticTime(LocalDateTime.now());
        testStatistics.setStartTime(LocalDateTime.now().minusHours(1));
        testStatistics.setEndTime(LocalDateTime.now());
        testStatistics.setIncrementCount(100L);
        testStatistics.setEstimatedDiskSizeBytes(25600L);
        testStatistics.setAvgRowSizeBytes(256L);
        testStatistics.setIntervalType("MINUTES");
        testStatistics.setIntervalValue(10);
        testStatistics.setCreatedTime(LocalDateTime.now());
        testStatistics.setAdditionalInfo("test info");
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
        DbMonitorStatistics result = repository.insert(testStatistics);

        // Then
        assertNotNull(result);
        verify(jdbcTemplate).update(any(), any(KeyHolder.class));
    }

    @Test
    void testBatchInsert() {
        // Given
        List<DbMonitorStatistics> statisticsList = Arrays.asList(testStatistics);

        // 使用doReturn来避免泛型问题
        doReturn(new int[]{1}).when(jdbcTemplate).batchUpdate(anyString(), eq(statisticsList), eq(1), any());

        // When
        int result = repository.batchInsert(statisticsList);

        // Then
        assertEquals(1, result);
        verify(jdbcTemplate).batchUpdate(anyString(), eq(statisticsList), eq(1), any());
    }

    @Test
    void testBatchInsert_EmptyList() {
        // When
        int result = repository.batchInsert(Arrays.asList());

        // Then
        assertEquals(0, result);
        verify(jdbcTemplate, never()).batchUpdate(anyString(), anyList(), anyInt(), 
                any(ParameterizedPreparedStatementSetter.class));
    }

    @Test
    void testBatchInsert_NullList() {
        // When
        int result = repository.batchInsert(null);

        // Then
        assertEquals(0, result);
        verify(jdbcTemplate, never()).batchUpdate(anyString(), anyList(), anyInt(), 
                any(ParameterizedPreparedStatementSetter.class));
    }

    @Test
    void testFindById_Found() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(1L)))
                .thenReturn(testStatistics);

        // When
        Optional<DbMonitorStatistics> result = repository.findById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testStatistics.getId(), result.get().getId());
        verify(jdbcTemplate).queryForObject(anyString(), any(RowMapper.class), eq(1L));
    }

    @Test
    void testFindById_NotFound() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq(1L)))
                .thenThrow(new EmptyResultDataAccessException(1));

        // When
        Optional<DbMonitorStatistics> result = repository.findById(1L);

        // Then
        assertFalse(result.isPresent());
        verify(jdbcTemplate).queryForObject(anyString(), any(RowMapper.class), eq(1L));
    }

    @Test
    void testFindByTimeRange() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().minusHours(2);
        LocalDateTime endTime = LocalDateTime.now();
        List<DbMonitorStatistics> expectedList = Arrays.asList(testStatistics);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(startTime), eq(endTime)))
                .thenReturn(expectedList);

        // When
        List<DbMonitorStatistics> result = repository.findByTimeRange(startTime, endTime);

        // Then
        assertEquals(1, result.size());
        assertEquals(testStatistics.getId(), result.get(0).getId());
        verify(jdbcTemplate).query(anyString(), any(RowMapper.class), eq(startTime), eq(endTime));
    }

    @Test
    void testFindByDataSourceAndTable() {
        // Given
        List<DbMonitorStatistics> expectedList = Arrays.asList(testStatistics);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("primary"), eq("test_table")))
                .thenReturn(expectedList);

        // When
        List<DbMonitorStatistics> result = repository.findByDataSourceAndTable("primary", "test_table");

        // Then
        assertEquals(1, result.size());
        assertEquals("primary", result.get(0).getDataSourceName());
        assertEquals("test_table", result.get(0).getTableName());
        verify(jdbcTemplate).query(anyString(), any(RowMapper.class), eq("primary"), eq("test_table"));
    }

    @Test
    void testFindByDataSourceAndTableAndTimeRange() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().minusHours(2);
        LocalDateTime endTime = LocalDateTime.now();
        List<DbMonitorStatistics> expectedList = Arrays.asList(testStatistics);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), 
                eq("primary"), eq("test_table"), eq(startTime), eq(endTime)))
                .thenReturn(expectedList);

        // When
        List<DbMonitorStatistics> result = repository.findByDataSourceAndTableAndTimeRange(
                "primary", "test_table", startTime, endTime);

        // Then
        assertEquals(1, result.size());
        assertEquals("primary", result.get(0).getDataSourceName());
        assertEquals("test_table", result.get(0).getTableName());
        verify(jdbcTemplate).query(anyString(), any(RowMapper.class), 
                eq("primary"), eq("test_table"), eq(startTime), eq(endTime));
    }

    @Test
    void testFindLatestByDataSourceAndTable_Found() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq("primary"), eq("test_table")))
                .thenReturn(testStatistics);

        // When
        Optional<DbMonitorStatistics> result = repository.findLatestByDataSourceAndTable("primary", "test_table");

        // Then
        assertTrue(result.isPresent());
        assertEquals("primary", result.get().getDataSourceName());
        assertEquals("test_table", result.get().getTableName());
        verify(jdbcTemplate).queryForObject(anyString(), any(RowMapper.class), eq("primary"), eq("test_table"));
    }

    @Test
    void testFindLatestByDataSourceAndTable_NotFound() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), any(RowMapper.class), eq("primary"), eq("test_table")))
                .thenThrow(new EmptyResultDataAccessException(1));

        // When
        Optional<DbMonitorStatistics> result = repository.findLatestByDataSourceAndTable("primary", "test_table");

        // Then
        assertFalse(result.isPresent());
        verify(jdbcTemplate).queryForObject(anyString(), any(RowMapper.class), eq("primary"), eq("test_table"));
    }

    @Test
    void testDeleteByCreatedTimeBefore() {
        // Given
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(30);
        when(jdbcTemplate.update(anyString(), eq(cutoffTime))).thenReturn(5);

        // When
        int result = repository.deleteByCreatedTimeBefore(cutoffTime);

        // Then
        assertEquals(5, result);
        verify(jdbcTemplate).update(anyString(), eq(cutoffTime));
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
    void testUpdate() {
        // Given
        when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

        // When
        boolean result = repository.update(testStatistics);

        // Then
        assertTrue(result);
        verify(jdbcTemplate).update(anyString(), any(Object[].class));
    }

    @Test
    void testCount() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class))).thenReturn(100L);

        // When
        long result = repository.count();

        // Then
        assertEquals(100L, result);
        verify(jdbcTemplate).queryForObject(anyString(), eq(Long.class));
    }

    @Test
    void testCountByDataSourceAndTable() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("primary"), eq("test_table")))
                .thenReturn(50L);

        // When
        long result = repository.countByDataSourceAndTable("primary", "test_table");

        // Then
        assertEquals(50L, result);
        verify(jdbcTemplate).queryForObject(anyString(), eq(Long.class), eq("primary"), eq("test_table"));
    }
}
