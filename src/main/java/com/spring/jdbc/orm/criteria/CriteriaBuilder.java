package com.spring.jdbc.orm.criteria;

import com.spring.jdbc.orm.core.interfaces.Criteria;
import com.spring.jdbc.orm.criteria.impl.BetweenCriteria;
import com.spring.jdbc.orm.criteria.impl.InCriteria;
import com.spring.jdbc.orm.criteria.impl.NullCriteria;
import com.spring.jdbc.orm.criteria.impl.SimpleCriteria;

import java.util.Collection;

/**
 * 传统条件构建器
 * 文件位置: src/main/java/com/example/orm/criteria/CriteriaBuilder.java
 */
public class CriteriaBuilder {

    public static Criteria eq(String field, Object value) {
        return new SimpleCriteria(field, "=", value);
    }

    public static Criteria ne(String field, Object value) {
        return new SimpleCriteria(field, "!=", value);
    }

    public static Criteria gt(String field, Object value) {
        return new SimpleCriteria(field, ">", value);
    }

    public static Criteria gte(String field, Object value) {
        return new SimpleCriteria(field, ">=", value);
    }

    public static Criteria lt(String field, Object value) {
        return new SimpleCriteria(field, "<", value);
    }

    public static Criteria lte(String field, Object value) {
        return new SimpleCriteria(field, "<=", value);
    }

    public static Criteria like(String field, String pattern) {
        return new SimpleCriteria(field, "LIKE", pattern);
    }

    public static Criteria in(String field, Collection<?> values) {
        return new InCriteria(field, values);
    }

    public static Criteria isNull(String field) {
        return new NullCriteria(field, true);
    }

    public static Criteria isNotNull(String field) {
        return new NullCriteria(field, false);
    }

    public static Criteria between(String field, Object start, Object end) {
        return new BetweenCriteria(field, start, end);
    }
}
