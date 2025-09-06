package io.flexdata.spring.orm.routing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 路由规则配置类
 * 支持从YAML配置文件中读取各种路由策略配置
 */
@Component
@ConfigurationProperties(prefix = "flexdata.routing")
public class RoutingRuleConfig {

    /**
     * 默认数据源
     */
    private String defaultDataSource = "main";

    /**
     * 数据源别名映射
     */
    private Map<String, String> aliases;

    /**
     * 读写分离配置
     */
    private ReadWriteSplitConfig readWriteSplit;

    /**
     * 分片配置
     */
    private Map<String, ShardingConfig> sharding;

    /**
     * 多租户配置
     */
    private MultiTenantConfig multiTenant;

    /**
     * 表名映射配置
     */
    private Map<String, String> tableMappings;

    /**
     * 自定义路由规则
     */
    private List<CustomRoutingRule> customRules;

    /**
     * 负载均衡配置
     */
    private Map<String, LoadBalanceConfig> loadBalance = new HashMap<>();

    /**
     * 缓存配置
     */
    private CacheConfig cache;

    // Getters and Setters
    public String getDefaultDataSource() {
        return defaultDataSource;
    }

    public void setDefaultDataSource(String defaultDataSource) {
        this.defaultDataSource = defaultDataSource;
    }

    public Map<String, String> getAliases() {
        return aliases;
    }

    public void setAliases(Map<String, String> aliases) {
        this.aliases = aliases;
    }

    public ReadWriteSplitConfig getReadWriteSplit() {
        return readWriteSplit;
    }

    public void setReadWriteSplit(ReadWriteSplitConfig readWriteSplit) {
        this.readWriteSplit = readWriteSplit;
    }

    public Map<String, ShardingConfig> getSharding() {
        return sharding;
    }

    public void setSharding(Map<String, ShardingConfig> sharding) {
        this.sharding = sharding;
    }

    public MultiTenantConfig getMultiTenant() {
        return multiTenant;
    }

    public void setMultiTenant(MultiTenantConfig multiTenant) {
        this.multiTenant = multiTenant;
    }

    public Map<String, String> getTableMappings() {
        return tableMappings;
    }

    public void setTableMappings(Map<String, String> tableMappings) {
        this.tableMappings = tableMappings;
    }

    public List<CustomRoutingRule> getCustomRules() {
        return customRules;
    }

    public void setCustomRules(List<CustomRoutingRule> customRules) {
        this.customRules = customRules;
    }

    public Map<String, LoadBalanceConfig> getLoadBalance() {
        return loadBalance;
    }

    public void setLoadBalance(Map<String, LoadBalanceConfig> loadBalance) {
        this.loadBalance = loadBalance;
    }

    public CacheConfig getCache() {
        return cache;
    }

    public void setCache(CacheConfig cache) {
        this.cache = cache;
    }

    /**
     * 读写分离配置
     */
    public static class ReadWriteSplitConfig {
        private boolean enabled = false;
        private String masterDataSource;
        private List<String> slaveDataSources;
        private String strategy = "round_robin"; // round_robin, random, weight
        private Map<String, Integer> weights; // 权重配置

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getMasterDataSource() {
            return masterDataSource;
        }

        public void setMasterDataSource(String masterDataSource) {
            this.masterDataSource = masterDataSource;
        }

        public List<String> getSlaveDataSources() {
            return slaveDataSources;
        }

        public void setSlaveDataSources(List<String> slaveDataSources) {
            this.slaveDataSources = slaveDataSources;
        }

        public String getStrategy() {
            return strategy;
        }

        public void setStrategy(String strategy) {
            this.strategy = strategy;
        }

        public Map<String, Integer> getWeights() {
            return weights;
        }

        public void setWeights(Map<String, Integer> weights) {
            this.weights = weights;
        }
    }

    /**
     * 分片配置
     */
    public static class ShardingConfig {
        private boolean enabled = false;
        private String strategy = "mod"; // mod, range, hash, custom
        private String shardingKey;
        private int shardCount = 2;
        private String tableTemplate = "{table}_{0}";
        private Map<String, String> dataSourceMapping;
        private List<RangeConfig> ranges; // 范围分片配置
        private String customExpression; // SpEL表达式

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getStrategy() {
            return strategy;
        }

        public void setStrategy(String strategy) {
            this.strategy = strategy;
        }

        public String getShardingKey() {
            return shardingKey;
        }

        public void setShardingKey(String shardingKey) {
            this.shardingKey = shardingKey;
        }

        public int getShardCount() {
            return shardCount;
        }

        public void setShardCount(int shardCount) {
            this.shardCount = shardCount;
        }

        public String getTableTemplate() {
            return tableTemplate;
        }

        public void setTableTemplate(String tableTemplate) {
            this.tableTemplate = tableTemplate;
        }

        public Map<String, String> getDataSourceMapping() {
            return dataSourceMapping;
        }

        public void setDataSourceMapping(Map<String, String> dataSourceMapping) {
            this.dataSourceMapping = dataSourceMapping;
        }

        public List<RangeConfig> getRanges() {
            return ranges;
        }

        public void setRanges(List<RangeConfig> ranges) {
            this.ranges = ranges;
        }

        public String getCustomExpression() {
            return customExpression;
        }

        public void setCustomExpression(String customExpression) {
            this.customExpression = customExpression;
        }
    }

    /**
     * 范围配置
     */
    public static class RangeConfig {
        private String start;
        private String end;
        private String dataSource;
        private String tableSuffix;

        // Getters and Setters
        public String getStart() {
            return start;
        }

        public void setStart(String start) {
            this.start = start;
        }

        public String getEnd() {
            return end;
        }

        public void setEnd(String end) {
            this.end = end;
        }

        public String getDataSource() {
            return dataSource;
        }

        public void setDataSource(String dataSource) {
            this.dataSource = dataSource;
        }

        public String getTableSuffix() {
            return tableSuffix;
        }

        public void setTableSuffix(String tableSuffix) {
            this.tableSuffix = tableSuffix;
        }
    }

    /**
     * 多租户配置
     */
    public static class MultiTenantConfig {
        private boolean enabled = false;
        private String strategy = "datasource"; // datasource, schema, table
        private String tenantResolver = "header"; // header, subdomain, parameter, custom
        private String tenantKey = "tenant-id";
        private String defaultTenant = "default";
        private Map<String, String> tenantMappings;
        private String customExpression; // SpEL表达式

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getStrategy() {
            return strategy;
        }

        public void setStrategy(String strategy) {
            this.strategy = strategy;
        }

        public String getTenantResolver() {
            return tenantResolver;
        }

        public void setTenantResolver(String tenantResolver) {
            this.tenantResolver = tenantResolver;
        }

        public String getTenantKey() {
            return tenantKey;
        }

        public void setTenantKey(String tenantKey) {
            this.tenantKey = tenantKey;
        }

        public String getDefaultTenant() {
            return defaultTenant;
        }

        public void setDefaultTenant(String defaultTenant) {
            this.defaultTenant = defaultTenant;
        }

        public Map<String, String> getTenantMappings() {
            return tenantMappings;
        }

        public void setTenantMappings(Map<String, String> tenantMappings) {
            this.tenantMappings = tenantMappings;
        }

        public String getCustomExpression() {
            return customExpression;
        }

        public void setCustomExpression(String customExpression) {
            this.customExpression = customExpression;
        }
    }

    /**
     * 自定义路由规则
     */
    public static class CustomRoutingRule {
        private String name;
        private String condition; // SpEL表达式条件
        private String dataSource;
        private String table;
        private int priority = 0;
        private boolean enabled = true;

        // Getters and Setters
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getCondition() {
            return condition;
        }

        public void setCondition(String condition) {
            this.condition = condition;
        }

        public String getDataSource() {
            return dataSource;
        }

        public void setDataSource(String dataSource) {
            this.dataSource = dataSource;
        }

        public String getTable() {
            return table;
        }

        public void setTable(String table) {
            this.table = table;
        }

        public int getPriority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * 负载均衡配置
     */
    public static class LoadBalanceConfig {
        private String strategy = "round_robin"; // round_robin, random, weight, least_connections
        private List<String> dataSources;
        private Map<String, Integer> weights;
        private boolean healthCheck = true;
        private int healthCheckInterval = 30000; // 毫秒

        // Getters and Setters
        public String getStrategy() {
            return strategy;
        }

        public void setStrategy(String strategy) {
            this.strategy = strategy;
        }

        public List<String> getDataSources() {
            return dataSources;
        }

        public void setDataSources(List<String> dataSources) {
            this.dataSources = dataSources;
        }

        public Map<String, Integer> getWeights() {
            return weights;
        }

        public void setWeights(Map<String, Integer> weights) {
            this.weights = weights;
        }

        public boolean isHealthCheck() {
            return healthCheck;
        }

        public void setHealthCheck(boolean healthCheck) {
            this.healthCheck = healthCheck;
        }

        public int getHealthCheckInterval() {
            return healthCheckInterval;
        }

        public void setHealthCheckInterval(int healthCheckInterval) {
            this.healthCheckInterval = healthCheckInterval;
        }
    }

    /**
     * 缓存配置
     */
    public static class CacheConfig {
        private boolean enabled = false;
        private long defaultTtl = 300000; // 默认5分钟（毫秒）
        private int maxSize = 1000; // 默认最大1000条

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getDefaultTtl() {
            return defaultTtl;
        }

        public void setDefaultTtl(long defaultTtl) {
            this.defaultTtl = defaultTtl;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }
    }
}