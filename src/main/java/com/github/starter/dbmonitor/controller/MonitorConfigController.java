package com.github.starter.dbmonitor.controller;

import com.github.starter.dbmonitor.entity.MonitorConfig;
import com.github.starter.dbmonitor.service.MonitorConfigService;
import com.github.starter.dbmonitor.service.DatabaseSecurityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 监控配置管理接口
 */
@RestController
@RequestMapping("/api/monitor-config")
@Slf4j
public class MonitorConfigController {
    
    @Autowired
    private MonitorConfigService monitorConfigService;

    @Autowired
    private DatabaseSecurityService databaseSecurityService;
    
    /**
     * 创建监控配置
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createConfig(@RequestBody MonitorConfig config) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            MonitorConfig createdConfig = monitorConfigService.createConfig(config);
            response.put("success", true);
            response.put("message", "监控配置创建成功");
            response.put("data", createdConfig);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("创建监控配置失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "创建监控配置失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    /**
     * 更新监控配置
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateConfig(@PathVariable Long id, @RequestBody MonitorConfig config) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            config.setId(id);
            boolean updated = monitorConfigService.updateConfig(config);
            
            if (updated) {
                response.put("success", true);
                response.put("message", "监控配置更新成功");
                response.put("data", config);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "监控配置更新失败");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            log.error("更新监控配置失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "更新监控配置失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    /**
     * 根据ID获取监控配置
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getConfigById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<MonitorConfig> config = monitorConfigService.getConfigById(id);
            
            if (config.isPresent()) {
                response.put("success", true);
                response.put("data", config.get());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "监控配置不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            log.error("获取监控配置失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "获取监控配置失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 获取所有监控配置
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllConfigs(
            @RequestParam(required = false) String dataSourceName,
            @RequestParam(required = false) Boolean enabled) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            List<MonitorConfig> configs;
            
            if (dataSourceName != null && !dataSourceName.isEmpty()) {
                configs = monitorConfigService.getConfigsByDataSource(dataSourceName);
            } else if (enabled != null && enabled) {
                configs = monitorConfigService.getEnabledConfigs();
            } else {
                configs = monitorConfigService.getAllConfigs();
            }
            
            response.put("success", true);
            response.put("data", configs);
            response.put("total", configs.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取监控配置列表失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "获取监控配置列表失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 删除监控配置
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteConfig(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean deleted = monitorConfigService.deleteConfig(id);
            
            if (deleted) {
                response.put("success", true);
                response.put("message", "监控配置删除成功");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "监控配置删除失败");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            log.error("删除监控配置失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "删除监控配置失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    /**
     * 启用或禁用监控配置
     */
    @PutMapping("/{id}/enabled")
    public ResponseEntity<Map<String, Object>> updateConfigEnabled(
            @PathVariable Long id, 
            @RequestParam Boolean enabled,
            @RequestParam(required = false) String updatedBy) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean updated = monitorConfigService.updateConfigEnabled(id, enabled, updatedBy);
            
            if (updated) {
                response.put("success", true);
                response.put("message", enabled ? "监控配置已启用" : "监控配置已禁用");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "更新监控配置状态失败");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            log.error("更新监控配置状态失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "更新监控配置状态失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    /**
     * 批量启用或禁用监控配置
     */
    @PutMapping("/batch/enabled")
    public ResponseEntity<Map<String, Object>> batchUpdateConfigEnabled(
            @RequestBody Map<String, Object> requestBody) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            @SuppressWarnings("unchecked")
            List<Long> ids = (List<Long>) requestBody.get("ids");
            Boolean enabled = (Boolean) requestBody.get("enabled");
            String updatedBy = (String) requestBody.get("updatedBy");
            
            int updatedCount = monitorConfigService.batchUpdateConfigEnabled(ids, enabled, updatedBy);
            
            response.put("success", true);
            response.put("message", String.format("成功更新了 %d 个监控配置的状态", updatedCount));
            response.put("updatedCount", updatedCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("批量更新监控配置状态失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "批量更新监控配置状态失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    /**
     * 测试监控配置是否有效
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testConfig(@RequestBody MonitorConfig config) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean valid = monitorConfigService.testConfig(config);
            
            response.put("success", true);
            response.put("valid", valid);
            response.put("message", valid ? "监控配置有效" : "监控配置无效");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("测试监控配置失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("valid", false);
            response.put("message", "测试监控配置失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    /**
     * 获取表的所有列信息
     */
    @GetMapping("/table-columns")
    public ResponseEntity<Map<String, Object>> getTableColumns(
            @RequestParam String dataSourceName,
            @RequestParam String tableName) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 参数验证
            databaseSecurityService.sanitizeDataSourceName(dataSourceName);
            databaseSecurityService.sanitizeTableName(tableName);

            List<String> columns = monitorConfigService.getTableColumns(dataSourceName, tableName);

            response.put("success", true);
            response.put("data", columns);
            response.put("total", columns.size());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("参数验证失败: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "参数验证失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error("获取表列信息失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "获取表列信息失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 自动检测表的时间字段
     */
    @GetMapping("/detect-time-columns")
    public ResponseEntity<Map<String, Object>> detectTimeColumns(
            @RequestParam String dataSourceName,
            @RequestParam String tableName) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 参数验证
            databaseSecurityService.sanitizeDataSourceName(dataSourceName);
            databaseSecurityService.sanitizeTableName(tableName);

            List<String> timeColumns = monitorConfigService.detectTimeColumns(dataSourceName, tableName);

            response.put("success", true);
            response.put("data", timeColumns);
            response.put("total", timeColumns.size());
            response.put("message", timeColumns.isEmpty() ? "未检测到时间字段" : "检测到 " + timeColumns.size() + " 个时间字段");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("参数验证失败: {}", e.getMessage());
            response.put("success", false);
            response.put("message", "参数验证失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            log.error("自动检测时间字段失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "自动检测时间字段失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 根据配置名称获取监控配置
     */
    @GetMapping("/by-name/{configName}")
    public ResponseEntity<Map<String, Object>> getConfigByName(@PathVariable String configName) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<MonitorConfig> config = monitorConfigService.getConfigByName(configName);
            
            if (config.isPresent()) {
                response.put("success", true);
                response.put("data", config.get());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "监控配置不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            log.error("根据名称获取监控配置失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "获取监控配置失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * 根据数据源和表名获取监控配置
     */
    @GetMapping("/by-table")
    public ResponseEntity<Map<String, Object>> getConfigByDataSourceAndTable(
            @RequestParam String dataSourceName,
            @RequestParam String tableName) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            Optional<MonitorConfig> config = monitorConfigService.getConfigByDataSourceAndTable(dataSourceName, tableName);
            
            if (config.isPresent()) {
                response.put("success", true);
                response.put("data", config.get());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "监控配置不存在");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }
        } catch (Exception e) {
            log.error("根据数据源和表名获取监控配置失败: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "获取监控配置失败: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}