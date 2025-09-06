package com.spring.jdbc.orm.core.sql.complex;

import java.util.Map;

/**
 * EXISTS查询
 */
public class ExistsQuery {
    private final SubQuery subQuery;
    private final boolean notExists;
    
    public ExistsQuery(SubQuery subQuery, boolean notExists) {
        this.subQuery = subQuery;
        this.notExists = notExists;
    }
    
    public String toSql(Map<String, String> tableAliases) {
        StringBuilder sql = new StringBuilder();
        if (notExists) {
            sql.append("NOT ");
        }
        sql.append("EXISTS (");
        sql.append(subQuery.toSql());
        sql.append(")");
        return sql.toString();
    }
    
    // Getters
    public SubQuery getSubQuery() { return subQuery; }
    public boolean isNotExists() { return notExists; }
}