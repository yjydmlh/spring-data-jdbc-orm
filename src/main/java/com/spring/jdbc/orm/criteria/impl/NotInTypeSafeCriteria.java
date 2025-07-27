package com.spring.jdbc.orm.criteria.impl;

import java.util.Collection;

/**
 * NOT IN条件实现
 * 文件位置: src/main/java/com/example/orm/criteria/impl/NotInTypeSafeCriteria.java
 */
public class NotInTypeSafeCriteria<T> extends InTypeSafeCriteria<T> {

    public NotInTypeSafeCriteria(String field, Collection<?> values) {
        super(field, values);
    }

    @Override
    public String toSql() {
        return super.toSql().replace(" IN ", " NOT IN ");
    }
}
