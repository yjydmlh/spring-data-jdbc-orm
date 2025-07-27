package com.spring.jdbc.orm.criteria.impl;

import java.util.HashMap;
import java.util.Map;

public class NullCriteria extends AbstractCriteria {
    private final boolean isNull;

    public NullCriteria(String field, boolean isNull) {
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
