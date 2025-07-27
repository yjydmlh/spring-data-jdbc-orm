package com.spring.jdbc.orm.criteria.impl;

import com.spring.jdbc.orm.core.interfaces.TypeSafeCriteria;

import java.util.HashMap;
import java.util.Map;

/**
 * 复合类型安全条件实现
 * 文件位置: src/main/java/com/example/orm/criteria/impl/CompositeTypeSafeCriteria.java
 */
public class CompositeTypeSafeCriteria<T> implements TypeSafeCriteria<T> {
    private final TypeSafeCriteria<T> left;
    private final String operator;
    private final TypeSafeCriteria<T> right;

    public CompositeTypeSafeCriteria(TypeSafeCriteria<T> left, String operator, TypeSafeCriteria<T> right) {
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    @Override
    public String toSql() {
        return "(" + left.toSql() + " " + operator + " " + right.toSql() + ")";
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<>();
        params.putAll(left.getParameters());
        params.putAll(right.getParameters());
        return params;
    }

    @Override
    public TypeSafeCriteria<T> and(TypeSafeCriteria<T> other) {
        return new CompositeTypeSafeCriteria<>(this, "AND", other);
    }

    @Override
    public TypeSafeCriteria<T> or(TypeSafeCriteria<T> other) {
        return new CompositeTypeSafeCriteria<>(this, "OR", other);
    }
}
