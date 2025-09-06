package io.flexdata.spring.orm.core.interfaces;

import java.util.Map;

/**
 * 类型安全的查询条件接口
 * 文件位置: src/main/java/com/example/orm/core/interfaces/TypeSafeCriteria.java
 */
public interface TypeSafeCriteria<T> {
    /**
     * 转换为SQL字符串
     */
    String toSql();

    /**
     * 获取查询参数
     */
    Map<String, Object> getParameters();

    /**
     * AND操作
     */
    TypeSafeCriteria<T> and(TypeSafeCriteria<T> other);

    /**
     * OR操作
     */
    TypeSafeCriteria<T> or(TypeSafeCriteria<T> other);
}
