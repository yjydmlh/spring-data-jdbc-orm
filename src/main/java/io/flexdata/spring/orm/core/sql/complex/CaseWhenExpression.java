package io.flexdata.spring.orm.core.sql.complex;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CASE WHEN表达式
 */
public class CaseWhenExpression implements SqlFragment {
    private final List<WhenClause> whenClauses;
    private final String elseExpression;
    private final String alias;
    
    public CaseWhenExpression(List<WhenClause> whenClauses, String elseExpression, String alias) {
        this.whenClauses = whenClauses;
        this.elseExpression = elseExpression;
        this.alias = alias;
    }
    
    @Override
    public String toSql(Map<String, Object> context) {
        StringBuilder sql = new StringBuilder("CASE");
        for (WhenClause when : whenClauses) {
            sql.append(" WHEN ").append(when.getCondition())
               .append(" THEN ").append(when.getResult());
        }
        if (elseExpression != null) {
            sql.append(" ELSE ").append(elseExpression);
        }
        sql.append(" END");
        if (alias != null) {
            sql.append(" AS ").append(alias);
        }
        return sql.toString();
    }
    
    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> parameters = new HashMap<>();
        
        // 收集WHEN子句中的参数
        for (WhenClause when : whenClauses) {
            if (when.getConditionParameters() != null) {
                parameters.putAll(when.getConditionParameters());
            }
            if (when.getResultParameters() != null) {
                parameters.putAll(when.getResultParameters());
            }
        }
        
        return parameters;
    }
    
    public List<WhenClause> getWhenClauses() { return whenClauses; }
    public String getElseExpression() { return elseExpression; }
    public String getAlias() { return alias; }
}

// WhenClause moved to separate file