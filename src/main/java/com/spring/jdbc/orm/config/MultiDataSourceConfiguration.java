package com.spring.jdbc.orm.config;

import com.spring.jdbc.orm.core.datasource.DynamicDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 多数据源配置类
 * 支持动态数据源切换
 */
@Configuration
@ConditionalOnProperty(name = "spring.jdbc.orm.multi-datasource.enabled", havingValue = "true")
public class MultiDataSourceConfiguration {
    
    /**
     * 主数据源（默认）
     */
    @Bean("defaultDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.default")
    public DataSource defaultDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    /**
     * 从数据源（只读）
     */
    @Bean("readOnlyDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.readonly")
    @ConditionalOnProperty(name = "spring.datasource.readonly.url")
    public DataSource readOnlyDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    /**
     * 第二个数据源
     */
    @Bean("secondaryDataSource")
    @ConfigurationProperties(prefix = "spring.datasource.secondary")
    @ConditionalOnProperty(name = "spring.datasource.secondary.url")
    public DataSource secondaryDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    /**
     * 动态数据源
     */
    @Bean
    @Primary
    public DynamicDataSource dynamicDataSource() {
        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        
        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("default", defaultDataSource());
        
        // 添加只读数据源（如果配置了）
        try {
            DataSource readOnlyDs = readOnlyDataSource();
            if (readOnlyDs != null) {
                targetDataSources.put("readonly", readOnlyDs);
                targetDataSources.put("slave", readOnlyDs); // 别名
            }
        } catch (Exception e) {
            // 只读数据源未配置，忽略
        }
        
        // 添加第二个数据源（如果配置了）
        try {
            DataSource secondaryDs = secondaryDataSource();
            if (secondaryDs != null) {
                targetDataSources.put("secondary", secondaryDs);
            }
        } catch (Exception e) {
            // 第二个数据源未配置，忽略
        }
        
        dynamicDataSource.setTargetDataSources(targetDataSources);
        dynamicDataSource.setDefaultTargetDataSource(defaultDataSource());
        
        return dynamicDataSource;
    }
    
    /**
     * 基于动态数据源的JdbcTemplate
     */
    @Bean
    @Primary
    public NamedParameterJdbcTemplate namedParameterJdbcTemplate() {
        return new NamedParameterJdbcTemplate(dynamicDataSource());
    }
}