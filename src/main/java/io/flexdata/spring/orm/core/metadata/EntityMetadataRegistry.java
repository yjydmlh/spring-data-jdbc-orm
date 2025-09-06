package io.flexdata.spring.orm.core.metadata;

import com.google.common.base.CaseFormat;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 实体元数据注册表
 * 文件位置: src/main/java/com/example/orm/core/metadata/EntityMetadataRegistry.java
 */
@Component
public class EntityMetadataRegistry {
    private final Map<Class<?>, EntityMetadata> metadataCache = new ConcurrentHashMap<>();

    public EntityMetadata getMetadata(Class<?> entityClass) {
        return metadataCache.computeIfAbsent(entityClass, this::buildMetadata);
    }

    private EntityMetadata buildMetadata(Class<?> entityClass) {
        Table tableAnnotation = entityClass.getAnnotation(Table.class);
        String tableName = tableAnnotation != null ? tableAnnotation.value() :
                CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, entityClass.getSimpleName());

        Map<String, FieldMetadata> fields = new HashMap<>();
        Field idField = null;

        for (Field field : entityClass.getDeclaredFields()) {
            field.setAccessible(true);

            if (field.isAnnotationPresent(Id.class)) {
                idField = field;
            }

            Column columnAnnotation = field.getAnnotation(Column.class);
            String columnName = columnAnnotation != null ? columnAnnotation.value() :
                    CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, field.getName());

            FieldMetadata fieldMeta = new FieldMetadata(
                    field.getName(),
                    columnName,
                    field.getType(),
                    field.isAnnotationPresent(Id.class),
                    columnAnnotation != null
            );

            fields.put(field.getName(), fieldMeta);
        }

        return new EntityMetadata(entityClass, tableName, fields, idField);
    }

    public void clearCache() {
        metadataCache.clear();
    }

    public int getCacheSize() {
        return metadataCache.size();
    }
}
