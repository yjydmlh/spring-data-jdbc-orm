package com.spring.jdbc.orm.template.impl;

import com.spring.jdbc.orm.template.TypeSafeOrmTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

/**
 * 审计ORM模板
 */
@Component
public class AuditableOrmTemplate {

    private final TypeSafeOrmTemplate ormTemplate;

    public AuditableOrmTemplate(TypeSafeOrmTemplate ormTemplate) {
        this.ormTemplate = ormTemplate;
    }

    /**
     * 保存实体并设置审计字段
     */
    public <T> T saveWithAudit(T entity, String currentUser) {
        setAuditFields(entity, currentUser);
        return ormTemplate.save(entity);
    }

    private void setAuditFields(Object entity, String currentUser) {
        Class<?> entityClass = entity.getClass();
        LocalDateTime now = LocalDateTime.now();

        // 设置创建时间和创建人
        setFieldValue(entity, entityClass, "createdAt", now);
        setFieldValue(entity, entityClass, "createdBy", currentUser);

        // 设置更新时间和更新人
        setFieldValue(entity, entityClass, "updatedAt", now);
        setFieldValue(entity, entityClass, "updatedBy", currentUser);
    }

    private void setFieldValue(Object entity, Class<?> entityClass, String fieldName, Object value) {
        try {
            Field field = entityClass.getDeclaredField(fieldName);
            field.setAccessible(true);

            // 如果是新实体，设置创建字段；否则只设置更新字段
            if (fieldName.startsWith("created")) {
                Object currentValue = field.get(entity);
                if (currentValue == null) {
                    field.set(entity, value);
                }
            } else {
                field.set(entity, value);
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // 字段不存在或无法访问，忽略
        }
    }
}
