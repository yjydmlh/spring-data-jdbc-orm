package com.spring.jdbc.orm.annotation;

import java.lang.annotation.*;

/**
 * 数据源切换注解
 * 用于方法级别的数据源指定
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataSource {
    
    /**
     * 数据源标识
     * @return 数据源key
     */
    String value() default "default";
    
    /**
     * 数据源别名（常用预定义值）
     */
    enum Type {
        /**
         * 默认数据源（主库）
         */
        DEFAULT("default"),
        
        /**
         * 主库（写操作）
         */
        MASTER("default"),
        
        /**
         * 从库（只读）
         */
        SLAVE("readonly"),
        
        /**
         * 只读数据源
         */
        READONLY("readonly"),
        
        /**
         * 第二个数据源
         */
        SECONDARY("secondary");
        
        private final String key;
        
        Type(String key) {
            this.key = key;
        }
        
        public String getKey() {
            return key;
        }
    }
    
    /**
     * 使用预定义的数据源类型
     * @return 数据源类型
     */
    Type type() default Type.DEFAULT;
}