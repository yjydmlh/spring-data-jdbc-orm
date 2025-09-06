package io.flexdata.spring.orm.criteria.impl;

import java.util.HashMap;
import java.util.Map;

/**
 * NULL条件实现
 * 文件位置: src/main/java/com/example/orm/criteria/impl/NullTypeSafeCriteria.java
 */
public class NullTypeSafeCriteria<T> extends AbstractTypeSafeCriteria<T> {
    private final boolean isNull;

    public NullTypeSafeCriteria(String field, boolean isNull) {
        super(field, isNull ? "IS NULL" : "IS NOT NULL", null);
        this.isNull = isNull;
    }

    @Override
    public String toSql() {
        return field + " " + (isNull ? "IS NULL" : "IS NOT NULL");
    }

    @Override
    public Map<String, Object> getParameters() {
        return new HashMap<>();
    }
}
