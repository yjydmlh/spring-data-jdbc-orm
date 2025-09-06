package com.spring.jdbc.orm.repository;

import com.spring.jdbc.orm.core.interfaces.Criteria;
import com.spring.jdbc.orm.core.interfaces.EnhancedQueryBuilder;
import com.spring.jdbc.orm.core.mapper.RowMapperFactory;
import com.spring.jdbc.orm.core.sql.EnhancedCriteriaBuilder;
import com.spring.jdbc.orm.core.sql.EnhancedSqlGenerator;
import com.spring.jdbc.orm.core.sql.SortDirection;
import com.spring.jdbc.orm.core.sql.complex.SubQuery;
import com.spring.jdbc.orm.core.sql.complex.UnionQuery;
import com.spring.jdbc.orm.core.sql.complex.ComplexSelectQuery;
import com.spring.jdbc.orm.core.metadata.EntityMetadata;
import com.spring.jdbc.orm.core.metadata.EntityMetadataRegistry;
import com.spring.jdbc.orm.core.metadata.FieldMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 增强Repository实现类
 * 提供完整的复杂SQL查询功能
 */
public class EnhancedRepositoryImpl<T, ID> implements EnhancedRepository<T, ID> {
    
    private final Class<T> entityClass;
    private final Class<ID> idClass;
    private final String tableName;
    private final String idFieldName;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final EnhancedSqlGenerator sqlGenerator;
    private final RowMapperFactory rowMapperFactory;
    private final EntityMetadataRegistry metadataRegistry;
    private boolean cacheEnabled = false;
    
    public EnhancedRepositoryImpl(Class<T> entityClass, 
                                 Class<ID> idClass,
                                 String tableName,
                                 String idFieldName,
                                 NamedParameterJdbcTemplate jdbcTemplate,
                                 EnhancedSqlGenerator sqlGenerator,
                                 RowMapperFactory rowMapperFactory,
                                 EntityMetadataRegistry metadataRegistry) {
        this.entityClass = entityClass;
        this.idClass = idClass;
        this.tableName = tableName;
        this.idFieldName = idFieldName;
        this.jdbcTemplate = jdbcTemplate;
        this.sqlGenerator = sqlGenerator;
        this.rowMapperFactory = rowMapperFactory;
        this.metadataRegistry = metadataRegistry;
    }
    
    // ========== 基础CRUD操作 ==========
    
    @Override
    @Transactional
    public T save(T entity) {
        try {
            // 获取实体的ID值
            Object idValue = getIdValue(entity);
            
            if (idValue == null || (idValue instanceof Number && ((Number) idValue).longValue() == 0)) {
                // 插入新记录
                return insert(entity);
            } else {
                // 更新现有记录
                return update(entity);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to save entity: " + entity.getClass().getSimpleName(), e);
        }
    }
    
    private T insert(T entity) throws Exception {
        // 构建INSERT SQL
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
        StringBuilder values = new StringBuilder(" VALUES (");
        Map<String, Object> params = new HashMap<>();
        
        // 获取实体元数据
        EntityMetadata metadata = getEntityMetadata();
        List<String> columns = new ArrayList<>();
        
        for (FieldMetadata field : metadata.getFields().values()) {
            if (!field.getColumnName().equals(idFieldName)) { // 跳过自增ID
                Object value = getFieldValue(entity, field.getFieldName());
                if (value != null) {
                    columns.add(field.getColumnName());
                    params.put(field.getColumnName(), value);
                }
            }
        }
        
        sql.append(String.join(", ", columns)).append(")");
        values.append(columns.stream().map(col -> ":" + col).collect(java.util.stream.Collectors.joining(", "))).append(")");
        sql.append(values);
        
        // 执行插入
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(sql.toString(), new MapSqlParameterSource(params), keyHolder);
        
        // 设置生成的ID
        if (keyHolder.getKey() != null) {
            setIdValue(entity, keyHolder.getKey());
        }
        
        return entity;
    }
    
    private T update(T entity) throws Exception {
        // 构建UPDATE SQL
        StringBuilder sql = new StringBuilder("UPDATE ").append(tableName).append(" SET ");
        Map<String, Object> params = new HashMap<>();
        
        // 获取实体元数据
        EntityMetadata metadata = getEntityMetadata();
        List<String> setClauses = new ArrayList<>();
        
        for (FieldMetadata field : metadata.getFields().values()) {
            if (!field.getColumnName().equals(idFieldName)) {
                Object value = getFieldValue(entity, field.getFieldName());
                setClauses.add(field.getColumnName() + " = :" + field.getColumnName());
                params.put(field.getColumnName(), value);
            }
        }
        
        sql.append(String.join(", ", setClauses));
        sql.append(" WHERE ").append(idFieldName).append(" = :id");
        params.put("id", getIdValue(entity));
        
        // 执行更新
        int updated = jdbcTemplate.update(sql.toString(), params);
        if (updated == 0) {
            throw new RuntimeException("Entity not found for update: " + getIdValue(entity));
        }
        
        return entity;
    }
    
    // 辅助方法
    private EntityMetadata getEntityMetadata() {
        return metadataRegistry.getMetadata(entityClass);
    }
    
    private Object getFieldValue(T entity, String fieldName) {
        try {
            java.lang.reflect.Field field = entityClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(entity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get field value: " + fieldName, e);
        }
    }
    
    private ID getIdValue(T entity) {
        try {
            java.lang.reflect.Field field = entityClass.getDeclaredField(idFieldName);
            field.setAccessible(true);
            return (ID) field.get(entity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get ID value", e);
        }
    }
    
    private void setIdValue(T entity, Number generatedId) {
        try {
            java.lang.reflect.Field field = entityClass.getDeclaredField(idFieldName);
            field.setAccessible(true);
            field.set(entity, generatedId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set ID value", e);
        }
    }
    
    @Override
    @Transactional
    public List<T> saveAll(Iterable<T> entities) {
        List<T> result = new ArrayList<>();
        for (T entity : entities) {
            result.add(save(entity));
        }
        return result;
    }
    
    @Override
    public Optional<T> findById(ID id) {
        String sql = "SELECT * FROM " + tableName + " WHERE " + idFieldName + " = :id";
        Map<String, Object> params = Collections.singletonMap("id", id);
        
        List<T> results = jdbcTemplate.query(sql, params, rowMapperFactory.getRowMapper(entityClass));
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
    
    @Override
    public List<T> findAll() {
        String sql = "SELECT * FROM " + tableName;
        return jdbcTemplate.query(sql, rowMapperFactory.getRowMapper(entityClass));
    }
    
    @Override
    @Transactional
    public void deleteById(ID id) {
        String sql = "DELETE FROM " + tableName + " WHERE " + idFieldName + " = :id";
        Map<String, Object> params = Collections.singletonMap("id", id);
        jdbcTemplate.update(sql, params);
    }
    
    @Override
    @Transactional
    public void delete(T entity) {
        try {
            Field idField = entityClass.getDeclaredField(idFieldName);
            idField.setAccessible(true);
            ID id = (ID) idField.get(entity);
            deleteById(id);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete entity", e);
        }
    }
    
    @Override
    public boolean existsById(ID id) {
        String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE " + idFieldName + " = :id";
        Map<String, Object> params = Collections.singletonMap("id", id);
        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null && count > 0;
    }
    
    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        Long count = jdbcTemplate.queryForObject(sql, Collections.emptyMap(), Long.class);
        return count != null ? count : 0L;
    }
    
    // ========== 增强查询构建器 ==========
    
    @Override
    public EnhancedQueryBuilder<T> createQuery() {
        return new EnhancedQueryBuilderImpl<>(entityClass, sqlGenerator, jdbcTemplate, rowMapperFactory)
                .from(tableName);
    }
    
    @Override
    public <R> EnhancedQueryBuilder<R> createQuery(Class<R> resultType) {
        return new EnhancedQueryBuilderImpl<>(resultType, sqlGenerator, jdbcTemplate, rowMapperFactory)
                .from(tableName);
    }
    
    // ========== 条件查询 ==========
    
    @Override
    public List<T> findByCriteria(Criteria criteria) {
        return createQuery()
                .where(criteria)
                .execute();
    }
    
    @Override
    public Optional<T> findFirstByCriteria(Criteria criteria) {
        T result = createQuery()
                .where(criteria)
                .limit(1)
                .executeFirst();
        return Optional.ofNullable(result);
    }
    
    @Override
    public long countByCriteria(Criteria criteria) {
        return createQuery()
                .where(criteria)
                .count();
    }
    
    @Override
    public boolean existsByCriteria(Criteria criteria) {
        return countByCriteria(criteria) > 0;
    }
    
    @Override
    public Page<T> findByCriteria(Criteria criteria, Pageable pageable) {
        return createQuery()
                .where(criteria)
                .executePage(pageable);
    }
    
    // ========== 多表JOIN查询 ==========
    
    @Override
    public EnhancedQueryBuilder<T> join(String table, String alias, String onCondition) {
        return createQuery().join(table, alias, onCondition);
    }
    
    @Override
    public EnhancedQueryBuilder<T> leftJoin(String table, String alias, String onCondition) {
        return createQuery().leftJoin(table, alias, onCondition);
    }
    
    @Override
    public EnhancedQueryBuilder<T> rightJoin(String table, String alias, String onCondition) {
        return createQuery().rightJoin(table, alias, onCondition);
    }
    
    @Override
    public EnhancedQueryBuilder<T> fullJoin(String table, String alias, String onCondition) {
        return createQuery().fullJoin(table, alias, onCondition);
    }
    
    // ========== 子查询支持 ==========
    
    @Override
    public SubQuery createSubQuery() {
        return new SubQuery(new ComplexSelectQuery());
    }
    
    @Override
    public List<T> findBySubQueryIn(String field, SubQuery subQuery) {
        return createQuery()
                .whereIn(field, subQuery)
                .execute();
    }
    
    @Override
    public List<T> findByExists(SubQuery subQuery) {
        return createQuery()
                .whereExists(subQuery)
                .execute();
    }
    
    @Override
    public List<T> findByNotExists(SubQuery subQuery) {
        return createQuery()
                .whereNotExists(subQuery)
                .execute();
    }
    
    // ========== 聚合查询 ==========
    
    @Override
    public List<Map<String, Object>> groupBy(String... fields) {
        return groupBy(null, fields);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> groupBy(Criteria criteria, String... fields) {
        EnhancedQueryBuilder<Map> query = createQuery(Map.class)
                .select(fields)
                .selectCount("*", "count");
        
        if (criteria != null) {
            query.where(criteria);
        }
        
        List<?> result = query.groupBy(fields).execute();
        return (List<Map<String, Object>>) result;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> groupByHaving(Criteria whereClause, String[] groupFields, Criteria havingClause) {
        EnhancedQueryBuilder<Map> query = createQuery(Map.class)
                .select(groupFields)
                .selectCount("*", "count");
        
        if (whereClause != null) {
            query.where(whereClause);
        }
        
        query.groupBy(groupFields);
        
        if (havingClause != null) {
            query.having(havingClause);
        }
        
        List<?> result = query.execute();
        return (List<Map<String, Object>>) result;
    }
    
    @Override
    public <N extends Number> N sum(String field, Class<N> resultType) {
        return sum(field, null, resultType);
    }
    
    @Override
    public <N extends Number> N sum(String field, Criteria criteria, Class<N> resultType) {
        String sql = "SELECT SUM(" + field + ") FROM " + tableName;
        Map<String, Object> params = new HashMap<>();
        
        if (criteria != null) {
            sql += " WHERE " + criteria.toSql();
            params.putAll(criteria.getParameters());
        }
        
        return jdbcTemplate.queryForObject(sql, params, resultType);
    }
    
    @Override
    public <N extends Number> N avg(String field, Class<N> resultType) {
        return avg(field, null, resultType);
    }
    
    @Override
    public <N extends Number> N avg(String field, Criteria criteria, Class<N> resultType) {
        String sql = "SELECT AVG(" + field + ") FROM " + tableName;
        Map<String, Object> params = new HashMap<>();
        
        if (criteria != null) {
            sql += " WHERE " + criteria.toSql();
            params.putAll(criteria.getParameters());
        }
        
        return jdbcTemplate.queryForObject(sql, params, resultType);
    }
    
    @Override
    public <V> V max(String field, Class<V> resultType) {
        return max(field, null, resultType);
    }
    
    @Override
    public <V> V max(String field, Criteria criteria, Class<V> resultType) {
        String sql = "SELECT MAX(" + field + ") FROM " + tableName;
        Map<String, Object> params = new HashMap<>();
        
        if (criteria != null) {
            sql += " WHERE " + criteria.toSql();
            params.putAll(criteria.getParameters());
        }
        
        return jdbcTemplate.queryForObject(sql, params, resultType);
    }
    
    @Override
    public <V> V min(String field, Class<V> resultType) {
        return min(field, null, resultType);
    }
    
    @Override
    public <V> V min(String field, Criteria criteria, Class<V> resultType) {
        String sql = "SELECT MIN(" + field + ") FROM " + tableName;
        Map<String, Object> params = new HashMap<>();
        
        if (criteria != null) {
            sql += " WHERE " + criteria.toSql();
            params.putAll(criteria.getParameters());
        }
        
        return jdbcTemplate.queryForObject(sql, params, resultType);
    }
    
    // ========== 批量操作 ==========
    
    @Override
    @Transactional
    public int updateByCriteria(Map<String, Object> updates, Criteria criteria) {
        StringBuilder sql = new StringBuilder("UPDATE " + tableName + " SET ");
        Map<String, Object> params = new HashMap<>();
        
        // 构建SET子句
        List<String> setParts = new ArrayList<>();
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String paramName = "update_" + entry.getKey();
            setParts.add(entry.getKey() + " = :" + paramName);
            params.put(paramName, entry.getValue());
        }
        sql.append(String.join(", ", setParts));
        
        // 添加WHERE子句
        if (criteria != null) {
            sql.append(" WHERE ").append(criteria.toSql());
            params.putAll(criteria.getParameters());
        }
        
        return jdbcTemplate.update(sql.toString(), params);
    }
    
    @Override
    @Transactional
    public int deleteByCriteria(Criteria criteria) {
        String sql = "DELETE FROM " + tableName;
        Map<String, Object> params = new HashMap<>();
        
        if (criteria != null) {
            sql += " WHERE " + criteria.toSql();
            params.putAll(criteria.getParameters());
        }
        
        return jdbcTemplate.update(sql, params);
    }
    
    // ========== 原生SQL支持 ==========
    
    @Override
    public List<T> findByNativeQuery(String sql, Map<String, Object> parameters) {
        return jdbcTemplate.query(sql, parameters, rowMapperFactory.getRowMapper(entityClass));
    }
    
    @Override
    public <R> List<R> findByNativeQuery(String sql, Map<String, Object> parameters, Class<R> resultType) {
        return jdbcTemplate.query(sql, parameters, rowMapperFactory.getRowMapper(resultType));
    }
    
    @Override
    @Transactional
    public int executeNativeUpdate(String sql, Map<String, Object> parameters) {
        return jdbcTemplate.update(sql, parameters);
    }
    
    @Override
    public <R> R queryForObject(String sql, Map<String, Object> parameters, Class<R> resultType) {
        return jdbcTemplate.queryForObject(sql, parameters, resultType);
    }
    
    // ========== 动态查询 ==========
    
    @Override
    public DynamicQueryBuilder<T> dynamicQuery() {
        return new DynamicQueryBuilderImpl();
    }
    
    @Override
    public List<T> findByExample(T example) {
        return findByExampleInternal(example, false);
    }
    
    @Override
    public List<T> findByExampleIgnoreNull(T example) {
        return findByExampleInternal(example, true);
    }
    
    private List<T> findByExampleInternal(T example, boolean ignoreNull) {
        List<Criteria> criteriaList = new ArrayList<>();
        
        try {
            Field[] fields = entityClass.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                Object value = field.get(example);
                
                if (value != null || !ignoreNull) {
                    criteriaList.add(EnhancedCriteriaBuilder.eq(field.getName(), value));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to build example query", e);
        }
        
        if (criteriaList.isEmpty()) {
            return findAll();
        }
        
        Criteria criteria = EnhancedCriteriaBuilder.and(criteriaList.toArray(new Criteria[0]));
        return findByCriteria(criteria);
    }
    
    // ========== 缓存支持 ==========
    
    @Override
    public EnhancedRepository<T, ID> enableCache() {
        this.cacheEnabled = true;
        return this;
    }
    
    @Override
    public EnhancedRepository<T, ID> disableCache() {
        this.cacheEnabled = false;
        return this;
    }
    
    @Override
    public void clearCache() {
        if (cacheEnabled) {
            // 清除实体缓存
            String cacheKey = "entity:" + entityClass.getSimpleName();
            // 这里可以集成具体的缓存实现，如Redis、Caffeine等
            // cacheManager.evict(cacheKey);
            
            // 清除查询结果缓存
            String queryCacheKey = "query:" + entityClass.getSimpleName();
            // cacheManager.evict(queryCacheKey);
            
            // 记录缓存清除日志
            System.out.println("Cache cleared for entity: " + entityClass.getSimpleName());
        }
    }
    
    // ========== 事务支持 ==========
    
    @Override
    @Transactional
    public <R> R executeInTransaction(TransactionCallback<T, ID, R> callback) {
        return callback.doInTransaction(this);
    }
    
    // ========== 动态查询构建器实现 ==========
    
    private class DynamicQueryBuilderImpl implements DynamicQueryBuilder<T> {
        
        private final List<Criteria> criteriaList = new ArrayList<>();
        private String orderField;
        private SortDirection sortDirection;
        
        @Override
        public DynamicQueryBuilder<T> eqIfNotNull(String field, Object value) {
            if (value != null) {
                criteriaList.add(EnhancedCriteriaBuilder.eq(field, value));
            }
            return this;
        }
        
        @Override
        public DynamicQueryBuilder<T> likeIfNotEmpty(String field, String value) {
            if (value != null && !value.trim().isEmpty()) {
                criteriaList.add(EnhancedCriteriaBuilder.like(field, "%" + value + "%"));
            }
            return this;
        }
        
        @Override
        public DynamicQueryBuilder<T> inIfNotEmpty(String field, List<?> values) {
            if (values != null && !values.isEmpty()) {
                criteriaList.add(EnhancedCriteriaBuilder.in(field, values));
            }
            return this;
        }
        
        @Override
        public DynamicQueryBuilder<T> betweenIfNotNull(String field, Object start, Object end) {
            if (start != null && end != null) {
                criteriaList.add(EnhancedCriteriaBuilder.between(field, start, end));
            }
            return this;
        }
        
        @Override
        public DynamicQueryBuilder<T> gteIfNotNull(String field, Object value) {
            if (value != null) {
                criteriaList.add(EnhancedCriteriaBuilder.gte(field, value));
            }
            return this;
        }
        
        @Override
        public DynamicQueryBuilder<T> lteIfNotNull(String field, Object value) {
            if (value != null) {
                criteriaList.add(EnhancedCriteriaBuilder.lte(field, value));
            }
            return this;
        }
        
        @Override
        public DynamicQueryBuilder<T> addCondition(boolean condition, Criteria criteria) {
            if (condition && criteria != null) {
                criteriaList.add(criteria);
            }
            return this;
        }
        
        @Override
        public DynamicQueryBuilder<T> orderBy(String field, SortDirection direction) {
            this.orderField = field;
            this.sortDirection = direction;
            return this;
        }
        
        @Override
        public List<T> execute() {
            EnhancedQueryBuilder<T> query = createQuery();
            
            if (!criteriaList.isEmpty()) {
                Criteria criteria = EnhancedCriteriaBuilder.and(criteriaList.toArray(new Criteria[0]));
                query.where(criteria);
            }
            
            if (orderField != null && sortDirection != null) {
                query.orderBy(orderField, sortDirection);
            }
            
            return query.execute();
        }
        
        @Override
        public Page<T> execute(Pageable pageable) {
            EnhancedQueryBuilder<T> query = createQuery();
            
            if (!criteriaList.isEmpty()) {
                Criteria criteria = EnhancedCriteriaBuilder.and(criteriaList.toArray(new Criteria[0]));
                query.where(criteria);
            }
            
            if (orderField != null && sortDirection != null) {
                query.orderBy(orderField, sortDirection);
            }
            
            return query.executePage(pageable);
        }
        
        @Override
        public long count() {
            EnhancedQueryBuilder<T> query = createQuery();
            
            if (!criteriaList.isEmpty()) {
                Criteria criteria = EnhancedCriteriaBuilder.and(criteriaList.toArray(new Criteria[0]));
                query.where(criteria);
            }
            
            return query.count();
        }
    }
}