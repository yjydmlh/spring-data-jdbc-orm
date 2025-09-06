package io.flexdata.spring.orm.core.interfaces;

import io.flexdata.spring.orm.core.sql.SortDirection;
import io.flexdata.spring.orm.core.sql.complex.SubQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * 增强的查询构建器接口
 * 支持复杂SQL语法：多表JOIN、子查询、聚合函数、窗口函数等
 */
public interface EnhancedQueryBuilder<T> {
    
    // 基础查询方法
    EnhancedQueryBuilder<T> select(String... fields);
    EnhancedQueryBuilder<T> selectAs(String field, String alias);
    EnhancedQueryBuilder<T> selectSubQuery(SubQuery subQuery, String alias);
    EnhancedQueryBuilder<T> selectAggregate(String function, String field, String alias);
    EnhancedQueryBuilder<T> selectCount(String field, String alias);
    EnhancedQueryBuilder<T> selectSum(String field, String alias);
    EnhancedQueryBuilder<T> selectAvg(String field, String alias);
    EnhancedQueryBuilder<T> selectMax(String field, String alias);
    EnhancedQueryBuilder<T> selectMin(String field, String alias);
    EnhancedQueryBuilder<T> distinct();
    
    // FROM子句
    EnhancedQueryBuilder<T> from(String table);
    EnhancedQueryBuilder<T> from(String table, String alias);
    EnhancedQueryBuilder<T> fromSubQuery(SubQuery subQuery, String alias);
    
    // JOIN操作
    EnhancedQueryBuilder<T> join(String table, String alias, String onCondition);
    EnhancedQueryBuilder<T> leftJoin(String table, String alias, String onCondition);
    EnhancedQueryBuilder<T> rightJoin(String table, String alias, String onCondition);
    EnhancedQueryBuilder<T> fullJoin(String table, String alias, String onCondition);
    EnhancedQueryBuilder<T> crossJoin(String table, String alias);
    
    // 子查询JOIN
    EnhancedQueryBuilder<T> joinSubQuery(SubQuery subQuery, String alias, String onCondition);
    EnhancedQueryBuilder<T> leftJoinSubQuery(SubQuery subQuery, String alias, String onCondition);
    
    // WHERE条件
    EnhancedQueryBuilder<T> where(Criteria criteria);
    EnhancedQueryBuilder<T> where(String condition);
    EnhancedQueryBuilder<T> whereExists(SubQuery subQuery);
    EnhancedQueryBuilder<T> whereNotExists(SubQuery subQuery);
    EnhancedQueryBuilder<T> whereIn(String field, SubQuery subQuery);
    EnhancedQueryBuilder<T> whereNotIn(String field, SubQuery subQuery);
    
    // 复杂WHERE条件（支持表别名）
    EnhancedQueryBuilder<T> whereTableField(String tableAlias, String field, String operator, Object value);
    EnhancedQueryBuilder<T> whereTableFieldEquals(String tableAlias, String field, Object value);
    EnhancedQueryBuilder<T> whereTableFieldIn(String tableAlias, String field, List<Object> values);
    
    // 条件组合
    EnhancedQueryBuilder<T> and(Criteria criteria);
    EnhancedQueryBuilder<T> or(Criteria criteria);
    
    // GROUP BY和HAVING
    EnhancedQueryBuilder<T> groupBy(String... fields);
    EnhancedQueryBuilder<T> having(Criteria criteria);
    EnhancedQueryBuilder<T> having(String condition);
    
    // ORDER BY
    EnhancedQueryBuilder<T> orderBy(String field, SortDirection direction);
    EnhancedQueryBuilder<T> orderBy(String field);
    EnhancedQueryBuilder<T> orderByAsc(String field);
    EnhancedQueryBuilder<T> orderByDesc(String field);
    
    // 分页
    EnhancedQueryBuilder<T> limit(int limit);
    EnhancedQueryBuilder<T> offset(int offset);
    EnhancedQueryBuilder<T> page(int page, int size);
    
    // 窗口函数
    EnhancedQueryBuilder<T> selectWindowFunction(String function, String field, String alias);
    EnhancedQueryBuilder<T> selectRowNumber(String alias);
    EnhancedQueryBuilder<T> selectRank(String alias);
    EnhancedQueryBuilder<T> selectDenseRank(String alias);
    
    // CTE（公共表表达式）
    EnhancedQueryBuilder<T> withCte(String name, SubQuery query);
    EnhancedQueryBuilder<T> withCte(String name, SubQuery query, String... columnNames);
    
    // UNION操作
    EnhancedQueryBuilder<T> union(EnhancedQueryBuilder<T> other);
    EnhancedQueryBuilder<T> unionAll(EnhancedQueryBuilder<T> other);
    EnhancedQueryBuilder<T> intersect(EnhancedQueryBuilder<T> other);
    EnhancedQueryBuilder<T> except(EnhancedQueryBuilder<T> other);
    
    // 执行方法
    List<T> execute();
    Page<T> executePage(Pageable pageable);
    long count();
    T executeFirst();
    T executeUnique();
    
    // 获取生成的SQL和参数
    String toSql();
    Map<String, Object> getParameters();
    
    // 原生SQL支持
    EnhancedQueryBuilder<T> nativeWhere(String sql, Map<String, Object> params);
    EnhancedQueryBuilder<T> nativeHaving(String sql, Map<String, Object> params);
    
    // 动态查询支持
    EnhancedQueryBuilder<T> when(boolean condition, QueryBuilderFunction<EnhancedQueryBuilder<T>> function);
    
    /**
     * 函数式接口，用于动态查询
     */
    @FunctionalInterface
    interface QueryBuilderFunction<T> {
        T apply(T queryBuilder);
    }
}