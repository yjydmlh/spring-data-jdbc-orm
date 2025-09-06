package io.flexdata.spring.orm.plugin;

/**
 * ORM插件接口
 */
public interface OrmPlugin {
    /**
     * 插件名称
     */
    String getName();

    /**
     * 插件版本
     */
    String getVersion();

    /**
     * 插件描述
     */
    String getDescription();

    /**
     * 插件初始化
     */
    void initialize();

    /**
     * 插件销毁
     */
    void destroy();

    /**
     * 插件是否启用
     */
    boolean isEnabled();

    /**
     * SQL执行前钩子
     */
    default void beforeSqlExecution(String sql, Object... params) {}

    /**
     * SQL执行后钩子
     */
    default void afterSqlExecution(String sql, long executionTime, Object... params) {}

    /**
     * 实体保存前钩子
     */
    default void beforeEntitySave(Object entity) {}

    /**
     * 实体保存后钩子
     */
    default void afterEntitySave(Object entity) {}
}
