package io.flexdata.spring.orm.config;

import io.flexdata.spring.orm.core.metadata.EntityMetadataRegistry;
import io.flexdata.spring.orm.plugin.OrmPluginManager;
import io.flexdata.spring.orm.template.TypeSafeOrmTemplate;
import io.flexdata.spring.orm.template.impl.AuditableOrmTemplate;
import io.flexdata.spring.orm.template.impl.BatchOrmOperations;
import io.flexdata.spring.orm.template.impl.CachedOrmTemplate;
import io.flexdata.spring.orm.template.impl.TransactionalOrmTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/**
 * 类型安全ORM自动配置
 */
@Configuration
@EnableConfigurationProperties({TypeSafeOrmProperties.class})
@EnableCaching
public class TypeSafeOrmAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OrmPluginManager ormPluginManager() {
        return new OrmPluginManager();
    }

    @Bean
    @ConditionalOnProperty(name = "orm.features.transactional", havingValue = "true", matchIfMissing = true)
    public TransactionalOrmTemplate transactionalOrmTemplate(TypeSafeOrmTemplate ormTemplate) {
        return new TransactionalOrmTemplate(ormTemplate);
    }

    @Bean
    @ConditionalOnProperty(name = "orm.features.batch", havingValue = "true", matchIfMissing = true)
    public BatchOrmOperations batchOrmOperations(NamedParameterJdbcTemplate jdbcTemplate,
                                                 EntityMetadataRegistry metadataRegistry) {
        return new BatchOrmOperations(jdbcTemplate, metadataRegistry);
    }

    @Bean
    @ConditionalOnProperty(name = "orm.features.cache", havingValue = "true")
    public CachedOrmTemplate cachedOrmTemplate(TypeSafeOrmTemplate ormTemplate) {
        return new CachedOrmTemplate(ormTemplate);
    }

    @Bean
    @ConditionalOnProperty(name = "orm.features.audit", havingValue = "true")
    public AuditableOrmTemplate auditableOrmTemplate(TypeSafeOrmTemplate ormTemplate) {
        return new AuditableOrmTemplate(ormTemplate);
    }

    @Bean
    @ConditionalOnMissingBean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("orm-entities", "orm-queries");
    }
}
