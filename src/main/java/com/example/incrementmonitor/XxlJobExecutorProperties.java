package com.example.incrementmonitor;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "xxl.job.executor")
public class XxlJobExecutorProperties {

    /**
     * XXL-Job admin addresses, multiple separated by comma. (e.g. http://address1,http://address2)
     */
    private String adminAddresses;

    /**
     * Executor app name.
     */
    private String appname = "data-increment-monitor-executor";

    /**
     * Executor IP. If not specified, it will be automatically obtained.
     */
    private String ip;

    /**
     * Executor port, default 0 (random).
     */
    private int port = 0;

    /**
     * Access token.
     */
    private String accessToken;

    /**
     * Log path.
     */
    private String logPath = "/tmp/xxl-job-logs";

    /**
     * Log retention days.
     */
    private int logRetentionDays = 30;

    public String getAdminAddresses() {
        return adminAddresses;
    }

    public void setAdminAddresses(String adminAddresses) {
        this.adminAddresses = adminAddresses;
    }

    public String getAppname() {
        return appname;
    }

    public void setAppname(String appname) {
        this.appname = appname;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public int getLogRetentionDays() {
        return logRetentionDays;
    }

    public void setLogRetentionDays(int logRetentionDays) {
        this.logRetentionDays = logRetentionDays;
    }
}