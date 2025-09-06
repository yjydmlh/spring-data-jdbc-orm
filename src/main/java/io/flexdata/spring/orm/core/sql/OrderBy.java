package io.flexdata.spring.orm.core.sql;

/**
 * 排序信息
 * 文件位置: src/main/java/com/example/orm/core/sql/OrderBy.java
 */
public class OrderBy {
    private final String field;
    private final SortDirection direction;

    public OrderBy(String field, SortDirection direction) {
        this.field = field;
        this.direction = direction;
    }

    public static OrderBy asc(String field) {
        return new OrderBy(field, SortDirection.ASC);
    }

    public static OrderBy desc(String field) {
        return new OrderBy(field, SortDirection.DESC);
    }

    public String getField() {
        return field;
    }

    public SortDirection getDirection() {
        return direction;
    }

    @Override
    public String toString() {
        return field + " " + direction;
    }
}
