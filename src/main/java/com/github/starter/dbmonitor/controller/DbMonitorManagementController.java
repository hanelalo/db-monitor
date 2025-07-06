package com.github.starter.dbmonitor.controller;

import com.github.starter.dbmonitor.config.condition.ConditionalOnEndpointsEnabled;
import com.github.starter.dbmonitor.service.DbMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 数据库监控管理接口
 */
@RestController
@RequestMapping("/api/db-monitor")
@ConditionalOnEndpointsEnabled
@ConditionalOnProperty(prefix = "db.monitor.metrics.endpoints", name = "management-enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class DbMonitorManagementController {
    
    @Autowired
    private DbMonitorService dbMonitorService;
    
    /**
     * 手动触发监控任务（非分片模式）
     */
    @PostMapping("/trigger")
    public ResponseEntity<String> triggerMonitoring() {
        try {
            dbMonitorService.executeMonitoring();
            return ResponseEntity.ok("监控任务已触发（非分片模式）");
        } catch (Exception e) {
            log.error("手动触发监控任务失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("触发监控任务失败: " + e.getMessage());
        }
    }

    /**
     * 手动触发监控任务（分片模式）
     */
    @PostMapping("/trigger/shard")
    public ResponseEntity<String> triggerMonitoringWithShard(@RequestParam String shardingParam) {
        try {
            dbMonitorService.executeMonitoring(shardingParam);
            return ResponseEntity.ok("分片监控任务已触发，分片参数: " + shardingParam);
        } catch (Exception e) {
            log.error("手动触发分片监控任务失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("触发分片监控任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 清理过期数据
     */
    @DeleteMapping("/cleanup")
    public ResponseEntity<String> cleanupExpiredData() {
        try {
            dbMonitorService.cleanupExpiredData();
            return ResponseEntity.ok("过期数据清理完成");
        } catch (Exception e) {
            log.error("清理过期数据失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("清理过期数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取监控任务状态
     */
    @GetMapping("/status")
    public ResponseEntity<String> getMonitoringStatus() {
        try {
            // 这里可以返回更详细的监控状态信息
            return ResponseEntity.ok("监控服务运行正常");
        } catch (Exception e) {
            log.error("获取监控状态失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("获取监控状态失败: " + e.getMessage());
        }
    }
}
