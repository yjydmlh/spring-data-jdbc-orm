package io.flexdata.spring.orm.core.sql.advanced;

import java.util.*;

/**
 * 窗口函数构建器
 * 支持ROW_NUMBER、RANK、DENSE_RANK、LAG、LEAD等窗口函数
 */
public class WindowFunctionBuilder {
    
    private String functionName;
    private List<String> arguments;
    private List<String> partitionBy;
    private List<OrderByClause> orderBy;
    private WindowFrame frame;
    private String alias;
    
    public WindowFunctionBuilder() {
        this.arguments = new ArrayList<>();
        this.partitionBy = new ArrayList<>();
        this.orderBy = new ArrayList<>();
    }
    
    /**
     * 设置窗口函数名称
     */
    public WindowFunctionBuilder function(String functionName) {
        this.functionName = functionName;
        return this;
    }
    
    /**
     * 添加函数参数
     */
    public WindowFunctionBuilder argument(String argument) {
        this.arguments.add(argument);
        return this;
    }
    
    /**
     * 设置PARTITION BY子句
     */
    public WindowFunctionBuilder partitionBy(String... fields) {
        this.partitionBy.addAll(Arrays.asList(fields));
        return this;
    }
    
    /**
     * 设置ORDER BY子句
     */
    public WindowFunctionBuilder orderBy(String field, String direction) {
        this.orderBy.add(new OrderByClause(field, direction));
        return this;
    }
    
    /**
     * 设置ORDER BY ASC
     */
    public WindowFunctionBuilder orderByAsc(String field) {
        return orderBy(field, "ASC");
    }
    
    /**
     * 设置ORDER BY DESC
     */
    public WindowFunctionBuilder orderByDesc(String field) {
        return orderBy(field, "DESC");
    }
    
    /**
     * 设置窗口框架
     */
    public WindowFunctionBuilder frame(WindowFrame frame) {
        this.frame = frame;
        return this;
    }
    
    /**
     * 设置ROWS窗口框架
     */
    public WindowFunctionBuilder rows(String start, String end) {
        this.frame = new WindowFrame("ROWS", start, end);
        return this;
    }
    
    /**
     * 设置RANGE窗口框架
     */
    public WindowFunctionBuilder range(String start, String end) {
        this.frame = new WindowFrame("RANGE", start, end);
        return this;
    }
    
    /**
     * 设置别名
     */
    public WindowFunctionBuilder as(String alias) {
        this.alias = alias;
        return this;
    }
    
    /**
     * 构建窗口函数SQL
     */
    public String build() {
        StringBuilder sql = new StringBuilder();
        
        // 函数名和参数
        sql.append(functionName);
        if (!arguments.isEmpty()) {
            sql.append("(").append(String.join(", ", arguments)).append(")");
        } else {
            sql.append("()");
        }
        
        // OVER子句
        sql.append(" OVER (");
        
        List<String> overParts = new ArrayList<>();
        
        // PARTITION BY
        if (!partitionBy.isEmpty()) {
            overParts.add("PARTITION BY " + String.join(", ", partitionBy));
        }
        
        // ORDER BY
        if (!orderBy.isEmpty()) {
            List<String> orderParts = new ArrayList<>();
            for (OrderByClause clause : orderBy) {
                orderParts.add(clause.field + " " + clause.direction);
            }
            overParts.add("ORDER BY " + String.join(", ", orderParts));
        }
        
        // 窗口框架
        if (frame != null) {
            overParts.add(frame.toSql());
        }
        
        sql.append(String.join(" ", overParts));
        sql.append(")");
        
        // 别名
        if (alias != null && !alias.isEmpty()) {
            sql.append(" AS ").append(alias);
        }
        
        return sql.toString();
    }
    
    // ========== 静态工厂方法 ==========
    
    /**
     * ROW_NUMBER()窗口函数
     */
    public static WindowFunctionBuilder rowNumber() {
        return new WindowFunctionBuilder().function("ROW_NUMBER");
    }
    
    /**
     * RANK()窗口函数
     */
    public static WindowFunctionBuilder rank() {
        return new WindowFunctionBuilder().function("RANK");
    }
    
    /**
     * DENSE_RANK()窗口函数
     */
    public static WindowFunctionBuilder denseRank() {
        return new WindowFunctionBuilder().function("DENSE_RANK");
    }
    
    /**
     * LAG()窗口函数
     */
    public static WindowFunctionBuilder lag(String field) {
        return new WindowFunctionBuilder().function("LAG").argument(field);
    }
    
    /**
     * LAG()窗口函数（带偏移量）
     */
    public static WindowFunctionBuilder lag(String field, int offset) {
        return new WindowFunctionBuilder().function("LAG").argument(field).argument(String.valueOf(offset));
    }
    
    /**
     * LAG()窗口函数（带偏移量和默认值）
     */
    public static WindowFunctionBuilder lag(String field, int offset, String defaultValue) {
        return new WindowFunctionBuilder().function("LAG")
                .argument(field)
                .argument(String.valueOf(offset))
                .argument(defaultValue);
    }
    
    /**
     * LEAD()窗口函数
     */
    public static WindowFunctionBuilder lead(String field) {
        return new WindowFunctionBuilder().function("LEAD").argument(field);
    }
    
    /**
     * LEAD()窗口函数（带偏移量）
     */
    public static WindowFunctionBuilder lead(String field, int offset) {
        return new WindowFunctionBuilder().function("LEAD").argument(field).argument(String.valueOf(offset));
    }
    
    /**
     * LEAD()窗口函数（带偏移量和默认值）
     */
    public static WindowFunctionBuilder lead(String field, int offset, String defaultValue) {
        return new WindowFunctionBuilder().function("LEAD")
                .argument(field)
                .argument(String.valueOf(offset))
                .argument(defaultValue);
    }
    
    /**
     * FIRST_VALUE()窗口函数
     */
    public static WindowFunctionBuilder firstValue(String field) {
        return new WindowFunctionBuilder().function("FIRST_VALUE").argument(field);
    }
    
    /**
     * LAST_VALUE()窗口函数
     */
    public static WindowFunctionBuilder lastValue(String field) {
        return new WindowFunctionBuilder().function("LAST_VALUE").argument(field);
    }
    
    /**
     * NTH_VALUE()窗口函数
     */
    public static WindowFunctionBuilder nthValue(String field, int n) {
        return new WindowFunctionBuilder().function("NTH_VALUE")
                .argument(field)
                .argument(String.valueOf(n));
    }
    
    /**
     * NTILE()窗口函数
     */
    public static WindowFunctionBuilder ntile(int buckets) {
        return new WindowFunctionBuilder().function("NTILE").argument(String.valueOf(buckets));
    }
    
    /**
     * PERCENT_RANK()窗口函数
     */
    public static WindowFunctionBuilder percentRank() {
        return new WindowFunctionBuilder().function("PERCENT_RANK");
    }
    
    /**
     * CUME_DIST()窗口函数
     */
    public static WindowFunctionBuilder cumeDist() {
        return new WindowFunctionBuilder().function("CUME_DIST");
    }
    
    /**
     * 聚合函数作为窗口函数使用
     */
    public static WindowFunctionBuilder sum(String field) {
        return new WindowFunctionBuilder().function("SUM").argument(field);
    }
    
    public static WindowFunctionBuilder avg(String field) {
        return new WindowFunctionBuilder().function("AVG").argument(field);
    }
    
    public static WindowFunctionBuilder count(String field) {
        return new WindowFunctionBuilder().function("COUNT").argument(field);
    }
    
    public static WindowFunctionBuilder max(String field) {
        return new WindowFunctionBuilder().function("MAX").argument(field);
    }
    
    public static WindowFunctionBuilder min(String field) {
        return new WindowFunctionBuilder().function("MIN").argument(field);
    }
    
    // ========== 内部类 ==========
    
    /**
     * ORDER BY子句
     */
    public static class OrderByClause {
        private final String field;
        private final String direction;
        
        public OrderByClause(String field, String direction) {
            this.field = field;
            this.direction = direction;
        }
        
        public String getField() {
            return field;
        }
        
        public String getDirection() {
            return direction;
        }
    }
    
    /**
     * 窗口框架
     */
    public static class WindowFrame {
        private final String type; // ROWS 或 RANGE
        private final String start;
        private final String end;
        
        public WindowFrame(String type, String start, String end) {
            this.type = type;
            this.start = start;
            this.end = end;
        }
        
        public String toSql() {
            if (end != null) {
                return type + " BETWEEN " + start + " AND " + end;
            } else {
                return type + " " + start;
            }
        }
        
        // 常用窗口框架
        public static WindowFrame unboundedPreceding() {
            return new WindowFrame("ROWS", "UNBOUNDED PRECEDING", null);
        }
        
        public static WindowFrame currentRow() {
            return new WindowFrame("ROWS", "CURRENT ROW", null);
        }
        
        public static WindowFrame unboundedFollowing() {
            return new WindowFrame("ROWS", "UNBOUNDED FOLLOWING", null);
        }
        
        public static WindowFrame betweenUnboundedPrecedingAndCurrentRow() {
            return new WindowFrame("ROWS", "UNBOUNDED PRECEDING", "CURRENT ROW");
        }
        
        public static WindowFrame betweenCurrentRowAndUnboundedFollowing() {
            return new WindowFrame("ROWS", "CURRENT ROW", "UNBOUNDED FOLLOWING");
        }
        
        public static WindowFrame preceding(int n) {
            return new WindowFrame("ROWS", n + " PRECEDING", null);
        }
        
        public static WindowFrame following(int n) {
            return new WindowFrame("ROWS", n + " FOLLOWING", null);
        }
        
        public static WindowFrame betweenPrecedingAndFollowing(int precedingN, int followingN) {
            return new WindowFrame("ROWS", precedingN + " PRECEDING", followingN + " FOLLOWING");
        }
    }
}