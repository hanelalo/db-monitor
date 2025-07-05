package com.github.starter.dbmonitor.controller;

import com.github.starter.dbmonitor.entity.DbMonitorStatistics;
import com.github.starter.dbmonitor.service.DbMonitorService;
import com.github.starter.dbmonitor.service.DbMonitorMetricsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据库监控数据暴露接口
 */
@RestController
@RequestMapping("/api/db-monitor")
@Slf4j
public class DbMonitorController {
    
    @Autowired
    private DbMonitorService dbMonitorService;
    
    @Autowired
    private DbMonitorMetricsService metricsService;
    
    /**
     * 获取最新的监控统计数据
     */
    @GetMapping("/statistics")
    public ResponseEntity<List<DbMonitorStatistics>> getLatestStatistics() {
        try {
            List<DbMonitorStatistics> statistics = dbMonitorService.getLatestStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("获取监控统计数据失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 获取指定表的监控统计数据
     */
    @GetMapping("/statistics/{tableName}")
    public ResponseEntity<List<DbMonitorStatistics>> getTableStatistics(@PathVariable String tableName) {
        try {
            List<DbMonitorStatistics> statistics = dbMonitorService.getTableStatistics(tableName);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("获取表 {} 的监控统计数据失败: {}", tableName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 获取磁盘空间汇总信息
     */
    @GetMapping("/disk-space/summary")
    public ResponseEntity<Map<String, Object>> getDiskSpaceSummary() {
        try {
            Map<String, Object> summary = dbMonitorService.getDiskSpaceSummary();
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("获取磁盘空间汇总信息失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 获取 Prometheus 格式的监控指标
     */
    @GetMapping("/metrics")
    public ResponseEntity<String> getPrometheusMetrics() {
        try {
            String metrics = metricsService.generatePrometheusMetrics();
            return ResponseEntity.ok()
                    .header("Content-Type", "text/plain; version=0.0.4; charset=utf-8")
                    .body(metrics);
        } catch (Exception e) {
            log.error("获取 Prometheus 指标失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("# Error generating metrics");
        }
    }
    
    /**
     * 获取 JSON 格式的监控指标
     */
    @GetMapping("/metrics/json")
    public ResponseEntity<Map<String, Object>> getJsonMetrics() {
        try {
            Map<String, Object> metrics = metricsService.generateJsonMetrics();
            return ResponseEntity.ok(metrics);
        } catch (Exception e) {
            log.error("获取 JSON 指标失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 手动触发监控任务
     */
    @PostMapping("/trigger")
    public ResponseEntity<String> triggerMonitoring() {
        try {
            dbMonitorService.executeMonitoring();
            return ResponseEntity.ok("监控任务已触发");
        } catch (Exception e) {
            log.error("手动触发监控任务失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("触发监控任务失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取监控健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        try {
            Map<String, Object> health = metricsService.getHealthStatus();
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("获取健康状态失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
}