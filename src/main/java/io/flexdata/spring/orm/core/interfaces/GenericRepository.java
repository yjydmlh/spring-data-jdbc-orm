package io.flexdata.spring.orm.core.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * 通用仓储接口
 * 文件位置: src/main/java/com/example/orm/core/interfaces/GenericRepository.java
 */
public interface GenericRepository<T, ID> {
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
    List<T> findByCriteria(Criteria criteria);

    /**
     * 分页查询
     */
    Page<T> findByCriteria(Criteria criteria, Pageable pageable);

    /**
     * 统计总数
     */
    long count();

    /**
     * 根据条件统计
     */
    long countByCriteria(Criteria criteria);

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
    void deleteByCriteria(Criteria criteria);

    /**
     * 根据ID检查是否存在
     */
    boolean existsById(ID id);
}
