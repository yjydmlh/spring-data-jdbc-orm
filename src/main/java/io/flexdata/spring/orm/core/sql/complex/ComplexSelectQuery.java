package io.flexdata.spring.orm.core.sql.complex;

import io.flexdata.spring.orm.core.interfaces.Criteria;
import io.flexdata.spring.orm.core.sql.OrderBy;
import io.flexdata.spring.orm.repository.JoinType;
import io.flexdata.spring.orm.core.sql.SortDirection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 复杂SELECT查询构建器
 * 支持多表JOIN、子查询、聚合函数等复杂SQL语法
 */
public class ComplexSelectQuery {
    private List<SelectField> selectFields = new ArrayList<>();
    private List<TableReference> fromTables = new ArrayList<>();
    private List<JoinClause> joins = new ArrayList<>();
    private Criteria whereClause;
    private List<String> groupByFields = new ArrayList<>();
    private Criteria havingClause;
    private List<OrderBy> orderByFields = new ArrayList<>();
    private Integer limit;
    private Integer offset;
    private List<CteDefinition> cteDefinitions = new ArrayList<>();
    private boolean distinct = false;
    
    // 构建器方法
    public ComplexSelectQuery select(String field) {
        selectFields.add(new SelectField(field));
        return this;
    }
    
    public ComplexSelectQuery select(String field, String alias) {
        selectFields.add(new SelectField(field, alias));
        return this;
    }
    
    public ComplexSelectQuery selectSubQuery(SubQuery subQuery, String alias) {
        selectFields.add(new SelectField(subQuery, alias));
        return this;
    }
    
    public ComplexSelectQuery selectAggregate(AggregateFunction function) {
        selectFields.add(new SelectField(function));
        return this;
    }
    
    public ComplexSelectQuery addSelectField(String fieldExpression) {
        selectFields.add(new SelectField(fieldExpression));
        return this;
    }
    
    public ComplexSelectQuery from(String table) {
        fromTables.add(new TableReference(table));
        return this;
    }
    
    public ComplexSelectQuery from(String table, String alias) {
        fromTables.add(new TableReference(table, alias));
        return this;
    }
    
    public ComplexSelectQuery fromSubQuery(SubQuery subQuery, String alias) {
        fromTables.add(new TableReference(subQuery, alias));
        return this;
    }
    
    public ComplexSelectQuery join(String table, String alias, String onCondition) {
        joins.add(new JoinClause(JoinType.INNER, table, alias, onCondition));
        return this;
    }
    
    public ComplexSelectQuery leftJoin(String table, String alias, String onCondition) {
        joins.add(new JoinClause(JoinType.LEFT, table, alias, onCondition));
        return this;
    }
    
    public ComplexSelectQuery rightJoin(String table, String alias, String onCondition) {
        joins.add(new JoinClause(JoinType.RIGHT, table, alias, onCondition));
        return this;
    }
    
    public ComplexSelectQuery fullJoin(String table, String alias, String onCondition) {
        joins.add(new JoinClause(JoinType.FULL, table, alias, onCondition));
        return this;
    }
    
    public ComplexSelectQuery where(Criteria criteria) {
        this.whereClause = criteria;
        return this;
    }
    
    public ComplexSelectQuery groupBy(String... fields) {
        for (String field : fields) {
            groupByFields.add(field);
        }
        return this;
    }
    
    public ComplexSelectQuery having(Criteria criteria) {
        this.havingClause = criteria;
        return this;
    }
    
    public ComplexSelectQuery orderBy(String field, String direction) {
        orderByFields.add(new OrderBy(field, direction.equalsIgnoreCase("DESC") ? 
            SortDirection.DESC :
            SortDirection.ASC));
        return this;
    }
    
    public ComplexSelectQuery limit(int limit) {
        this.limit = limit;
        return this;
    }
    
    public ComplexSelectQuery offset(int offset) {
        this.offset = offset;
        return this;
    }
    
    public ComplexSelectQuery distinct() {
        this.distinct = true;
        return this;
    }
    
    public ComplexSelectQuery withCte(String name, SubQuery query) {
        cteDefinitions.add(new CteDefinition(name, query));
        return this;
    }
    
    // Getters
    public List<SelectField> getSelectFields() { return selectFields; }
    public List<TableReference> getFromTables() { return fromTables; }
    public List<JoinClause> getJoins() { return joins; }
    public Criteria getWhereClause() { return whereClause; }
    public List<String> getGroupByFields() { return groupByFields; }
    public Criteria getHavingClause() { return havingClause; }
    public List<OrderBy> getOrderByFields() { return orderByFields; }
    public Integer getLimit() { return limit; }
    public Integer getOffset() { return offset; }
    public List<CteDefinition> getCteDefinitions() { return cteDefinitions; }
    public boolean isDistinct() { return distinct; }
    
    /**
     * 获取所有查询参数
     */
    public Map<String, Object> getAllParameters() {
        Map<String, Object> parameters = new HashMap<>();
        
        // 收集WHERE条件参数
        if (whereClause != null) {
            parameters.putAll(whereClause.getParameters());
        }
        
        // 收集HAVING条件参数
        if (havingClause != null) {
            parameters.putAll(havingClause.getParameters());
        }
        
        // 收集JOIN条件参数
        for (JoinClause join : joins) {
            if (join.getOnConditionParameters() != null) {
                parameters.putAll(join.getOnConditionParameters());
            }
        }
        
        // 收集子查询参数
        for (TableReference table : fromTables) {
            if (table.isSubQuery() && table.getSubQuery() != null) {
                parameters.putAll(table.getSubQuery().getSelectQuery().getAllParameters());
            }
        }
        
        // 收集CTE参数
        for (CteDefinition cte : cteDefinitions) {
            parameters.putAll(cte.getQuery().getAllParameters());
        }
        
        return parameters;
    }
}