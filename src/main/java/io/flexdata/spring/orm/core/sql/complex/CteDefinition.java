package io.flexdata.spring.orm.core.sql.complex;

import java.util.List;

/**
 * CTE定义
 */
public class CteDefinition {
    private final String name;
    private final SubQuery query;
    private final List<String> columnNames;
    
    public CteDefinition(String name, SubQuery query) {
        this.name = name;
        this.query = query;
        this.columnNames = null;
    }
    
    public CteDefinition(String name, SubQuery query, List<String> columnNames) {
        this.name = name;
        this.query = query;
        this.columnNames = columnNames;
    }
    
    public String getName() { return name; }
    public SubQuery getQuery() { return query; }
    public List<String> getColumnNames() { return columnNames; }
}