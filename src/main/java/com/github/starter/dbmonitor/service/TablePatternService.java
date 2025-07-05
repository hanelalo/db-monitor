package com.github.starter.dbmonitor.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 表名模式匹配服务
 */
@Service
@Slf4j
public class TablePatternService {
    
    /**
     * 根据模式匹配表名
     */
    public List<String> getMatchedTableNames(JdbcTemplate jdbcTemplate, List<String> tablePatterns) {
        if (CollectionUtils.isEmpty(tablePatterns)) {
            log.warn("没有配置需要监控的表名模式");
            return new ArrayList<>();
        }
        
        try {
            // 获取数据库中所有表名
            List<String> allTableNames = getAllTableNames(jdbcTemplate);
            
            // 匹配表名
            Set<String> matchedTableNames = new HashSet<>();
            
            for (String pattern : tablePatterns) {
                List<String> matched = matchTablesByPattern(allTableNames, pattern);
                matchedTableNames.addAll(matched);
            }
            
            List<String> result = new ArrayList<>(matchedTableNames);
            log.info("根据模式 {} 匹配到 {} 个表: {}", tablePatterns, result.size(), result);
            
            return result;
            
        } catch (Exception e) {
            log.error("匹配表名时发生错误: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 获取数据库中所有表名
     */
    private List<String> getAllTableNames(JdbcTemplate jdbcTemplate) {
        try {
            // 尝试使用标准的 INFORMATION_SCHEMA 查询
            String sql = "SELECT table_name FROM information_schema.tables " +
                        "WHERE table_schema = DATABASE() AND table_type = 'BASE TABLE'";
            
            try {
                return jdbcTemplate.queryForList(sql, String.class);
            } catch (Exception e) {
                log.debug("使用 INFORMATION_SCHEMA 查询失败，尝试使用 SHOW TABLES: {}", e.getMessage());
            }
            
            // 如果上面的查询失败，尝试使用 SHOW TABLES
            return jdbcTemplate.queryForList("SHOW TABLES", String.class);
            
        } catch (Exception e) {
            log.error("获取数据库表名失败: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }
    
    /**
     * 根据模式匹配表名
     */
    private List<String> matchTablesByPattern(List<String> allTableNames, String pattern) {
        List<String> matched = new ArrayList<>();
        
        if (pattern == null || pattern.trim().isEmpty()) {
            return matched;
        }
        
        try {
            // 如果模式不包含通配符，直接精确匹配
            if (!pattern.contains("*") && !pattern.contains("?")) {
                if (allTableNames.contains(pattern)) {
                    matched.add(pattern);
                }
                return matched;
            }
            
            // 将通配符模式转换为正则表达式
            String regex = convertWildcardToRegex(pattern);
            Pattern compiledPattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            
            // 匹配表名
            for (String tableName : allTableNames) {
                if (compiledPattern.matcher(tableName).matches()) {
                    matched.add(tableName);
                }
            }
            
            log.debug("模式 '{}' 匹配到表: {}", pattern, matched);
            
        } catch (Exception e) {
            log.error("匹配表名模式 '{}' 时发生错误: {}", pattern, e.getMessage(), e);
        }
        
        return matched;
    }
    
    /**
     * 将通配符模式转换为正则表达式
     */
    private String convertWildcardToRegex(String wildcard) {
        // 转义特殊字符
        String escaped = wildcard.replace("\\", "\\\\")
                                 .replace("^", "\\^")
                                 .replace("$", "\\$")
                                 .replace(".", "\\.")
                                 .replace("|", "\\|")
                                 .replace("(", "\\(")
                                 .replace(")", "\\)")
                                 .replace("[", "\\[")
                                 .replace("]", "\\]")
                                 .replace("{", "\\{")
                                 .replace("}", "\\}")
                                 .replace("+", "\\+");
        
        // 将通配符转换为正则表达式
        escaped = escaped.replace("*", ".*")  // * 匹配任意字符
                        .replace("?", ".");   // ? 匹配单个字符
        
        return "^" + escaped + "$";
    }
    
    /**
     * 验证表名是否存在
     */
    public boolean isTableExists(JdbcTemplate jdbcTemplate, String tableName) {
        try {
            String sql = "SELECT COUNT(*) FROM information_schema.tables " +
                        "WHERE table_schema = DATABASE() AND table_name = ?";
            
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName);
            return count != null && count > 0;
            
        } catch (Exception e) {
            log.debug("检查表 {} 是否存在时发生错误: {}", tableName, e.getMessage());
            
            // 如果上面的查询失败，尝试直接查询表
            try {
                jdbcTemplate.queryForObject("SELECT 1 FROM " + tableName + " LIMIT 1", Integer.class);
                return true;
            } catch (Exception ex) {
                return false;
            }
        }
    }
    
    /**
     * 获取表的列信息
     */
    public List<String> getTableColumns(JdbcTemplate jdbcTemplate, String tableName) {
        try {
            String sql = "SELECT column_name FROM information_schema.columns " +
                        "WHERE table_schema = DATABASE() AND table_name = ?";
            
            return jdbcTemplate.queryForList(sql, String.class, tableName);
            
        } catch (Exception e) {
            log.error("获取表 {} 的列信息失败: {}", tableName, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}