package com.github.starter.dbmonitor.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * JdbcTableOperationRepository 单元测试
 */
@ExtendWith(MockitoExtension.class)
class JdbcTableOperationRepositoryTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData metaData;

    @InjectMocks
    private JdbcTableOperationRepository repository;

    @BeforeEach
    void setUp() throws Exception {
        when(jdbcTemplate.getDataSource()).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
    }

    @Test
    void testGetAllTableNames_Success() {
        // Given
        List<String> expectedTables = Arrays.asList("table1", "table2", "table3");
        when(jdbcTemplate.queryForList(anyString(), eq(String.class))).thenReturn(expectedTables);

        // When
        List<String> result = repository.getAllTableNames();

        // Then
        assertEquals(3, result.size());
        assertEquals(expectedTables, result);
        verify(jdbcTemplate).queryForList(anyString(), eq(String.class));
    }

    @Test
    void testGetAllTableNames_FallbackToShowTables() {
        // Given
        List<String> expectedTables = Arrays.asList("table1", "table2");
        when(jdbcTemplate.queryForList(contains("information_schema"), eq(String.class)))
                .thenThrow(new RuntimeException("Information schema not available"));
        when(jdbcTemplate.queryForList("SHOW TABLES", String.class)).thenReturn(expectedTables);

        // When
        List<String> result = repository.getAllTableNames();

        // Then
        assertEquals(2, result.size());
        assertEquals(expectedTables, result);
        verify(jdbcTemplate).queryForList(contains("information_schema"), eq(String.class));
        verify(jdbcTemplate).queryForList("SHOW TABLES", String.class);
    }

    @Test
    void testCheckTableExists_True() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("test_table")))
                .thenReturn(1);

        // When
        boolean result = repository.checkTableExists("test_table");

        // Then
        assertTrue(result);
        verify(jdbcTemplate).queryForObject(anyString(), eq(Integer.class), eq("test_table"));
    }

    @Test
    void testCheckTableExists_False() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("test_table")))
                .thenReturn(0);

        // When
        boolean result = repository.checkTableExists("test_table");

        // Then
        assertFalse(result);
        verify(jdbcTemplate).queryForObject(anyString(), eq(Integer.class), eq("test_table"));
    }

    @Test
    void testCheckTableExists_FallbackToDirectQuery() {
        // Given
        when(jdbcTemplate.queryForObject(contains("information_schema"), eq(Integer.class), eq("test_table")))
                .thenThrow(new RuntimeException("Information schema not available"));
        when(jdbcTemplate.queryForObject("SELECT 1 FROM test_table LIMIT 1", Integer.class))
                .thenReturn(1);

        // When
        boolean result = repository.checkTableExists("test_table");

        // Then
        assertTrue(result);
        verify(jdbcTemplate).queryForObject(contains("information_schema"), eq(Integer.class), eq("test_table"));
        verify(jdbcTemplate).queryForObject("SELECT 1 FROM test_table LIMIT 1", Integer.class);
    }

    @Test
    void testGetTableColumns() {
        // Given
        List<String> expectedColumns = Arrays.asList("id", "name", "created_time");
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq("test_table")))
                .thenReturn(expectedColumns);

        // When
        List<String> result = repository.getTableColumns("test_table");

        // Then
        assertEquals(3, result.size());
        assertEquals(expectedColumns, result);
        verify(jdbcTemplate).queryForList(anyString(), eq(String.class), eq("test_table"));
    }

    @Test
    void testGetTableColumnDetails() {
        // Given
        Map<String, Object> column1 = new HashMap<>();
        column1.put("column_name", "id");
        column1.put("data_type", "bigint");
        column1.put("is_nullable", "NO");

        Map<String, Object> column2 = new HashMap<>();
        column2.put("column_name", "name");
        column2.put("data_type", "varchar");
        column2.put("is_nullable", "YES");

        List<Map<String, Object>> expectedColumns = Arrays.asList(column1, column2);
        when(jdbcTemplate.queryForList(anyString(), eq("test_table"))).thenReturn(expectedColumns);

        // When
        List<Map<String, Object>> result = repository.getTableColumnDetails("test_table");

        // Then
        assertEquals(2, result.size());
        assertEquals("id", result.get(0).get("column_name"));
        assertEquals("name", result.get(1).get("column_name"));
        verify(jdbcTemplate).queryForList(anyString(), eq("test_table"));
    }

    @Test
    void testDetectTimeColumns() {
        // Given
        List<String> expectedTimeColumns = Arrays.asList("created_time", "updated_time");
        when(jdbcTemplate.queryForList(anyString(), eq(String.class), eq("test_table")))
                .thenReturn(expectedTimeColumns);

        // When
        List<String> result = repository.detectTimeColumns("test_table");

        // Then
        assertEquals(2, result.size());
        assertEquals(expectedTimeColumns, result);
        verify(jdbcTemplate).queryForList(anyString(), eq(String.class), eq("test_table"));
    }

    @Test
    void testCheckColumnExists_True() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("test_table"), eq("created_time")))
                .thenReturn(1);

        // When
        boolean result = repository.checkColumnExists("test_table", "created_time");

        // Then
        assertTrue(result);
        verify(jdbcTemplate).queryForObject(anyString(), eq(Integer.class), eq("test_table"), eq("created_time"));
    }

    @Test
    void testCheckColumnExists_False() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq("test_table"), eq("non_existent")))
                .thenReturn(0);

        // When
        boolean result = repository.checkColumnExists("test_table", "non_existent");

        // Then
        assertFalse(result);
        verify(jdbcTemplate).queryForObject(anyString(), eq(Integer.class), eq("test_table"), eq("non_existent"));
    }

    @Test
    void testQueryTableIncrement() {
        // Given
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = LocalDateTime.now();
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq(startTime), eq(endTime)))
                .thenReturn(100L);

        // When
        Long result = repository.queryTableIncrement("test_table", "created_time", startTime, endTime);

        // Then
        assertEquals(100L, result);
        verify(jdbcTemplate).queryForObject(anyString(), eq(Long.class), eq(startTime), eq(endTime));
    }

    @Test
    void testGetAvgRowSizeFromInformationSchema() {
        // Given
        when(jdbcTemplate.queryForObject(anyString(), eq(Long.class), eq("test_table")))
                .thenReturn(256L);

        // When
        Long result = repository.getAvgRowSizeFromInformationSchema("test_table");

        // Then
        assertEquals(256L, result);
        verify(jdbcTemplate).queryForObject(anyString(), eq(Long.class), eq("test_table"));
    }

    @Test
    void testGetTableStatusInfo() {
        // Given
        Map<String, Object> expectedStatus = new HashMap<>();
        expectedStatus.put("table_rows", 1000L);
        expectedStatus.put("data_length", 256000L);
        expectedStatus.put("avg_row_length", 256L);

        List<Map<String, Object>> statusList = Arrays.asList(expectedStatus);
        when(jdbcTemplate.queryForList(anyString(), eq("test_table"))).thenReturn(statusList);

        // When
        Map<String, Object> result = repository.getTableStatusInfo("test_table");

        // Then
        assertNotNull(result);
        assertEquals(1000L, result.get("table_rows"));
        assertEquals(256000L, result.get("data_length"));
        verify(jdbcTemplate).queryForList(anyString(), eq("test_table"));
    }

    @Test
    void testExecuteQuery() {
        // Given
        Map<String, Object> row1 = new HashMap<>();
        row1.put("id", 1L);
        row1.put("name", "test");

        List<Map<String, Object>> expectedResult = Arrays.asList(row1);
        when(jdbcTemplate.queryForList("SELECT * FROM test_table WHERE id = ?", 1L))
                .thenReturn(expectedResult);

        // When
        List<Map<String, Object>> result = repository.executeQuery("SELECT * FROM test_table WHERE id = ?", 1L);

        // Then
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).get("id"));
        verify(jdbcTemplate).queryForList("SELECT * FROM test_table WHERE id = ?", 1L);
    }

    @Test
    void testExecuteUpdate() {
        // Given
        when(jdbcTemplate.update("UPDATE test_table SET name = ? WHERE id = ?", "new_name", 1L))
                .thenReturn(1);

        // When
        int result = repository.executeUpdate("UPDATE test_table SET name = ? WHERE id = ?", "new_name", 1L);

        // Then
        assertEquals(1, result);
        verify(jdbcTemplate).update("UPDATE test_table SET name = ? WHERE id = ?", "new_name", 1L);
    }

    @Test
    void testGetDatabaseType_MySQL() throws Exception {
        // Given
        when(metaData.getURL()).thenReturn("jdbc:mysql://localhost:3306/test");

        // When
        String result = repository.getDatabaseType();

        // Then
        assertEquals("mysql", result);
    }

    @Test
    void testGetDatabaseType_PostgreSQL() throws Exception {
        // Given
        when(metaData.getURL()).thenReturn("jdbc:postgresql://localhost:5432/test");

        // When
        String result = repository.getDatabaseType();

        // Then
        assertEquals("postgresql", result);
    }

    @Test
    void testGetDatabaseType_H2() throws Exception {
        // Given
        when(metaData.getURL()).thenReturn("jdbc:h2:mem:test");

        // When
        String result = repository.getDatabaseType();

        // Then
        assertEquals("h2", result);
    }

    @Test
    void testTestConnection_Success() {
        // Given
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);

        // When
        boolean result = repository.testConnection();

        // Then
        assertTrue(result);
        verify(jdbcTemplate).queryForObject("SELECT 1", Integer.class);
    }

    @Test
    void testTestConnection_Failure() {
        // Given
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
                .thenThrow(new RuntimeException("Connection failed"));

        // When
        boolean result = repository.testConnection();

        // Then
        assertFalse(result);
        verify(jdbcTemplate).queryForObject("SELECT 1", Integer.class);
    }
}
