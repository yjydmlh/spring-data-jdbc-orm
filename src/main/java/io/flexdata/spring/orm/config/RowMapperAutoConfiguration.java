package io.flexdata.spring.orm.config;

import io.flexdata.spring.orm.core.mapper.RowMapperFactory;
import io.flexdata.spring.orm.core.metadata.EntityMetadataRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RowMapper自动配置类
 * 提供RowMapperFactory的默认配置
 */
@Configuration
public class RowMapperAutoConfiguration {

    /**
     * 创建默认的RowMapperFactory Bean
     * 如果用户没有自定义RowMapperFactory，则使用默认配置
     */
    @Bean
    @ConditionalOnMissingBean
    public RowMapperFactory rowMapperFactory(EntityMetadataRegistry metadataRegistry) {
        return new RowMapperFactory(metadataRegistry);
    }

    /**
     * 创建默认的RowMapperConfig Bean
     * 如果用户没有自定义配置，则使用默认配置
     */
    @Bean
    @ConditionalOnMissingBean
    public RowMapperConfig rowMapperConfig() {
        return new RowMapperConfig();
    }
}