package io.flexdata.spring.orm.routing.engine;

import io.flexdata.spring.orm.routing.config.RoutingRuleConfig;
import io.flexdata.spring.orm.routing.config.RoutingCacheManager;
import io.flexdata.spring.orm.routing.config.RoutingMonitor;
import io.flexdata.spring.orm.routing.selector.DataSourceSelector;
import io.flexdata.spring.orm.routing.context.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 路由策略引擎
 * 负责根据配置的路由规则自动选择合适的数据源和表名
 */
@Component
public class RoutingEngine {

    private static final Logger logger = LoggerFactory.getLogger(RoutingEngine.class);

    private final RoutingRuleConfig routingConfig;
    private final SpelExpressionEvaluator spelEvaluator;
    private final List<DataSourceSelector> dataSourceSelectors;
    private RoutingCacheManager cacheManager;
    private RoutingMonitor routingMonitor;

    public RoutingEngine(RoutingRuleConfig routingConfig, 
                        SpelExpressionEvaluator spelEvaluator,
                        List<DataSourceSelector> dataSourceSelectors) {
        this.routingConfig = routingConfig;
        this.spelEvaluator = spelEvaluator;
        this.dataSourceSelectors = dataSourceSelectors;
    }

    /**
     * 设置缓存管理器
     */
    public void setCacheManager(RoutingCacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    /**
     * 设置监控器
     */
    public void setRoutingMonitor(RoutingMonitor routingMonitor) {
        this.routingMonitor = routingMonitor;
    }

    /**
     * 路由结果
     */
    public static class RoutingResult {
        private String dataSource;
        private String tableName;
        private String reason;

        public RoutingResult(String dataSource, String tableName, String reason) {
            this.dataSource = dataSource;
            this.tableName = tableName;
            this.reason = reason;
        }

        // Getters and Setters
        public String getDataSource() {
            return dataSource;
        }

        public void setDataSource(String dataSource) {
            this.dataSource = dataSource;
        }

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    /**
     * 执行路由决策
     * 
     * @param context 路由上下文
     * @return 路由结果
     */
    public RoutingResult route(RoutingContext context) {
        long startTime = System.currentTimeMillis();
        String cacheKey = generateCacheKey(context);
        
        // 尝试从缓存获取路由结果
         RoutingCacheManager.RoutingResult cachedResult = null;
         if (cacheManager != null) {
             cachedResult = cacheManager.get(cacheKey);
             if (cachedResult != null) {
                 if (routingMonitor != null) {
                     routingMonitor.recordCacheHit();
                 }
                 logger.debug("Cache hit for routing key: {}", cacheKey);
                 return new RoutingResult(cachedResult.getDataSource(), cachedResult.getTable(), "Cache hit");
             }
         }
         
         if (routingMonitor != null) {
             routingMonitor.recordCacheMiss();
         }
         
         RoutingResult result = performRouting(context);
         
         // 缓存路由结果
         if (result != null && cacheManager != null) {
             RoutingCacheManager.RoutingResult cacheResult = 
                 new RoutingCacheManager.RoutingResult(result.getDataSource(), result.getTableName());
             cacheManager.put(cacheKey, cacheResult);
         }
         
         long duration = System.currentTimeMillis() - startTime;
         if (routingMonitor != null) {
             routingMonitor.recordRoutingDuration(duration);
             if (result != null) {
                 routingMonitor.recordDataSourceUsage(result.getDataSource());
                 routingMonitor.recordTableUsage(result.getTableName());
             }
         }
        
        logger.debug("Routing completed for table {} in {}ms, result: {}", 
                    context.getTableName(), duration, result.getDataSource());
        
        return result;
    }
    
    /**
     * 执行实际的路由逻辑
     */
    private RoutingResult performRouting(RoutingContext context) {
        // 1. 首先检查自定义路由规则
        RoutingResult customResult = evaluateCustomRules(context);
        if (customResult != null) {
            return customResult;
        }

        // 2. 检查多租户路由
        RoutingResult tenantResult = evaluateMultiTenantRouting(context);
        if (tenantResult != null) {
            return tenantResult;
        }

        // 3. 检查分片路由
        RoutingResult shardingResult = evaluateShardingRouting(context);
        if (shardingResult != null) {
            return shardingResult;
        }

        // 4. 检查读写分离
        RoutingResult readWriteResult = evaluateReadWriteSplitRouting(context);
        if (readWriteResult != null) {
            return readWriteResult;
        }

        // 5. 检查负载均衡
        RoutingResult loadBalanceResult = evaluateLoadBalanceRouting(context);
        if (loadBalanceResult != null) {
            return loadBalanceResult;
        }

        // 6. 使用数据源选择器
        for (DataSourceSelector selector : dataSourceSelectors) {
            if (selector.supports(context)) {
                String dataSource = selector.selectDataSource(context);
                if (StringUtils.hasText(dataSource)) {
                    String tableName = resolveTableName(context, dataSource);
                    return new RoutingResult(dataSource, tableName, "DataSourceSelector: " + selector.getClass().getSimpleName());
                }
            }
        }

        // 7. 返回默认数据源
        String defaultDataSource = routingConfig.getDefaultDataSource();
        String tableName = resolveTableName(context, defaultDataSource);
        return new RoutingResult(defaultDataSource, tableName, "Default routing");
    }
    
    /**
     * 生成缓存键
     */
    private String generateCacheKey(RoutingContext context) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(context.getTableName());
        keyBuilder.append(":").append(context.getOperationType());
        
        // 添加关键参数到缓存键
        Map<String, Object> parameters = context.getParameters();
        if (parameters != null && !parameters.isEmpty()) {
            parameters.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> keyBuilder.append(":").append(entry.getKey()).append("=").append(entry.getValue()));
        }
        
        return keyBuilder.toString();
    }

    /**
     * 评估自定义路由规则
     */
    private RoutingResult evaluateCustomRules(RoutingContext context) {
        List<RoutingRuleConfig.CustomRoutingRule> customRules = routingConfig.getCustomRules();
        if (customRules == null || customRules.isEmpty()) {
            return null;
        }

        // 按优先级排序
        customRules.sort((r1, r2) -> Integer.compare(r2.getPriority(), r1.getPriority()));

        for (RoutingRuleConfig.CustomRoutingRule rule : customRules) {
            if (!rule.isEnabled()) {
                continue;
            }

            if (StringUtils.hasText(rule.getCondition())) {
                try {
                    boolean matches = spelEvaluator.evaluateCondition(rule.getCondition(), context);
                    if (matches) {
                        String dataSource = resolveDataSourceExpression(rule.getDataSource(), context);
                        String tableName = resolveTableExpression(rule.getTable(), context);
                        return new RoutingResult(dataSource, tableName, "Custom rule: " + rule.getName());
                    }
                } catch (Exception e) {
                     // 记录日志但继续处理其他规则
                     logger.warn("Error evaluating custom rule '{}': {}", rule.getName(), e.getMessage());
                     if (routingMonitor != null) {
                         routingMonitor.recordRoutingError();
                     }
                 }
            }
        }

        return null;
    }

    /**
     * 评估多租户路由
     */
    private RoutingResult evaluateMultiTenantRouting(RoutingContext context) {
        RoutingRuleConfig.MultiTenantConfig config = routingConfig.getMultiTenant();
        if (config == null || !config.isEnabled()) {
            return null;
        }

        String tenantId = resolveTenantId(context, config);
        if (!StringUtils.hasText(tenantId)) {
            tenantId = config.getDefaultTenant();
        }

        String dataSource = null;
        String tableName = context.getTableName();

        // 根据多租户策略选择数据源或表名
        switch (config.getStrategy()) {
            case "datasource":
                Map<String, String> tenantMappings = config.getTenantMappings();
                if (tenantMappings != null) {
                    dataSource = tenantMappings.get(tenantId);
                }
                break;
            case "schema":
                // Schema级别的多租户，通常需要在表名前加schema前缀
                tableName = tenantId + "." + tableName;
                break;
            case "table":
                // 表级别的多租户，在表名后加租户后缀
                tableName = tableName + "_" + tenantId;
                break;
        }

        if (StringUtils.hasText(dataSource) || !tableName.equals(context.getTableName())) {
            return new RoutingResult(dataSource, tableName, "Multi-tenant routing: " + tenantId);
        }

        return null;
    }

    /**
     * 评估分片路由
     */
    private RoutingResult evaluateShardingRouting(RoutingContext context) {
        Map<String, RoutingRuleConfig.ShardingConfig> shardingConfigs = routingConfig.getSharding();
        if (shardingConfigs == null || shardingConfigs.isEmpty()) {
            return null;
        }

        String tableName = context.getTableName();
        RoutingRuleConfig.ShardingConfig config = shardingConfigs.get(tableName);
        if (config == null || !config.isEnabled()) {
            return null;
        }

        Object shardingValue = context.getParameter(config.getShardingKey());
        if (shardingValue == null) {
            return null;
        }

        String dataSource = null;
        String actualTableName = tableName;

        switch (config.getStrategy()) {
            case "mod":
                int shardIndex = Math.abs(shardingValue.hashCode()) % config.getShardCount();
                dataSource = getShardDataSource(config, shardIndex);
                actualTableName = config.getTableTemplate().replace("{0}", String.valueOf(shardIndex));
                break;
            case "range":
                RoutingRuleConfig.RangeConfig rangeConfig = findRangeConfig(config, shardingValue);
                if (rangeConfig != null) {
                    dataSource = rangeConfig.getDataSource();
                    if (StringUtils.hasText(rangeConfig.getTableSuffix())) {
                        actualTableName = tableName + "_" + rangeConfig.getTableSuffix();
                    }
                }
                break;
            case "hash":
                int hashIndex = Math.abs(shardingValue.toString().hashCode()) % config.getShardCount();
                dataSource = getShardDataSource(config, hashIndex);
                actualTableName = config.getTableTemplate().replace("{0}", String.valueOf(hashIndex));
                break;
            case "custom":
                if (StringUtils.hasText(config.getCustomExpression())) {
                    try {
                        dataSource = spelEvaluator.evaluateExpression(config.getCustomExpression(), context, String.class);
                    } catch (Exception e) {
                         logger.warn("Error evaluating custom sharding expression: {}", e.getMessage());
                         if (routingMonitor != null) {
                             routingMonitor.recordRoutingError();
                         }
                     }
                }
                break;
        }

        if (StringUtils.hasText(dataSource)) {
            return new RoutingResult(dataSource, actualTableName, "Sharding routing: " + config.getStrategy());
        }

        return null;
    }

    /**
     * 评估读写分离路由
     */
    private RoutingResult evaluateReadWriteSplitRouting(RoutingContext context) {
        RoutingRuleConfig.ReadWriteSplitConfig config = routingConfig.getReadWriteSplit();
        if (config == null || !config.isEnabled()) {
            return null;
        }

        String dataSource;
        if (context.isReadOperation()) {
            // 读操作，选择从库
            dataSource = selectSlaveDataSource(config);
        } else {
            // 写操作，选择主库
            dataSource = config.getMasterDataSource();
        }

        if (StringUtils.hasText(dataSource)) {
            String tableName = resolveTableName(context, dataSource);
            return new RoutingResult(dataSource, tableName, "Read-write split: " + (context.isReadOperation() ? "read" : "write"));
        }

        return null;
    }

    /**
     * 评估负载均衡路由
     */
    private RoutingResult evaluateLoadBalanceRouting(RoutingContext context) {
        Map<String, RoutingRuleConfig.LoadBalanceConfig> loadBalanceConfigs = routingConfig.getLoadBalance();
        if (loadBalanceConfigs == null || loadBalanceConfigs.isEmpty()) {
            return null;
        }

        // 这里可以根据具体的负载均衡组名来选择配置
        // 暂时使用第一个配置作为示例
        RoutingRuleConfig.LoadBalanceConfig config = loadBalanceConfigs.values().iterator().next();
        
        String dataSource = selectLoadBalancedDataSource(config);
        if (StringUtils.hasText(dataSource)) {
            String tableName = resolveTableName(context, dataSource);
            return new RoutingResult(dataSource, tableName, "Load balance: " + config.getStrategy());
        }

        return null;
    }

    /**
     * 解析租户ID
     */
    private String resolveTenantId(RoutingContext context, RoutingRuleConfig.MultiTenantConfig config) {
        if (StringUtils.hasText(config.getCustomExpression())) {
            try {
                return spelEvaluator.evaluateExpression(config.getCustomExpression(), context, String.class);
            } catch (Exception e) {
                 logger.warn("Error evaluating tenant expression: {}", e.getMessage());
                 if (routingMonitor != null) {
                     routingMonitor.recordRoutingError();
                 }
             }
        }

        // 根据租户解析策略获取租户ID
        switch (config.getTenantResolver()) {
            case "header":
                return context.getHeader(config.getTenantKey());
            case "parameter":
                Object param = context.getParameter(config.getTenantKey());
                return param != null ? param.toString() : null;
            case "subdomain":
                // 从请求URL中解析子域名
                // 这里需要根据实际的HTTP请求上下文来实现
                return null;
            default:
                return null;
        }
    }

    /**
     * 解析数据源表达式
     */
    private String resolveDataSourceExpression(String expression, RoutingContext context) {
        if (!StringUtils.hasText(expression)) {
            return null;
        }

        if (expression.startsWith("#{") && expression.endsWith("}")) {
            // SpEL表达式
            try {
                return spelEvaluator.evaluateExpression(expression, context, String.class);
            } catch (Exception e) {
                 logger.warn("Error evaluating dataSource expression: {}", e.getMessage());
                 if (routingMonitor != null) {
                     routingMonitor.recordRoutingError();
                 }
                 return null;
             }
        }

        // 检查别名映射
        Map<String, String> aliases = routingConfig.getAliases();
        if (aliases != null && aliases.containsKey(expression)) {
            return aliases.get(expression);
        }

        return expression;
    }

    /**
     * 解析表名表达式
     */
    private String resolveTableExpression(String expression, RoutingContext context) {
        if (!StringUtils.hasText(expression)) {
            return context.getTableName();
        }

        if (expression.startsWith("#{") && expression.endsWith("}")) {
            // SpEL表达式
            try {
                return spelEvaluator.evaluateExpression(expression, context, String.class);
            } catch (Exception e) {
                 logger.warn("Error evaluating table expression: {}", e.getMessage());
                 if (routingMonitor != null) {
                     routingMonitor.recordRoutingError();
                 }
                 return context.getTableName();
             }
        }

        return expression;
    }

    /**
     * 解析表名
     */
    private String resolveTableName(RoutingContext context, String dataSource) {
        String tableName = context.getTableName();
        
        // 检查表名映射
        Map<String, String> tableMappings = routingConfig.getTableMappings();
        if (tableMappings != null && tableMappings.containsKey(tableName)) {
            return tableMappings.get(tableName);
        }

        return tableName;
    }

    /**
     * 获取分片数据源
     */
    private String getShardDataSource(RoutingRuleConfig.ShardingConfig config, int shardIndex) {
        Map<String, String> dataSourceMapping = config.getDataSourceMapping();
        if (dataSourceMapping != null) {
            return dataSourceMapping.get(String.valueOf(shardIndex));
        }
        return "shard" + shardIndex;
    }

    /**
     * 查找范围配置
     */
    private RoutingRuleConfig.RangeConfig findRangeConfig(RoutingRuleConfig.ShardingConfig config, Object value) {
        List<RoutingRuleConfig.RangeConfig> ranges = config.getRanges();
        if (ranges == null || ranges.isEmpty()) {
            return null;
        }

        String strValue = value.toString();
        for (RoutingRuleConfig.RangeConfig range : ranges) {
            if (isInRange(strValue, range.getStart(), range.getEnd())) {
                return range;
            }
        }

        return null;
    }

    /**
     * 检查值是否在范围内
     */
    private boolean isInRange(String value, String start, String end) {
        try {
            // 尝试数值比较
            long longValue = Long.parseLong(value);
            long longStart = Long.parseLong(start);
            long longEnd = Long.parseLong(end);
            return longValue >= longStart && longValue <= longEnd;
        } catch (NumberFormatException e) {
            // 字符串比较
            return value.compareTo(start) >= 0 && value.compareTo(end) <= 0;
        }
    }

    /**
     * 选择从库数据源
     */
    private String selectSlaveDataSource(RoutingRuleConfig.ReadWriteSplitConfig config) {
        List<String> slaves = config.getSlaveDataSources();
        if (slaves == null || slaves.isEmpty()) {
            return config.getMasterDataSource(); // 没有从库时使用主库
        }

        switch (config.getStrategy()) {
            case "random":
                return slaves.get(ThreadLocalRandom.current().nextInt(slaves.size()));
            case "weight":
                return selectWeightedDataSource(slaves, config.getWeights());
            case "round_robin":
            default:
                // 简单的轮询实现（实际应用中需要考虑线程安全）
                return slaves.get((int) (System.currentTimeMillis() % slaves.size()));
        }
    }

    /**
     * 选择负载均衡数据源
     */
    private String selectLoadBalancedDataSource(RoutingRuleConfig.LoadBalanceConfig config) {
        List<String> dataSources = config.getDataSources();
        if (dataSources == null || dataSources.isEmpty()) {
            return null;
        }

        switch (config.getStrategy()) {
            case "random":
                return dataSources.get(ThreadLocalRandom.current().nextInt(dataSources.size()));
            case "weight":
                return selectWeightedDataSource(dataSources, config.getWeights());
            case "round_robin":
            default:
                return dataSources.get((int) (System.currentTimeMillis() % dataSources.size()));
        }
    }

    /**
     * 根据权重选择数据源
     */
    private String selectWeightedDataSource(List<String> dataSources, Map<String, Integer> weights) {
        if (weights == null || weights.isEmpty()) {
            return dataSources.get(ThreadLocalRandom.current().nextInt(dataSources.size()));
        }

        int totalWeight = weights.values().stream().mapToInt(Integer::intValue).sum();
        int randomWeight = ThreadLocalRandom.current().nextInt(totalWeight);
        
        int currentWeight = 0;
        for (String dataSource : dataSources) {
            Integer weight = weights.get(dataSource);
            if (weight != null) {
                currentWeight += weight;
                if (randomWeight < currentWeight) {
                    return dataSource;
                }
            }
        }

        return dataSources.get(0); // 默认返回第一个
    }

    /**
     * 获取自定义规则数量
     */
    public int getCustomRulesCount() {
        return routingConfig.getCustomRules() != null ? routingConfig.getCustomRules().size() : 0;
    }

    /**
     * 检查读写分离是否启用
     */
    public boolean isReadWriteSplitEnabled() {
        return routingConfig.getReadWriteSplit() != null && routingConfig.getReadWriteSplit().isEnabled();
    }

    /**
     * 检查多租户是否启用
     */
    public boolean isMultiTenantEnabled() {
        return routingConfig.getMultiTenant() != null && routingConfig.getMultiTenant().isEnabled();
    }

    /**
     * 检查分片是否启用
     */
    public boolean isShardingEnabled() {
        return routingConfig.getSharding() != null && 
               routingConfig.getSharding().values().stream().anyMatch(config -> config.isEnabled());
    }

    /**
     * 获取可用的数据源列表
     */
    public java.util.Set<String> getAvailableDataSources() {
        java.util.Set<String> dataSources = new java.util.HashSet<>();
        
        // 添加默认数据源
        if (routingConfig.getDefaultDataSource() != null) {
            dataSources.add(routingConfig.getDefaultDataSource());
        }
        
        // 添加别名映射中的数据源
        if (routingConfig.getAliases() != null) {
            dataSources.addAll(routingConfig.getAliases().values());
        }
        
        // 添加读写分离数据源
        if (routingConfig.getReadWriteSplit() != null) {
            RoutingRuleConfig.ReadWriteSplitConfig rwConfig = routingConfig.getReadWriteSplit();
            if (rwConfig.getMasterDataSource() != null) {
                dataSources.add(rwConfig.getMasterDataSource());
            }
            if (rwConfig.getSlaveDataSources() != null) {
                dataSources.addAll(rwConfig.getSlaveDataSources());
            }
        }
        
        // 添加分片数据源
        if (routingConfig.getSharding() != null) {
            routingConfig.getSharding().values().forEach(config -> {
                if (config.getDataSourceMapping() != null) {
                    dataSources.addAll(config.getDataSourceMapping().values());
                }
            });
        }
        
        // 添加多租户数据源
        if (routingConfig.getMultiTenant() != null && 
            routingConfig.getMultiTenant().getTenantMappings() != null) {
            dataSources.addAll(routingConfig.getMultiTenant().getTenantMappings().values());
        }
        
        // 添加负载均衡组中的数据源
        if (routingConfig.getLoadBalance() != null) {
            routingConfig.getLoadBalance().values().forEach(config -> {
                if (config.getDataSources() != null) {
                    dataSources.addAll(config.getDataSources());
                }
            });
        }
        
        return dataSources;
    }
}