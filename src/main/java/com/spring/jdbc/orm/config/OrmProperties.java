package com.spring.jdbc.orm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ORM框架配置属性
 */
@ConfigurationProperties(prefix = "spring.jdbc.orm")
public class OrmProperties {

    /**
     * 是否启用SQL日志
     */
    private boolean enableSqlLogging = false;

    /**
     * 是否启用性能监控
     */
    private boolean enablePerformanceMonitoring = false;

    /**
     * 元数据缓存大小
     */
    private int metadataCacheSize = 1000;

    /**
     * 是否启用批量操作优化
     */
    private boolean enableBatchOptimization = true;

    // Getters and Setters
    public boolean isEnableSqlLogging() {
        return enableSqlLogging;
    }

    public void setEnableSqlLogging(boolean enableSqlLogging) {
        this.enableSqlLogging = enableSqlLogging;
    }

    public boolean isEnablePerformanceMonitoring() {
        return enablePerformanceMonitoring;
    }

    public void setEnablePerformanceMonitoring(boolean enablePerformanceMonitoring) {
        this.enablePerformanceMonitoring = enablePerformanceMonitoring;
    }

    public int getMetadataCacheSize() {
        return metadataCacheSize;
    }

    public void setMetadataCacheSize(int metadataCacheSize) {
        this.metadataCacheSize = metadataCacheSize;
    }

    public boolean isEnableBatchOptimization() {
        return enableBatchOptimization;
    }

    public void setEnableBatchOptimization(boolean enableBatchOptimization) {
        this.enableBatchOptimization = enableBatchOptimization;
    }
}
