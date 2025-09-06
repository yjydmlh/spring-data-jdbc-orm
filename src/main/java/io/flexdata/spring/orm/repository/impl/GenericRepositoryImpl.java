package io.flexdata.spring.orm.repository.impl;

import io.flexdata.spring.orm.core.interfaces.Criteria;
import io.flexdata.spring.orm.core.interfaces.GenericRepository;
import io.flexdata.spring.orm.core.metadata.EntityMetadata;
import io.flexdata.spring.orm.core.metadata.EntityMetadataRegistry;
import io.flexdata.spring.orm.core.metadata.FieldMetadata;
import io.flexdata.spring.orm.core.sql.SqlGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import io.flexdata.spring.orm.core.mapper.RowMapperFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 通用仓储实现
 * 文件位置: src/main/java/com/example/orm/repository/impl/GenericRepositoryImpl.java
 */
public class GenericRepositoryImpl<T, ID> implements GenericRepository<T, ID> {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SqlGenerator sqlGenerator;
    private final EntityMetadataRegistry metadataRegistry;
    private final RowMapperFactory rowMapperFactory;
    private final Class<T> entityClass;

    public GenericRepositoryImpl(NamedParameterJdbcTemplate jdbcTemplate,
                                 SqlGenerator sqlGenerator,
                                 EntityMetadataRegistry metadataRegistry,
                                 RowMapperFactory rowMapperFactory,
                                 Class<T> entityClass) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlGenerator = sqlGenerator;
        this.metadataRegistry = metadataRegistry;
        this.rowMapperFactory = rowMapperFactory;
        this.entityClass = entityClass;
    }

    @Override
    public T save(T entity) {
        EntityMetadata metadata = metadataRegistry.getMetadata(entityClass);
        Object idValue = getIdValue(entity, metadata);

        if (idValue == null) {
            return insert(entity);
        } else {
            return update(entity);
        }
    }

    private T insert(T entity) {
        String sql = sqlGenerator.generateInsert(entityClass, entity);
        Map<String, Object> params = entityToMap(entity);

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql, new MapSqlParameterSource(params), keyHolder);

        if (keyHolder.getKey() != null) {
            setIdValue(entity, keyHolder.getKey());
        }

        return entity;
    }

    private T update(T entity) {
        String sql = sqlGenerator.generateUpdate(entityClass, entity);
        Map<String, Object> params = entityToMap(entity);

        jdbcTemplate.update(sql, params);
        return entity;
    }

    @Override
    public List<T> saveAll(Iterable<T> entities) {
        List<T> result = new ArrayList<>();
        for (T entity : entities) {
            result.add(save(entity));
        }
        return result;
    }

    @Override
    public Optional<T> findById(ID id) {
        EntityMetadata metadata = metadataRegistry.getMetadata(entityClass);
        FieldMetadata idField = findIdField(metadata);

        if (idField == null) {
            throw new IllegalStateException("No ID field found for entity: " + entityClass.getName());
        }

        Criteria criteria = createEqualsCriteria(idField.getFieldName(), id);
        List<T> results = findByCriteria(criteria);

        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<T> findAll() {
        return findByCriteria(null);
    }

    @Override
    public List<T> findByCriteria(Criteria criteria) {
        String sql = sqlGenerator.generateSelect(entityClass, criteria, null, null, null, null);
        Map<String, Object> params = criteria != null ? criteria.getParameters() : new HashMap<>();

        return jdbcTemplate.query(sql, params, rowMapperFactory.getRowMapper(entityClass));
    }

    @Override
    public Page<T> findByCriteria(Criteria criteria, Pageable pageable) {
        long total = countByCriteria(criteria);

        String sql = sqlGenerator.generateSelect(entityClass, criteria, null, null,
                pageable.getPageSize(), (int) pageable.getOffset());
        Map<String, Object> params = criteria != null ? criteria.getParameters() : new HashMap<>();

        List<T> content = jdbcTemplate.query(sql, params, rowMapperFactory.getRowMapper(entityClass));

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public long count() {
        return countByCriteria(null);
    }

    @Override
    public long countByCriteria(Criteria criteria) {
        String sql = sqlGenerator.generateCount(entityClass, criteria);
        Map<String, Object> params = criteria != null ? criteria.getParameters() : new HashMap<>();

        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null ? count : 0L;
    }

    @Override
    public void deleteById(ID id) {
        EntityMetadata metadata = metadataRegistry.getMetadata(entityClass);
        FieldMetadata idField = findIdField(metadata);

        if (idField == null) {
            throw new IllegalStateException("No ID field found for entity: " + entityClass.getName());
        }

        Criteria criteria = createEqualsCriteria(idField.getFieldName(), id);
        deleteByCriteria(criteria);
    }

    @Override
    public void delete(T entity) {
        EntityMetadata metadata = metadataRegistry.getMetadata(entityClass);
        Object idValue = getIdValue(entity, metadata);

        if (idValue != null) {
            deleteById((ID) idValue);
        }
    }

    @Override
    public void deleteByCriteria(Criteria criteria) {
        String sql = sqlGenerator.generateDelete(entityClass, criteria);
        Map<String, Object> params = criteria != null ? criteria.getParameters() : new HashMap<>();

        jdbcTemplate.update(sql, params);
    }

    @Override
    public boolean existsById(ID id) {
        return findById(id).isPresent();
    }

    // 工具方法
    private Object getIdValue(T entity, EntityMetadata metadata) {
        FieldMetadata idField = findIdField(metadata);
        if (idField == null) return null;

        try {
            Field field = entityClass.getDeclaredField(idField.getFieldName());
            field.setAccessible(true);
            return field.get(entity);
        } catch (Exception e) {
            return null;
        }
    }

    private void setIdValue(T entity, Object idValue) {
        EntityMetadata metadata = metadataRegistry.getMetadata(entityClass);
        FieldMetadata idField = findIdField(metadata);

        if (idField != null) {
            try {
                Field field = entityClass.getDeclaredField(idField.getFieldName());
                field.setAccessible(true);
                field.set(entity, idValue);
            } catch (Exception e) {
                // 忽略设置失败
            }
        }
    }

    private FieldMetadata findIdField(EntityMetadata metadata) {
        return metadata.getFields().values().stream()
                .filter(FieldMetadata::isPrimaryKey)
                .findFirst()
                .orElse(null);
    }

    private Map<String, Object> entityToMap(T entity) {
        EntityMetadata metadata = metadataRegistry.getMetadata(entityClass);
        Map<String, Object> params = new HashMap<>();

        for (FieldMetadata field : metadata.getFields().values()) {
            try {
                Field javaField = entityClass.getDeclaredField(field.getFieldName());
                javaField.setAccessible(true);
                Object value = javaField.get(entity);
                if (value != null) {
                    params.put(field.getFieldName(), value);
                }
            } catch (Exception e) {
                // 忽略获取失败的字段
            }
        }

        return params;
    }

    private Criteria createEqualsCriteria(String fieldName, Object value) {
        return new Criteria() {
            @Override
            public String toSql() {
                return fieldName + " = :" + fieldName;
            }

            @Override
            public Map<String, Object> getParameters() {
                Map<String, Object> params = new HashMap<>();
                params.put(fieldName, value);
                return params;
            }

            @Override
            public Criteria and(Criteria other) {
                return null; // 简化实现
            }

            @Override
            public Criteria or(Criteria other) {
                return null; // 简化实现
            }
        };
    }
}
