package com.github.starter.dbmonitor.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DatabaseSecurityService 单元测试
 */
@ExtendWith(MockitoExtension.class)
public class DatabaseSecurityServiceTest {

    private DatabaseSecurityService databaseSecurityService;

    @BeforeEach
    void setUp() {
        databaseSecurityService = new DatabaseSecurityService();
    }

    @Test
    void testValidTableName() {
        // 测试有效的表名
        assertTrue(databaseSecurityService.isValidTableName("user_table"));
        assertTrue(databaseSecurityService.isValidTableName("test123"));
        assertTrue(databaseSecurityService.isValidTableName("table_name_with_underscores"));
        assertTrue(databaseSecurityService.isValidTableName("TableWithCamelCase"));
    }

    @Test
    void testInvalidTableName() {
        // 测试无效的表名
        assertFalse(databaseSecurityService.isValidTableName(null));
        assertFalse(databaseSecurityService.isValidTableName(""));
        assertFalse(databaseSecurityService.isValidTableName("   "));
        assertFalse(databaseSecurityService.isValidTableName("123table")); // 数字开头
        assertFalse(databaseSecurityService.isValidTableName("table-name")); // 包含连字符
        assertFalse(databaseSecurityService.isValidTableName("table name")); // 包含空格
        assertFalse(databaseSecurityService.isValidTableName("table;DROP")); // 包含分号
        assertFalse(databaseSecurityService.isValidTableName("SELECT")); // SQL关键字
        assertFalse(databaseSecurityService.isValidTableName("INSERT")); // SQL关键字
    }

    @Test
    void testValidColumnName() {
        // 测试有效的列名
        assertTrue(databaseSecurityService.isValidColumnName("id"));
        assertTrue(databaseSecurityService.isValidColumnName("user_name"));
        assertTrue(databaseSecurityService.isValidColumnName("created_time"));
        assertTrue(databaseSecurityService.isValidColumnName("columnName"));
        assertTrue(databaseSecurityService.isValidColumnName("column123"));
    }

    @Test
    void testInvalidColumnName() {
        // 测试无效的列名
        assertFalse(databaseSecurityService.isValidColumnName(null));
        assertFalse(databaseSecurityService.isValidColumnName(""));
        assertFalse(databaseSecurityService.isValidColumnName("   "));
        assertFalse(databaseSecurityService.isValidColumnName("123column")); // 数字开头
        assertFalse(databaseSecurityService.isValidColumnName("column-name")); // 包含连字符
        assertFalse(databaseSecurityService.isValidColumnName("column name")); // 包含空格
        assertFalse(databaseSecurityService.isValidColumnName("column;DROP")); // 包含分号
        assertFalse(databaseSecurityService.isValidColumnName("WHERE")); // SQL关键字
        assertFalse(databaseSecurityService.isValidColumnName("ORDER")); // SQL关键字
    }

    @Test
    void testValidDataSourceName() {
        // 测试有效的数据源名称
        assertTrue(databaseSecurityService.isValidDataSourceName("primary"));
        assertTrue(databaseSecurityService.isValidDataSourceName("secondary"));
        assertTrue(databaseSecurityService.isValidDataSourceName("dataSource1"));
        assertTrue(databaseSecurityService.isValidDataSourceName("data_source"));
    }

    @Test
    void testInvalidDataSourceName() {
        // 测试无效的数据源名称
        assertFalse(databaseSecurityService.isValidDataSourceName(null));
        assertFalse(databaseSecurityService.isValidDataSourceName(""));
        assertFalse(databaseSecurityService.isValidDataSourceName("   "));
        assertFalse(databaseSecurityService.isValidDataSourceName("123source")); // 数字开头
        assertFalse(databaseSecurityService.isValidDataSourceName("data-source")); // 包含连字符
        assertFalse(databaseSecurityService.isValidDataSourceName("data source")); // 包含空格
    }

    @Test
    void testSanitizeTableName() {
        // 测试表名清理
        assertEquals("user_table", databaseSecurityService.sanitizeTableName("user_table"));
        
        // 测试异常情况
        assertThrows(IllegalArgumentException.class, () -> 
            databaseSecurityService.sanitizeTableName("invalid-table"));
        assertThrows(IllegalArgumentException.class, () -> 
            databaseSecurityService.sanitizeTableName("SELECT"));
        assertThrows(IllegalArgumentException.class, () -> 
            databaseSecurityService.sanitizeTableName(null));
    }

    @Test
    void testSanitizeColumnName() {
        // 测试列名清理
        assertEquals("created_time", databaseSecurityService.sanitizeColumnName("created_time"));
        
        // 测试异常情况
        assertThrows(IllegalArgumentException.class, () -> 
            databaseSecurityService.sanitizeColumnName("invalid-column"));
        assertThrows(IllegalArgumentException.class, () -> 
            databaseSecurityService.sanitizeColumnName("WHERE"));
        assertThrows(IllegalArgumentException.class, () -> 
            databaseSecurityService.sanitizeColumnName(null));
    }

    @Test
    void testSanitizeDataSourceName() {
        // 测试数据源名称清理
        assertEquals("primary", databaseSecurityService.sanitizeDataSourceName("primary"));
        
        // 测试异常情况
        assertThrows(IllegalArgumentException.class, () -> 
            databaseSecurityService.sanitizeDataSourceName("invalid-source"));
        assertThrows(IllegalArgumentException.class, () -> 
            databaseSecurityService.sanitizeDataSourceName(null));
    }

    @Test
    void testLongNames() {
        // 测试超长名称
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 65; i++) {
            sb.append("a");
        }
        String longName = sb.toString(); // 超过64字符限制
        assertFalse(databaseSecurityService.isValidTableName(longName));
        assertFalse(databaseSecurityService.isValidColumnName(longName));
        assertFalse(databaseSecurityService.isValidDataSourceName(longName));
    }

    @Test
    void testCaseInsensitiveSQLKeywords() {
        // 测试大小写不敏感的SQL关键字检测
        assertFalse(databaseSecurityService.isValidTableName("select"));
        assertFalse(databaseSecurityService.isValidTableName("Select"));
        assertFalse(databaseSecurityService.isValidTableName("SELECT"));
        assertFalse(databaseSecurityService.isValidColumnName("where"));
        assertFalse(databaseSecurityService.isValidColumnName("Where"));
        assertFalse(databaseSecurityService.isValidColumnName("WHERE"));
    }
}
