package io.flexdata.spring.orm.routing.annotation;

import java.lang.annotation.*;

/**
 * 表路由注解
 * 用于指定方法或类使用的表名
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RouteTable {

    /**
     * 表名
     * 支持SpEL表达式，如：#{tableName + '_' + (#userId % 4)}
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
     * 分片策略
     * 可选值：mod, range, hash, custom
     */
    String shardingStrategy() default "";

    /**
     * 分片键
     * 用于分片计算的参数名
     */
    String shardingKey() default "";

    /**
     * 分片数量
     * 仅在使用mod或hash策略时有效
     */
    int shardCount() default 0;

    /**
     * 表名模板
     * 如：user_{0}, order_{0} 等
     */
    String template() default "";
}