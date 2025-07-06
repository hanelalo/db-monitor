package com.github.starter.dbmonitor.job;

import com.github.starter.dbmonitor.service.DbMonitorService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.context.XxlJobHelper;
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
     * 数据库监控任务（支持分片）
     */
    @XxlJob("dbMonitorJob")
    public ReturnT<String> dbMonitorJob(String param) {
        log.info("开始执行数据库监控任务，参数: {}", param);

        try {
            // 获取分片参数
            String shardingParam = getShardingParam();

            // 执行监控任务（支持分片）
            dbMonitorService.executeMonitoring(shardingParam);

            if (shardingParam != null && !shardingParam.trim().isEmpty()) {
                log.info("数据库监控任务执行成功（分片模式: {}）", shardingParam);
            } else {
                log.info("数据库监控任务执行成功（非分片模式）");
            }
            return ReturnT.SUCCESS;

        } catch (Exception e) {
            log.error("数据库监控任务执行失败: {}", e.getMessage(), e);
            return new ReturnT<>(ReturnT.FAIL_CODE, "监控任务执行失败: " + e.getMessage());
        }
    }

    /**
     * 获取XXL-Job分片参数
     *
     * @return 分片参数字符串，格式："shardIndex/shardTotal"，如果不是分片任务则返回null
     */
    private String getShardingParam() {
        try {
            // 获取分片索引（从0开始）
            int shardIndex = XxlJobHelper.getShardIndex();
            // 获取分片总数
            int shardTotal = XxlJobHelper.getShardTotal();

            // 如果分片总数大于1，说明是分片任务
            if (shardTotal > 1) {
                String shardingParam = shardIndex + "/" + shardTotal;
                log.debug("检测到XXL-Job分片参数: {}", shardingParam);
                return shardingParam;
            } else {
                log.debug("非分片任务，分片总数: {}", shardTotal);
                return null;
            }

        } catch (Exception e) {
            log.warn("获取XXL-Job分片参数失败，将以非分片模式执行: {}", e.getMessage());
            return null;
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