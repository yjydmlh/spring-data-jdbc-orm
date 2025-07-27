package com.spring.jdbc.orm.criteria.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class InCriteria extends AbstractCriteria {
    private final Collection<?> values;

    public InCriteria(String field, Collection<?> values) {
        super(field, "IN", values);
        this.values = values;
    }

    @Override
    public String toSql() {
        StringBuilder sb = new StringBuilder();
        sb.append(field).append(" IN (");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(":").append(field.replace(".", "_")).append("_").append(i);
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<>();
        int i = 0;
        for (Object value : values) {
            params.put(field.replace(".", "_") + "_" + i, value);
            i++;
        }
        return params;
    }
}
