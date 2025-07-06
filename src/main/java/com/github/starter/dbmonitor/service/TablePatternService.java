package com.github.starter.dbmonitor.service;

import com.github.starter.dbmonitor.repository.JdbcTableOperationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    
    @Autowired
    private JdbcTableOperationRepository tableOperationRepository;

    @Autowired
    private DatabaseSecurityService databaseSecurityService;
    
    /**
     * 根据模式匹配表名
     */
    public List<String> getMatchedTableNames(List<String> tablePatterns) {
        if (CollectionUtils.isEmpty(tablePatterns)) {
            log.warn("没有配置需要监控的表名模式");
            return new ArrayList<>();
        }
        
        try {
            // 获取数据库中所有表名
            List<String> allTableNames = getAllTableNames();
            
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
    private List<String> getAllTableNames() {
        try {
            // 使用轻量级Repository获取表名
            return tableOperationRepository.getAllTableNames();
            
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
    public boolean isTableExists(String tableName) {
        try {
            return tableOperationRepository.checkTableExists(tableName);

        } catch (Exception e) {
            log.debug("检查表 {} 是否存在时发生错误: {}", tableName, e.getMessage());
            return false;
        }
    }

    /**
     * 获取表的列信息
     */
    public List<String> getTableColumns(String tableName) {
        try {
            return tableOperationRepository.getTableColumns(tableName);
        } catch (Exception e) {
            log.error("获取表 {} 的列信息失败: {}", tableName, e.getMessage(), e);
            return new ArrayList<>();
        }
    }
}