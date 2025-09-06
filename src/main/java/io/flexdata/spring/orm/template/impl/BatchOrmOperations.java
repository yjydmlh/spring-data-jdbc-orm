package io.flexdata.spring.orm.template.impl;

import io.flexdata.spring.orm.core.metadata.EntityMetadata;
import io.flexdata.spring.orm.core.metadata.EntityMetadataRegistry;
import io.flexdata.spring.orm.core.metadata.FieldMetadata;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量操作工具
 */
@Component
public class BatchOrmOperations {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final EntityMetadataRegistry metadataRegistry;

    public BatchOrmOperations(NamedParameterJdbcTemplate jdbcTemplate,
                              EntityMetadataRegistry metadataRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        this.metadataRegistry = metadataRegistry;
    }

    /**
     * 批量插入
     */
    public <T> void batchInsert(List<T> entities) {
        if (entities.isEmpty()) return;

        Class<?> entityClass = entities.get(0).getClass();
        EntityMetadata metadata = metadataRegistry.getMetadata(entityClass);

        // 构建批量插入SQL
        String sql = buildBatchInsertSql(metadata);

        // 准备参数
        SqlParameterSource[] batchParams = entities.stream()
                .map(entity -> entityToParameterSource(entity, metadata))
                .toArray(SqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(sql, batchParams);
    }

    /**
     * 批量更新
     */
    public <T> void batchUpdate(List<T> entities) {
        if (entities.isEmpty()) return;

        Class<?> entityClass = entities.get(0).getClass();
        EntityMetadata metadata = metadataRegistry.getMetadata(entityClass);

        // 构建批量更新SQL
        String sql = buildBatchUpdateSql(metadata);

        // 准备参数
        SqlParameterSource[] batchParams = entities.stream()
                .map(entity -> entityToParameterSource(entity, metadata))
                .toArray(SqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(sql, batchParams);
    }

    private String buildBatchInsertSql(EntityMetadata metadata) {
        List<String> columns = new ArrayList<>();
        List<String> values = new ArrayList<>();

        for (FieldMetadata field : metadata.getFields().values()) {
            if (!field.isPrimaryKey()) { // 跳过自增主键
                columns.add(field.getColumnName());
                values.add(":" + field.getFieldName());
            }
        }

        return String.format("INSERT INTO %s (%s) VALUES (%s)",
                metadata.getTableName(),
                String.join(", ", columns),
                String.join(", ", values));
    }

    private String buildBatchUpdateSql(EntityMetadata metadata) {
        List<String> setClause = new ArrayList<>();
        FieldMetadata idField = null;

        for (FieldMetadata field : metadata.getFields().values()) {
            if (field.isPrimaryKey()) {
                idField = field;
            } else {
                setClause.add(field.getColumnName() + " = :" + field.getFieldName());
            }
        }

        String sql = String.format("UPDATE %s SET %s WHERE %s = :%s",
                metadata.getTableName(),
                String.join(", ", setClause),
                idField.getColumnName(),
                idField.getFieldName());

        return sql;
    }

    private SqlParameterSource entityToParameterSource(Object entity, EntityMetadata metadata) {
        MapSqlParameterSource params = new MapSqlParameterSource();

        for (FieldMetadata field : metadata.getFields().values()) {
            try {
                Field javaField = entity.getClass().getDeclaredField(field.getFieldName());
                javaField.setAccessible(true);
                Object value = javaField.get(entity);
                params.addValue(field.getFieldName(), value);
            } catch (Exception e) {
                // 忽略获取失败的字段
            }
        }

        return params;
    }
}
