package io.flexdata.spring.orm.core.sql.complex;

/**
 * SELECT字段
 */
public class SelectField {
    private final String fieldExpression;
    private final String alias;
    private final SubQuery subQuery;
    private final AggregateFunction aggregateFunction;
    private final FieldType type;
    
    public enum FieldType {
        SIMPLE, SUBQUERY, AGGREGATE, EXPRESSION
    }
    
    public SelectField(String fieldExpression) {
        this.fieldExpression = fieldExpression;
        this.alias = null;
        this.subQuery = null;
        this.aggregateFunction = null;
        this.type = FieldType.SIMPLE;
    }
    
    public SelectField(String fieldExpression, String alias) {
        this.fieldExpression = fieldExpression;
        this.alias = alias;
        this.subQuery = null;
        this.aggregateFunction = null;
        this.type = FieldType.EXPRESSION;
    }
    
    public SelectField(SubQuery subQuery, String alias) {
        this.fieldExpression = null;
        this.alias = alias;
        this.subQuery = subQuery;
        this.aggregateFunction = null;
        this.type = FieldType.SUBQUERY;
    }
    
    public SelectField(AggregateFunction aggregateFunction) {
        this.fieldExpression = null;
        this.alias = aggregateFunction.getAlias();
        this.subQuery = null;
        this.aggregateFunction = aggregateFunction;
        this.type = FieldType.AGGREGATE;
    }
    
    public String getFieldExpression() { return fieldExpression; }
    public String getAlias() { return alias; }
    public SubQuery getSubQuery() { return subQuery; }
    public AggregateFunction getAggregateFunction() { return aggregateFunction; }
    public FieldType getType() { return type; }
}