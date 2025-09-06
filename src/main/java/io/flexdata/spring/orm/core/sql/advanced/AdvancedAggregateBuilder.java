package io.flexdata.spring.orm.core.sql.advanced;

import java.util.*;

/**
 * 高级聚合函数构建器
 * 支持复杂的聚合操作，包括条件聚合、分组聚合、嵌套聚合等
 */
public class AdvancedAggregateBuilder {
    
    private String functionName;
    private List<String> arguments;
    private String condition; // FILTER (WHERE condition)
    private boolean distinct;
    private String alias;
    private List<String> orderBy; // 用于某些聚合函数如STRING_AGG
    private String separator; // 用于STRING_AGG等函数
    
    public AdvancedAggregateBuilder() {
        this.arguments = new ArrayList<>();
        this.orderBy = new ArrayList<>();
    }
    
    /**
     * 设置聚合函数名称
     */
    public AdvancedAggregateBuilder function(String functionName) {
        this.functionName = functionName;
        return this;
    }
    
    /**
     * 添加函数参数
     */
    public AdvancedAggregateBuilder argument(String argument) {
        this.arguments.add(argument);
        return this;
    }
    
    /**
     * 设置DISTINCT
     */
    public AdvancedAggregateBuilder distinct() {
        this.distinct = true;
        return this;
    }
    
    /**
     * 设置条件过滤 (FILTER WHERE)
     */
    public AdvancedAggregateBuilder filter(String condition) {
        this.condition = condition;
        return this;
    }
    
    /**
     * 设置ORDER BY（用于STRING_AGG等函数）
     */
    public AdvancedAggregateBuilder orderBy(String... fields) {
        this.orderBy.addAll(Arrays.asList(fields));
        return this;
    }
    
    /**
     * 设置分隔符（用于STRING_AGG等函数）
     */
    public AdvancedAggregateBuilder separator(String separator) {
        this.separator = separator;
        return this;
    }
    
    /**
     * 设置别名
     */
    public AdvancedAggregateBuilder as(String alias) {
        this.alias = alias;
        return this;
    }
    
    /**
     * 构建聚合函数SQL
     */
    public String build() {
        StringBuilder sql = new StringBuilder();
        
        // 函数名
        sql.append(functionName).append("(");
        
        // DISTINCT
        if (distinct) {
            sql.append("DISTINCT ");
        }
        
        // 参数
        if (!arguments.isEmpty()) {
            sql.append(String.join(", ", arguments));
        }
        
        // 分隔符（用于STRING_AGG）
        if (separator != null) {
            sql.append(", '").append(separator).append("'");
        }
        
        // ORDER BY（在函数内部，用于STRING_AGG等）
        if (!orderBy.isEmpty()) {
            sql.append(" ORDER BY ").append(String.join(", ", orderBy));
        }
        
        sql.append(")");
        
        // FILTER WHERE
        if (condition != null && !condition.isEmpty()) {
            sql.append(" FILTER (WHERE ").append(condition).append(")");
        }
        
        // 别名
        if (alias != null && !alias.isEmpty()) {
            sql.append(" AS ").append(alias);
        }
        
        return sql.toString();
    }
    
    // ========== 静态工厂方法 - 基础聚合函数 ==========
    
    /**
     * COUNT聚合函数
     */
    public static AdvancedAggregateBuilder count(String field) {
        return new AdvancedAggregateBuilder().function("COUNT").argument(field);
    }
    
    /**
     * COUNT(*)
     */
    public static AdvancedAggregateBuilder countAll() {
        return new AdvancedAggregateBuilder().function("COUNT").argument("*");
    }
    
    /**
     * COUNT DISTINCT
     */
    public static AdvancedAggregateBuilder countDistinct(String field) {
        return new AdvancedAggregateBuilder().function("COUNT").argument(field).distinct();
    }
    
    /**
     * SUM聚合函数
     */
    public static AdvancedAggregateBuilder sum(String field) {
        return new AdvancedAggregateBuilder().function("SUM").argument(field);
    }
    
    /**
     * AVG聚合函数
     */
    public static AdvancedAggregateBuilder avg(String field) {
        return new AdvancedAggregateBuilder().function("AVG").argument(field);
    }
    
    /**
     * MAX聚合函数
     */
    public static AdvancedAggregateBuilder max(String field) {
        return new AdvancedAggregateBuilder().function("MAX").argument(field);
    }
    
    /**
     * MIN聚合函数
     */
    public static AdvancedAggregateBuilder min(String field) {
        return new AdvancedAggregateBuilder().function("MIN").argument(field);
    }
    
    // ========== 静态工厂方法 - 高级聚合函数 ==========
    
    /**
     * STRING_AGG聚合函数（字符串聚合）
     */
    public static AdvancedAggregateBuilder stringAgg(String field, String separator) {
        return new AdvancedAggregateBuilder().function("STRING_AGG")
                .argument(field)
                .separator(separator);
    }
    
    /**
     * GROUP_CONCAT聚合函数（MySQL）
     */
    public static AdvancedAggregateBuilder groupConcat(String field, String separator) {
        return new AdvancedAggregateBuilder().function("GROUP_CONCAT")
                .argument(field)
                .separator(separator);
    }
    
    /**
     * ARRAY_AGG聚合函数（数组聚合）
     */
    public static AdvancedAggregateBuilder arrayAgg(String field) {
        return new AdvancedAggregateBuilder().function("ARRAY_AGG").argument(field);
    }
    
    /**
     * JSON_AGG聚合函数（JSON聚合）
     */
    public static AdvancedAggregateBuilder jsonAgg(String field) {
        return new AdvancedAggregateBuilder().function("JSON_AGG").argument(field);
    }
    
    /**
     * JSON_OBJECT_AGG聚合函数（JSON对象聚合）
     */
    public static AdvancedAggregateBuilder jsonObjectAgg(String keyField, String valueField) {
        return new AdvancedAggregateBuilder().function("JSON_OBJECT_AGG")
                .argument(keyField)
                .argument(valueField);
    }
    
    /**
     * STDDEV聚合函数（标准差）
     */
    public static AdvancedAggregateBuilder stddev(String field) {
        return new AdvancedAggregateBuilder().function("STDDEV").argument(field);
    }
    
    /**
     * VARIANCE聚合函数（方差）
     */
    public static AdvancedAggregateBuilder variance(String field) {
        return new AdvancedAggregateBuilder().function("VARIANCE").argument(field);
    }
    
    /**
     * CORR聚合函数（相关系数）
     */
    public static AdvancedAggregateBuilder corr(String field1, String field2) {
        return new AdvancedAggregateBuilder().function("CORR")
                .argument(field1)
                .argument(field2);
    }
    
    /**
     * COVAR_POP聚合函数（总体协方差）
     */
    public static AdvancedAggregateBuilder covarPop(String field1, String field2) {
        return new AdvancedAggregateBuilder().function("COVAR_POP")
                .argument(field1)
                .argument(field2);
    }
    
    /**
     * COVAR_SAMP聚合函数（样本协方差）
     */
    public static AdvancedAggregateBuilder covarSamp(String field1, String field2) {
        return new AdvancedAggregateBuilder().function("COVAR_SAMP")
                .argument(field1)
                .argument(field2);
    }
    
    /**
     * PERCENTILE_CONT聚合函数（连续百分位数）
     */
    public static AdvancedAggregateBuilder percentileCont(double percentile) {
        return new AdvancedAggregateBuilder().function("PERCENTILE_CONT")
                .argument(String.valueOf(percentile));
    }
    
    /**
     * PERCENTILE_DISC聚合函数（离散百分位数）
     */
    public static AdvancedAggregateBuilder percentileDisc(double percentile) {
        return new AdvancedAggregateBuilder().function("PERCENTILE_DISC")
                .argument(String.valueOf(percentile));
    }
    
    /**
     * MODE聚合函数（众数）
     */
    public static AdvancedAggregateBuilder mode() {
        return new AdvancedAggregateBuilder().function("MODE");
    }
    
    /**
     * BOOL_AND聚合函数（逻辑与）
     */
    public static AdvancedAggregateBuilder boolAnd(String condition) {
        return new AdvancedAggregateBuilder().function("BOOL_AND").argument(condition);
    }
    
    /**
     * BOOL_OR聚合函数（逻辑或）
     */
    public static AdvancedAggregateBuilder boolOr(String condition) {
        return new AdvancedAggregateBuilder().function("BOOL_OR").argument(condition);
    }
    
    /**
     * BIT_AND聚合函数（位与）
     */
    public static AdvancedAggregateBuilder bitAnd(String field) {
        return new AdvancedAggregateBuilder().function("BIT_AND").argument(field);
    }
    
    /**
     * BIT_OR聚合函数（位或）
     */
    public static AdvancedAggregateBuilder bitOr(String field) {
        return new AdvancedAggregateBuilder().function("BIT_OR").argument(field);
    }
    
    // ========== 条件聚合的便捷方法 ==========
    
    /**
     * 条件计数
     */
    public static AdvancedAggregateBuilder countWhere(String condition) {
        return countAll().filter(condition);
    }
    
    /**
     * 条件求和
     */
    public static AdvancedAggregateBuilder sumWhere(String field, String condition) {
        return sum(field).filter(condition);
    }
    
    /**
     * 条件平均值
     */
    public static AdvancedAggregateBuilder avgWhere(String field, String condition) {
        return avg(field).filter(condition);
    }
    
    /**
     * 条件最大值
     */
    public static AdvancedAggregateBuilder maxWhere(String field, String condition) {
        return max(field).filter(condition);
    }
    
    /**
     * 条件最小值
     */
    public static AdvancedAggregateBuilder minWhere(String field, String condition) {
        return min(field).filter(condition);
    }
    
    // ========== 复合聚合函数 ==========
    
    /**
     * 创建自定义聚合函数
     */
    public static AdvancedAggregateBuilder custom(String functionName, String... arguments) {
        AdvancedAggregateBuilder builder = new AdvancedAggregateBuilder().function(functionName);
        for (String arg : arguments) {
            builder.argument(arg);
        }
        return builder;
    }
    
    /**
     * CASE WHEN聚合
     * 例如：SUM(CASE WHEN condition THEN value ELSE 0 END)
     */
    public static AdvancedAggregateBuilder sumCase(String condition, String thenValue, String elseValue) {
        String caseExpr = "CASE WHEN " + condition + " THEN " + thenValue + " ELSE " + elseValue + " END";
        return sum(caseExpr);
    }
    
    /**
     * COUNT CASE WHEN
     * 例如：COUNT(CASE WHEN condition THEN 1 END)
     */
    public static AdvancedAggregateBuilder countCase(String condition) {
        String caseExpr = "CASE WHEN " + condition + " THEN 1 END";
        return count(caseExpr);
    }
    
    /**
     * 百分比计算
     * 例如：COUNT(CASE WHEN condition THEN 1 END) * 100.0 / COUNT(*)
     */
    public static String buildPercentage(String condition, String alias) {
        return "(" + countCase(condition).build() + " * 100.0 / " + countAll().build() + ") AS " + alias;
    }
}