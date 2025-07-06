package com.github.starter.dbmonitor.config.condition;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.annotation.*;

/**
 * 条件注解：当监控端点功能启用时
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ConditionalOnProperty(
    prefix = "db.monitor.metrics.endpoints", 
    name = "enabled", 
    havingValue = "true", 
    matchIfMissing = true
)
public @interface ConditionalOnEndpointsEnabled {
}
