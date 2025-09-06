package io.flexdata.spring.orm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 类型安全ORM配置属性
 */
@ConfigurationProperties(prefix = "orm")
public class TypeSafeOrmProperties {

    /**
     * 功能开关
     */
    private Features features = new Features();

    /**
     * 插件配置
     */
    private Plugin plugin = new Plugin();

    /**
     * 性能配置
     */
    private Performance performance = new Performance();

    public static class Features {
        /**
         * 启用事务性操作
         */
        private boolean transactional = true;

        /**
         * 启用批量操作
         */
        private boolean batch = true;

        /**
         * 启用缓存
         */
        private boolean cache = false;

        /**
         * 启用审计
         */
        private boolean audit = false;

        // Getters and Setters
        public boolean isTransactional() {
            return transactional;
        }

        public void setTransactional(boolean transactional) {
            this.transactional = transactional;
        }

        public boolean isBatch() {
            return batch;
        }

        public void setBatch(boolean batch) {
            this.batch = batch;
        }

        public boolean isCache() {
            return cache;
        }

        public void setCache(boolean cache) {
            this.cache = cache;
        }

        public boolean isAudit() {
            return audit;
        }

        public void setAudit(boolean audit) {
            this.audit = audit;
        }
    }

    public static class Plugin {
        /**
         * 性能监控插件
         */
        private PluginConfig performance = new PluginConfig();

        /**
         * 日志插件
         */
        private PluginConfig logging = new PluginConfig(true);

        /**
         * 验证插件
         */
        private PluginConfig validation = new PluginConfig();

        public static class PluginConfig {
            private boolean enabled = false;

            public PluginConfig() {}

            public PluginConfig(boolean enabled) {
                this.enabled = enabled;
            }

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }
        }

        // Getters and Setters
        public PluginConfig getPerformance() {
            return performance;
        }

        public void setPerformance(PluginConfig performance) {
            this.performance = performance;
        }

        public PluginConfig getLogging() {
            return logging;
        }

        public void setLogging(PluginConfig logging) {
            this.logging = logging;
        }

        public PluginConfig getValidation() {
            return validation;
        }

        public void setValidation(PluginConfig validation) {
            this.validation = validation;
        }
    }

    public static class Performance {
        /**
         * 慢查询阈值（毫秒）
         */
        private long slowQueryThreshold = 1000;

        /**
         * 批量操作大小
         */
        private int batchSize = 1000;

        /**
         * 连接池最大大小
         */
        private int maxPoolSize = 20;

        // Getters and Setters
        public long getSlowQueryThreshold() {
            return slowQueryThreshold;
        }

        public void setSlowQueryThreshold(long slowQueryThreshold) {
            this.slowQueryThreshold = slowQueryThreshold;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }
    }

    // Getters and Setters
    public Features getFeatures() {
        return features;
    }

    public void setFeatures(Features features) {
        this.features = features;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }

    public Performance getPerformance() {
        return performance;
    }

    public void setPerformance(Performance performance) {
        this.performance = performance;
    }
}
