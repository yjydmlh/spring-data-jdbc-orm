package io.flexdata.spring.orm.core.sql;

/**
 * 排序方向枚举
 * 文件位置: src/main/java/com/example/orm/core/sql/SortDirection.java
 */
public enum SortDirection {
    ASC("ASC"),
    DESC("DESC");

    private final String value;

    SortDirection(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
