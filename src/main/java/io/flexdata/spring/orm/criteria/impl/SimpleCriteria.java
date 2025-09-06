package io.flexdata.spring.orm.criteria.impl;

import java.util.HashMap;
import java.util.Map;

/**
 * 简单条件实现
 * 文件位置: src/main/java/com/example/orm/criteria/impl/SimpleCriteria.java
 */
public class SimpleCriteria extends AbstractCriteria {

    public SimpleCriteria(String field, String operator, Object value) {
        super(field, operator, value);
    }

    @Override
    public String toSql() {
        return field + " " + operator + " :" + field.replace(".", "_");
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> params = new HashMap<>();
        params.put(field.replace(".", "_"), value);
        return params;
    }
}
