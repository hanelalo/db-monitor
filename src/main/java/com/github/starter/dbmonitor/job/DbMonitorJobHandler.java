package com.github.starter.dbmonitor.job;

import com.github.starter.dbmonitor.service.DbMonitorService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 数据库监控任务处理器
 */
@Component
@Slf4j
public class DbMonitorJobHandler {
    
    @Autowired
    private DbMonitorService dbMonitorService;
    
    /**
     * 数据库监控任务
     */
    @XxlJob("dbMonitorJob")
    public ReturnT<String> dbMonitorJob(String param) {
        log.info("开始执行数据库监控任务，参数: {}", param);
        
        try {
            // 执行监控任务
            dbMonitorService.executeMonitoring();
            
            log.info("数据库监控任务执行成功");
            return ReturnT.SUCCESS;
            
        } catch (Exception e) {
            log.error("数据库监控任务执行失败: {}", e.getMessage(), e);
            return new ReturnT<>(ReturnT.FAIL_CODE, "监控任务执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 数据清理任务
     */
    @XxlJob("dbMonitorCleanupJob")
    public ReturnT<String> dbMonitorCleanupJob(String param) {
        log.info("开始执行数据库监控数据清理任务，参数: {}", param);
        
        try {
            // 执行数据清理
            dbMonitorService.cleanupExpiredData();
            
            log.info("数据库监控数据清理任务执行成功");
            return ReturnT.SUCCESS;
            
        } catch (Exception e) {
            log.error("数据库监控数据清理任务执行失败: {}", e.getMessage(), e);
            return new ReturnT<>(ReturnT.FAIL_CODE, "数据清理任务执行失败: " + e.getMessage());
        }
    }
    
    /**
     * 手动触发监控任务
     */
    @XxlJob("dbMonitorManualJob")
    public ReturnT<String> dbMonitorManualJob(String param) {
        log.info("手动触发数据库监控任务，参数: {}", param);
        
        try {
            // 执行监控任务
            dbMonitorService.executeMonitoring();
            
            log.info("手动触发数据库监控任务执行成功");
            return ReturnT.SUCCESS;
            
        } catch (Exception e) {
            log.error("手动触发数据库监控任务执行失败: {}", e.getMessage(), e);
            return new ReturnT<>(ReturnT.FAIL_CODE, "手动监控任务执行失败: " + e.getMessage());
        }
    }
}