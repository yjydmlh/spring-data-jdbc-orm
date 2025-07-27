package com.spring.jdbc.orm.repository.impl;

import com.spring.jdbc.orm.core.interfaces.Criteria;
import com.spring.jdbc.orm.core.interfaces.TypeSafeCriteria;
import com.spring.jdbc.orm.core.interfaces.TypeSafeRepository;
import com.spring.jdbc.orm.core.metadata.EntityMetadata;
import com.spring.jdbc.orm.core.metadata.EntityMetadataRegistry;
import com.spring.jdbc.orm.core.metadata.FieldMetadata;
import com.spring.jdbc.orm.core.sql.SqlGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 类型安全仓储实现
 * 文件位置: src/main/java/com/example/orm/repository/impl/TypeSafeRepositoryImpl.java
 */
public class TypeSafeRepositoryImpl<T, ID> implements TypeSafeRepository<T, ID> {
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final SqlGenerator sqlGenerator;
    private final EntityMetadataRegistry metadataRegistry;
    private final Class<T> entityClass;

    public TypeSafeRepositoryImpl(NamedParameterJdbcTemplate jdbcTemplate,
                                  SqlGenerator sqlGenerator,
                                  EntityMetadataRegistry metadataRegistry,
                                  Class<T> entityClass) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlGenerator = sqlGenerator;
        this.metadataRegistry = metadataRegistry;
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

        // 设置生成的ID
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

        TypeSafeCriteria<T> criteria = createEqualsCriteria(idField.getFieldName(), id);
        List<T> results = findByCriteria(criteria);

        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<T> findAll() {
        return findByCriteria(null);
    }

    @Override
    public List<T> findByCriteria(TypeSafeCriteria<T> criteria) {
        String sql = sqlGenerator.generateSelect(entityClass, convertCriteria(criteria), null, null, null, null);
        Map<String, Object> params = criteria != null ? criteria.getParameters() : new HashMap<>();

        return jdbcTemplate.query(sql, params, new BeanPropertyRowMapper<>(entityClass));
    }

    @Override
    public Page<T> findByCriteria(TypeSafeCriteria<T> criteria, Pageable pageable) {
        // 计算总数
        long total = countByCriteria(criteria);

        // 查询数据
        String sql = sqlGenerator.generateSelect(entityClass, convertCriteria(criteria), null, null,
                pageable.getPageSize(), (int) pageable.getOffset());
        Map<String, Object> params = criteria != null ? criteria.getParameters() : new HashMap<>();

        List<T> content = jdbcTemplate.query(sql, params, new BeanPropertyRowMapper<>(entityClass));

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public long count() {
        return countByCriteria(null);
    }

    @Override
    public long countByCriteria(TypeSafeCriteria<T> criteria) {
        String sql = sqlGenerator.generateCount(entityClass, convertCriteria(criteria));
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

        TypeSafeCriteria<T> criteria = createEqualsCriteria(idField.getFieldName(), id);
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
    public void deleteByCriteria(TypeSafeCriteria<T> criteria) {
        String sql = sqlGenerator.generateDelete(entityClass, convertCriteria(criteria));
        Map<String, Object> params = criteria != null ? criteria.getParameters() : new HashMap<>();

        jdbcTemplate.update(sql, params);
    }

    @Override
    public boolean existsById(ID id) {
        return findById(id).isPresent();
    }

    @Override
    public Optional<T> findOne(TypeSafeCriteria<T> criteria) {
        List<T> results = findByCriteria(criteria);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<T> findAll(TypeSafeCriteria<T> criteria, Sort sort) {
        // 简化实现，实际应该支持Sort转换
        return findByCriteria(criteria);
    }

    @Override
    public boolean exists(TypeSafeCriteria<T> criteria) {
        return countByCriteria(criteria) > 0;
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

    private TypeSafeCriteria<T> createEqualsCriteria(String fieldName, Object value) {
        return new TypeSafeCriteria<T>() {
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
            public TypeSafeCriteria<T> and(TypeSafeCriteria<T> other) {
                return null; // 简化实现
            }

            @Override
            public TypeSafeCriteria<T> or(TypeSafeCriteria<T> other) {
                return null; // 简化实现
            }
        };
    }

    // 转换TypeSafeCriteria为老的Criteria接口
    private Criteria convertCriteria(TypeSafeCriteria<T> typeSafeCriteria) {
        if (typeSafeCriteria == null) return null;

        return new Criteria() {
            @Override
            public String toSql() {
                return typeSafeCriteria.toSql();
            }

            @Override
            public Map<String, Object> getParameters() {
                return typeSafeCriteria.getParameters();
            }

            @Override
            public Criteria and(Criteria other) {
                throw new UnsupportedOperationException("Use TypeSafeCriteria instead");
            }

            @Override
            public Criteria or(Criteria other) {
                throw new UnsupportedOperationException("Use TypeSafeCriteria instead");
            }
        };
    }
}
