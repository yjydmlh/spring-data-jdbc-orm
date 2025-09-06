package io.flexdata.spring.orm.repository;

import io.flexdata.spring.orm.core.interfaces.Criteria;
import io.flexdata.spring.orm.core.interfaces.EnhancedQueryBuilder;
import io.flexdata.spring.orm.core.sql.complex.SubQuery;
import io.flexdata.spring.orm.core.sql.SortDirection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 增强Repository接口
 * 提供复杂SQL查询能力，包括多表JOIN、子查询、聚合函数等
 * 
 * @param <T> 实体类型
 * @param <ID> 主键类型
 */
public interface EnhancedRepository<T, ID> {
    
    // ========== 基础CRUD操作 ==========
    
    /**
     * 保存实体
     */
    T save(T entity);
    
    /**
     * 批量保存
     */
    List<T> saveAll(Iterable<T> entities);
    
    /**
     * 根据ID查找
     */
    Optional<T> findById(ID id);
    
    /**
     * 查找所有
     */
    List<T> findAll();
    
    /**
     * 根据ID删除
     */
    void deleteById(ID id);
    
    /**
     * 删除实体
     */
    void delete(T entity);
    
    /**
     * 检查是否存在
     */
    boolean existsById(ID id);
    
    /**
     * 统计总数
     */
    long count();
    
    // ========== 增强查询构建器 ==========
    
    /**
     * 创建增强查询构建器
     */
    EnhancedQueryBuilder<T> createQuery();
    
    /**
     * 创建增强查询构建器（指定返回类型）
     */
    <R> EnhancedQueryBuilder<R> createQuery(Class<R> resultType);
    
    // ========== 条件查询 ==========
    
    /**
     * 根据条件查找
     */
    List<T> findByCriteria(Criteria criteria);
    
    /**
     * 根据条件查找第一个
     */
    Optional<T> findFirstByCriteria(Criteria criteria);
    
    /**
     * 根据条件统计
     */
    long countByCriteria(Criteria criteria);
    
    /**
     * 根据条件检查是否存在
     */
    boolean existsByCriteria(Criteria criteria);
    
    /**
     * 根据条件分页查询
     */
    Page<T> findByCriteria(Criteria criteria, Pageable pageable);
    
    // ========== 多表JOIN查询 ==========
    
    /**
     * 创建JOIN查询
     */
    EnhancedQueryBuilder<T> join(String table, String alias, String onCondition);
    
    /**
     * 创建LEFT JOIN查询
     */
    EnhancedQueryBuilder<T> leftJoin(String table, String alias, String onCondition);
    
    /**
     * 创建RIGHT JOIN查询
     */
    EnhancedQueryBuilder<T> rightJoin(String table, String alias, String onCondition);
    
    /**
     * 创建FULL JOIN查询
     */
    EnhancedQueryBuilder<T> fullJoin(String table, String alias, String onCondition);
    
    // ========== 子查询支持 ==========
    
    /**
     * 创建子查询
     */
    SubQuery createSubQuery();
    
    /**
     * 根据子查询IN条件查找
     */
    List<T> findBySubQueryIn(String field, SubQuery subQuery);
    
    /**
     * 根据EXISTS子查询查找
     */
    List<T> findByExists(SubQuery subQuery);
    
    /**
     * 根据NOT EXISTS子查询查找
     */
    List<T> findByNotExists(SubQuery subQuery);
    
    // ========== 聚合查询 ==========
    
    /**
     * 分组统计
     */
    List<Map<String, Object>> groupBy(String... fields);
    
    /**
     * 分组统计（带条件）
     */
    List<Map<String, Object>> groupBy(Criteria criteria, String... fields);
    
    /**
     * 分组统计（带HAVING条件）
     */
    List<Map<String, Object>> groupByHaving(Criteria whereClause, String[] groupFields, Criteria havingClause);
    
    /**
     * 求和
     */
    <N extends Number> N sum(String field, Class<N> resultType);
    
    /**
     * 求和（带条件）
     */
    <N extends Number> N sum(String field, Criteria criteria, Class<N> resultType);
    
    /**
     * 求平均值
     */
    <N extends Number> N avg(String field, Class<N> resultType);
    
    /**
     * 求平均值（带条件）
     */
    <N extends Number> N avg(String field, Criteria criteria, Class<N> resultType);
    
    /**
     * 求最大值
     */
    <V> V max(String field, Class<V> resultType);
    
    /**
     * 求最大值（带条件）
     */
    <V> V max(String field, Criteria criteria, Class<V> resultType);
    
    /**
     * 求最小值
     */
    <V> V min(String field, Class<V> resultType);
    
    /**
     * 求最小值（带条件）
     */
    <V> V min(String field, Criteria criteria, Class<V> resultType);
    
    // ========== 批量操作 ==========
    
    /**
     * 批量更新
     */
    int updateByCriteria(Map<String, Object> updates, Criteria criteria);
    
    /**
     * 批量删除
     */
    int deleteByCriteria(Criteria criteria);
    
    // ========== 原生SQL支持 ==========
    
    /**
     * 执行原生查询
     */
    List<T> findByNativeQuery(String sql, Map<String, Object> parameters);
    
    /**
     * 执行原生查询（指定返回类型）
     */
    <R> List<R> findByNativeQuery(String sql, Map<String, Object> parameters, Class<R> resultType);
    
    /**
     * 执行原生更新
     */
    int executeNativeUpdate(String sql, Map<String, Object> parameters);
    
    /**
     * 执行原生查询返回单个结果
     */
    <R> R queryForObject(String sql, Map<String, Object> parameters, Class<R> resultType);
    
    // ========== 动态查询 ==========
    
    /**
     * 动态查询构建器
     */
    DynamicQueryBuilder<T> dynamicQuery();
    
    /**
     * 根据示例查询
     */
    List<T> findByExample(T example);
    
    /**
     * 根据示例查询（忽略null字段）
     */
    List<T> findByExampleIgnoreNull(T example);
    
    // ========== 缓存支持 ==========
    
    /**
     * 启用查询缓存
     */
    EnhancedRepository<T, ID> enableCache();
    
    /**
     * 禁用查询缓存
     */
    EnhancedRepository<T, ID> disableCache();
    
    /**
     * 清除缓存
     */
    void clearCache();
    
    // ========== 事务支持 ==========
    
    /**
     * 在事务中执行
     */
    <R> R executeInTransaction(TransactionCallback<T, ID, R> callback);
    
    /**
     * 事务回调接口
     */
    @FunctionalInterface
    interface TransactionCallback<T, ID, R> {
        R doInTransaction(EnhancedRepository<T, ID> repository);
    }
    
    // ========== 动态查询构建器接口 ==========
    
    interface DynamicQueryBuilder<T> {
        
        /**
         * 添加等于条件（如果值不为null）
         */
        DynamicQueryBuilder<T> eqIfNotNull(String field, Object value);
        
        /**
         * 添加LIKE条件（如果值不为null且不为空）
         */
        DynamicQueryBuilder<T> likeIfNotEmpty(String field, String value);
        
        /**
         * 添加IN条件（如果集合不为空）
         */
        DynamicQueryBuilder<T> inIfNotEmpty(String field, List<?> values);
        
        /**
         * 添加范围条件
         */
        DynamicQueryBuilder<T> betweenIfNotNull(String field, Object start, Object end);
        
        /**
         * 添加大于等于条件
         */
        DynamicQueryBuilder<T> gteIfNotNull(String field, Object value);
        
        /**
         * 添加小于等于条件
         */
        DynamicQueryBuilder<T> lteIfNotNull(String field, Object value);
        
        /**
         * 添加自定义条件
         */
        DynamicQueryBuilder<T> addCondition(boolean condition, Criteria criteria);
        
        /**
         * 排序
         */
        DynamicQueryBuilder<T> orderBy(String field, SortDirection direction);
        
        /**
         * 执行查询
         */
        List<T> execute();
        
        /**
         * 执行分页查询
         */
        Page<T> execute(Pageable pageable);
        
        /**
         * 执行统计
         */
        long count();
    }
}