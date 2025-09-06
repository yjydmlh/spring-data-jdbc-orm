package io.flexdata.spring.orm.health;

import io.flexdata.spring.orm.core.metadata.EntityMetadataRegistry;
import io.flexdata.spring.orm.plugin.OrmPluginManager;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * ORM框架健康检查指示器
 */
@Component
@ConditionalOnClass(HealthIndicator.class)
public class OrmHealthIndicator implements HealthIndicator {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final EntityMetadataRegistry metadataRegistry;
    private final OrmPluginManager pluginManager;

    public OrmHealthIndicator(NamedParameterJdbcTemplate jdbcTemplate,
                              EntityMetadataRegistry metadataRegistry,
                              OrmPluginManager pluginManager) {
        this.jdbcTemplate = jdbcTemplate;
        this.metadataRegistry = metadataRegistry;
        this.pluginManager = pluginManager;
    }

    @Override
    public Health health() {
        try {
            Map<String, Object> details = new HashMap<>();

            // 检查数据库连接
            boolean dbConnected = checkDatabaseConnection();
            details.put("database.connected", dbConnected);

            // 检查元数据缓存
            int cacheSize = metadataRegistry.getCacheSize();
            details.put("metadata.cacheSize", cacheSize);

            // 检查插件状态
            int enabledPlugins = pluginManager.getEnabledPlugins().size();
            int totalPlugins = pluginManager.getAllPlugins().size();
            details.put("plugins.enabled", enabledPlugins);
            details.put("plugins.total", totalPlugins);

            // 添加插件详细信息
            Map<String, String> pluginDetails = new HashMap<>();
            pluginManager.getAllPlugins().forEach(plugin -> {
                pluginDetails.put(plugin.getName(), plugin.isEnabled() ? "UP" : "DOWN");
            });
            details.put("plugins.details", pluginDetails);

            if (dbConnected) {
                return Health.up()
                        .withDetails(details)
                        .build();
            } else {
                return Health.down()
                        .withDetail("reason", "数据库连接失败")
                        .withDetails(details)
                        .build();
            }

        } catch (Exception e) {
            return Health.down()
                    .withException(e)
                    .build();
        }
    }

    private boolean checkDatabaseConnection() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", new HashMap<>(), Integer.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
