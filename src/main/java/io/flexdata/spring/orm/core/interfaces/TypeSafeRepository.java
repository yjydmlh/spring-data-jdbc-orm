package io.flexdata.spring.orm.core.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

/**
 * 类型安全的仓储接口
 * 文件位置: src/main/java/com/example/orm/core/interfaces/TypeSafeRepository.java
 */
public interface TypeSafeRepository<T, ID> {
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
     * 根据条件查找
     */
    List<T> findByCriteria(TypeSafeCriteria<T> criteria);

    /**
     * 分页查询
     */
    Page<T> findByCriteria(TypeSafeCriteria<T> criteria, Pageable pageable);

    /**
     * 统计总数
     */
    long count();

    /**
     * 根据条件统计
     */
    long countByCriteria(TypeSafeCriteria<T> criteria);

    /**
     * 根据ID删除
     */
    void deleteById(ID id);

    /**
     * 删除实体
     */
    void delete(T entity);

    /**
     * 根据条件删除
     */
    void deleteByCriteria(TypeSafeCriteria<T> criteria);

    /**
     * 根据ID检查是否存在
     */
    boolean existsById(ID id);

    /**
     * 查找单个实体
     */
    Optional<T> findOne(TypeSafeCriteria<T> criteria);

    /**
     * 排序查询
     */
    List<T> findAll(TypeSafeCriteria<T> criteria, Sort sort);

    /**
     * 根据条件检查是否存在
     */
    boolean exists(TypeSafeCriteria<T> criteria);
}
