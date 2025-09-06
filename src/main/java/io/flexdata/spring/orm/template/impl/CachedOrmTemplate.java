package io.flexdata.spring.orm.template.impl;

import io.flexdata.spring.orm.core.interfaces.TypeSafeCriteria;
import io.flexdata.spring.orm.template.TypeSafeOrmTemplate;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * 缓存ORM模板
 */
@Component
public class CachedOrmTemplate {

    private final TypeSafeOrmTemplate ormTemplate;

    public CachedOrmTemplate(TypeSafeOrmTemplate ormTemplate) {
        this.ormTemplate = ormTemplate;
    }

    /**
     * 缓存查询结果
     */
    @Cacheable(value = "orm-entities", key = "#entityClass.simpleName + ':' + #id")
    public <T, ID> Optional<T> findByIdCached(Class<T> entityClass, ID id) {
        return ormTemplate.findById(entityClass, id);
    }

    /**
     * 缓存查询结果
     */
    @Cacheable(value = "orm-queries", key = "#entityClass.simpleName + ':' + #criteria.toString()")
    public <T> List<T> findByCriteriaCached(Class<T> entityClass, TypeSafeCriteria<T> criteria) {
        return ormTemplate.findByCriteria(entityClass, criteria);
    }

    /**
     * 保存并清除缓存
     */
    @CacheEvict(value = {"orm-entities", "orm-queries"}, key = "#entity.class.simpleName + '*'")
    public <T> T saveAndEvictCache(T entity) {
        return ormTemplate.save(entity);
    }

    /**
     * 删除并清除缓存
     */
    @CacheEvict(value = {"orm-entities", "orm-queries"}, key = "#entityClass.simpleName + '*'")
    public <T, ID> void deleteByIdAndEvictCache(Class<T> entityClass, ID id) {
        ormTemplate.deleteById(entityClass, id);
    }
}
