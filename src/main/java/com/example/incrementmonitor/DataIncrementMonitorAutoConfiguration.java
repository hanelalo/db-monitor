package com.example.incrementmonitor;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
@ConditionalOnClass(DataSource.class)
@EnableConfigurationProperties(DataIncrementMonitorProperties.class)
@org.springframework.scheduling.annotation.EnableScheduling
public class DataIncrementMonitorAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public DataIncrementMonitorService dataIncrementMonitorService(javax.sql.DataSource dataSource,
                                                                   org.springframework.jdbc.core.JdbcTemplate jdbcTemplate,
                                                                   DataIncrementMonitorProperties properties) {
        return new DataIncrementMonitorService(dataSource, jdbcTemplate, properties);
    }
}