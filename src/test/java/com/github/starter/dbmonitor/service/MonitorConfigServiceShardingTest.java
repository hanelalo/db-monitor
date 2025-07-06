package com.github.starter.dbmonitor.service;

import com.github.starter.dbmonitor.entity.MonitorConfig;
import com.github.starter.dbmonitor.repository.JdbcMonitorConfigRepository;
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
 * 监控配置服务分片查询功能测试
 */
@ExtendWith(MockitoExtension.class)
class MonitorConfigServiceShardingTest {

    @Mock
    private JdbcMonitorConfigRepository monitorConfigRepository;

    @InjectMocks
    private MonitorConfigService monitorConfigService;

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
            testConfigs.add(config);
        }
    }

    @Test
    void testGetEnabledConfigs_WithoutSharding() {
        // Given
        when(monitorConfigRepository.findAllEnabled()).thenReturn(testConfigs);

        // When
        List<MonitorConfig> result = monitorConfigService.getEnabledConfigs();

        // Then
        assertEquals(testConfigs.size(), result.size());
        verify(monitorConfigRepository).findAllEnabled();
        verify(monitorConfigRepository, never()).findAllEnabledWithSharding(anyInt(), anyInt());
    }

    @Test
    void testGetEnabledConfigs_WithSharding_ValidParam() {
        // Given
        String shardingParam = "1/3";
        List<MonitorConfig> shardConfigs = testConfigs.subList(0, 3);
        when(monitorConfigRepository.findAllEnabledWithSharding(1, 3)).thenReturn(shardConfigs);

        // When
        List<MonitorConfig> result = monitorConfigService.getEnabledConfigs(shardingParam);

        // Then
        assertEquals(3, result.size());
        verify(monitorConfigRepository).findAllEnabledWithSharding(1, 3);
        verify(monitorConfigRepository, never()).findAllEnabled();
    }

    @Test
    void testGetEnabledConfigs_WithSharding_NullParam() {
        // Given
        when(monitorConfigRepository.findAllEnabled()).thenReturn(testConfigs);

        // When
        List<MonitorConfig> result = monitorConfigService.getEnabledConfigs(null);

        // Then
        assertEquals(testConfigs.size(), result.size());
        verify(monitorConfigRepository).findAllEnabled();
        verify(monitorConfigRepository, never()).findAllEnabledWithSharding(anyInt(), anyInt());
    }

    @Test
    void testGetEnabledConfigs_WithSharding_EmptyParam() {
        // Given
        when(monitorConfigRepository.findAllEnabled()).thenReturn(testConfigs);

        // When
        List<MonitorConfig> result = monitorConfigService.getEnabledConfigs("");

        // Then
        assertEquals(testConfigs.size(), result.size());
        verify(monitorConfigRepository).findAllEnabled();
        verify(monitorConfigRepository, never()).findAllEnabledWithSharding(anyInt(), anyInt());
    }

    @Test
    void testGetEnabledConfigs_WithSharding_InvalidFormat() {
        // Given
        String shardingParam = "invalid";

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> monitorConfigService.getEnabledConfigs(shardingParam));
        
        assertTrue(exception.getMessage().contains("分片参数格式错误"));
        verify(monitorConfigRepository, never()).findAllEnabled();
        verify(monitorConfigRepository, never()).findAllEnabledWithSharding(anyInt(), anyInt());
    }

    @Test
    void testGetEnabledConfigs_WithSharding_InvalidValues() {
        // Given
        String shardingParam = "3/3"; // shardIndex >= shardTotal

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> monitorConfigService.getEnabledConfigs(shardingParam));
        
        assertTrue(exception.getMessage().contains("分片参数值错误"));
        verify(monitorConfigRepository, never()).findAllEnabled();
        verify(monitorConfigRepository, never()).findAllEnabledWithSharding(anyInt(), anyInt());
    }

    @Test
    void testGetEnabledConfigs_WithSharding_NegativeValues() {
        // Given
        String shardingParam = "-1/3";

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> monitorConfigService.getEnabledConfigs(shardingParam));
        
        assertTrue(exception.getMessage().contains("分片参数值错误"));
    }

    @Test
    void testGetEnabledConfigs_WithSharding_ZeroTotal() {
        // Given
        String shardingParam = "0/0";

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> monitorConfigService.getEnabledConfigs(shardingParam));
        
        assertTrue(exception.getMessage().contains("分片参数值错误"));
    }

    @Test
    void testGetEnabledConfigs_WithSharding_NonNumericValues() {
        // Given
        String shardingParam = "a/b";

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, 
            () -> monitorConfigService.getEnabledConfigs(shardingParam));
        
        assertTrue(exception.getMessage().contains("无法解析数字"));
    }

    @Test
    void testGetEnabledConfigs_WithSharding_EdgeCase() {
        // Given
        String shardingParam = "0/1"; // 单分片
        when(monitorConfigRepository.findAllEnabledWithSharding(0, 1)).thenReturn(testConfigs);

        // When
        List<MonitorConfig> result = monitorConfigService.getEnabledConfigs(shardingParam);

        // Then
        assertEquals(testConfigs.size(), result.size());
        verify(monitorConfigRepository).findAllEnabledWithSharding(0, 1);
    }
}
