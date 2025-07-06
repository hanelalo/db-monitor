package com.github.starter.dbmonitor.controller;

import com.github.starter.dbmonitor.config.condition.ConditionalOnEndpointsEnabled;
import com.github.starter.dbmonitor.config.condition.ConditionalOnMetricsEndpointsEnabled;
import com.github.starter.dbmonitor.service.DbMonitorMetricsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 数据库监控指标暴露接口
 */
@RestController
@RequestMapping("/api/db-monitor")
@ConditionalOnEndpointsEnabled
@ConditionalOnMetricsEndpointsEnabled
@Slf4j
public class DbMonitorMetricsController {
    
    @Autowired
    private DbMonitorMetricsService metricsService;
    
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
     * 获取监控健康状态
     */
    @GetMapping("/health")
    @ConditionalOnProperty(prefix = "db.monitor.metrics.endpoints", name = "health-enabled", havingValue = "true", matchIfMissing = true)
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        try {
            Map<String, Object> health = metricsService.getHealthStatus();
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("获取健康状态失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
