package io.flexdata.spring.orm.core.metadata;

/**
 * 字段元数据
 * 文件位置: src/main/java/com/example/orm/core/metadata/FieldMetadata.java
 */
public class FieldMetadata {
    private final String fieldName;
    private final String columnName;
    private final Class<?> fieldType;
    private final boolean isPrimaryKey;
    private final boolean hasColumnAnnotation;

    public FieldMetadata(String fieldName, String columnName, Class<?> fieldType,
                         boolean isPrimaryKey, boolean hasColumnAnnotation) {
        this.fieldName = fieldName;
        this.columnName = columnName;
        this.fieldType = fieldType;
        this.isPrimaryKey = isPrimaryKey;
        this.hasColumnAnnotation = hasColumnAnnotation;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getColumnName() {
        return columnName;
    }

    public Class<?> getFieldType() {
        return fieldType;
    }

    public boolean isPrimaryKey() {
        return isPrimaryKey;
    }

    public boolean hasColumnAnnotation() {
        return hasColumnAnnotation;
    }
}
