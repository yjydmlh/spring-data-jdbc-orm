package io.flexdata.spring.orm.routing.annotation;

import java.lang.annotation.*;

/**
 * 自动路由注解
 * 用于在方法或类级别启用自动路由功能
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AutoRouting {

    /**
     * 是否启用自动路由
     * 默认为true
     */
    boolean enabled() default true;

    /**
     * 路由策略
     * 可以指定特定的路由策略名称
     */
    String strategy() default "";

    /**
     * 优先级
     * 数值越大优先级越高
     */
    int priority() default 0;

    /**
     * 自定义路由规则表达式
     * 支持SpEL表达式
     */
    String condition() default "";

    /**
     * 目标数据源
     * 支持SpEL表达式
     */
    String dataSource() default "";

    /**
     * 目标表名
     * 支持SpEL表达式
     */
    String table() default "";

    /**
     * 是否缓存路由结果
     * 默认为false
     */
    boolean cache() default false;

    /**
     * 缓存键表达式
     * 仅在cache=true时有效
     */
    String cacheKey() default "";

    /**
     * 缓存过期时间（秒）
     * 默认300秒（5分钟）
     */
    int cacheExpire() default 300;
}