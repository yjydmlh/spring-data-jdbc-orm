package com.spring.jdbc.orm.template;

import com.spring.jdbc.orm.core.interfaces.Criteria;
import com.spring.jdbc.orm.core.interfaces.GenericRepository;
import com.spring.jdbc.orm.core.metadata.EntityMetadataRegistry;
import com.spring.jdbc.orm.core.sql.SqlGenerator;
import com.spring.jdbc.orm.repository.impl.GenericRepositoryImpl;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 传统ORM模板
 * 文件位置: src/main/java/com/example/orm/template/OrmTemplate.java
 */
@Component
public class OrmTemplate {
    private final EntityMetadataRegistry metadataRegistry;
    private final SqlGenerator sqlGenerator;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final Map<Class<?>, GenericRepository<?, ?>> repositoryCache = new ConcurrentHashMap<>();

    public OrmTemplate(EntityMetadataRegistry metadataRegistry,
                       SqlGenerator sqlGenerator,
                       NamedParameterJdbcTemplate jdbcTemplate) {
        this.metadataRegistry = metadataRegistry;
        this.sqlGenerator = sqlGenerator;
        this.jdbcTemplate = jdbcTemplate;
    }

    @SuppressWarnings("unchecked")
    public <T, ID> GenericRepository<T, ID> getRepository(Class<T> entityClass) {
        return (GenericRepository<T, ID>) repositoryCache.computeIfAbsent(entityClass,
                clazz -> new GenericRepositoryImpl<>(jdbcTemplate, sqlGenerator, metadataRegistry, clazz));
    }

    // 便捷方法
    public <T> List<T> findAll(Class<T> entityClass) {
        return getRepository(entityClass).findAll();
    }

    public <T, ID> Optional<T> findById(Class<T> entityClass, ID id) {
        return getRepository(entityClass).findById(id);
    }

    public <T> List<T> findByCriteria(Class<T> entityClass, Criteria criteria) {
        return getRepository(entityClass).findByCriteria(criteria);
    }

    public <T> T save(T entity) {
        @SuppressWarnings("unchecked")
        Class<T> entityClass = (Class<T>) entity.getClass();
        return getRepository(entityClass).save(entity);
    }

    public <T, ID> void deleteById(Class<T> entityClass, ID id) {
        getRepository(entityClass).deleteById(id);
    }

    // 获取原始JdbcTemplate以支持复杂查询
    public NamedParameterJdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }
}
