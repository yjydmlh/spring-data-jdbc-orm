package com.spring.jdbc.orm.core.interfaces;

import com.spring.jdbc.orm.core.sql.SortDirection;
import com.spring.jdbc.orm.core.util.SFunction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

/**
 * 类型安全查询构建器接口
 * 文件位置: src/main/java/com/example/orm/core/interfaces/TypeSafeQueryBuilder.java
 */
public interface TypeSafeQueryBuilder<T> {
    TypeSafeQueryBuilder<T> select(SFunction<T, ?>... fields);
    TypeSafeQueryBuilder<T> where(TypeSafeCriteria<T> criteria);
    TypeSafeQueryBuilder<T> orderBy(SFunction<T, ?> field, SortDirection direction);
    TypeSafeQueryBuilder<T> orderBy(SFunction<T, ?> field);
    TypeSafeQueryBuilder<T> orderByAsc(SFunction<T, ?> field);
    TypeSafeQueryBuilder<T> orderByDesc(SFunction<T, ?> field);
    TypeSafeQueryBuilder<T> limit(int limit);
    TypeSafeQueryBuilder<T> offset(int offset);
    TypeSafeQueryBuilder<T> groupBy(SFunction<T, ?>... fields);
    TypeSafeQueryBuilder<T> having(TypeSafeCriteria<T> criteria);
    List<T> execute();
    Page<T> executePage(Pageable pageable);
    long count();
}
