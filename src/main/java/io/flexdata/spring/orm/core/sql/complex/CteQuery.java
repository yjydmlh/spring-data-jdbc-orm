package io.flexdata.spring.orm.core.sql.complex;

import io.flexdata.spring.orm.core.sql.OrderBy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CTE查询
 */
public class CteQuery {
    private final List<CteDefinition> cteDefinitions;
    private final ComplexSelectQuery mainQuery;
    
    public static class CteDefinition {
        private final String name;
        private final ComplexSelectQuery query;
        
        public CteDefinition(String name, ComplexSelectQuery query) {
            this.name = name;
            this.query = query;
        }
        
        public String getName() { return name; }
        public ComplexSelectQuery getQuery() { return query; }
    }
    
    public CteQuery(ComplexSelectQuery mainQuery) {
        this.mainQuery = mainQuery;
        this.cteDefinitions = new ArrayList<>();
    }
    
    public CteQuery with(String name, ComplexSelectQuery query) {
        cteDefinitions.add(new CteDefinition(name, query));
        return this;
    }
    
    public String toSql(Map<String, String> tableAliases) {
        if (cteDefinitions.isEmpty()) {
            return generateQuerySql(mainQuery);
        }
        
        StringBuilder sql = new StringBuilder();
        sql.append("WITH ");
        
        for (int i = 0; i < cteDefinitions.size(); i++) {
            if (i > 0) sql.append(", ");
            CteDefinition cte = cteDefinitions.get(i);
            sql.append(cte.getName()).append(" AS (");
            sql.append(generateQuerySql(cte.getQuery()));
            sql.append(")");
        }
        
        sql.append(" ").append(generateQuerySql(mainQuery));
        return sql.toString();
    }
    
    private String generateQuerySql(ComplexSelectQuery query) {
        StringBuilder sql = new StringBuilder("SELECT ");
        
        // SELECT字段
        if (query.getSelectFields().isEmpty()) {
            sql.append("*");
        } else {
            List<String> fieldStrings = new ArrayList<>();
            for (SelectField field : query.getSelectFields()) {
                fieldStrings.add(field.toString());
            }
            sql.append(String.join(", ", fieldStrings));
        }
        
        // FROM表
        if (!query.getFromTables().isEmpty()) {
            sql.append(" FROM ");
            for (int i = 0; i < query.getFromTables().size(); i++) {
                if (i > 0) sql.append(", ");
                TableReference table = query.getFromTables().get(i);
                sql.append(table.getTableName());
                if (table.getAlias() != null) {
                    sql.append(" ").append(table.getAlias());
                }
            }
        }
        
        // WHERE条件
        if (query.getWhereClause() != null) {
            sql.append(" WHERE ").append(query.getWhereClause().toSql());
        }
        
        // GROUP BY
        if (!query.getGroupByFields().isEmpty()) {
            sql.append(" GROUP BY ").append(String.join(", ", query.getGroupByFields()));
        }
        
        // ORDER BY
        if (!query.getOrderByFields().isEmpty()) {
            sql.append(" ORDER BY ");
            for (int i = 0; i < query.getOrderByFields().size(); i++) {
                if (i > 0) sql.append(", ");
                OrderBy order = query.getOrderByFields().get(i);
                sql.append(order.getField()).append(" ").append(order.getDirection());
            }
        }
        
        return sql.toString();
    }
    
    // Getters
    public List<CteDefinition> getCteDefinitions() { return cteDefinitions; }
    public ComplexSelectQuery getMainQuery() { return mainQuery; }
}