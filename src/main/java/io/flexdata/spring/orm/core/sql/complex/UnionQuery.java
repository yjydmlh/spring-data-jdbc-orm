package io.flexdata.spring.orm.core.sql.complex;

import io.flexdata.spring.orm.core.sql.OrderBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * UNION查询
 */
public class UnionQuery {
    private final List<ComplexSelectQuery> queries;
    private final UnionType unionType;
    
    public enum UnionType {
        UNION, UNION_ALL, INTERSECT, EXCEPT
    }
    
    public UnionQuery(UnionType unionType) {
        this.unionType = unionType;
        this.queries = new ArrayList<>();
    }
    
    public void addQuery(ComplexSelectQuery query) {
        queries.add(query);
    }
    
    public String toSql(Map<String, String> tableAliases) {
        if (queries.isEmpty()) {
            return "";
        }
        
        StringBuilder sql = new StringBuilder();
        for (int i = 0; i < queries.size(); i++) {
            if (i > 0) {
                sql.append(" ").append(unionType.name().replace("_", " ")).append(" ");
            }
            sql.append("(").append(generateQuerySql(queries.get(i))).append(")");
        }
        
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
    public List<ComplexSelectQuery> getQueries() { return queries; }
    public UnionType getUnionType() { return unionType; }
}