package io.flexdata.spring.orm.template.impl;

import io.flexdata.spring.orm.core.interfaces.Criteria;
import io.flexdata.spring.orm.core.interfaces.TypeSafeCriteria;
import io.flexdata.spring.orm.core.interfaces.TypeSafeQueryBuilder;
import io.flexdata.spring.orm.core.metadata.EntityMetadataRegistry;
import io.flexdata.spring.orm.core.sql.OrderBy;
import io.flexdata.spring.orm.core.sql.SortDirection;
import io.flexdata.spring.orm.core.sql.SqlGenerator;
import io.flexdata.spring.orm.core.util.FieldUtils;
import io.flexdata.spring.orm.core.util.SFunction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import io.flexdata.spring.orm.core.mapper.RowMapperFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.*;

/**
 * 类型安全查询构建器实现
 * 文件位置: src/main/java/com/example/orm/template/impl/TypeSafeQueryBuilderImpl.java
 */
public class TypeSafeQueryBuilderImpl<T> implements TypeSafeQueryBuilder<T> {
    private final Class<T> entityClass;
    private final SqlGenerator sqlGenerator;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final EntityMetadataRegistry metadataRegistry;
    private final RowMapperFactory rowMapperFactory;

    private List<String> selectedFields;
    private TypeSafeCriteria<T> criteria;
    private List<OrderBy> orderBy = new ArrayList<>();
    private Integer limit;
    private Integer offset;
    private List<String> groupByFields;
    private TypeSafeCriteria<T> havingCriteria;

    public TypeSafeQueryBuilderImpl(Class<T> entityClass,
                                    SqlGenerator sqlGenerator,
                                    NamedParameterJdbcTemplate jdbcTemplate,
                                    EntityMetadataRegistry metadataRegistry,
                                    RowMapperFactory rowMapperFactory) {
        this.entityClass = entityClass;
        this.sqlGenerator = sqlGenerator;
        this.jdbcTemplate = jdbcTemplate;
        this.metadataRegistry = metadataRegistry;
        this.rowMapperFactory = rowMapperFactory;
    }

    @Override
    public TypeSafeQueryBuilder<T> select(SFunction<T, ?>... fields) {
        this.selectedFields = Arrays.asList(FieldUtils.getFieldNames(fields));
        return this;
    }

    @Override
    public TypeSafeQueryBuilder<T> where(TypeSafeCriteria<T> criteria) {
        this.criteria = criteria;
        return this;
    }

    @Override
    public TypeSafeQueryBuilder<T> orderBy(SFunction<T, ?> field, SortDirection direction) {
        this.orderBy.add(new OrderBy(FieldUtils.getFieldName(field), direction));
        return this;
    }

    @Override
    public TypeSafeQueryBuilder<T> orderBy(SFunction<T, ?> field) {
        return orderByAsc(field);
    }

    @Override
    public TypeSafeQueryBuilder<T> orderByAsc(SFunction<T, ?> field) {
        return orderBy(field, SortDirection.ASC);
    }

    @Override
    public TypeSafeQueryBuilder<T> orderByDesc(SFunction<T, ?> field) {
        return orderBy(field, SortDirection.DESC);
    }

    @Override
    public TypeSafeQueryBuilder<T> limit(int limit) {
        this.limit = limit;
        return this;
    }

    @Override
    public TypeSafeQueryBuilder<T> offset(int offset) {
        this.offset = offset;
        return this;
    }

    @Override
    public TypeSafeQueryBuilder<T> groupBy(SFunction<T, ?>... fields) {
        this.groupByFields = Arrays.asList(FieldUtils.getFieldNames(fields));
        return this;
    }

    @Override
    public TypeSafeQueryBuilder<T> having(TypeSafeCriteria<T> criteria) {
        this.havingCriteria = criteria;
        return this;
    }

    @Override
    public List<T> execute() {
        String sql = buildSql();
        Map<String, Object> params = buildParameters();

        return jdbcTemplate.query(sql, params, rowMapperFactory.getRowMapper(entityClass));
    }

    @Override
    public Page<T> executePage(Pageable pageable) {
        throw new UnsupportedOperationException("Page query not implemented yet");
    }

    @Override
    public long count() {
        String sql = sqlGenerator.generateCount(entityClass, convertCriteria(criteria));
        Map<String, Object> params = buildParameters();

        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null ? count : 0L;
    }

    private String buildSql() {
        return sqlGenerator.generateSelect(entityClass, convertCriteria(criteria),
                selectedFields, orderBy, limit, offset);
    }

    private Map<String, Object> buildParameters() {
        Map<String, Object> params = new HashMap<>();
        if (criteria != null) {
            params.putAll(criteria.getParameters());
        }
        if (havingCriteria != null) {
            params.putAll(havingCriteria.getParameters());
        }
        return params;
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
