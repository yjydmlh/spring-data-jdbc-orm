package io.flexdata.spring.orm.core.sql;

import io.flexdata.spring.orm.core.interfaces.Criteria;
import io.flexdata.spring.orm.core.metadata.EntityMetadata;
import io.flexdata.spring.orm.core.metadata.EntityMetadataRegistry;
import io.flexdata.spring.orm.core.metadata.FieldMetadata;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SQL生成器
 * 文件位置: src/main/java/com/example/orm/core/sql/SqlGenerator.java
 */
@Component
public class SqlGenerator {
    private final EntityMetadataRegistry metadataRegistry;

    public SqlGenerator(EntityMetadataRegistry metadataRegistry) {
        this.metadataRegistry = metadataRegistry;
    }

    public String generateSelect(Class<?> entityClass, Criteria criteria,
                                 List<String> selectedFields, List<OrderBy> orderBy,
                                 Integer limit, Integer offset) {
        EntityMetadata metadata = metadataRegistry.getMetadata(entityClass);
        StringBuilder sql = new StringBuilder("SELECT ");

        // SELECT字段
        if (selectedFields == null || selectedFields.isEmpty()) {
            sql.append("*");
        } else {
            sql.append(selectedFields.stream()
                    .map(field -> metadata.getColumnName(field))
                    .collect(Collectors.joining(", ")));
        }

        sql.append(" FROM ").append(metadata.getTableName());

        // WHERE条件
        if (criteria != null) {
            sql.append(" WHERE ").append(convertFieldNames(criteria.toSql(), metadata));
        }

        // ORDER BY
        if (orderBy != null && !orderBy.isEmpty()) {
            sql.append(" ORDER BY ");
            sql.append(orderBy.stream()
                    .map(order -> metadata.getColumnName(order.getField()) + " " + order.getDirection())
                    .collect(Collectors.joining(", ")));
        }

        // LIMIT and OFFSET
        if (limit != null) {
            sql.append(" LIMIT ").append(limit);
        }
        if (offset != null) {
            sql.append(" OFFSET ").append(offset);
        }

        return sql.toString();
    }

    public String generateInsert(Class<?> entityClass, Object entity) {
        EntityMetadata metadata = metadataRegistry.getMetadata(entityClass);
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(metadata.getTableName()).append(" (");

        List<String> columns = new ArrayList<>();
        List<String> values = new ArrayList<>();

        for (FieldMetadata field : metadata.getFields().values()) {
            if (!field.isPrimaryKey() || getFieldValue(entity, field.getFieldName()) != null) {
                columns.add(field.getColumnName());
                values.add(":" + field.getFieldName());
            }
        }

        sql.append(String.join(", ", columns));
        sql.append(") VALUES (");
        sql.append(String.join(", ", values));
        sql.append(")");

        return sql.toString();
    }

    public String generateUpdate(Class<?> entityClass, Object entity) {
        EntityMetadata metadata = metadataRegistry.getMetadata(entityClass);
        StringBuilder sql = new StringBuilder("UPDATE ");
        sql.append(metadata.getTableName()).append(" SET ");

        List<String> setClause = new ArrayList<>();
        FieldMetadata idField = null;

        for (FieldMetadata field : metadata.getFields().values()) {
            if (field.isPrimaryKey()) {
                idField = field;
            } else {
                setClause.add(field.getColumnName() + " = :" + field.getFieldName());
            }
        }

        sql.append(String.join(", ", setClause));

        if (idField != null) {
            sql.append(" WHERE ").append(idField.getColumnName()).append(" = :").append(idField.getFieldName());
        }

        return sql.toString();
    }

    public String generateDelete(Class<?> entityClass, Criteria criteria) {
        EntityMetadata metadata = metadataRegistry.getMetadata(entityClass);
        StringBuilder sql = new StringBuilder("DELETE FROM ");
        sql.append(metadata.getTableName());

        if (criteria != null) {
            sql.append(" WHERE ").append(convertFieldNames(criteria.toSql(), metadata));
        }

        return sql.toString();
    }

    public String generateCount(Class<?> entityClass, Criteria criteria) {
        EntityMetadata metadata = metadataRegistry.getMetadata(entityClass);
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ");
        sql.append(metadata.getTableName());

        if (criteria != null) {
            sql.append(" WHERE ").append(convertFieldNames(criteria.toSql(), metadata));
        }

        return sql.toString();
    }

    private String convertFieldNames(String sql, EntityMetadata metadata) {
        String result = sql;
        for (FieldMetadata field : metadata.getFields().values()) {
            result = result.replaceAll("\\b" + field.getFieldName() + "\\b", field.getColumnName());
        }
        return result;
    }

    private Object getFieldValue(Object entity, String fieldName) {
        try {
            Field field = entity.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(entity);
        } catch (Exception e) {
            return null;
        }
    }
}
