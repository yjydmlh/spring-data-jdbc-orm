package io.flexdata.spring.orm.criteria;

import io.flexdata.spring.orm.core.interfaces.TypeSafeCriteria;
import io.flexdata.spring.orm.core.util.FieldUtils;
import io.flexdata.spring.orm.core.util.SFunction;
import io.flexdata.spring.orm.criteria.impl.*;

import java.util.Collection;

/**
 * 类型安全的条件构建器
 * 文件位置: src/main/java/com/example/orm/criteria/TypeSafeCriteriaBuilder.java
 */
public class TypeSafeCriteriaBuilder<T> {

    public TypeSafeCriteria<T> eq(SFunction<T, ?> field, Object value) {
        return new SimpleTypeSafeCriteria<>(FieldUtils.getFieldName(field), "=", value);
    }

    public TypeSafeCriteria<T> ne(SFunction<T, ?> field, Object value) {
        return new SimpleTypeSafeCriteria<>(FieldUtils.getFieldName(field), "!=", value);
    }

    public TypeSafeCriteria<T> gt(SFunction<T, ?> field, Object value) {
        return new SimpleTypeSafeCriteria<>(FieldUtils.getFieldName(field), ">", value);
    }

    public TypeSafeCriteria<T> gte(SFunction<T, ?> field, Object value) {
        return new SimpleTypeSafeCriteria<>(FieldUtils.getFieldName(field), ">=", value);
    }

    public TypeSafeCriteria<T> lt(SFunction<T, ?> field, Object value) {
        return new SimpleTypeSafeCriteria<>(FieldUtils.getFieldName(field), "<", value);
    }

    public TypeSafeCriteria<T> lte(SFunction<T, ?> field, Object value) {
        return new SimpleTypeSafeCriteria<>(FieldUtils.getFieldName(field), "<=", value);
    }

    public TypeSafeCriteria<T> like(SFunction<T, ?> field, String pattern) {
        return new SimpleTypeSafeCriteria<>(FieldUtils.getFieldName(field), "LIKE", pattern);
    }

    public TypeSafeCriteria<T> notLike(SFunction<T, ?> field, String pattern) {
        return new SimpleTypeSafeCriteria<>(FieldUtils.getFieldName(field), "NOT LIKE", pattern);
    }

    public TypeSafeCriteria<T> in(SFunction<T, ?> field, Collection<?> values) {
        return new InTypeSafeCriteria<>(FieldUtils.getFieldName(field), values);
    }

    public TypeSafeCriteria<T> notIn(SFunction<T, ?> field, Collection<?> values) {
        return new NotInTypeSafeCriteria<>(FieldUtils.getFieldName(field), values);
    }

    public TypeSafeCriteria<T> isNull(SFunction<T, ?> field) {
        return new NullTypeSafeCriteria<>(FieldUtils.getFieldName(field), true);
    }

    public TypeSafeCriteria<T> isNotNull(SFunction<T, ?> field) {
        return new NullTypeSafeCriteria<>(FieldUtils.getFieldName(field), false);
    }

    public TypeSafeCriteria<T> between(SFunction<T, ?> field, Object start, Object end) {
        return new BetweenTypeSafeCriteria<>(FieldUtils.getFieldName(field), start, end);
    }

    public static <T> TypeSafeCriteriaBuilder<T> create() {
        return new TypeSafeCriteriaBuilder<>();
    }
}
