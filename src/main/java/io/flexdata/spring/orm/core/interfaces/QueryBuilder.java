package io.flexdata.spring.orm.core.interfaces;

import io.flexdata.spring.orm.core.sql.SortDirection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

/**
 * 查询构建器接口
 * 文件位置: src/main/java/com/example/orm/core/interfaces/QueryBuilder.java
 */
public interface QueryBuilder<T> {
    QueryBuilder<T> select(String... fields);
    QueryBuilder<T> where(Criteria criteria);
    QueryBuilder<T> orderBy(String field, SortDirection direction);
    QueryBuilder<T> limit(int limit);
    QueryBuilder<T> offset(int offset);
    QueryBuilder<T> join(String table, String on);
    QueryBuilder<T> leftJoin(String table, String on);
    QueryBuilder<T> groupBy(String... fields);
    QueryBuilder<T> having(Criteria criteria);
    List<T> execute();
    Page<T> executePage(Pageable pageable);
}
