package io.flexdata.spring.orm.template;

import io.flexdata.spring.orm.core.interfaces.TypeSafeCriteria;
import io.flexdata.spring.orm.core.interfaces.TypeSafeRepository;
import io.flexdata.spring.orm.core.mapper.RowMapperFactory;
import io.flexdata.spring.orm.core.metadata.EntityMetadataRegistry;
import io.flexdata.spring.orm.core.sql.SqlGenerator;
import io.flexdata.spring.orm.criteria.TypeSafeCriteriaBuilder;
import io.flexdata.spring.orm.repository.impl.TypeSafeRepositoryImpl;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 类型安全ORM模板
 * 文件位置: src/main/java/com/example/orm/template/TypeSafeOrmTemplate.java
 */
@Component
public class TypeSafeOrmTemplate {
    private final EntityMetadataRegistry metadataRegistry;
    private final SqlGenerator sqlGenerator;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RowMapperFactory rowMapperFactory;
    private final Map<Class<?>, TypeSafeRepository<?, ?>> repositoryCache = new ConcurrentHashMap<>();

    public TypeSafeOrmTemplate(EntityMetadataRegistry metadataRegistry,
                               SqlGenerator sqlGenerator,
                               NamedParameterJdbcTemplate jdbcTemplate,
                               RowMapperFactory rowMapperFactory) {
        this.metadataRegistry = metadataRegistry;
        this.sqlGenerator = sqlGenerator;
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapperFactory = rowMapperFactory;
    }

    @SuppressWarnings("unchecked")
    public <T, ID> TypeSafeRepository<T, ID> getRepository(Class<T> entityClass) {
        return (TypeSafeRepository<T, ID>) repositoryCache.computeIfAbsent(entityClass,
                clazz -> new TypeSafeRepositoryImpl<>(jdbcTemplate, sqlGenerator, metadataRegistry, rowMapperFactory, clazz));
    }

    public <T> TypeSafeCriteriaBuilder<T> criteria(Class<T> entityClass) {
        return TypeSafeCriteriaBuilder.create();
    }

    // 便捷方法
    public <T> List<T> findAll(Class<T> entityClass) {
        return getRepository(entityClass).findAll();
    }

    public <T, ID> Optional<T> findById(Class<T> entityClass, ID id) {
        return getRepository(entityClass).findById(id);
    }

    public <T> List<T> findByCriteria(Class<T> entityClass, TypeSafeCriteria<T> criteria) {
        return getRepository(entityClass).findByCriteria(criteria);
    }

    public <T> T save(T entity) {
        @SuppressWarnings("unchecked")
        Class<T> entityClass = (Class<T>) entity.getClass();
        return getRepository(entityClass).save(entity);
    }

    public <T> List<T> saveAll(List<T> entities) {
        if (entities.isEmpty()) return entities;

        @SuppressWarnings("unchecked")
        Class<T> entityClass = (Class<T>) entities.get(0).getClass();
        return getRepository(entityClass).saveAll(entities);
    }

    public <T, ID> void deleteById(Class<T> entityClass, ID id) {
        getRepository(entityClass).deleteById(id);
    }

    public <T> void delete(T entity) {
        @SuppressWarnings("unchecked")
        Class<T> entityClass = (Class<T>) entity.getClass();
        getRepository(entityClass).delete(entity);
    }

    public <T> void deleteByCriteria(Class<T> entityClass, TypeSafeCriteria<T> criteria) {
        getRepository(entityClass).deleteByCriteria(criteria);
    }

    public <T> long count(Class<T> entityClass) {
        return getRepository(entityClass).count();
    }

    public <T> long countByCriteria(Class<T> entityClass, TypeSafeCriteria<T> criteria) {
        return getRepository(entityClass).countByCriteria(criteria);
    }

    public <T, ID> boolean existsById(Class<T> entityClass, ID id) {
        return getRepository(entityClass).existsById(id);
    }

    public <T> boolean exists(Class<T> entityClass, TypeSafeCriteria<T> criteria) {
        return getRepository(entityClass).exists(criteria);
    }

    public <T> Optional<T> findOne(Class<T> entityClass, TypeSafeCriteria<T> criteria) {
        return getRepository(entityClass).findOne(criteria);
    }

    // 获取原始JdbcTemplate以支持复杂查询
    public NamedParameterJdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }
}
