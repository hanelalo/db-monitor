package com.github.starter.dbmonitor.config.condition;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.annotation.*;

/**
 * 条件注解：当配置管理端点启用时
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnProperty(
    prefix = "db.monitor.metrics.endpoints", 
    name = "config-enabled", 
    havingValue = "true", 
    matchIfMissing = true
)
public @interface ConditionalOnConfigEndpointsEnabled {
}
