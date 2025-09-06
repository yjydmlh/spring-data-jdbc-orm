package io.flexdata.spring.orm.routing.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCache;
import org.springframework.cache.support.SimpleCacheManager;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 路由缓存管理器
 * 管理路由结果的缓存，支持TTL和LRU策略
 * 
 * @author FlexData
 * @since 1.0.0
 */
public class RoutingCacheManager {

    private static final Logger logger = LoggerFactory.getLogger(RoutingCacheManager.class);
    private static final String ROUTING_CACHE_NAME = "routing-cache";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final RoutingRuleConfig routingConfig;
    private final Map<String, CacheEntry> cacheMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor(
            r -> {
                Thread t = new Thread(r, "routing-cache-cleanup");
                t.setDaemon(true);
                return t;
            });

    // 缓存配置
    private final long defaultTtlMs;
    private final int maxCacheSize;
    private final boolean enableCache;

    public RoutingCacheManager(RoutingRuleConfig routingConfig) {
        this.routingConfig = routingConfig;
        this.enableCache = routingConfig.getCache() != null && routingConfig.getCache().isEnabled();
        this.defaultTtlMs = routingConfig.getCache() != null ? 
                routingConfig.getCache().getDefaultTtl() : 300000; // 默认5分钟
        this.maxCacheSize = routingConfig.getCache() != null ? 
                routingConfig.getCache().getMaxSize() : 1000; // 默认1000条
    }

    @PostConstruct
    public void init() {
        if (enableCache) {
            // 启动定期清理任务
            cleanupExecutor.scheduleAtFixedRate(this::cleanupExpiredEntries, 60, 60, TimeUnit.SECONDS);
            logger.info("Routing cache manager initialized with TTL: {}ms, Max Size: {}", 
                    defaultTtlMs, maxCacheSize);
        }
    }

    /**
     * 获取缓存的路由结果
     */
    public RoutingResult get(String cacheKey) {
        if (!enableCache || cacheKey == null) {
            return null;
        }

        CacheEntry entry = cacheMap.get(cacheKey);
        if (entry == null) {
            return null;
        }

        // 检查是否过期
        if (entry.isExpired()) {
            cacheMap.remove(cacheKey);
            return null;
        }

        // 更新访问时间（LRU）
        entry.updateAccessTime();
        return entry.getResult();
    }

    /**
     * 缓存路由结果
     */
    public void put(String cacheKey, RoutingResult result, long ttlMs) {
        if (!enableCache || cacheKey == null || result == null) {
            return;
        }

        // 检查缓存大小限制
        if (cacheMap.size() >= maxCacheSize) {
            evictLeastRecentlyUsed();
        }

        long actualTtl = ttlMs > 0 ? ttlMs : defaultTtlMs;
        CacheEntry entry = new CacheEntry(result, actualTtl);
        cacheMap.put(cacheKey, entry);
    }

    /**
     * 使用默认TTL缓存路由结果
     */
    public void put(String cacheKey, RoutingResult result) {
        put(cacheKey, result, defaultTtlMs);
    }

    /**
     * 移除缓存项
     */
    public void evict(String cacheKey) {
        if (cacheKey != null) {
            cacheMap.remove(cacheKey);
        }
    }

    /**
     * 清空所有缓存
     */
    public void clear() {
        cacheMap.clear();
        logger.info("Routing cache cleared");
    }

    /**
     * 获取缓存统计信息
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("enabled", enableCache);
        stats.put("size", cacheMap.size());
        stats.put("max-size", maxCacheSize);
        stats.put("default-ttl-ms", defaultTtlMs);
        
        if (enableCache) {
            long expiredCount = cacheMap.values().stream()
                    .mapToLong(entry -> entry.isExpired() ? 1 : 0)
                    .sum();
            stats.put("expired-entries", expiredCount);
            
            // 最近访问时间统计
            OptionalLong lastAccess = cacheMap.values().stream()
                    .mapToLong(CacheEntry::getLastAccessTime)
                    .max();
            if (lastAccess.isPresent()) {
                stats.put("last-access-time", LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(lastAccess.getAsLong()),
                        java.time.ZoneId.systemDefault()).format(FORMATTER));
            }
        }
        
        return stats;
    }

    /**
     * 生成缓存键
     */
    public static String generateCacheKey(String dataSource, String table, Map<String, Object> context) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append("ds:").append(dataSource != null ? dataSource : "null");
        keyBuilder.append("|table:").append(table != null ? table : "null");
        
        if (context != null && !context.isEmpty()) {
            // 对context进行排序以确保一致的缓存键
            TreeMap<String, Object> sortedContext = new TreeMap<>(context);
            keyBuilder.append("|ctx:");
            sortedContext.forEach((k, v) -> 
                    keyBuilder.append(k).append("=").append(v).append(";"));
        }
        
        return keyBuilder.toString();
    }

    /**
     * 清理过期的缓存项
     */
    private void cleanupExpiredEntries() {
        if (!enableCache) {
            return;
        }

        int removedCount = 0;
        Iterator<Map.Entry<String, CacheEntry>> iterator = cacheMap.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<String, CacheEntry> entry = iterator.next();
            if (entry.getValue().isExpired()) {
                iterator.remove();
                removedCount++;
            }
        }

        if (removedCount > 0) {
            logger.debug("Cleaned up {} expired routing cache entries", removedCount);
        }
    }

    /**
     * 驱逐最近最少使用的缓存项
     */
    private void evictLeastRecentlyUsed() {
        if (cacheMap.isEmpty()) {
            return;
        }

        String lruKey = null;
        long oldestAccessTime = Long.MAX_VALUE;

        for (Map.Entry<String, CacheEntry> entry : cacheMap.entrySet()) {
            long accessTime = entry.getValue().getLastAccessTime();
            if (accessTime < oldestAccessTime) {
                oldestAccessTime = accessTime;
                lruKey = entry.getKey();
            }
        }

        if (lruKey != null) {
            cacheMap.remove(lruKey);
            logger.debug("Evicted LRU routing cache entry: {}", lruKey);
        }
    }

    /**
     * 缓存项
     */
    private static class CacheEntry {
        private final RoutingResult result;
        private final long expirationTime;
        private volatile long lastAccessTime;

        public CacheEntry(RoutingResult result, long ttlMs) {
            this.result = result;
            this.lastAccessTime = System.currentTimeMillis();
            this.expirationTime = this.lastAccessTime + ttlMs;
        }

        public RoutingResult getResult() {
            return result;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expirationTime;
        }

        public long getLastAccessTime() {
            return lastAccessTime;
        }

        public void updateAccessTime() {
            this.lastAccessTime = System.currentTimeMillis();
        }
    }

    /**
     * 路由结果
     */
    public static class RoutingResult {
        private final String dataSource;
        private final String table;
        private final Map<String, Object> metadata;

        public RoutingResult(String dataSource, String table) {
            this(dataSource, table, null);
        }

        public RoutingResult(String dataSource, String table, Map<String, Object> metadata) {
            this.dataSource = dataSource;
            this.table = table;
            this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        }

        public String getDataSource() {
            return dataSource;
        }

        public String getTable() {
            return table;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        @Override
        public String toString() {
            return "RoutingResult{" +
                    "dataSource='" + dataSource + '\'' +
                    ", table='" + table + '\'' +
                    ", metadata=" + metadata +
                    '}';
        }
    }

    /**
     * 销毁资源
     */
    public void destroy() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        clear();
    }
}