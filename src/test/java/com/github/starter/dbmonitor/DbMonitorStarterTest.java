package com.github.starter.dbmonitor;

import com.github.starter.dbmonitor.config.DbMonitorProperties;
import com.github.starter.dbmonitor.service.DbMonitorService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * 数据库监控Starter测试
 */
@SpringBootTest
@TestPropertySource(properties = {
    "db.monitor.enabled=true",
    "db.monitor.table-names=test_table",
    "db.monitor.time-interval.type=MINUTES",
    "db.monitor.time-interval.value=5"
})
public class DbMonitorStarterTest {
    
    @Test
    public void contextLoads() {
        // 测试Spring上下文是否正确加载
    }
    
    @Test
    public void testConfiguration() {
        // 测试配置是否正确
    }
}