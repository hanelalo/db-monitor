package com.example.incrementmonitor;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "data.increment.monitor")
public class DataIncrementMonitorProperties {

    /**
     * Cron expression controlling how often the database is queried.
     * Defaults to every 10 minutes.
     */
    private String cron = "0 */10 * * * *";

    /**
     * Look-back time window (seconds) when calculating increment statistics.
     */
    private long lookbackSeconds = 600L;

    /**
     * Comma-separated list of table names or wildcard patterns (e.g. orders_*,user_login)
     * to be monitored.
     */
    private String tables;

    /**
     * Whether to register XXL-Job handler (if XXL framework present).
     */
    private boolean xxlJobEnabled = true;

    /**
     * Name of the XXL-Job handler registered by this starter.
     */
    private String xxlJobHandlerName = "dataIncrementMonitorJob";

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public long getLookbackSeconds() {
        return lookbackSeconds;
    }

    public void setLookbackSeconds(long lookbackSeconds) {
        this.lookbackSeconds = lookbackSeconds;
    }

    public String getTables() {
        return tables;
    }

    public void setTables(String tables) {
        this.tables = tables;
    }

    public boolean isXxlJobEnabled() {
        return xxlJobEnabled;
    }

    public void setXxlJobEnabled(boolean xxlJobEnabled) {
        this.xxlJobEnabled = xxlJobEnabled;
    }

    public String getXxlJobHandlerName() {
        return xxlJobHandlerName;
    }

    public void setXxlJobHandlerName(String xxlJobHandlerName) {
        this.xxlJobHandlerName = xxlJobHandlerName;
    }
}