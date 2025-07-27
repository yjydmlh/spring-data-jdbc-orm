package com.spring.jdbc.orm.plugin;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * ORM插件管理器
 */
@Component
public class OrmPluginManager {
    private final List<OrmPlugin> plugins = new CopyOnWriteArrayList<>();

    /**
     * 注册插件
     */
    public void registerPlugin(OrmPlugin plugin) {
        if (plugin.isEnabled()) {
            plugins.add(plugin);
            plugin.initialize();
        }
    }

    /**
     * 注销插件
     */
    public void unregisterPlugin(OrmPlugin plugin) {
        if (plugins.remove(plugin)) {
            plugin.destroy();
        }
    }

    /**
     * 获取所有插件
     */
    public List<OrmPlugin> getAllPlugins() {
        return plugins.stream().collect(Collectors.toList());
    }

    /**
     * 获取启用的插件
     */
    public List<OrmPlugin> getEnabledPlugins() {
        return plugins.stream()
                .filter(OrmPlugin::isEnabled)
                .collect(Collectors.toList());
    }

    /**
     * 执行SQL前钩子
     */
    public void beforeSqlExecution(String sql, Object... params) {
        getEnabledPlugins().forEach(plugin -> plugin.beforeSqlExecution(sql, params));
    }

    /**
     * 执行SQL后钩子
     */
    public void afterSqlExecution(String sql, long executionTime, Object... params) {
        getEnabledPlugins().forEach(plugin -> plugin.afterSqlExecution(sql, executionTime, params));
    }

    /**
     * 实体保存前钩子
     */
    public void beforeEntitySave(Object entity) {
        getEnabledPlugins().forEach(plugin -> plugin.beforeEntitySave(entity));
    }

    /**
     * 实体保存后钩子
     */
    public void afterEntitySave(Object entity) {
        getEnabledPlugins().forEach(plugin -> plugin.afterEntitySave(entity));
    }
}
