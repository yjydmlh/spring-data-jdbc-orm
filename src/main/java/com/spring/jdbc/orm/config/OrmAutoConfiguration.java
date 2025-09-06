package com.spring.jdbc.orm.config;

import com.spring.jdbc.orm.core.mapper.RowMapperFactory;
import com.spring.jdbc.orm.core.metadata.EntityMetadataRegistry;
import com.spring.jdbc.orm.core.sql.SqlGenerator;
import com.spring.jdbc.orm.template.OrmTemplate;
import com.spring.jdbc.orm.template.TypeSafeOrmTemplate;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

/**
 * ORM框架自动配置类
 */
@Configuration
@ConditionalOnClass({NamedParameterJdbcTemplate.class, DataSource.class})
@AutoConfigureAfter(JdbcTemplateAutoConfiguration.class)
public class OrmAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public EntityMetadataRegistry entityMetadataRegistry() {
        return new EntityMetadataRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public SqlGenerator sqlGenerator(EntityMetadataRegistry metadataRegistry) {
        return new SqlGenerator(metadataRegistry);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(NamedParameterJdbcTemplate.class)
    public OrmTemplate ormTemplate(EntityMetadataRegistry metadataRegistry,
                                   SqlGenerator sqlGenerator,
                                   NamedParameterJdbcTemplate jdbcTemplate,
                                   RowMapperFactory rowMapperFactory) {
        return new OrmTemplate(metadataRegistry, sqlGenerator, jdbcTemplate, rowMapperFactory);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(NamedParameterJdbcTemplate.class)
    public TypeSafeOrmTemplate typeSafeOrmTemplate(EntityMetadataRegistry metadataRegistry,
                                                   SqlGenerator sqlGenerator,
                                                   NamedParameterJdbcTemplate jdbcTemplate,
                                                   RowMapperFactory rowMapperFactory) {
        return new TypeSafeOrmTemplate(metadataRegistry, sqlGenerator, jdbcTemplate, rowMapperFactory);
    }
}
