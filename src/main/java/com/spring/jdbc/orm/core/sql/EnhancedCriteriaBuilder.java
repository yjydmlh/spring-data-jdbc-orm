package com.spring.jdbc.orm.core.sql;

import com.spring.jdbc.orm.core.interfaces.Criteria;
import com.spring.jdbc.orm.core.sql.complex.*;

import java.util.*;

/**
 * 增强条件构建器
 * 支持复杂SQL条件：子查询、EXISTS、表别名、复杂逻辑组合等
 */
public class EnhancedCriteriaBuilder {
    
    /**
     * 创建等于条件
     */
    public static Criteria eq(String field, Object value) {
        return new SimpleCriteria(field, "=", value);
    }
    
    /**
     * 创建不等于条件
     */
    public static Criteria ne(String field, Object value) {
        return new SimpleCriteria(field, "<>", value);
    }
    
    /**
     * 创建大于条件
     */
    public static Criteria gt(String field, Object value) {
        return new SimpleCriteria(field, ">", value);
    }
    
    /**
     * 创建大于等于条件
     */
    public static Criteria gte(String field, Object value) {
        return new SimpleCriteria(field, ">=", value);
    }
    
    /**
     * 创建小于条件
     */
    public static Criteria lt(String field, Object value) {
        return new SimpleCriteria(field, "<", value);
    }
    
    /**
     * 创建小于等于条件
     */
    public static Criteria lte(String field, Object value) {
        return new SimpleCriteria(field, "<=", value);
    }
    
    /**
     * 创建LIKE条件
     */
    public static Criteria like(String field, String pattern) {
        return new SimpleCriteria(field, "LIKE", pattern);
    }
    
    /**
     * 创建NOT LIKE条件
     */
    public static Criteria notLike(String field, String pattern) {
        return new SimpleCriteria(field, "NOT LIKE", pattern);
    }
    
    /**
     * 创建IN条件
     */
    public static Criteria in(String field, Collection<?> values) {
        return new InCriteria(field, new ArrayList<>(values));
    }
    
    /**
     * 创建IN条件（数组）
     */
    public static Criteria in(String field, Object... values) {
        return new InCriteria(field, Arrays.asList(values));
    }
    
    /**
     * 创建NOT IN条件
     */
    public static Criteria notIn(String field, Collection<?> values) {
        return new NotInCriteria(field, new ArrayList<>(values));
    }
    
    /**
     * 创建NOT IN条件（数组）
     */
    public static Criteria notIn(String field, Object... values) {
        return new NotInCriteria(field, Arrays.asList(values));
    }
    
    /**
     * 创建BETWEEN条件
     */
    public static Criteria between(String field, Object start, Object end) {
        return new BetweenCriteria(field, start, end);
    }
    
    /**
     * 创建IS NULL条件
     */
    public static Criteria isNull(String field) {
        return new NullCriteria(field, true);
    }
    
    /**
     * 创建IS NOT NULL条件
     */
    public static Criteria isNotNull(String field) {
        return new NullCriteria(field, false);
    }
    
    /**
     * 创建表字段条件（带表别名）
     */
    public static Criteria tableField(String tableAlias, String field, String operator, Object value) {
        String fullField = tableAlias + "." + field;
        return new SimpleCriteria(fullField, operator, value);
    }
    
    /**
     * 创建表字段等于条件
     */
    public static Criteria tableEq(String tableAlias, String field, Object value) {
        return tableField(tableAlias, field, "=", value);
    }
    
    /**
     * 创建表字段IN条件
     */
    public static Criteria tableIn(String tableAlias, String field, Collection<?> values) {
        String fullField = tableAlias + "." + field;
        return new InCriteria(fullField, new ArrayList<>(values));
    }
    
    /**
     * 创建字段比较条件（两个字段比较）
     */
    public static Criteria fieldCompare(String field1, String operator, String field2) {
        return new FieldCompareCriteria(field1, operator, field2);
    }
    
    /**
     * 创建字段相等条件
     */
    public static Criteria fieldEq(String field1, String field2) {
        return fieldCompare(field1, "=", field2);
    }
    
    /**
     * 创建表字段比较条件
     */
    public static Criteria tableFieldCompare(String table1, String field1, String operator, String table2, String field2) {
        String fullField1 = table1 + "." + field1;
        String fullField2 = table2 + "." + field2;
        return new FieldCompareCriteria(fullField1, operator, fullField2);
    }
    
    /**
     * 创建表字段相等条件
     */
    public static Criteria tableFieldEq(String table1, String field1, String table2, String field2) {
        return tableFieldCompare(table1, field1, "=", table2, field2);
    }
    
    /**
     * 创建子查询IN条件
     */
    public static Criteria inSubQuery(String field, SubQuery subQuery) {
        return new SubQueryCriteria(field, "IN", subQuery);
    }
    
    /**
     * 创建子查询NOT IN条件
     */
    public static Criteria notInSubQuery(String field, SubQuery subQuery) {
        return new SubQueryCriteria(field, "NOT IN", subQuery);
    }
    
    /**
     * 创建EXISTS条件
     */
    public static Criteria exists(SubQuery subQuery) {
        return new ExistsCriteria(new ExistsQuery(subQuery, false));
    }
    
    /**
     * 创建NOT EXISTS条件
     */
    public static Criteria notExists(SubQuery subQuery) {
        return new ExistsCriteria(new ExistsQuery(subQuery, true));
    }
    
    /**
     * 创建子查询比较条件
     */
    public static Criteria subQueryCompare(String field, String operator, SubQuery subQuery) {
        return new SubQueryCriteria(field, operator, subQuery);
    }
    
    /**
     * 创建ANY子查询条件
     */
    public static Criteria any(String field, String operator, SubQuery subQuery) {
        return new SubQueryCriteria(field, operator + " ANY", subQuery);
    }
    
    /**
     * 创建ALL子查询条件
     */
    public static Criteria all(String field, String operator, SubQuery subQuery) {
        return new SubQueryCriteria(field, operator + " ALL", subQuery);
    }
    
    /**
     * 创建CASE WHEN条件
     */
    public static Criteria caseWhen(CaseWhenExpression caseExpression, String operator, Object value) {
        return new CaseWhenCriteria(caseExpression, operator, value);
    }
    
    /**
     * 创建原生SQL条件
     */
    public static Criteria nativeSql(String sql) {
        return new NativeCriteria(sql, new HashMap<>());
    }
    
    /**
     * 创建原生SQL条件（带参数）
     */
    public static Criteria nativeSql(String sql, Map<String, Object> parameters) {
        return new NativeCriteria(sql, parameters);
    }
    
    /**
     * 创建AND组合条件
     */
    public static Criteria and(Criteria... criteria) {
        if (criteria.length == 0) {
            return null;
        }
        if (criteria.length == 1) {
            return criteria[0];
        }
        
        Criteria result = criteria[0];
        for (int i = 1; i < criteria.length; i++) {
            result = result.and(criteria[i]);
        }
        return result;
    }
    
    /**
     * 创建OR组合条件
     */
    public static Criteria or(Criteria... criteria) {
        if (criteria.length == 0) {
            return null;
        }
        if (criteria.length == 1) {
            return criteria[0];
        }
        
        Criteria result = criteria[0];
        for (int i = 1; i < criteria.length; i++) {
            result = result.or(criteria[i]);
        }
        return result;
    }
    
    /**
     * 创建NOT条件
     */
    public static Criteria not(Criteria criteria) {
        return new NotCriteria(criteria);
    }
    
    // 内部实现类
    
    private static class SimpleCriteria implements Criteria {
        private final String field;
        private final String operator;
        private final Object value;
        private final String paramName;
        
        public SimpleCriteria(String field, String operator, Object value) {
            this.field = field;
            this.operator = operator;
            this.value = value;
            this.paramName = field.replace(".", "_") + "_" + System.nanoTime();
        }
        
        @Override
        public String toSql() {
            return field + " " + operator + " :" + paramName;
        }
        
        @Override
        public Map<String, Object> getParameters() {
            Map<String, Object> params = new HashMap<>();
            params.put(paramName, value);
            return params;
        }
        
        @Override
        public Criteria and(Criteria other) {
            return new CompositeCriteria(this, "AND", other);
        }
        
        @Override
        public Criteria or(Criteria other) {
            return new CompositeCriteria(this, "OR", other);
        }
    }
    
    private static class InCriteria implements Criteria {
        private final String field;
        private final List<Object> values;
        private final String paramName;
        
        public InCriteria(String field, List<Object> values) {
            this.field = field;
            this.values = values;
            this.paramName = field.replace(".", "_") + "_in_" + System.nanoTime();
        }
        
        @Override
        public String toSql() {
            return field + " IN (:" + paramName + ")";
        }
        
        @Override
        public Map<String, Object> getParameters() {
            Map<String, Object> params = new HashMap<>();
            params.put(paramName, values);
            return params;
        }
        
        @Override
        public Criteria and(Criteria other) {
            return new CompositeCriteria(this, "AND", other);
        }
        
        @Override
        public Criteria or(Criteria other) {
            return new CompositeCriteria(this, "OR", other);
        }
    }
    
    private static class NotInCriteria implements Criteria {
        private final String field;
        private final List<Object> values;
        private final String paramName;
        
        public NotInCriteria(String field, List<Object> values) {
            this.field = field;
            this.values = values;
            this.paramName = field.replace(".", "_") + "_not_in_" + System.nanoTime();
        }
        
        @Override
        public String toSql() {
            return field + " NOT IN (:" + paramName + ")";
        }
        
        @Override
        public Map<String, Object> getParameters() {
            Map<String, Object> params = new HashMap<>();
            params.put(paramName, values);
            return params;
        }
        
        @Override
        public Criteria and(Criteria other) {
            return new CompositeCriteria(this, "AND", other);
        }
        
        @Override
        public Criteria or(Criteria other) {
            return new CompositeCriteria(this, "OR", other);
        }
    }
    
    private static class BetweenCriteria implements Criteria {
        private final String field;
        private final Object start;
        private final Object end;
        private final String startParamName;
        private final String endParamName;
        
        public BetweenCriteria(String field, Object start, Object end) {
            this.field = field;
            this.start = start;
            this.end = end;
            long nanoTime = System.nanoTime();
            this.startParamName = field.replace(".", "_") + "_start_" + nanoTime;
            this.endParamName = field.replace(".", "_") + "_end_" + nanoTime;
        }
        
        @Override
        public String toSql() {
            return field + " BETWEEN :" + startParamName + " AND :" + endParamName;
        }
        
        @Override
        public Map<String, Object> getParameters() {
            Map<String, Object> params = new HashMap<>();
            params.put(startParamName, start);
            params.put(endParamName, end);
            return params;
        }
        
        @Override
        public Criteria and(Criteria other) {
            return new CompositeCriteria(this, "AND", other);
        }
        
        @Override
        public Criteria or(Criteria other) {
            return new CompositeCriteria(this, "OR", other);
        }
    }
    
    private static class NullCriteria implements Criteria {
        private final String field;
        private final boolean isNull;
        
        public NullCriteria(String field, boolean isNull) {
            this.field = field;
            this.isNull = isNull;
        }
        
        @Override
        public String toSql() {
            return field + (isNull ? " IS NULL" : " IS NOT NULL");
        }
        
        @Override
        public Map<String, Object> getParameters() {
            return new HashMap<>();
        }
        
        @Override
        public Criteria and(Criteria other) {
            return new CompositeCriteria(this, "AND", other);
        }
        
        @Override
        public Criteria or(Criteria other) {
            return new CompositeCriteria(this, "OR", other);
        }
    }
    
    private static class FieldCompareCriteria implements Criteria {
        private final String field1;
        private final String operator;
        private final String field2;
        
        public FieldCompareCriteria(String field1, String operator, String field2) {
            this.field1 = field1;
            this.operator = operator;
            this.field2 = field2;
        }
        
        @Override
        public String toSql() {
            return field1 + " " + operator + " " + field2;
        }
        
        @Override
        public Map<String, Object> getParameters() {
            return new HashMap<>();
        }
        
        @Override
        public Criteria and(Criteria other) {
            return new CompositeCriteria(this, "AND", other);
        }
        
        @Override
        public Criteria or(Criteria other) {
            return new CompositeCriteria(this, "OR", other);
        }
    }
    
    private static class SubQueryCriteria implements Criteria {
        private final String field;
        private final String operator;
        private final SubQuery subQuery;
        
        public SubQueryCriteria(String field, String operator, SubQuery subQuery) {
            this.field = field;
            this.operator = operator;
            this.subQuery = subQuery;
        }
        
        @Override
        public String toSql() {
            return field + " " + operator + " (" + subQuery.toSql() + ")";
        }
        
        @Override
        public Map<String, Object> getParameters() {
            return subQuery.getParameters();
        }
        
        @Override
        public Criteria and(Criteria other) {
            return new CompositeCriteria(this, "AND", other);
        }
        
        @Override
        public Criteria or(Criteria other) {
            return new CompositeCriteria(this, "OR", other);
        }
    }
    
    private static class ExistsCriteria implements Criteria {
        private final ExistsQuery existsQuery;
        
        public ExistsCriteria(ExistsQuery existsQuery) {
            this.existsQuery = existsQuery;
        }
        
        @Override
        public String toSql() {
            return existsQuery.toSql(new HashMap<>());
        }
        
        @Override
        public Map<String, Object> getParameters() {
            return existsQuery.getSubQuery().getParameters();
        }
        
        @Override
        public Criteria and(Criteria other) {
            return new CompositeCriteria(this, "AND", other);
        }
        
        @Override
        public Criteria or(Criteria other) {
            return new CompositeCriteria(this, "OR", other);
        }
    }
    
    private static class CaseWhenCriteria implements Criteria {
        private final CaseWhenExpression caseExpression;
        private final String operator;
        private final Object value;
        private final String paramName;
        
        public CaseWhenCriteria(CaseWhenExpression caseExpression, String operator, Object value) {
            this.caseExpression = caseExpression;
            this.operator = operator;
            this.value = value;
            this.paramName = "case_when_" + System.nanoTime();
        }
        
        @Override
        public String toSql() {
            return "(" + caseExpression.toSql(new HashMap<>()) + ") " + operator + " :" + paramName;
        }
        
        @Override
        public Map<String, Object> getParameters() {
            Map<String, Object> params = new HashMap<>(caseExpression.getParameters());
            params.put(paramName, value);
            return params;
        }
        
        @Override
        public Criteria and(Criteria other) {
            return new CompositeCriteria(this, "AND", other);
        }
        
        @Override
        public Criteria or(Criteria other) {
            return new CompositeCriteria(this, "OR", other);
        }
    }
    
    private static class NativeCriteria implements Criteria {
        private final String sql;
        private final Map<String, Object> parameters;
        
        public NativeCriteria(String sql, Map<String, Object> parameters) {
            this.sql = sql;
            this.parameters = parameters != null ? parameters : new HashMap<>();
        }
        
        @Override
        public String toSql() {
            return sql;
        }
        
        @Override
        public Map<String, Object> getParameters() {
            return parameters;
        }
        
        @Override
        public Criteria and(Criteria other) {
            return new CompositeCriteria(this, "AND", other);
        }
        
        @Override
        public Criteria or(Criteria other) {
            return new CompositeCriteria(this, "OR", other);
        }
    }
    
    private static class NotCriteria implements Criteria {
        private final Criteria criteria;
        
        public NotCriteria(Criteria criteria) {
            this.criteria = criteria;
        }
        
        @Override
        public String toSql() {
            return "NOT (" + criteria.toSql() + ")";
        }
        
        @Override
        public Map<String, Object> getParameters() {
            return criteria.getParameters();
        }
        
        @Override
        public Criteria and(Criteria other) {
            return new CompositeCriteria(this, "AND", other);
        }
        
        @Override
        public Criteria or(Criteria other) {
            return new CompositeCriteria(this, "OR", other);
        }
    }
    
    private static class CompositeCriteria implements Criteria {
        private final Criteria left;
        private final String operator;
        private final Criteria right;
        
        public CompositeCriteria(Criteria left, String operator, Criteria right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }
        
        @Override
        public String toSql() {
            return "(" + left.toSql() + " " + operator + " " + right.toSql() + ")";
        }
        
        @Override
        public Map<String, Object> getParameters() {
            Map<String, Object> params = new HashMap<>();
            params.putAll(left.getParameters());
            params.putAll(right.getParameters());
            return params;
        }
        
        @Override
        public Criteria and(Criteria other) {
            return new CompositeCriteria(this, "AND", other);
        }
        
        @Override
        public Criteria or(Criteria other) {
            return new CompositeCriteria(this, "OR", other);
        }
    }
}