package io.flexdata.spring.orm.core.sql.complex;

import io.flexdata.spring.orm.core.interfaces.Criteria;

import java.util.ArrayList;
import java.util.List;

/**
 * 聚合查询
 */
public class AggregateQuery {
    private final List<AggregateFunction> aggregateFunctions;
    private final List<String> groupByFields;
    private Criteria havingClause;
    private final TableReference fromTable;
    
    public AggregateQuery(TableReference fromTable) {
        this.fromTable = fromTable;
        this.aggregateFunctions = new ArrayList<>();
        this.groupByFields = new ArrayList<>();
    }
    
    public AggregateQuery addAggregate(AggregateFunction function) {
        aggregateFunctions.add(function);
        return this;
    }
    
    public AggregateQuery groupBy(String... fields) {
        for (String field : fields) {
            groupByFields.add(field);
        }
        return this;
    }
    
    public AggregateQuery having(Criteria criteria) {
        this.havingClause = criteria;
        return this;
    }
    
    public List<AggregateFunction> getAggregateFunctions() { return aggregateFunctions; }
    public List<String> getGroupByFields() { return groupByFields; }
    public Criteria getHavingClause() { return havingClause; }
    public TableReference getFromTable() { return fromTable; }
}