package io.flexdata.spring.orm.routing.config;

import io.flexdata.spring.orm.routing.engine.RoutingEngine;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.scheduling.annotation.Scheduled;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * 路由监控器
 * 收集和暴露路由引擎的运行指标和统计信息
 * 
 * @author FlexData
 * @since 1.0.0
 */
@Endpoint(id = "routing")
public class RoutingMonitor {

    private static final Logger logger = LoggerFactory.getLogger(RoutingMonitor.class);
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RoutingEngine routingEngine;
    
    @Autowired(required = false)
    private MeterRegistry meterRegistry;

    // 路由统计指标
    private final Map<String, LongAdder> dataSourceUsageCount = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> tableUsageCount = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> ruleHitCount = new ConcurrentHashMap<>();
    private final AtomicLong totalRoutingCount = new AtomicLong(0);
    private final AtomicLong routingErrorCount = new AtomicLong(0);
    private final AtomicLong cacheHitCount = new AtomicLong(0);
    private final AtomicLong cacheMissCount = new AtomicLong(0);
    
    // Micrometer指标
    private Counter routingCounter;
    private Counter errorCounter;
    private Counter cacheHitCounter;
    private Counter cacheMissCounter;
    private Timer routingTimer;
    
    private final long startTime = System.currentTimeMillis();

    public RoutingMonitor(RoutingEngine routingEngine) {
        this.routingEngine = routingEngine;
    }

    @PostConstruct
    public void initMetrics() {
        if (meterRegistry != null) {
            // 注册Micrometer指标
            routingCounter = Counter.builder("routing.requests.total")
                    .description("Total number of routing requests")
                    .register(meterRegistry);
            
            errorCounter = Counter.builder("routing.errors.total")
                    .description("Total number of routing errors")
                    .register(meterRegistry);
            
            cacheHitCounter = Counter.builder("routing.cache.hits")
                    .description("Total number of routing cache hits")
                    .register(meterRegistry);
            
            cacheMissCounter = Counter.builder("routing.cache.misses")
                    .description("Total number of routing cache misses")
                    .register(meterRegistry);
            
            routingTimer = Timer.builder("routing.duration")
                    .description("Routing decision duration")
                    .register(meterRegistry);
            
            // 注册Gauge指标
            Gauge.builder("routing.datasources.active", this, monitor -> (double) dataSourceUsageCount.size())
                    .description("Number of active data sources")
                    .register(meterRegistry);
            
            Gauge.builder("routing.rules.active", this, monitor -> (double) routingEngine.getCustomRulesCount())
                    .description("Number of active routing rules")
                    .register(meterRegistry);
        }
    }

    /**
     * 记录数据源使用
     */
    public void recordDataSourceUsage(String dataSource) {
        if (dataSource != null) {
            dataSourceUsageCount.computeIfAbsent(dataSource, k -> new LongAdder()).increment();
            totalRoutingCount.incrementAndGet();
            
            if (routingCounter != null) {
                routingCounter.increment();
            }
        }
    }

    /**
     * 记录表使用
     */
    public void recordTableUsage(String table) {
        if (table != null) {
            tableUsageCount.computeIfAbsent(table, k -> new LongAdder()).increment();
        }
    }

    /**
     * 记录规则命中
     */
    public void recordRuleHit(String ruleName) {
        if (ruleName != null) {
            ruleHitCount.computeIfAbsent(ruleName, k -> new LongAdder()).increment();
        }
    }

    /**
     * 记录路由错误
     */
    public void recordRoutingError() {
        routingErrorCount.incrementAndGet();
        
        if (errorCounter != null) {
            errorCounter.increment();
        }
    }

    /**
     * 记录缓存命中
     */
    public void recordCacheHit() {
        cacheHitCount.incrementAndGet();
        
        if (cacheHitCounter != null) {
            cacheHitCounter.increment();
        }
    }

    /**
     * 记录缓存未命中
     */
    public void recordCacheMiss() {
        cacheMissCount.incrementAndGet();
        
        if (cacheMissCounter != null) {
            cacheMissCounter.increment();
        }
    }

    /**
     * 记录路由耗时
     */
    public void recordRoutingDuration(long durationMs) {
        if (routingTimer != null) {
            routingTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 获取路由统计信息
     */
    @ReadOperation
    public Map<String, Object> getRoutingStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 基本统计
        stats.put("start-time", LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(startTime), 
                java.time.ZoneId.systemDefault()).format(FORMATTER));
        stats.put("uptime-ms", System.currentTimeMillis() - startTime);
        stats.put("total-routing-requests", totalRoutingCount.get());
        stats.put("routing-errors", routingErrorCount.get());
        
        // 缓存统计
        long totalCacheRequests = cacheHitCount.get() + cacheMissCount.get();
        stats.put("cache-hit-count", cacheHitCount.get());
        stats.put("cache-miss-count", cacheMissCount.get());
        stats.put("cache-hit-rate", totalCacheRequests > 0 ? 
                String.format("%.2f%%", (double) cacheHitCount.get() / totalCacheRequests * 100) : "0.00%");
        
        // 数据源使用统计
        Map<String, Long> dsUsage = new HashMap<>();
        dataSourceUsageCount.forEach((ds, count) -> dsUsage.put(ds, count.sum()));
        stats.put("data-source-usage", dsUsage);
        
        // 表使用统计
        Map<String, Long> tableUsage = new HashMap<>();
        tableUsageCount.forEach((table, count) -> tableUsage.put(table, count.sum()));
        stats.put("table-usage", tableUsage);
        
        // 规则命中统计
        Map<String, Long> ruleHits = new HashMap<>();
        ruleHitCount.forEach((rule, count) -> ruleHits.put(rule, count.sum()));
        stats.put("rule-hits", ruleHits);
        
        // 路由引擎状态
        Map<String, Object> engineStatus = new HashMap<>();
        engineStatus.put("active-rules-count", routingEngine.getCustomRulesCount());
        engineStatus.put("read-write-split-enabled", routingEngine.isReadWriteSplitEnabled());
        engineStatus.put("multi-tenant-enabled", routingEngine.isMultiTenantEnabled());
        engineStatus.put("sharding-enabled", routingEngine.isShardingEnabled());
        stats.put("engine-status", engineStatus);
        
        return stats;
    }

    /**
     * 重置统计信息
     */
    public void resetStats() {
        dataSourceUsageCount.clear();
        tableUsageCount.clear();
        ruleHitCount.clear();
        totalRoutingCount.set(0);
        routingErrorCount.set(0);
        cacheHitCount.set(0);
        cacheMissCount.set(0);
        
        logger.info("Routing statistics have been reset");
    }

    /**
     * 定期输出统计信息到日志
     */
    @Scheduled(fixedRate = 300000) // 每5分钟
    public void logStats() {
        if (logger.isInfoEnabled() && totalRoutingCount.get() > 0) {
            Map<String, Object> stats = getRoutingStats();
            logger.info("Routing Statistics: {}", stats);
        }
    }
}