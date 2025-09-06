package com.spring.jdbc.orm.core.table;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 动态表名注解
 * 用于在方法级别指定要操作的表名
 * 支持表达式和动态值
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Table {
    
    /**
     * 表名映射配置
     * 格式："logicalTableName:physicalTableName"
     * 支持多个映射，用逗号分隔
     * 例如："user:user_2024,order:order_2024"
     * 
     * @return 表名映射配置
     */
    String[] value() default {};
    
    /**
     * 逻辑表名
     * 当只需要映射单个表时使用
     * 
     * @return 逻辑表名
     */
    String logicalName() default "";
    
    /**
     * 物理表名
     * 当只需要映射单个表时使用
     * 支持SpEL表达式
     * 
     * @return 物理表名
     */
    String physicalName() default "";
    
    /**
     * 是否继承类级别的表名映射
     * 默认为true，表示方法级别的映射会与类级别的映射合并
     * 如果为false，则只使用方法级别的映射
     * 
     * @return 是否继承类级别映射
     */
    boolean inherit() default true;
    
    /**
     * 表名映射的优先级
     * 数值越小优先级越高
     * 用于解决多个注解冲突时的优先级问题
     * 
     * @return 优先级
     */
    int priority() default 0;
}