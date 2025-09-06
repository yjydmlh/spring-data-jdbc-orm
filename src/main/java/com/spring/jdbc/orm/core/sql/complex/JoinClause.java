package com.spring.jdbc.orm.core.sql.complex;

import com.spring.jdbc.orm.repository.JoinType;
import java.util.List;
import java.util.Map;

/**
 * 复杂SQL相关的数据结构定义
 */

// TableReference moved to separate file

// SelectField moved to separate file

/**
 * JOIN子句
 */
public class JoinClause {
    private final JoinType joinType;
    private final String tableName;
    private final String alias;
    private final String onCondition;
    private final SubQuery subQuery;
    private final boolean isSubQuery;
    
    public JoinClause(JoinType joinType, String tableName, String alias, String onCondition) {
        this.joinType = joinType;
        this.tableName = tableName;
        this.alias = alias;
        this.onCondition = onCondition;
        this.subQuery = null;
        this.isSubQuery = false;
    }
    
    public JoinClause(JoinType joinType, SubQuery subQuery, String alias, String onCondition) {
        this.joinType = joinType;
        this.tableName = null;
        this.alias = alias;
        this.onCondition = onCondition;
        this.subQuery = subQuery;
        this.isSubQuery = true;
    }
    
    public JoinType getJoinType() { return joinType; }
    public String getTableName() { return tableName; }
    public String getAlias() { return alias; }
    public String getOnCondition() { return onCondition; }
    public SubQuery getSubQuery() { return subQuery; }
    public boolean isSubQuery() { return isSubQuery; }
    
    public Map<String, Object> getOnConditionParameters() {
        // 简单实现，实际应该解析ON条件中的参数
        return new java.util.HashMap<>();
    }
}



// AggregateFunction moved to separate file

// CteDefinition moved to separate file

// SqlFragment moved to separate file

// CaseWhenExpression moved to separate file