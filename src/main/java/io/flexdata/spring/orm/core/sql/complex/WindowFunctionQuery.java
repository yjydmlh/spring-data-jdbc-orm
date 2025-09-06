package io.flexdata.spring.orm.core.sql.complex;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 窗口函数查询
 */
public class WindowFunctionQuery {
    private final String functionName;
    private final String field;
    private final List<String> partitionBy;
    private final List<String> orderBy;
    
    public WindowFunctionQuery(String functionName, String field) {
        this.functionName = functionName;
        this.field = field;
        this.partitionBy = new ArrayList<>();
        this.orderBy = new ArrayList<>();
    }
    
    public WindowFunctionQuery partitionBy(String... fields) {
        for (String field : fields) {
            partitionBy.add(field);
        }
        return this;
    }
    
    public WindowFunctionQuery orderBy(String... fields) {
        for (String field : fields) {
            orderBy.add(field);
        }
        return this;
    }
    
    public String toSql(Map<String, String> tableAliases) {
        StringBuilder sql = new StringBuilder();
        sql.append(functionName).append("(");
        
        if (field != null && !field.isEmpty()) {
            sql.append(field);
        }
        
        sql.append(") OVER (");
        
        if (!partitionBy.isEmpty()) {
            sql.append("PARTITION BY ");
            for (int i = 0; i < partitionBy.size(); i++) {
                if (i > 0) sql.append(", ");
                sql.append(partitionBy.get(i));
            }
        }
        
        if (!orderBy.isEmpty()) {
            if (!partitionBy.isEmpty()) sql.append(" ");
            sql.append("ORDER BY ");
            for (int i = 0; i < orderBy.size(); i++) {
                if (i > 0) sql.append(", ");
                sql.append(orderBy.get(i));
            }
        }
        
        sql.append(")");
        return sql.toString();
    }
    
    // Getters
    public String getFunctionName() { return functionName; }
    public String getField() { return field; }
    public List<String> getPartitionBy() { return partitionBy; }
    public List<String> getOrderBy() { return orderBy; }
}