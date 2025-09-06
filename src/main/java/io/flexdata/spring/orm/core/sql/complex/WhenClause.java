package io.flexdata.spring.orm.core.sql.complex;

import java.util.HashMap;
import java.util.Map;

/**
 * WHEN子句
 */
public class WhenClause {
    private final String condition;
    private final String result;
    
    public WhenClause(String condition, String result) {
        this.condition = condition;
        this.result = result;
    }
    
    public String getCondition() { return condition; }
    public String getResult() { return result; }
    
    public Map<String, Object> getConditionParameters() {
        // 简单实现，实际应该解析条件中的参数
        return new HashMap<>();
    }
    
    public Map<String, Object> getResultParameters() {
        // 简单实现，实际应该解析结果中的参数
        return new HashMap<>();
    }
}