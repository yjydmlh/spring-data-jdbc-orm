package io.flexdata.spring.orm.repository;

import io.flexdata.spring.orm.core.interfaces.Criteria;
import io.flexdata.spring.orm.core.interfaces.GenericRepository;
import io.flexdata.spring.orm.core.interfaces.QueryBuilder;
import io.flexdata.spring.orm.core.sql.OrderBy;
import io.flexdata.spring.orm.core.sql.SortDirection;
import io.flexdata.spring.orm.core.sql.SqlGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import io.flexdata.spring.orm.core.mapper.RowMapperFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.*;

/**
 * 查询构建器实现
 * 文件位置: src/main/java/com/example/orm/repository/QueryBuilderImpl.java
 */
public class QueryBuilderImpl<T> implements QueryBuilder<T> {
    private final GenericRepository<T, ?> repository;
    private final Class<T> entityClass;
    private final SqlGenerator sqlGenerator;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RowMapperFactory rowMapperFactory;

    private List<String> selectedFields;
    private Criteria criteria;
    private List<OrderBy> orderBy = new ArrayList<>();
    private Integer limit;
    private Integer offset;
    private List<String> joins = new ArrayList<>();
    private List<String> groupByFields;
    private Criteria havingCriteria;

    public QueryBuilderImpl(GenericRepository<T, ?> repository, Class<T> entityClass,
                            SqlGenerator sqlGenerator, NamedParameterJdbcTemplate jdbcTemplate,
                            RowMapperFactory rowMapperFactory) {
        this.repository = repository;
        this.entityClass = entityClass;
        this.sqlGenerator = sqlGenerator;
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapperFactory = rowMapperFactory;
    }

    @Override
    public QueryBuilder<T> select(String... fields) {
        this.selectedFields = Arrays.asList(fields);
        return this;
    }

    @Override
    public QueryBuilder<T> where(Criteria criteria) {
        this.criteria = criteria;
        return this;
    }

    @Override
    public QueryBuilder<T> orderBy(String field, SortDirection direction) {
        this.orderBy.add(new OrderBy(field, direction));
        return this;
    }

    @Override
    public QueryBuilder<T> limit(int limit) {
        this.limit = limit;
        return this;
    }

    @Override
    public QueryBuilder<T> offset(int offset) {
        this.offset = offset;
        return this;
    }

    @Override
    public QueryBuilder<T> join(String table, String on) {
        joins.add("JOIN " + table + " ON " + on);
        return this;
    }

    @Override
    public QueryBuilder<T> leftJoin(String table, String on) {
        joins.add("LEFT JOIN " + table + " ON " + on);
        return this;
    }

    @Override
    public QueryBuilder<T> groupBy(String... fields) {
        this.groupByFields = Arrays.asList(fields);
        return this;
    }

    @Override
    public QueryBuilder<T> having(Criteria criteria) {
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
        return repository.findByCriteria(criteria, pageable);
    }

    private String buildSql() {
        return sqlGenerator.generateSelect(entityClass, criteria, selectedFields, orderBy, limit, offset);
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
}
