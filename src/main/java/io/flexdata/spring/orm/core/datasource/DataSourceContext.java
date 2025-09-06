package io.flexdata.spring.orm.core.datasource;

import org.springframework.util.Assert;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据源上下文管理器
 * 提供线程安全的数据源切换功能
 * 支持任意多个数据源的配置和管理
 */
public class DataSourceContext {
    
    private static final ThreadLocal<String> CURRENT_DATASOURCE = new ThreadLocal<>();
    private static final ThreadLocal<Map<String, String>> DATASOURCE_MAPPINGS = new ThreadLocal<>();
    private static final Map<String, DataSourceInfo> REGISTERED_DATASOURCES = new ConcurrentHashMap<>();
    
    /**
     * 数据源信息
     */
    public static class DataSourceInfo {
        private final String key;
        private final String name;
        private final String description;
        private final Map<String, Object> properties;
        
        public DataSourceInfo(String key, String name, String description) {
            this.key = key;
            this.name = name;
            this.description = description;
            this.properties = new HashMap<>();
        }
        
        public String getKey() { return key; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public Map<String, Object> getProperties() { return properties; }
        
        public DataSourceInfo setProperty(String key, Object value) {
            properties.put(key, value);
            return this;
        }
    }
    
    /**
     * 设置当前线程的数据源
     * @param dataSourceKey 数据源标识
     */
    public static void setDataSource(String dataSourceKey) {
        Assert.hasText(dataSourceKey, "DataSource key cannot be null or empty");
        CURRENT_DATASOURCE.set(dataSourceKey);
    }
    
    /**
     * 获取当前线程的数据源
     * @return 数据源标识，如果未设置则返回null
     */
    public static String getDataSource() {
        return CURRENT_DATASOURCE.get();
    }
    
    /**
     * 清除当前线程的数据源设置
     */
    public static void clearDataSource() {
        CURRENT_DATASOURCE.remove();
    }
    
    /**
     * 在指定数据源上执行操作
     * @param dataSourceKey 数据源标识
     * @param action 要执行的操作
     * @param <T> 返回值类型
     * @return 操作结果
     */
    public static <T> T executeWithDataSource(String dataSourceKey, DataSourceAction<T> action) {
        String originalDataSource = getDataSource();
        try {
            setDataSource(dataSourceKey);
            return action.execute();
        } finally {
            if (originalDataSource != null) {
                setDataSource(originalDataSource);
            } else {
                clearDataSource();
            }
        }
    }
    
    /**
     * 在指定数据源上执行无返回值操作
     * @param dataSourceKey 数据源标识
     * @param action 要执行的操作
     */
    public static void executeWithDataSource(String dataSourceKey, Runnable action) {
        executeWithDataSource(dataSourceKey, () -> {
            action.run();
            return null;
        });
    }
    
    /**
     * 注册数据源信息
     * @param dataSourceInfo 数据源信息
     */
    public static void registerDataSource(DataSourceInfo dataSourceInfo) {
        Assert.notNull(dataSourceInfo, "DataSource info cannot be null");
        Assert.hasText(dataSourceInfo.getKey(), "DataSource key cannot be null or empty");
        REGISTERED_DATASOURCES.put(dataSourceInfo.getKey(), dataSourceInfo);
    }
    
    /**
     * 注册数据源
     * @param key 数据源标识
     * @param name 数据源名称
     * @param description 数据源描述
     */
    public static void registerDataSource(String key, String name, String description) {
        registerDataSource(new DataSourceInfo(key, name, description));
    }
    
    /**
     * 获取已注册的数据源信息
     * @param key 数据源标识
     * @return 数据源信息，如果未注册则返回null
     */
    public static DataSourceInfo getDataSourceInfo(String key) {
        return REGISTERED_DATASOURCES.get(key);
    }
    
    /**
     * 获取所有已注册的数据源
     * @return 数据源信息映射的副本
     */
    public static Map<String, DataSourceInfo> getAllDataSources() {
        return new HashMap<>(REGISTERED_DATASOURCES);
    }
    
    /**
     * 检查数据源是否已注册
     * @param key 数据源标识
     * @return 是否已注册
     */
    public static boolean isDataSourceRegistered(String key) {
        return REGISTERED_DATASOURCES.containsKey(key);
    }
    
    /**
     * 设置数据源映射
     * 用于将逻辑数据源名称映射到物理数据源标识
     * @param logicalName 逻辑数据源名称
     * @param physicalKey 物理数据源标识
     */
    public static void setDataSourceMapping(String logicalName, String physicalKey) {
        Assert.hasText(logicalName, "Logical name cannot be null or empty");
        Assert.hasText(physicalKey, "Physical key cannot be null or empty");
        
        Map<String, String> mappings = DATASOURCE_MAPPINGS.get();
        if (mappings == null) {
            mappings = new HashMap<>();
            DATASOURCE_MAPPINGS.set(mappings);
        }
        mappings.put(logicalName, physicalKey);
    }
    
    /**
     * 获取数据源映射
     * @param logicalName 逻辑数据源名称
     * @return 物理数据源标识，如果未设置映射则返回逻辑名称本身
     */
    public static String getDataSourceMapping(String logicalName) {
        Map<String, String> mappings = DATASOURCE_MAPPINGS.get();
        if (mappings == null || !mappings.containsKey(logicalName)) {
            return logicalName;
        }
        return mappings.get(logicalName);
    }
    
    /**
     * 清除数据源映射
     */
    public static void clearDataSourceMappings() {
        DATASOURCE_MAPPINGS.remove();
    }
    
    /**
     * 在指定数据源映射的作用域内执行操作
     * @param mappings 数据源映射
     * @param action 要执行的操作
     * @param <T> 返回值类型
     * @return 操作结果
     */
    public static <T> T executeWithDataSourceMappings(Map<String, String> mappings, DataSourceAction<T> action) {
        Map<String, String> originalMappings = DATASOURCE_MAPPINGS.get();
        Map<String, String> backupMappings = originalMappings != null ? new HashMap<>(originalMappings) : null;
        
        try {
            // 设置新的映射
            if (mappings != null && !mappings.isEmpty()) {
                for (Map.Entry<String, String> entry : mappings.entrySet()) {
                    setDataSourceMapping(entry.getKey(), entry.getValue());
                }
            }
            return action.execute();
        } finally {
            // 恢复原始映射
            clearDataSourceMappings();
            if (backupMappings != null) {
                for (Map.Entry<String, String> entry : backupMappings.entrySet()) {
                    setDataSourceMapping(entry.getKey(), entry.getValue());
                }
            }
        }
    }
    
    /**
     * 在多个数据源上顺序执行操作
     * @param dataSourceKeys 数据源标识列表
     * @param action 要执行的操作
     * @param <T> 返回值类型
     * @return 操作结果列表
     */
    public static <T> List<T> executeOnMultipleDataSources(List<String> dataSourceKeys, DataSourceAction<T> action) {
        List<T> results = new ArrayList<>();
        for (String dataSourceKey : dataSourceKeys) {
            T result = executeWithDataSource(dataSourceKey, action);
            results.add(result);
        }
        return results;
    }
    
    /**
     * 验证数据源是否可用
     * @param dataSourceKey 数据源标识
     * @return 是否可用
     */
    public static boolean validateDataSource(String dataSourceKey) {
        if (!isDataSourceRegistered(dataSourceKey)) {
            return false;
        }
        
        // 这里可以添加更多的验证逻辑，比如连接测试等
        // 目前只检查是否已注册
        return true;
    }
    
    /**
     * 数据源操作接口
     * @param <T> 返回值类型
     */
    @FunctionalInterface
    public interface DataSourceAction<T> {
        T execute();
    }
}