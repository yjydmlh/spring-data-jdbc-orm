package com.spring.jdbc.orm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RowMapper配置类
 * 用于配置数据库类型映射的相关选项
 */
@Component
@ConfigurationProperties(prefix = "spring.jdbc.orm.rowmapper")
public class RowMapperConfig {
    
    /**
     * 是否启用通用RowMapper（支持多种数据库类型）
     * 默认启用
     */
    private boolean enableUniversal = true;
    
    /**
     * 是否启用严格模式
     * 严格模式下，类型转换失败会抛出异常
     * 非严格模式下，转换失败会返回null或默认值
     */
    private boolean strictMode = false;
    
    /**
     * JSON解析失败时的处理策略
     * RETURN_NULL: 返回null
     * RETURN_STRING: 返回原始字符串
     * THROW_EXCEPTION: 抛出异常
     */
    private JsonFailureStrategy jsonFailureStrategy = JsonFailureStrategy.RETURN_STRING;
    
    /**
     * 数组类型处理策略
     * AUTO_DETECT: 自动检测数组类型
     * STRING_SPLIT: 使用字符串分割处理
     * JDBC_ARRAY: 使用JDBC Array接口
     */
    private ArrayHandlingStrategy arrayHandlingStrategy = ArrayHandlingStrategy.AUTO_DETECT;
    
    /**
     * UUID处理策略
     * STRING: 作为字符串处理
     * BINARY: 作为二进制处理
     * AUTO: 自动检测
     */
    private UuidHandlingStrategy uuidHandlingStrategy = UuidHandlingStrategy.AUTO;
    
    // Getters and Setters
    public boolean isEnableUniversal() {
        return enableUniversal;
    }
    
    public void setEnableUniversal(boolean enableUniversal) {
        this.enableUniversal = enableUniversal;
    }
    
    public boolean isStrictMode() {
        return strictMode;
    }
    
    public void setStrictMode(boolean strictMode) {
        this.strictMode = strictMode;
    }
    
    public JsonFailureStrategy getJsonFailureStrategy() {
        return jsonFailureStrategy;
    }
    
    public void setJsonFailureStrategy(JsonFailureStrategy jsonFailureStrategy) {
        this.jsonFailureStrategy = jsonFailureStrategy;
    }
    
    public ArrayHandlingStrategy getArrayHandlingStrategy() {
        return arrayHandlingStrategy;
    }
    
    public void setArrayHandlingStrategy(ArrayHandlingStrategy arrayHandlingStrategy) {
        this.arrayHandlingStrategy = arrayHandlingStrategy;
    }
    
    public UuidHandlingStrategy getUuidHandlingStrategy() {
        return uuidHandlingStrategy;
    }
    
    public void setUuidHandlingStrategy(UuidHandlingStrategy uuidHandlingStrategy) {
        this.uuidHandlingStrategy = uuidHandlingStrategy;
    }
    
    /**
     * JSON解析失败策略枚举
     */
    public enum JsonFailureStrategy {
        RETURN_NULL,
        RETURN_STRING,
        THROW_EXCEPTION
    }
    
    /**
     * 数组处理策略枚举
     */
    public enum ArrayHandlingStrategy {
        AUTO_DETECT,
        STRING_SPLIT,
        JDBC_ARRAY
    }
    
    /**
     * UUID处理策略枚举
     */
    public enum UuidHandlingStrategy {
        STRING,
        BINARY,
        AUTO
    }
}