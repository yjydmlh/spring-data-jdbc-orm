package io.flexdata.spring.orm.core.repository;

import io.flexdata.spring.orm.core.datasource.DataSourceContext;
import io.flexdata.spring.orm.core.table.TableContext;
import io.flexdata.spring.orm.core.interfaces.GenericRepository;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 增强的多数据源多表Repository接口
 * 提供动态数据源和表名切换的便捷方法
 * 
 * @param <T> 实体类型
 * @param <ID> 主键类型
 */
public interface EnhancedMultiRepository<T, ID> extends GenericRepository<T, ID> {
    
    // ==================== 数据源切换方法 ====================
    
    /**
     * 在指定数据源上保存实体
     * @param entity 实体对象
     * @param dataSourceKey 数据源标识
     * @return 保存后的实体
     */
    default T saveOnDataSource(T entity, String dataSourceKey) {
        return DataSourceContext.executeWithDataSource(dataSourceKey, () -> save(entity));
    }
    
    /**
     * 在指定数据源上根据ID查找实体
     * @param id 主键
     * @param dataSourceKey 数据源标识
     * @return 实体对象
     */
    default Optional<T> findByIdOnDataSource(ID id, String dataSourceKey) {
        return DataSourceContext.executeWithDataSource(dataSourceKey, () -> findById(id));
    }
    
    /**
     * 在指定数据源上查找所有实体
     * @param dataSourceKey 数据源标识
     * @return 实体列表
     */
    default List<T> findAllOnDataSource(String dataSourceKey) {
        return DataSourceContext.executeWithDataSource(dataSourceKey, () -> findAll());
    }
    
    /**
     * 在指定数据源上分页查找实体
     * @param pageable 分页参数
     * @param dataSourceKey 数据源标识
     * @return 实体列表
     */
    default List<T> findAllOnDataSource(Pageable pageable, String dataSourceKey) {
        return DataSourceContext.executeWithDataSource(dataSourceKey, () -> findByCriteria(null, pageable).getContent());
    }
    
    /**
     * 在指定数据源上删除实体
     * @param id 主键
     * @param dataSourceKey 数据源标识
     */
    default void deleteByIdOnDataSource(ID id, String dataSourceKey) {
        DataSourceContext.executeWithDataSource(dataSourceKey, () -> {
            deleteById(id);
            return null;
        });
    }
    
    // ==================== 表名切换方法 ====================
    
    /**
     * 在指定表上保存实体
     * @param entity 实体对象
     * @param logicalTableName 逻辑表名
     * @param physicalTableName 物理表名
     * @return 保存后的实体
     */
    default T saveOnTable(T entity, String logicalTableName, String physicalTableName) {
        return TableContext.executeWithTableMapping(logicalTableName, physicalTableName, () -> save(entity));
    }
    
    /**
     * 在指定表上根据ID查找实体
     * @param id 主键
     * @param logicalTableName 逻辑表名
     * @param physicalTableName 物理表名
     * @return 实体对象
     */
    default Optional<T> findByIdOnTable(ID id, String logicalTableName, String physicalTableName) {
        return TableContext.executeWithTableMapping(logicalTableName, physicalTableName, () -> findById(id));
    }
    
    /**
     * 在指定表上查找所有实体
     * @param logicalTableName 逻辑表名
     * @param physicalTableName 物理表名
     * @return 实体列表
     */
    default List<T> findAllOnTable(String logicalTableName, String physicalTableName) {
        return TableContext.executeWithTableMapping(logicalTableName, physicalTableName, () -> findAll());
    }
    
    /**
     * 在指定表上分页查找实体
     * @param pageable 分页参数
     * @param logicalTableName 逻辑表名
     * @param physicalTableName 物理表名
     * @return 实体列表
     */
    default List<T> findAllOnTable(Pageable pageable, String logicalTableName, String physicalTableName) {
        return TableContext.executeWithTableMapping(logicalTableName, physicalTableName, () -> findByCriteria(null, pageable).getContent());
    }
    
    /**
     * 在指定表上删除实体
     * @param id 主键
     * @param logicalTableName 逻辑表名
     * @param physicalTableName 物理表名
     */
    default void deleteByIdOnTable(ID id, String logicalTableName, String physicalTableName) {
        TableContext.executeWithTableMapping(logicalTableName, physicalTableName, () -> {
            deleteById(id);
            return null;
        });
    }
    
    // ==================== 数据源和表名组合切换方法 ====================
    
    /**
     * 在指定数据源和表上保存实体
     * @param entity 实体对象
     * @param dataSourceKey 数据源标识
     * @param logicalTableName 逻辑表名
     * @param physicalTableName 物理表名
     * @return 保存后的实体
     */
    default T saveOnDataSourceAndTable(T entity, String dataSourceKey, String logicalTableName, String physicalTableName) {
        return DataSourceContext.executeWithDataSource(dataSourceKey, () -> 
            TableContext.executeWithTableMapping(logicalTableName, physicalTableName, () -> save(entity)));
    }
    
    /**
     * 在指定数据源和表上根据ID查找实体
     * @param id 主键
     * @param dataSourceKey 数据源标识
     * @param logicalTableName 逻辑表名
     * @param physicalTableName 物理表名
     * @return 实体对象
     */
    default Optional<T> findByIdOnDataSourceAndTable(ID id, String dataSourceKey, String logicalTableName, String physicalTableName) {
        return DataSourceContext.executeWithDataSource(dataSourceKey, () -> 
            TableContext.executeWithTableMapping(logicalTableName, physicalTableName, () -> findById(id)));
    }
    
    /**
     * 在指定数据源和表上查找所有实体
     * @param dataSourceKey 数据源标识
     * @param logicalTableName 逻辑表名
     * @param physicalTableName 物理表名
     * @return 实体列表
     */
    default List<T> findAllOnDataSourceAndTable(String dataSourceKey, String logicalTableName, String physicalTableName) {
        return DataSourceContext.executeWithDataSource(dataSourceKey, () -> 
            TableContext.executeWithTableMapping(logicalTableName, physicalTableName, () -> findAll()));
    }
    
    /**
     * 在指定数据源和表上分页查找实体
     * @param pageable 分页参数
     * @param dataSourceKey 数据源标识
     * @param logicalTableName 逻辑表名
     * @param physicalTableName 物理表名
     * @return 实体列表
     */
    default List<T> findAllOnDataSourceAndTable(Pageable pageable, String dataSourceKey, String logicalTableName, String physicalTableName) {
        return DataSourceContext.executeWithDataSource(dataSourceKey, () -> 
            TableContext.executeWithTableMapping(logicalTableName, physicalTableName, () -> findByCriteria(null, pageable).getContent())
        );
    }
    
    /**
     * 在指定数据源和表上删除实体
     * @param id 主键
     * @param dataSourceKey 数据源标识
     * @param logicalTableName 逻辑表名
     * @param physicalTableName 物理表名
     */
    default void deleteByIdOnDataSourceAndTable(ID id, String dataSourceKey, String logicalTableName, String physicalTableName) {
        DataSourceContext.executeWithDataSource(dataSourceKey, () -> 
            TableContext.executeWithTableMapping(logicalTableName, physicalTableName, () -> {
                deleteById(id);
                return null;
            }));
    }
    
    // ==================== 批量操作方法 ====================
    
    /**
     * 在多个数据源上执行查询操作
     * @param dataSourceKeys 数据源标识列表
     * @return 每个数据源的查询结果
     */
    default List<List<T>> findAllOnMultipleDataSources(List<String> dataSourceKeys) {
        return DataSourceContext.executeOnMultipleDataSources(dataSourceKeys, () -> findAll());
    }
    
    /**
     * 在多个表上执行查询操作
     * @param tableMappings 表名映射
     * @return 查询结果
     */
    default List<T> findAllOnMultipleTables(Map<String, String> tableMappings) {
        return TableContext.executeWithTableMappings(tableMappings, () -> findAll());
    }
    
    /**
     * 在指定数据源的多个表上执行查询操作
     * @param dataSourceKey 数据源标识
     * @param tableMappings 表名映射
     * @return 查询结果
     */
    default List<T> findAllOnDataSourceAndMultipleTables(String dataSourceKey, Map<String, String> tableMappings) {
        return DataSourceContext.executeWithDataSource(dataSourceKey, () -> 
            TableContext.executeWithTableMappings(tableMappings, () -> findAll()));
    }
}