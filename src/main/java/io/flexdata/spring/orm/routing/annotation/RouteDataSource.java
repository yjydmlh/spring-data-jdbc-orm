package io.flexdata.spring.orm.routing.annotation;

import java.lang.annotation.*;

/**
 * 数据源路由注解
 * 用于指定方法或类使用的数据源
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RouteDataSource {

    /**
     * 数据源名称
     * 支持SpEL表达式，如：#{#userId % 4 == 0 ? 'shard0' : 'shard1'}
     */
    String value();

    /**
     * 路由条件
     * SpEL表达式，当条件为true时才应用此路由
     */
    String condition() default "";

    /**
     * 优先级
     * 数值越大优先级越高，默认为0
     */
    int priority() default 0;

    /**
     * 是否继承父级路由
     * 当方法级注解与类级注解同时存在时的处理策略
     */
    boolean inherit() default true;

    /**
     * 路由策略
     * 可选值：auto, manual, conditional
     */
    String strategy() default "auto";

    /**
     * 备用数据源
     * 当主数据源不可用时使用的备用数据源
     */
    String fallback() default "";

    /**
     * 是否强制路由
     * 如果为true，即使路由失败也不会使用默认数据源
     */
    boolean force() default false;
}