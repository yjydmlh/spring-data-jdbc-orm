package com.spring.jdbc.orm.core.metadata;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * 实体元数据
 * 文件位置: src/main/java/com/example/orm/core/metadata/EntityMetadata.java
 */
public class EntityMetadata {
    private final Class<?> entityClass;
    private final String tableName;
    private final Map<String, FieldMetadata> fields;
    private final Field idField;

    public EntityMetadata(Class<?> entityClass, String tableName,
                          Map<String, FieldMetadata> fields, Field idField) {
        this.entityClass = entityClass;
        this.tableName = tableName;
        this.fields = fields;
        this.idField = idField;
    }

    public String getTableName() {
        return tableName;
    }

    public Map<String, FieldMetadata> getFields() {
        return fields;
    }

    public Field getIdField() {
        return idField;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public String getColumnName(String fieldName) {
        FieldMetadata field = fields.get(fieldName);
        return field != null ? field.getColumnName() : fieldName;
    }
}
