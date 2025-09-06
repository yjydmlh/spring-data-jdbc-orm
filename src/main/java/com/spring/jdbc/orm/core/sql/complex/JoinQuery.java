package com.spring.jdbc.orm.core.sql.complex;

import com.spring.jdbc.orm.core.interfaces.Criteria;
import com.spring.jdbc.orm.repository.JoinType;

import java.util.ArrayList;
import java.util.List;

/**
 * JOIN查询
 */
public class JoinQuery {
    private final List<TableReference> tables;
    private final List<JoinClause> joins;
    private Criteria whereClause;
    
    public JoinQuery() {
        this.tables = new ArrayList<>();
        this.joins = new ArrayList<>();
    }
    
    public JoinQuery addTable(String tableName, String alias) {
        tables.add(new TableReference(tableName, alias));
        return this;
    }
    
    public JoinQuery addJoin(JoinType joinType, String tableName, String alias, String onCondition) {
        joins.add(new JoinClause(joinType, tableName, alias, onCondition));
        return this;
    }
    
    public JoinQuery where(Criteria criteria) {
        this.whereClause = criteria;
        return this;
    }
    
    public List<TableReference> getTables() { return tables; }
    public List<JoinClause> getJoins() { return joins; }
    public Criteria getWhereClause() { return whereClause; }
}