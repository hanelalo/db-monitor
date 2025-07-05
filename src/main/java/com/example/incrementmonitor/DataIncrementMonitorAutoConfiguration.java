package com.example.incrementmonitor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.ObjectProvider;
import io.micrometer.core.instrument.MeterRegistry;

import javax.sql.DataSource;

@Configuration
@ConditionalOnClass(DataSource.class)
@EnableConfigurationProperties(DataIncrementMonitorProperties.class)
public class DataIncrementMonitorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DataIncrementMonitorService dataIncrementMonitorService(javax.sql.DataSource dataSource,
                                                                   org.springframework.jdbc.core.JdbcTemplate jdbcTemplate,
                                                                   DataIncrementMonitorProperties properties,
                                                                   ObjectProvider<MeterRegistry> meterRegistryProvider) {
        return new DataIncrementMonitorService(dataSource, jdbcTemplate, properties, meterRegistryProvider.getIfAvailable());
    }
}