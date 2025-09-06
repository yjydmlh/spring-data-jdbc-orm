package io.flexdata.spring.orm.criteria.impl;

import io.flexdata.spring.orm.core.interfaces.Criteria;

import java.util.HashMap;
import java.util.Map;

/**
 * 复合条件实现
 * 文件位置: src/main/java/com/example/orm/criteria/impl/CompositeCriteria.java
 */
public class CompositeCriteria implements Criteria {
    private final Criteria left;
    private final String operator;
    private final Criteria right;

    public CompositeCriteria(Criteria left, String operator, Criteria right) {
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
    public Criteria and(Criteria other) {
        return new CompositeCriteria(this, "AND", other);
    }

    @Override
    public Criteria or(Criteria other) {
        return new CompositeCriteria(this, "OR", other);
    }
}
