package com.spring.jdbc.orm.template.impl;

import com.spring.jdbc.orm.core.interfaces.TypeSafeCriteria;
import com.spring.jdbc.orm.template.TypeSafeOrmTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 事务性ORM模板
 */
@Component
public class TransactionalOrmTemplate {

    private final TypeSafeOrmTemplate ormTemplate;

    public TransactionalOrmTemplate(TypeSafeOrmTemplate ormTemplate) {
        this.ormTemplate = ormTemplate;
    }

    /**
     * 事务性保存
     */
    @Transactional
    public <T> T saveTransactional(T entity) {
        return ormTemplate.save(entity);
    }

    /**
     * 事务性批量保存
     */
    @Transactional
    public <T> List<T> saveAllTransactional(List<T> entities) {
        return ormTemplate.saveAll(entities);
    }

    /**
     * 事务性删除
     */
    @Transactional
    public <T> void deleteTransactional(T entity) {
        ormTemplate.delete(entity);
    }

    /**
     * 事务性批量删除
     */
    @Transactional
    public <T> void deleteByCriteriaTransactional(Class<T> entityClass, TypeSafeCriteria<T> criteria) {
        ormTemplate.deleteByCriteria(entityClass, criteria);
    }

    /**
     * 事务性更新操作
     */
    @Transactional
    public <T> void updateBatch(List<T> entities) {
        entities.forEach(ormTemplate::save);
    }
}
