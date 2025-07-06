package com.github.starter.dbmonitor.controller;

import com.github.starter.dbmonitor.config.condition.ConditionalOnEndpointsEnabled;
import com.github.starter.dbmonitor.entity.DbMonitorStatistics;
import com.github.starter.dbmonitor.service.DbMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 数据库监控统计数据接口
 */
@RestController
@RequestMapping("/api/db-monitor")
@ConditionalOnEndpointsEnabled
@ConditionalOnProperty(prefix = "db.monitor.metrics.endpoints", name = "statistics-enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class DbMonitorStatisticsController {
    
    @Autowired
    private DbMonitorService dbMonitorService;
    
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
    public ResponseEntity<List<DbMonitorStatistics>> getStatisticsByTable(@PathVariable String tableName) {
        try {
            List<DbMonitorStatistics> statistics = dbMonitorService.getTableStatistics(tableName);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("获取表 {} 的监控统计数据失败: {}", tableName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}
