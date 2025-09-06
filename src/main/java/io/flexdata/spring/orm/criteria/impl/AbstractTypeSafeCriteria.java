package io.flexdata.spring.orm.criteria.impl;


import io.flexdata.spring.orm.core.interfaces.TypeSafeCriteria;

/**
 * 类型安全抽象条件基类
 * 文件位置: src/main/java/com/example/orm/criteria/impl/AbstractTypeSafeCriteria.java
 */
public abstract class AbstractTypeSafeCriteria<T> implements TypeSafeCriteria<T> {
    protected String field;
    protected Object value;
    protected String operator;

    public AbstractTypeSafeCriteria(String field, String operator, Object value) {
        this.field = field;
        this.operator = operator;
        this.value = value;
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
