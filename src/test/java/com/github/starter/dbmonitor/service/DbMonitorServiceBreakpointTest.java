package com.github.starter.dbmonitor.service;

import com.github.starter.dbmonitor.config.DbMonitorProperties;
import com.github.starter.dbmonitor.entity.MonitorConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 数据库监控服务断点续传功能测试
 */
@ExtendWith(MockitoExtension.class)
class DbMonitorServiceBreakpointTest {

    @Mock
    private DbMonitorProperties dbMonitorProperties;

    @Mock
    private MonitorConfigService monitorConfigService;

    @InjectMocks
    private DbMonitorService dbMonitorService;

    private MonitorConfig testConfig;

    @BeforeEach
    void setUp() {
        testConfig = new MonitorConfig();
        testConfig.setId(1L);
        testConfig.setConfigName("test_config");
        testConfig.setTableName("test_table");
        testConfig.setIntervalType("MINUTES");
        testConfig.setIntervalValue(10);
    }

    @Test
    void testCalculateTimeRanges_FirstTime() throws Exception {
        // Given
        testConfig.setLastStatisticTime(null); // 首次统计
        LocalDateTime currentTime = LocalDateTime.of(2025, 1, 1, 10, 0, 0);

        // When
        List<?> timeRanges = invokeCalculateTimeRanges(testConfig, currentTime);

        // Then
        assertEquals(1, timeRanges.size());
        // 验证时间范围是从当前时间往前推10分钟
    }

    @Test
    void testCalculateTimeRanges_BreakpointResume_ShortGap() throws Exception {
        // Given
        LocalDateTime lastStatisticTime = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        LocalDateTime currentTime = LocalDateTime.of(2025, 1, 1, 10, 10, 0);
        testConfig.setLastStatisticTime(lastStatisticTime);

        // When
        List<?> timeRanges = invokeCalculateTimeRanges(testConfig, currentTime);

        // Then
        assertEquals(1, timeRanges.size());
        // 验证时间范围是从10:00到10:10
    }

    @Test
    void testCalculateTimeRanges_BreakpointResume_LongGap() throws Exception {
        // Given
        LocalDateTime lastStatisticTime = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        LocalDateTime currentTime = LocalDateTime.of(2025, 1, 1, 12, 0, 0); // 2小时后
        testConfig.setLastStatisticTime(lastStatisticTime);

        // When
        List<?> timeRanges = invokeCalculateTimeRanges(testConfig, currentTime);

        // Then
        assertEquals(12, timeRanges.size()); // 2小时 = 12个10分钟时间段
    }

    @Test
    void testCalculateTimeRanges_BreakpointResume_PartialInterval() throws Exception {
        // Given
        LocalDateTime lastStatisticTime = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        LocalDateTime currentTime = LocalDateTime.of(2025, 1, 1, 10, 15, 0); // 15分钟后
        testConfig.setLastStatisticTime(lastStatisticTime);

        // When
        List<?> timeRanges = invokeCalculateTimeRanges(testConfig, currentTime);

        // Then
        assertEquals(2, timeRanges.size()); 
        // 第一个时间段：10:00-10:10
        // 第二个时间段：10:10-10:15
    }

    @Test
    void testCalculateTimeRanges_NoGap() throws Exception {
        // Given
        LocalDateTime lastStatisticTime = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        LocalDateTime currentTime = LocalDateTime.of(2025, 1, 1, 10, 0, 0); // 相同时间
        testConfig.setLastStatisticTime(lastStatisticTime);

        // When
        List<?> timeRanges = invokeCalculateTimeRanges(testConfig, currentTime);

        // Then
        assertEquals(0, timeRanges.size()); // 无需统计
    }

    @Test
    void testGetIntervalMinutes_Minutes() throws Exception {
        // Given
        testConfig.setIntervalType("MINUTES");
        testConfig.setIntervalValue(15);

        // When
        long intervalMinutes = invokeGetIntervalMinutes(testConfig);

        // Then
        assertEquals(15, intervalMinutes);
    }

    @Test
    void testGetIntervalMinutes_Hours() throws Exception {
        // Given
        testConfig.setIntervalType("HOURS");
        testConfig.setIntervalValue(2);

        // When
        long intervalMinutes = invokeGetIntervalMinutes(testConfig);

        // Then
        assertEquals(120, intervalMinutes); // 2小时 = 120分钟
    }

    @Test
    void testGetIntervalMinutes_Days() throws Exception {
        // Given
        testConfig.setIntervalType("DAYS");
        testConfig.setIntervalValue(1);

        // When
        long intervalMinutes = invokeGetIntervalMinutes(testConfig);

        // Then
        assertEquals(1440, intervalMinutes); // 1天 = 1440分钟
    }

    @Test
    void testGetIntervalMinutes_Unknown() throws Exception {
        // Given
        testConfig.setIntervalType("UNKNOWN");
        testConfig.setIntervalValue(30);

        // When
        long intervalMinutes = invokeGetIntervalMinutes(testConfig);

        // Then
        assertEquals(30, intervalMinutes); // 默认按分钟处理
    }

    // 使用反射调用私有方法进行测试
    private List<?> invokeCalculateTimeRanges(MonitorConfig config, LocalDateTime currentTime) throws Exception {
        Method method = DbMonitorService.class.getDeclaredMethod("calculateTimeRanges", MonitorConfig.class, LocalDateTime.class);
        method.setAccessible(true);
        return (List<?>) method.invoke(dbMonitorService, config, currentTime);
    }

    private long invokeGetIntervalMinutes(MonitorConfig config) throws Exception {
        Method method = DbMonitorService.class.getDeclaredMethod("getIntervalMinutes", MonitorConfig.class);
        method.setAccessible(true);
        return (Long) method.invoke(dbMonitorService, config);
    }
}
