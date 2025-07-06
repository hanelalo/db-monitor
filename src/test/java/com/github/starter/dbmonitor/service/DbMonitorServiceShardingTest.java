package com.github.starter.dbmonitor.service;

import com.github.starter.dbmonitor.entity.MonitorConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 数据库监控服务分片执行功能测试
 */
@ExtendWith(MockitoExtension.class)
class DbMonitorServiceShardingTest {

    @Mock
    private MonitorConfigService monitorConfigService;

    @InjectMocks
    private DbMonitorService dbMonitorService;

    private List<MonitorConfig> testConfigs;

    @BeforeEach
    void setUp() {
        // 创建测试配置
        testConfigs = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            MonitorConfig config = new MonitorConfig();
            config.setId((long) i);
            config.setConfigName("config_" + i);
            config.setTableName("table_" + i);
            config.setDataSourceName("dataSource");
            config.setTimeColumnName("created_time");
            config.setIntervalType("MINUTES");
            config.setIntervalValue(10);
            config.setEnabled(true);
            testConfigs.add(config);
        }
    }

    @Test
    void testExecuteMonitoringWithSharding_ValidParam() {
        // Given
        String shardingParam = "1/3";
        List<MonitorConfig> shardConfigs = testConfigs.subList(0, 3); // 模拟分片结果
        when(monitorConfigService.getEnabledConfigs(shardingParam)).thenReturn(shardConfigs);

        // When & Then - 验证不抛出异常
        assertDoesNotThrow(() -> dbMonitorService.executeMonitoring(shardingParam));
        
        // 验证调用了正确的方法
        verify(monitorConfigService).getEnabledConfigs(shardingParam);
    }

    @Test
    void testExecuteMonitoringWithSharding_EmptyConfigs() {
        // Given
        String shardingParam = "2/3";
        when(monitorConfigService.getEnabledConfigs(shardingParam)).thenReturn(new ArrayList<>());

        // When & Then - 验证不抛出异常
        assertDoesNotThrow(() -> dbMonitorService.executeMonitoring(shardingParam));
        
        // 验证调用了正确的方法
        verify(monitorConfigService).getEnabledConfigs(shardingParam);
    }

    @Test
    void testExecuteMonitoringWithSharding_InvalidParam() {
        // Given
        String shardingParam = "invalid";
        when(monitorConfigService.getEnabledConfigs(shardingParam))
            .thenThrow(new IllegalArgumentException("分片参数格式错误"));

        // When & Then
        assertThrows(RuntimeException.class, () -> dbMonitorService.executeMonitoring(shardingParam));
    }

    @Test
    void testExecuteMonitoringWithoutSharding() {
        // Given
        when(monitorConfigService.getEnabledConfigs()).thenReturn(testConfigs);

        // When & Then - 验证不抛出异常
        assertDoesNotThrow(() -> dbMonitorService.executeMonitoring(null));
        
        // 验证调用了正确的方法
        verify(monitorConfigService).getEnabledConfigs();
    }

    @Test
    void testExecuteMonitoringWithEmptyShardingParam() {
        // Given
        when(monitorConfigService.getEnabledConfigs()).thenReturn(testConfigs);

        // When & Then - 验证不抛出异常
        assertDoesNotThrow(() -> dbMonitorService.executeMonitoring(""));
        
        // 验证调用了正确的方法（空字符串应该走非分片模式）
        verify(monitorConfigService).getEnabledConfigs();
    }
}
