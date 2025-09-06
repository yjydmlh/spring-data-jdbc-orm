package io.flexdata.spring.orm.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能监控插件
 */
@Component
@ConditionalOnProperty(name = "orm.plugin.performance.enabled", havingValue = "true")
public class PerformanceMonitorPlugin implements OrmPlugin {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitorPlugin.class);

    private final ConcurrentHashMap<String, AtomicLong> sqlExecutionCount = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> sqlExecutionTime = new ConcurrentHashMap<>();
    private volatile boolean enabled = true;

    @Override
    public String getName() {
        return "PerformanceMonitor";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "监控SQL执行性能";
    }

    @Override
    public void initialize() {
        logger.info("性能监控插件已启动");
    }

    @Override
    public void destroy() {
        sqlExecutionCount.clear();
        sqlExecutionTime.clear();
        logger.info("性能监控插件已销毁");
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void afterSqlExecution(String sql, long executionTime, Object... params) {
        String sqlKey = extractSqlKey(sql);
        sqlExecutionCount.computeIfAbsent(sqlKey, k -> new AtomicLong(0)).incrementAndGet();
        sqlExecutionTime.computeIfAbsent(sqlKey, k -> new AtomicLong(0)).addAndGet(executionTime);

        if (executionTime > 1000) { // 超过1秒的慢查询
            logger.warn("慢查询检测: SQL={}, 执行时间={}ms", sql, executionTime);
        }
    }

    /**
     * 获取SQL执行统计
     */
    public void logStatistics() {
        logger.info("=== SQL执行统计 ===");
        sqlExecutionCount.forEach((sql, count) -> {
            AtomicLong totalTime = sqlExecutionTime.get(sql);
            long avgTime = totalTime != null ? totalTime.get() / count.get() : 0;
            logger.info("SQL: {}, 执行次数: {}, 平均耗时: {}ms", sql, count.get(), avgTime);
        });
    }

    private String extractSqlKey(String sql) {
        // 提取SQL的主要部分，忽略参数
        return sql.replaceAll("\\?|:\\w+", "?").replaceAll("\\s+", " ").trim();
    }
}
