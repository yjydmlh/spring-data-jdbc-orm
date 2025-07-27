package com.spring.jdbc.orm.core.interfaces;

import java.util.Map;

/**
 * 查询条件接口
 */
public interface Criteria {
    String toSql();
    Map<String, Object> getParameters();
    Criteria and(Criteria other);
    Criteria or(Criteria other);
}
