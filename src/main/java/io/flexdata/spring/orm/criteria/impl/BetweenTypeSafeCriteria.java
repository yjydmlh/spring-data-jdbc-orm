package io.flexdata.spring.orm.criteria.impl;

import java.util.HashMap;
import java.util.Map;

/**
 * BETWEEN条件实现
 * 文件位置: src/main/java/com/example/orm/criteria/impl/BetweenTypeSafeCriteria.java
 */
public class BetweenTypeSafeCriteria<T> extends AbstractTypeSafeCriteria<T> {
    private final Object start;
    private final Object end;

    public BetweenTypeSafeCriteria(String field, Object start, Object end) {
        super(field, "BETWEEN", null);
        this.start = start;
        this.end = end;
    }

    @Override
    public String toSql() {
        return field + " BETWEEN :" + field.replace(".", "_") + "_start AND :" +
                field.replace(".", "_") + "_end";
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put(field.replace(".", "_") + "_start", start);
        params.put(field.replace(".", "_") + "_end", end);
        return params;
    }
}
