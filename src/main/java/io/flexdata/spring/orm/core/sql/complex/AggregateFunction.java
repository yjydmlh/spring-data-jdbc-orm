package io.flexdata.spring.orm.core.sql.complex;

/**
 * 聚合函数
 */
public class AggregateFunction {
    private final String functionName;
    private final String field;
    private final String alias;
    private final boolean distinct;
    
    public AggregateFunction(String functionName, String field) {
        this.functionName = functionName;
        this.field = field;
        this.alias = null;
        this.distinct = false;
    }
    
    public AggregateFunction(String functionName, String field, String alias) {
        this.functionName = functionName;
        this.field = field;
        this.alias = alias;
        this.distinct = false;
    }
    
    public AggregateFunction(String functionName, String field, String alias, boolean distinct) {
        this.functionName = functionName;
        this.field = field;
        this.alias = alias;
        this.distinct = distinct;
    }
    
    public String getFunctionName() { return functionName; }
    public String getField() { return field; }
    public String getAlias() { return alias; }
    public boolean isDistinct() { return distinct; }
    
    public String toSql() {
        StringBuilder sql = new StringBuilder(functionName).append("(");
        if (distinct) {
            sql.append("DISTINCT ");
        }
        sql.append(field).append(")");
        return sql.toString();
    }
}