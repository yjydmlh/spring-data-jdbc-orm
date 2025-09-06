package com.spring.jdbc.orm.core.table;

import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

/**
 * 表名上下文管理器
 * 提供线程安全的动态表名切换功能
 */
public class TableContext {
    
    private static final ThreadLocal<Map<String, String>> TABLE_MAPPINGS = new ThreadLocal<>();
    
    /**
     * 设置表名映射
     * @param logicalTableName 逻辑表名（实体类对应的表名）
     * @param physicalTableName 物理表名（实际数据库中的表名）
     */
    public static void setTableMapping(String logicalTableName, String physicalTableName) {
        Assert.hasText(logicalTableName, "Logical table name cannot be null or empty");
        Assert.hasText(physicalTableName, "Physical table name cannot be null or empty");
        
        Map<String, String> mappings = TABLE_MAPPINGS.get();
        if (mappings == null) {
            mappings = new HashMap<>();
            TABLE_MAPPINGS.set(mappings);
        }
        mappings.put(logicalTableName, physicalTableName);
    }
    
    /**
     * 获取表名映射
     * @param logicalTableName 逻辑表名
     * @return 物理表名，如果未设置映射则返回逻辑表名本身
     */
    public static String getTableMapping(String logicalTableName) {
        Map<String, String> mappings = TABLE_MAPPINGS.get();
        if (mappings == null || !mappings.containsKey(logicalTableName)) {
            return logicalTableName;
        }
        return mappings.get(logicalTableName);
    }
    
    /**
     * 移除特定表名映射
     * @param logicalTableName 逻辑表名
     */
    public static void removeTableMapping(String logicalTableName) {
        Map<String, String> mappings = TABLE_MAPPINGS.get();
        if (mappings != null) {
            mappings.remove(logicalTableName);
            if (mappings.isEmpty()) {
                TABLE_MAPPINGS.remove();
            }
        }
    }
    
    /**
     * 清除当前线程的所有表名映射
     */
    public static void clearTableMappings() {
        TABLE_MAPPINGS.remove();
    }
    
    /**
     * 获取当前线程的所有表名映射
     * @return 表名映射的副本，如果没有映射则返回空Map
     */
    public static Map<String, String> getAllTableMappings() {
        Map<String, String> mappings = TABLE_MAPPINGS.get();
        return mappings == null ? new HashMap<>() : new HashMap<>(mappings);
    }
    
    /**
     * 在指定表名映射的作用域内执行操作
     * @param logicalTableName 逻辑表名
     * @param physicalTableName 物理表名
     * @param action 要执行的操作
     * @param <T> 返回值类型
     * @return 操作结果
     */
    public static <T> T executeWithTableMapping(String logicalTableName, String physicalTableName, TableAction<T> action) {
        String previousMapping = getTableMapping(logicalTableName);
        boolean hadMapping = !previousMapping.equals(logicalTableName);
        
        try {
            setTableMapping(logicalTableName, physicalTableName);
            return action.execute();
        } finally {
            if (hadMapping) {
                setTableMapping(logicalTableName, previousMapping);
            } else {
                removeTableMapping(logicalTableName);
            }
        }
    }
    
    /**
     * 在指定多个表名映射的作用域内执行操作
     * @param tableMappings 表名映射
     * @param action 要执行的操作
     * @param <T> 返回值类型
     * @return 操作结果
     */
    public static <T> T executeWithTableMappings(Map<String, String> tableMappings, TableAction<T> action) {
        Map<String, String> previousMappings = getAllTableMappings();
        
        try {
            // 设置新的映射
            for (Map.Entry<String, String> entry : tableMappings.entrySet()) {
                setTableMapping(entry.getKey(), entry.getValue());
            }
            return action.execute();
        } finally {
            // 恢复之前的映射
            clearTableMappings();
            for (Map.Entry<String, String> entry : previousMappings.entrySet()) {
                setTableMapping(entry.getKey(), entry.getValue());
            }
        }
    }
    
    /**
     * 在指定表名映射的作用域内执行操作（无返回值）
     * @param logicalTableName 逻辑表名
     * @param physicalTableName 物理表名
     * @param action 要执行的操作
     */
    public static void executeWithTableMapping(String logicalTableName, String physicalTableName, Runnable action) {
        executeWithTableMapping(logicalTableName, physicalTableName, () -> {
            action.run();
            return null;
        });
    }
    
    /**
     * 表操作函数式接口
     * @param <T> 返回值类型
     */
    @FunctionalInterface
    public interface TableAction<T> {
        T execute();
    }
}