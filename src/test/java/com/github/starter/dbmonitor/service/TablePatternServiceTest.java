package com.github.starter.dbmonitor.service;

import com.github.starter.dbmonitor.repository.JdbcTableOperationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * TablePatternService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class TablePatternServiceTest {

    @Mock
    private JdbcTableOperationRepository tableOperationRepository;

    @Mock
    private DatabaseSecurityService databaseSecurityService;

    @InjectMocks
    private TablePatternService tablePatternService;

    private List<String> allTableNames;

    @BeforeEach
    void setUp() {
        allTableNames = Arrays.asList(
                "user_table",
                "order_table", 
                "product_table",
                "log_table",
                "test_user_data",
                "test_order_info",
                "system_config"
        );
    }

    @Test
    void testGetMatchedTableNames_ExactMatch() {
        // Given
        List<String> patterns = Arrays.asList("user_table");
        when(tableOperationRepository.getAllTableNames()).thenReturn(allTableNames);

        // When
        List<String> result = tablePatternService.getMatchedTableNames(patterns);

        // Then
        assertEquals(1, result.size());
        assertTrue(result.contains("user_table"));
        verify(tableOperationRepository).getAllTableNames();
    }

    @Test
    void testGetMatchedTableNames_WildcardMatch() {
        // Given
        List<String> patterns = Arrays.asList("*_table");
        when(tableOperationRepository.getAllTableNames()).thenReturn(allTableNames);

        // When
        List<String> result = tablePatternService.getMatchedTableNames(patterns);

        // Then
        assertEquals(4, result.size());
        assertTrue(result.contains("user_table"));
        assertTrue(result.contains("order_table"));
        assertTrue(result.contains("product_table"));
        assertTrue(result.contains("log_table"));
        verify(tableOperationRepository).getAllTableNames();
    }

    @Test
    void testGetMatchedTableNames_MultiplePatterns() {
        // Given
        List<String> patterns = Arrays.asList("test_*", "*_config");
        when(tableOperationRepository.getAllTableNames()).thenReturn(allTableNames);

        // When
        List<String> result = tablePatternService.getMatchedTableNames(patterns);

        // Then
        assertEquals(3, result.size());
        assertTrue(result.contains("test_user_data"));
        assertTrue(result.contains("test_order_info"));
        assertTrue(result.contains("system_config"));
        verify(tableOperationRepository).getAllTableNames();
    }

    @Test
    void testGetMatchedTableNames_QuestionMarkWildcard() {
        // Given
        List<String> patterns = Arrays.asList("user_tabl?");
        when(tableOperationRepository.getAllTableNames()).thenReturn(allTableNames);

        // When
        List<String> result = tablePatternService.getMatchedTableNames(patterns);

        // Then
        assertEquals(1, result.size());
        assertTrue(result.contains("user_table"));
        verify(tableOperationRepository).getAllTableNames();
    }

    @Test
    void testGetMatchedTableNames_NoMatch() {
        // Given
        List<String> patterns = Arrays.asList("non_existent_*");
        when(tableOperationRepository.getAllTableNames()).thenReturn(allTableNames);

        // When
        List<String> result = tablePatternService.getMatchedTableNames(patterns);

        // Then
        assertEquals(0, result.size());
        verify(tableOperationRepository).getAllTableNames();
    }

    @Test
    void testGetMatchedTableNames_EmptyPatterns() {
        // Given
        List<String> patterns = Arrays.asList();

        // When
        List<String> result = tablePatternService.getMatchedTableNames(patterns);

        // Then
        assertEquals(0, result.size());
        verify(tableOperationRepository, never()).getAllTableNames();
    }

    @Test
    void testGetMatchedTableNames_NullPatterns() {
        // When
        List<String> result = tablePatternService.getMatchedTableNames(null);

        // Then
        assertEquals(0, result.size());
        verify(tableOperationRepository, never()).getAllTableNames();
    }

    @Test
    void testGetMatchedTableNames_RegexSpecialCharacters() {
        // Given
        List<String> patterns = Arrays.asList("test_user.*");
        List<String> tablesWithSpecialChars = Arrays.asList("test_user.data", "test_user_data");
        when(tableOperationRepository.getAllTableNames()).thenReturn(tablesWithSpecialChars);

        // When
        List<String> result = tablePatternService.getMatchedTableNames(patterns);

        // Then
        // 应该匹配 "test_user.data" 而不是 "test_user_data"，因为 . 被转义了
        assertEquals(1, result.size());
        assertTrue(result.contains("test_user.data"));
        verify(tableOperationRepository).getAllTableNames();
    }

    @Test
    void testIsTableExists_True() {
        // Given
        when(tableOperationRepository.checkTableExists("user_table")).thenReturn(true);

        // When
        boolean result = tablePatternService.isTableExists("user_table");

        // Then
        assertTrue(result);
        verify(tableOperationRepository).checkTableExists("user_table");
    }

    @Test
    void testIsTableExists_False() {
        // Given
        when(tableOperationRepository.checkTableExists("non_existent_table")).thenReturn(false);

        // When
        boolean result = tablePatternService.isTableExists("non_existent_table");

        // Then
        assertFalse(result);
        verify(tableOperationRepository).checkTableExists("non_existent_table");
    }

    @Test
    void testIsTableExists_Exception() {
        // Given
        when(tableOperationRepository.checkTableExists("error_table"))
                .thenThrow(new RuntimeException("Database error"));

        // When
        boolean result = tablePatternService.isTableExists("error_table");

        // Then
        assertFalse(result);
        verify(tableOperationRepository).checkTableExists("error_table");
    }

    @Test
    void testGetTableColumns() {
        // Given
        List<String> columns = Arrays.asList("id", "name", "created_time");
        when(tableOperationRepository.getTableColumns("user_table")).thenReturn(columns);

        // When
        List<String> result = tablePatternService.getTableColumns("user_table");

        // Then
        assertEquals(3, result.size());
        assertEquals("id", result.get(0));
        assertEquals("name", result.get(1));
        assertEquals("created_time", result.get(2));
        verify(tableOperationRepository).getTableColumns("user_table");
    }

    @Test
    void testGetTableColumns_Exception() {
        // Given
        when(tableOperationRepository.getTableColumns("error_table"))
                .thenThrow(new RuntimeException("Database error"));

        // When
        List<String> result = tablePatternService.getTableColumns("error_table");

        // Then
        assertEquals(0, result.size());
        verify(tableOperationRepository).getTableColumns("error_table");
    }

    @Test
    void testGetMatchedTableNames_DatabaseException() {
        // Given
        List<String> patterns = Arrays.asList("*_table");
        when(tableOperationRepository.getAllTableNames())
                .thenThrow(new RuntimeException("Database connection error"));

        // When
        List<String> result = tablePatternService.getMatchedTableNames(patterns);

        // Then
        assertEquals(0, result.size());
        verify(tableOperationRepository).getAllTableNames();
    }

    @Test
    void testConvertWildcardToRegex_ComplexPattern() {
        // Given
        List<String> patterns = Arrays.asList("test_*_data?");
        List<String> testTables = Arrays.asList(
                "test_user_data1",
                "test_order_data2", 
                "test_product_data",
                "test_user_data_extra"
        );
        when(tableOperationRepository.getAllTableNames()).thenReturn(testTables);

        // When
        List<String> result = tablePatternService.getMatchedTableNames(patterns);

        // Then
        assertEquals(2, result.size());
        assertTrue(result.contains("test_user_data1"));
        assertTrue(result.contains("test_order_data2"));
        assertFalse(result.contains("test_product_data")); // 没有单个字符匹配 ?
        assertFalse(result.contains("test_user_data_extra")); // 超过了单个字符匹配
        verify(tableOperationRepository).getAllTableNames();
    }

    @Test
    void testGetMatchedTableNames_DuplicateResults() {
        // Given
        List<String> patterns = Arrays.asList("user_*", "*_table");
        when(tableOperationRepository.getAllTableNames()).thenReturn(allTableNames);

        // When
        List<String> result = tablePatternService.getMatchedTableNames(patterns);

        // Then
        // user_table 应该只出现一次，即使它匹配两个模式
        assertEquals(4, result.size());
        long userTableCount = result.stream().filter(name -> name.equals("user_table")).count();
        assertEquals(1, userTableCount);
        verify(tableOperationRepository).getAllTableNames();
    }
}
