package io.flexdata.spring.orm.core.sql.complex;

import java.util.Map;

/**
 * SQL片段接口
 */
public interface SqlFragment {
    String toSql(Map<String, Object> context);
    Map<String, Object> getParameters();
}