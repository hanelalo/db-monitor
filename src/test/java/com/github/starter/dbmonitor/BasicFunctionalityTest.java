package com.github.starter.dbmonitor;

import com.github.starter.dbmonitor.service.DatabaseSecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 基础功能测试
 * 验证核心组件的基本功能
 */
public class BasicFunctionalityTest {

    private DatabaseSecurityService databaseSecurityService;

    @BeforeEach
    void setUp() {
        databaseSecurityService = new DatabaseSecurityService();
    }

    @Test
    void testDatabaseSecurityService_ValidNames() {
        // 测试有效的表名
        assertTrue(databaseSecurityService.isValidTableName("user_table"));
        assertTrue(databaseSecurityService.isValidTableName("test123"));
        assertTrue(databaseSecurityService.isValidTableName("validTable"));

        // 测试有效的列名
        assertTrue(databaseSecurityService.isValidColumnName("id"));
        assertTrue(databaseSecurityService.isValidColumnName("user_name"));
        assertTrue(databaseSecurityService.isValidColumnName("created_time"));

        // 测试有效的数据源名称
        assertTrue(databaseSecurityService.isValidDataSourceName("primary"));
        assertTrue(databaseSecurityService.isValidDataSourceName("secondary"));
    }

    @Test
    void testDatabaseSecurityService_InvalidNames() {
        // 测试无效的表名
        assertFalse(databaseSecurityService.isValidTableName(null));
        assertFalse(databaseSecurityService.isValidTableName(""));
        assertFalse(databaseSecurityService.isValidTableName("123table")); // 数字开头
        assertFalse(databaseSecurityService.isValidTableName("table-name")); // 包含连字符
        assertFalse(databaseSecurityService.isValidTableName("SELECT")); // SQL关键字

        // 测试无效的列名
        assertFalse(databaseSecurityService.isValidColumnName(null));
        assertFalse(databaseSecurityService.isValidColumnName(""));
        assertFalse(databaseSecurityService.isValidColumnName("123column")); // 数字开头
        assertFalse(databaseSecurityService.isValidColumnName("WHERE")); // SQL关键字

        // 测试无效的数据源名称
        assertFalse(databaseSecurityService.isValidDataSourceName(null));
        assertFalse(databaseSecurityService.isValidDataSourceName(""));
        assertFalse(databaseSecurityService.isValidDataSourceName("123source")); // 数字开头
    }

    @Test
    void testDatabaseSecurityService_Sanitization() {
        // 测试表名清理
        assertEquals("user_table", databaseSecurityService.sanitizeTableName("user_table"));
        
        // 测试异常情况
        assertThrows(IllegalArgumentException.class, () -> 
            databaseSecurityService.sanitizeTableName("invalid-table"));
        assertThrows(IllegalArgumentException.class, () -> 
            databaseSecurityService.sanitizeTableName("SELECT"));
        assertThrows(IllegalArgumentException.class, () -> 
            databaseSecurityService.sanitizeTableName(null));

        // 测试列名清理
        assertEquals("created_time", databaseSecurityService.sanitizeColumnName("created_time"));
        
        assertThrows(IllegalArgumentException.class, () -> 
            databaseSecurityService.sanitizeColumnName("invalid-column"));
        assertThrows(IllegalArgumentException.class, () -> 
            databaseSecurityService.sanitizeColumnName("WHERE"));

        // 测试数据源名称清理
        assertEquals("primary", databaseSecurityService.sanitizeDataSourceName("primary"));
        
        assertThrows(IllegalArgumentException.class, () -> 
            databaseSecurityService.sanitizeDataSourceName("invalid-source"));
    }

    @Test
    void testDatabaseSecurityService_SQLKeywords() {
        // 测试大小写不敏感的SQL关键字检测
        assertFalse(databaseSecurityService.isValidTableName("select"));
        assertFalse(databaseSecurityService.isValidTableName("Select"));
        assertFalse(databaseSecurityService.isValidTableName("SELECT"));
        
        assertFalse(databaseSecurityService.isValidColumnName("where"));
        assertFalse(databaseSecurityService.isValidColumnName("Where"));
        assertFalse(databaseSecurityService.isValidColumnName("WHERE"));
        
        assertFalse(databaseSecurityService.isValidTableName("INSERT"));
        assertFalse(databaseSecurityService.isValidTableName("UPDATE"));
        assertFalse(databaseSecurityService.isValidTableName("DELETE"));
        assertFalse(databaseSecurityService.isValidTableName("DROP"));
    }

    @Test
    void testDatabaseSecurityService_LengthLimits() {
        // 测试长度限制
        StringBuilder longName = new StringBuilder();
        for (int i = 0; i < 65; i++) {
            longName.append("a");
        }
        String tooLongName = longName.toString();
        
        assertFalse(databaseSecurityService.isValidTableName(tooLongName));
        assertFalse(databaseSecurityService.isValidColumnName(tooLongName));
        assertFalse(databaseSecurityService.isValidDataSourceName(tooLongName));
        
        // 测试边界情况 - 64字符应该是有效的
        StringBuilder maxLengthName = new StringBuilder();
        for (int i = 0; i < 64; i++) {
            maxLengthName.append("a");
        }
        String maxName = "a" + maxLengthName.toString().substring(1); // 确保以字母开头
        
        assertTrue(databaseSecurityService.isValidTableName(maxName));
        assertTrue(databaseSecurityService.isValidColumnName(maxName));
        assertTrue(databaseSecurityService.isValidDataSourceName(maxName));
    }

    @Test
    void testDatabaseSecurityService_SpecialCharacters() {
        // 测试特殊字符
        assertFalse(databaseSecurityService.isValidTableName("table name")); // 空格
        assertFalse(databaseSecurityService.isValidTableName("table;DROP")); // 分号
        assertFalse(databaseSecurityService.isValidTableName("table'name")); // 单引号
        assertFalse(databaseSecurityService.isValidTableName("table\"name")); // 双引号
        assertFalse(databaseSecurityService.isValidTableName("table\\name")); // 反斜杠
        assertFalse(databaseSecurityService.isValidTableName("table/name")); // 斜杠
        assertFalse(databaseSecurityService.isValidTableName("table*name")); // 星号
        assertFalse(databaseSecurityService.isValidTableName("table?name")); // 问号
        assertFalse(databaseSecurityService.isValidTableName("table<name")); // 小于号
        assertFalse(databaseSecurityService.isValidTableName("table>name")); // 大于号
        assertFalse(databaseSecurityService.isValidTableName("table|name")); // 管道符
        
        // 测试允许的字符
        assertTrue(databaseSecurityService.isValidTableName("table_name")); // 下划线
        assertTrue(databaseSecurityService.isValidTableName("tableName")); // 驼峰命名
        assertTrue(databaseSecurityService.isValidTableName("table123")); // 数字（非开头）
    }

    @Test
    void testDatabaseSecurityService_EdgeCases() {
        // 测试边界情况
        assertFalse(databaseSecurityService.isValidTableName("   ")); // 只有空格
        assertFalse(databaseSecurityService.isValidTableName("\t")); // 制表符
        assertFalse(databaseSecurityService.isValidTableName("\n")); // 换行符
        
        // 测试最短有效名称
        assertTrue(databaseSecurityService.isValidTableName("a"));
        assertTrue(databaseSecurityService.isValidColumnName("a"));
        assertTrue(databaseSecurityService.isValidDataSourceName("a"));
        
        // 测试常见的有效名称模式
        assertTrue(databaseSecurityService.isValidTableName("user"));
        assertTrue(databaseSecurityService.isValidTableName("User"));
        assertTrue(databaseSecurityService.isValidTableName("USER"));
        assertTrue(databaseSecurityService.isValidTableName("user_profile"));
        assertTrue(databaseSecurityService.isValidTableName("userProfile"));
        assertTrue(databaseSecurityService.isValidTableName("user123"));
        assertTrue(databaseSecurityService.isValidTableName("user_123"));
    }
}
