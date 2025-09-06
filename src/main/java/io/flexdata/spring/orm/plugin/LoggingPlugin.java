package io.flexdata.spring.orm.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 日志记录插件
 */
@Component
@ConditionalOnProperty(name = "orm.plugin.logging.enabled", havingValue = "true", matchIfMissing = true)
public class LoggingPlugin implements OrmPlugin {

    private static final Logger logger = LoggerFactory.getLogger(LoggingPlugin.class);
    private volatile boolean enabled = true;

    @Override
    public String getName() {
        return "Logging";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "记录SQL执行日志";
    }

    @Override
    public void initialize() {
        logger.info("日志记录插件已启动");
    }

    @Override
    public void destroy() {
        logger.info("日志记录插件已销毁");
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void beforeSqlExecution(String sql, Object... params) {
        if (logger.isDebugEnabled()) {
            logger.debug("执行SQL: {}, 参数: {}", sql, Arrays.toString(params));
        }
    }

    @Override
    public void afterSqlExecution(String sql, long executionTime, Object... params) {
        if (logger.isDebugEnabled()) {
            logger.debug("SQL执行完成: {}, 耗时: {}ms", sql, executionTime);
        }
    }

    @Override
    public void beforeEntitySave(Object entity) {
        if (logger.isTraceEnabled()) {
            logger.trace("保存实体前: {}", entity);
        }
    }

    @Override
    public void afterEntitySave(Object entity) {
        if (logger.isTraceEnabled()) {
            logger.trace("保存实体后: {}", entity);
        }
    }
}