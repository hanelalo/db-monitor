package com.example.incrementmonitor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
@ConditionalOnClass(DataSource.class)
@EnableConfigurationProperties(DataIncrementMonitorProperties.class)
public class DataIncrementMonitorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DataIncrementMonitorService dataIncrementMonitorService() {
        // TODO: later implement the real service
        return new DataIncrementMonitorService();
    }

    /**
     * Placeholder service bean so that applications can autowire it while we
     * flesh out the actual monitoring implementation in later steps.
     */
    public static class DataIncrementMonitorService {
        // empty for now
    }
}