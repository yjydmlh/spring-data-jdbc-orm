package com.spring.jdbc.orm.repository;

import com.spring.jdbc.orm.core.interfaces.*;
import com.spring.jdbc.orm.core.mapper.RowMapperFactory;
import com.spring.jdbc.orm.core.sql.*;
import com.spring.jdbc.orm.core.sql.complex.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.*;

/**
 * 增强查询构建器实现
 * 支持复杂SQL语法：多表JOIN、子查询、聚合函数、窗口函数等
 */
public class EnhancedQueryBuilderImpl<T> implements EnhancedQueryBuilder<T> {
    
    private final Class<T> entityClass;
    private final EnhancedSqlGenerator sqlGenerator;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RowMapperFactory rowMapperFactory;
    
    // 查询构建状态
    private final ComplexSelectQuery complexQuery;
    private final Map<String, Object> parameters;
    private final List<EnhancedQueryBuilder<T>> unionQueries;
    private UnionQuery.UnionType unionType;
    
    public EnhancedQueryBuilderImpl(Class<T> entityClass,
                                   EnhancedSqlGenerator sqlGenerator,
                                   NamedParameterJdbcTemplate jdbcTemplate,
                                   RowMapperFactory rowMapperFactory) {
        this.entityClass = entityClass;
        this.sqlGenerator = sqlGenerator;
        this.jdbcTemplate = jdbcTemplate;
        this.rowMapperFactory = rowMapperFactory;
        this.complexQuery = new ComplexSelectQuery();
        this.parameters = new HashMap<>();
        this.unionQueries = new ArrayList<>();
    }
    
    @Override
    public EnhancedQueryBuilder<T> select(String... fields) {
        for (String field : fields) {
            complexQuery.select(field);
        }
        return this;
    }
    
    @Override
    public EnhancedQueryBuilder<T> selectAs(String field, String alias) {
        complexQuery.select(field, alias);
        return this;
    }
    
    @Override
    public EnhancedQueryBuilder<T> selectSubQuery(SubQuery subQuery, String alias) {
        complexQuery.selectSubQuery(subQuery, alias);
        return this;
    }
    
    @Override
    public EnhancedQueryBuilder<T> selectAggregate(String function, String field, String alias) {
        AggregateFunction aggFunc = new AggregateFunction(function, field, alias);
        complexQuery.selectAggregate(aggFunc);
        return this;
    }
    
    @Override
    public EnhancedQueryBuilder<T> selectCount(String field, String alias) {
        return selectAggregate("COUNT", field, alias);
    }
    
    @Override
    public EnhancedQueryBuilder<T> selectSum(String field, String alias) {
        return selectAggregate("SUM", field, alias);
    }
    
    @Override
    public EnhancedQueryBuilder<T> selectAvg(String field, String alias) {
        return selectAggregate("AVG", field, alias);
    }
    
    @Override
    public EnhancedQueryBuilder<T> selectMax(String field, String alias) {
        return selectAggregate("MAX", field, alias);
    }
    
    @Override
    public EnhancedQueryBuilder<T> selectMin(String field, String alias) {
        return selectAggregate("MIN", field, alias);
    }
    
    @Override
    public EnhancedQueryBuilder<T> distinct() {
        complexQuery.distinct();
        return this;
    }
    
    @Override
    public EnhancedQueryBuilder<T> from(String table) {
        complexQuery.from(table);
        return this;
    }
    
    @Override
    public EnhancedQueryBuilder<T> from(String table, String alias) {
        complexQuery.from(table, alias);
        return this;
    }
    
    @Override
    public EnhancedQueryBuilder<T> fromSubQuery(SubQuery subQuery, String alias) {
        complexQuery.fromSubQuery(subQuery, alias);
        return this;
    }
    
    @Override
    public EnhancedQueryBuilder<T> join(String table, String alias, String onCondition) {
        complexQuery.join(table, alias, onCondition);
        return this;
    }
    
    @Override
    public EnhancedQueryBuilder<T> leftJoin(String table, String alias, String onCondition) {
        complexQuery.leftJoin(table, alias, onCondition);
        return this;
    }
    
    @Override
    public EnhancedQueryBuilder<T> rightJoin(String table, String alias, String onCondition) {
        complexQuery.rightJoin(table, alias, onCondition);
        return this;
    }
    
    @Override
    public EnhancedQueryBuilder<T> fullJoin(String table, String alias, String onCondition) {
        complexQuery.fullJoin(table, alias, onCondition);
        return this;
    }
    
    @Override
    public EnhancedQueryBuilder<T> crossJoin(String table, String alias) {
        // CROSS JOIN没有ON条件
        complexQuery.getJoins().add(new JoinClause(JoinType.CROSS, table, alias, null));
        return this;
    }
    
    @Override
    public EnhancedQueryBuilder<T> joinSubQuery(SubQuery subQuery, String alias, String onCondition) {
        complexQuery.getJoins().add(new JoinClause(JoinType.INNER, subQuery, alias, onCondition));
        return this;
    }
    
    @Override
    public EnhancedQueryBuilder<T> leftJoinSubQuery(SubQuery subQuery, String alias, String onCondition) {
        complexQuery.getJoins().add(new JoinClause(JoinType.LEFT, subQuery, alias, onCondition));
        return this;
    }
    
    @Override
    public EnhancedQueryBuilder<T> where(Criteria criteria) {
        complexQuery.where(criteria);
        if (criteria != null) {
            parameters.putAll(criteria.getParameters());
        }
        return this;
    }
    
    @Override
    public EnhancedQueryBuilder<T> where(String condition) {
        // 创建简单的字符串条件
        Criteria criteria = new SimpleCriteria(condition);
        return where(criteria);
    }
    
    @Override
    public EnhancedQueryBuilder<T> whereExists(SubQuery subQuery) {
        ExistsQuery existsQuery = new ExistsQuery(subQuery, false);
        Criteria criteria = new ExistsCriteria(existsQuery);
        return where(criteria);
    }
    
    @Override
    public EnhancedQueryBuilder<T> whereNotExists(SubQuery subQuery) {
        ExistsQuery existsQuery = new ExistsQuery(subQuery, true);
        Criteria criteria = new ExistsCriteria(existsQuery);
        return where(criteria);
    }
    
    @Override
    public EnhancedQueryBuilder<T> whereIn(String field, SubQuery subQuery) {
        Criteria criteria = new SubQueryCriteria(field, "IN", subQuery);
        return where(criteria);
    }
    
    @Override
    public EnhancedQueryBuilder<T> whereNotIn(String field, SubQuery subQuery) {
        Criteria criteria = new SubQueryCriteria(field, "NOT IN", subQuery);
        return where(criteria);
    }
    
    @Override
    public EnhancedQueryBuilder<T> whereTableField(String tableAlias, String field, String operator, Object value) {
        String fieldExpression = tableAlias + "." + field;
        Criteria criteria = new SimpleCriteria(fieldExpression, operator, value);
        return where(criteria);
    }
    
    @Override
    public EnhancedQueryBuilder<T> whereTableFieldEquals(String tableAlias, String field, Object value) {
        return whereTableField(tableAlias, field, "=", value);
    }
    
    @Override
    public EnhancedQueryBuilder<T> whereTableFieldIn(String tableAlias, String field, List<Object> values) {
        String fieldExpression = tableAlias + "." + field;
        Criteria criteria = new InCriteria(fieldExpression, values);
        return where(criteria);
    }
    
    @Override
    public EnhancedQueryBuilder<T> and(Criteria criteria) {
        if (complexQuery.getWhereClause() != null) {
            complexQuery.where(complexQuery.getWhereClause().and(criteria));
        } else {
            complexQuery.where(criteria);
        }
        if (criteria != null) {
            parameters.putAll(criteria.getParameters());
        }
        return this;
    }
    
    @Override
    public EnhancedQueryBuilder<T> or(Criteria criteria) {
        if (complexQuery.getWhereClause() != null) {
            complexQuery.where(complexQuery.getWhereClause().or(criteria));
        } else {
            complexQuery.where(criteria);
        }
        if (criteria != null) {
            parameters.putAll(criteria.getParameters());
        }
        return this;
    }
    
    @Override
    public EnhancedQueryBuilder<T> groupBy(String... fields) {
        complexQuery.groupBy(fields);
        return this;
    }
    
    @Override
    public EnhancedQueryBuilder<T> having(Criteria criteria) {
        complexQuery.having(criteria);
        if (criteria != null) {
            parameters.putAll(criteria.getParameters());
        }
        return this;
    }
    
    @Override
    public EnhancedQueryBuilder<T> having(String condition) {
        Criteria criteria = new SimpleCriteria(condition);
        return having(criteria);
    }
    
    @Override
    public EnhancedQueryBuilder<T> orderBy(String field, SortDirection direction) {
        complexQuery.orderBy(field, direction.name());
        return this;
    }
    
    @Override
    public EnhancedQueryBuilder<T> orderBy(String field) {
        return orderBy(field, SortDirection.ASC);
    }
    
    @Override
    public EnhancedQueryBuilder<T> orderByAsc(String field) {
        return orderBy(field, SortDirection.ASC);
    }
    
    @Override
    public EnhancedQueryBuilder<T> orderByDesc(String field) {
        return orderBy(field, SortDirection.DESC);
    }
    
    @Override
    public EnhancedQueryBuilder<T> limit(int limit) {
        complexQuery.limit(limit);
        return this;
    }
    
    @Override
    public EnhancedQueryBuilder<T> offset(int offset) {
        complexQuery.offset(offset);
        return this;
    }
    
    @Override
    public EnhancedQueryBuilder<T> page(int page, int size) {
        return limit(size).offset(page * size);
    }
    
    @Override
    public EnhancedQueryBuilder<T> selectWindowFunction(String function, String field, String alias) {
        StringBuilder windowFunc = new StringBuilder();
        windowFunc.append(function).append("(");
        
        if (field != null && !field.isEmpty()) {
            windowFunc.append(field);
        }
        
        windowFunc.append(") OVER ()");
        
        if (alias != null && !alias.isEmpty()) {
            windowFunc.append(" AS ").append(alias);
        }
        
        complexQuery.addSelectField(windowFunc.toString());
        return this;
    }
    
    @Override
    public EnhancedQueryBuilder<T> selectRowNumber(String alias) {
        return selectWindowFunction("ROW_NUMBER", null, alias);
    }
    
    @Override
    public EnhancedQueryBuilder<T> selectRank(String alias) {
        return selectWindowFunction("RANK", null, alias);
    }
    
    @Override
    public EnhancedQueryBuilder<T> selectDenseRank(String alias) {
        return selectWindowFunction("DENSE_RANK", null, alias);
    }
    
    @Override
    public EnhancedQueryBuilder<T> withCte(String name, SubQuery query) {
        complexQuery.withCte(name, query);
        return this;
    }
    
    @Override
    public EnhancedQueryBuilder<T> withCte(String name, SubQuery query, String... columnNames) {
        complexQuery.getCteDefinitions().add(new CteDefinition(name, query, Arrays.asList(columnNames)));
        return this;
    }
    
    @Override
    public EnhancedQueryBuilder<T> union(EnhancedQueryBuilder<T> other) {
        unionQueries.add(other);
        unionType = UnionQuery.UnionType.UNION;
        return this;
    }
    
    @Override
    public EnhancedQueryBuilder<T> unionAll(EnhancedQueryBuilder<T> other) {
        unionQueries.add(other);
        unionType = UnionQuery.UnionType.UNION_ALL;
        return this;
    }
    
    @Override
    public EnhancedQueryBuilder<T> intersect(EnhancedQueryBuilder<T> other) {
        unionQueries.add(other);
        unionType = UnionQuery.UnionType.INTERSECT;
        return this;
    }
    
    @Override
    public EnhancedQueryBuilder<T> except(EnhancedQueryBuilder<T> other) {
        unionQueries.add(other);
        unionType = UnionQuery.UnionType.EXCEPT;
        return this;
    }
    
    @Override
    public List<T> execute() {
        String sql = toSql();
        Map<String, Object> params = getParameters();
        return jdbcTemplate.query(sql, params, rowMapperFactory.getRowMapper(entityClass));
    }
    
    @Override
    public Page<T> executePage(Pageable pageable) {
        // 先执行count查询
        long total = count();
        
        // 执行分页查询
        EnhancedQueryBuilder<T> pagedQuery = limit(pageable.getPageSize()).offset((int) pageable.getOffset());
        List<T> content = pagedQuery.execute();
        
        return new PageImpl<>(content, pageable, total);
    }
    
    @Override
    public long count() {
        // 构建COUNT查询
        ComplexSelectQuery countQuery = new ComplexSelectQuery();
        countQuery.select("COUNT(*)");
        countQuery.getFromTables().addAll(complexQuery.getFromTables());
        countQuery.getJoins().addAll(complexQuery.getJoins());
        countQuery.where(complexQuery.getWhereClause());
        
        String sql = sqlGenerator.generateComplexSelect(countQuery);
        Map<String, Object> params = getParameters();
        
        Long count = jdbcTemplate.queryForObject(sql, params, Long.class);
        return count != null ? count : 0L;
    }
    
    @Override
    public T executeFirst() {
        List<T> results = limit(1).execute();
        return results.isEmpty() ? null : results.get(0);
    }
    
    @Override
    public T executeUnique() {
        List<T> results = execute();
        if (results.isEmpty()) {
            return null;
        }
        if (results.size() > 1) {
            throw new RuntimeException("Expected unique result, but found " + results.size() + " results");
        }
        return results.get(0);
    }
    
    @Override
    public String toSql() {
        if (!unionQueries.isEmpty()) {
            // 处理UNION查询
            UnionQuery unionQuery = new UnionQuery(unionType);
            unionQuery.addQuery(complexQuery);
            for (EnhancedQueryBuilder<T> builder : unionQueries) {
                if (builder instanceof EnhancedQueryBuilderImpl) {
                    unionQuery.addQuery(((EnhancedQueryBuilderImpl<T>) builder).complexQuery);
                }
            }
            return sqlGenerator.generateUnionQuery(unionQuery);
        } else {
            return sqlGenerator.generateComplexSelect(complexQuery);
        }
    }
    
    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> allParams = new HashMap<>(parameters);
        if (complexQuery.getAllParameters() != null) {
            allParams.putAll(complexQuery.getAllParameters());
        }
        return allParams;
    }
    
    @Override
    public EnhancedQueryBuilder<T> nativeWhere(String sql, Map<String, Object> params) {
        Criteria criteria = new NativeCriteria(sql, params);
        return where(criteria);
    }
    
    @Override
    public EnhancedQueryBuilder<T> nativeHaving(String sql, Map<String, Object> params) {
        Criteria criteria = new NativeCriteria(sql, params);
        return having(criteria);
    }
    
    @Override
    public EnhancedQueryBuilder<T> when(boolean condition, QueryBuilderFunction<EnhancedQueryBuilder<T>> function) {
        if (condition) {
            return function.apply(this);
        }
        return this;
    }
    
    // 内部辅助类
    private static class SimpleCriteria implements Criteria {
        private final String condition;
        private final Map<String, Object> parameters;
        
        public SimpleCriteria(String condition) {
            this.condition = condition;
            this.parameters = new HashMap<>();
        }
        
        public SimpleCriteria(String field, String operator, Object value) {
            this.condition = field + " " + operator + " :" + field.replace(".", "_");
            this.parameters = new HashMap<>();
            this.parameters.put(field.replace(".", "_"), value);
        }
        
        @Override
        public String toSql() {
            return condition;
        }
        
        @Override
        public Map<String, Object> getParameters() {
            return parameters;
        }
        
        @Override
        public Criteria and(Criteria other) {
            return new CompositeCriteria(this, "AND", other);
        }
        
        @Override
        public Criteria or(Criteria other) {
            return new CompositeCriteria(this, "OR", other);
        }
    }
    
    private static class CompositeCriteria implements Criteria {
        private final Criteria left;
        private final String operator;
        private final Criteria right;
        
        public CompositeCriteria(Criteria left, String operator, Criteria right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }
        
        @Override
        public String toSql() {
            return "(" + left.toSql() + " " + operator + " " + right.toSql() + ")";
        }
        
        @Override
        public Map<String, Object> getParameters() {
            Map<String, Object> params = new HashMap<>();
            params.putAll(left.getParameters());
            params.putAll(right.getParameters());
            return params;
        }
        
        @Override
        public Criteria and(Criteria other) {
            return new CompositeCriteria(this, "AND", other);
        }
        
        @Override
        public Criteria or(Criteria other) {
            return new CompositeCriteria(this, "OR", other);
        }
    }
    
    private static class ExistsCriteria implements Criteria {
        private final ExistsQuery existsQuery;
        
        public ExistsCriteria(ExistsQuery existsQuery) {
            this.existsQuery = existsQuery;
        }
        
        @Override
        public String toSql() {
            return existsQuery.toSql(new HashMap<>());
        }
        
        @Override
        public Map<String, Object> getParameters() {
            return existsQuery.getSubQuery().getParameters();
        }
        
        @Override
        public Criteria and(Criteria other) {
            return new CompositeCriteria(this, "AND", other);
        }
        
        @Override
        public Criteria or(Criteria other) {
            return new CompositeCriteria(this, "OR", other);
        }
    }
    
    private static class SubQueryCriteria implements Criteria {
        private final String field;
        private final String operator;
        private final SubQuery subQuery;
        
        public SubQueryCriteria(String field, String operator, SubQuery subQuery) {
            this.field = field;
            this.operator = operator;
            this.subQuery = subQuery;
        }
        
        @Override
        public String toSql() {
            return field + " " + operator + " (" + subQuery.toSql() + ")";
        }
        
        @Override
        public Map<String, Object> getParameters() {
            return subQuery.getParameters();
        }
        
        @Override
        public Criteria and(Criteria other) {
            return new CompositeCriteria(this, "AND", other);
        }
        
        @Override
        public Criteria or(Criteria other) {
            return new CompositeCriteria(this, "OR", other);
        }
    }
    
    private static class InCriteria implements Criteria {
        private final String field;
        private final List<Object> values;
        
        public InCriteria(String field, List<Object> values) {
            this.field = field;
            this.values = values;
        }
        
        @Override
        public String toSql() {
            return field + " IN (:" + field.replace(".", "_") + "_values)";
        }
        
        @Override
        public Map<String, Object> getParameters() {
            Map<String, Object> params = new HashMap<>();
            params.put(field.replace(".", "_") + "_values", values);
            return params;
        }
        
        @Override
        public Criteria and(Criteria other) {
            return new CompositeCriteria(this, "AND", other);
        }
        
        @Override
        public Criteria or(Criteria other) {
            return new CompositeCriteria(this, "OR", other);
        }
    }
    
    private static class NativeCriteria implements Criteria {
        private final String sql;
        private final Map<String, Object> parameters;
        
        public NativeCriteria(String sql, Map<String, Object> parameters) {
            this.sql = sql;
            this.parameters = parameters != null ? parameters : new HashMap<>();
        }
        
        @Override
        public String toSql() {
            return sql;
        }
        
        @Override
        public Map<String, Object> getParameters() {
            return parameters;
        }
        
        @Override
        public Criteria and(Criteria other) {
            return new CompositeCriteria(this, "AND", other);
        }
        
        @Override
        public Criteria or(Criteria other) {
            return new CompositeCriteria(this, "OR", other);
        }
    }
}