package io.flexdata.spring.orm.criteria.impl;

import io.flexdata.spring.orm.core.interfaces.Criteria;

/**
 * 抽象条件基类
 * 文件位置: src/main/java/com/example/orm/criteria/impl/AbstractCriteria.java
 */
public abstract class AbstractCriteria implements Criteria {
    protected String field;
    protected Object value;
    protected String operator;

    public AbstractCriteria(String field, String operator, Object value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
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
