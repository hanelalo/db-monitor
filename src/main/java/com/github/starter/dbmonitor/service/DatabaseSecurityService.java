package com.github.starter.dbmonitor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 数据库安全服务 - 防止SQL注入
 */
@Service
@Slf4j
public class DatabaseSecurityService {
    
    // 允许的表名和列名字符模式（字母、数字、下划线）
    private static final Pattern SAFE_NAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]*$");
    
    // 最大名称长度
    private static final int MAX_NAME_LENGTH = 64;
    
    // SQL关键字黑名单
    private static final Set<String> SQL_KEYWORDS = new HashSet<>(Arrays.asList(
        "SELECT", "INSERT", "UPDATE", "DELETE", "DROP", "CREATE", "ALTER", "TRUNCATE",
        "UNION", "OR", "AND", "WHERE", "FROM", "JOIN", "HAVING", "GROUP", "ORDER",
        "EXEC", "EXECUTE", "DECLARE", "CAST", "CONVERT", "SCRIPT", "JAVASCRIPT"
    ));
    
    /**
     * 验证表名是否安全
     */
    public boolean isValidTableName(String tableName) {
        if (!StringUtils.hasText(tableName)) {
            log.warn("表名为空");
            return false;
        }
        
        if (tableName.length() > MAX_NAME_LENGTH) {
            log.warn("表名长度超过限制: {}", tableName.length());
            return false;
        }
        
        if (!SAFE_NAME_PATTERN.matcher(tableName).matches()) {
            log.warn("表名包含非法字符: {}", tableName);
            return false;
        }
        
        if (SQL_KEYWORDS.contains(tableName.toUpperCase())) {
            log.warn("表名是SQL关键字: {}", tableName);
            return false;
        }
        
        return true;
    }
    
    /**
     * 验证列名是否安全
     */
    public boolean isValidColumnName(String columnName) {
        if (!StringUtils.hasText(columnName)) {
            log.warn("列名为空");
            return false;
        }
        
        if (columnName.length() > MAX_NAME_LENGTH) {
            log.warn("列名长度超过限制: {}", columnName.length());
            return false;
        }
        
        if (!SAFE_NAME_PATTERN.matcher(columnName).matches()) {
            log.warn("列名包含非法字符: {}", columnName);
            return false;
        }
        
        if (SQL_KEYWORDS.contains(columnName.toUpperCase())) {
            log.warn("列名是SQL关键字: {}", columnName);
            return false;
        }
        
        return true;
    }
    
    /**
     * 验证数据源名称是否安全
     */
    public boolean isValidDataSourceName(String dataSourceName) {
        if (!StringUtils.hasText(dataSourceName)) {
            log.warn("数据源名称为空");
            return false;
        }
        
        if (dataSourceName.length() > MAX_NAME_LENGTH) {
            log.warn("数据源名称长度超过限制: {}", dataSourceName.length());
            return false;
        }
        
        if (!SAFE_NAME_PATTERN.matcher(dataSourceName).matches()) {
            log.warn("数据源名称包含非法字符: {}", dataSourceName);
            return false;
        }
        
        return true;
    }
    
    /**
     * 清理和验证表名
     */
    public String sanitizeTableName(String tableName) {
        if (!isValidTableName(tableName)) {
            throw new IllegalArgumentException("无效的表名: " + tableName);
        }
        return tableName;
    }
    
    /**
     * 清理和验证列名
     */
    public String sanitizeColumnName(String columnName) {
        if (!isValidColumnName(columnName)) {
            throw new IllegalArgumentException("无效的列名: " + columnName);
        }
        return columnName;
    }
    
    /**
     * 清理和验证数据源名称
     */
    public String sanitizeDataSourceName(String dataSourceName) {
        if (!isValidDataSourceName(dataSourceName)) {
            throw new IllegalArgumentException("无效的数据源名称: " + dataSourceName);
        }
        return dataSourceName;
    }
}
