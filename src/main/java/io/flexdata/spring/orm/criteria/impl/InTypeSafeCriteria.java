package io.flexdata.spring.orm.criteria.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * IN条件实现
 * 文件位置: src/main/java/com/example/orm/criteria/impl/InTypeSafeCriteria.java
 */
public class InTypeSafeCriteria<T> extends AbstractTypeSafeCriteria<T> {
    private final Collection<?> values;

    public InTypeSafeCriteria(String field, Collection<?> values) {
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
