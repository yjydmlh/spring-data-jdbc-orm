package io.flexdata.spring.orm.core.interfaces;

import io.flexdata.spring.orm.core.datasource.DataSourceContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * 多数据源Repository接口
 * 提供便捷的数据源切换方法
 * @param <T> 实体类型
 * @param <ID> 主键类型
 */
public interface MultiDataSourceRepository<T, ID> extends GenericRepository<T, ID> {
    
    // ========== 数据源切换便捷方法 ==========
    
    /**
     * 在指定数据源上执行查询
     * @param dataSourceKey 数据源标识
     * @param action 查询操作
     * @param <R> 返回值类型
     * @return 查询结果
     */
    default <R> R withDataSource(String dataSourceKey, DataSourceContext.DataSourceAction<R> action) {
        return DataSourceContext.executeWithDataSource(dataSourceKey, action);
    }
    
    /**
     * 在只读数据源上查询所有记录
     * @return 所有记录
     */
    default List<T> findAllFromReadOnly() {
        return withDataSource("readonly", this::findAll);
    }
    
    /**
     * 在只读数据源上根据ID查询
     * @param id 主键
     * @return 查询结果
     */
    default Optional<T> findByIdFromReadOnly(ID id) {
        return withDataSource("readonly", () -> findById(id));
    }
    
    /**
     * 在只读数据源上分页查询
     * @param pageable 分页参数
     * @return 分页结果
     */
    default Page<T> findAllFromReadOnly(Pageable pageable) {
        return withDataSource("readonly", () -> findByCriteria(null, pageable));
    }
    
    /**
     * 在只读数据源上根据条件查询
     * @param criteria 查询条件
     * @return 查询结果
     */
    default List<T> findByCriteriaFromReadOnly(Criteria criteria) {
        return withDataSource("readonly", () -> findByCriteria(criteria));
    }
    
    /**
     * 在只读数据源上统计记录数
     * @return 记录总数
     */
    default long countFromReadOnly() {
        return withDataSource("readonly", this::count);
    }
    
    /**
     * 在第二个数据源上执行操作
     * @param action 操作
     * @param <R> 返回值类型
     * @return 操作结果
     */
    default <R> R withSecondaryDataSource(DataSourceContext.DataSourceAction<R> action) {
        return withDataSource("secondary", action);
    }
    
    /**
     * 在主数据源上执行写操作
     * @param action 写操作
     * @param <R> 返回值类型
     * @return 操作结果
     */
    default <R> R withMasterDataSource(DataSourceContext.DataSourceAction<R> action) {
        return withDataSource("default", action);
    }
    
    // ========== 读写分离便捷方法 ==========
    
    /**
     * 强制从主库读取（适用于写后立即读的场景）
     * @param id 主键
     * @return 查询结果
     */
    default Optional<T> findByIdFromMaster(ID id) {
        return withMasterDataSource(() -> findById(id));
    }
    
    /**
     * 在主库上执行保存操作
     * @param entity 实体对象
     * @return 保存后的实体
     */
    default T saveToMaster(T entity) {
        return withMasterDataSource(() -> save(entity));
    }
    
    /**
     * 在主库上执行批量保存
     * @param entities 实体列表
     * @return 保存后的实体列表
     */
    default List<T> saveAllToMaster(List<T> entities) {
        return withMasterDataSource(() -> saveAll(entities));
    }
    
    /**
     * 在主库上执行删除操作
     * @param id 主键
     */
    default void deleteByIdFromMaster(ID id) {
        withMasterDataSource(() -> {
            deleteById(id);
            return null;
        });
    }
}