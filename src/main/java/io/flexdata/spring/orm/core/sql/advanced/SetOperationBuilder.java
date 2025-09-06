package io.flexdata.spring.orm.core.sql.advanced;

import java.util.*;

/**
 * SQL集合操作构建器
 * 支持UNION、INTERSECT、EXCEPT等集合操作
 */
public class SetOperationBuilder {
    
    private List<SetOperation> operations;
    private List<String> orderBy;
    private Integer limit;
    private Integer offset;
    
    public SetOperationBuilder() {
        this.operations = new ArrayList<>();
        this.orderBy = new ArrayList<>();
    }
    
    /**
     * 添加第一个查询（基础查询）
     */
    public SetOperationBuilder query(String sql) {
        if (operations.isEmpty()) {
            operations.add(new SetOperation(null, sql, false));
        }
        return this;
    }
    
    /**
     * UNION操作
     */
    public SetOperationBuilder union(String sql) {
        operations.add(new SetOperation("UNION", sql, false));
        return this;
    }
    
    /**
     * UNION ALL操作
     */
    public SetOperationBuilder unionAll(String sql) {
        operations.add(new SetOperation("UNION", sql, true));
        return this;
    }
    
    /**
     * INTERSECT操作
     */
    public SetOperationBuilder intersect(String sql) {
        operations.add(new SetOperation("INTERSECT", sql, false));
        return this;
    }
    
    /**
     * INTERSECT ALL操作
     */
    public SetOperationBuilder intersectAll(String sql) {
        operations.add(new SetOperation("INTERSECT", sql, true));
        return this;
    }
    
    /**
     * EXCEPT操作（PostgreSQL）
     */
    public SetOperationBuilder except(String sql) {
        operations.add(new SetOperation("EXCEPT", sql, false));
        return this;
    }
    
    /**
     * EXCEPT ALL操作（PostgreSQL）
     */
    public SetOperationBuilder exceptAll(String sql) {
        operations.add(new SetOperation("EXCEPT", sql, true));
        return this;
    }
    
    /**
     * MINUS操作（Oracle）
     */
    public SetOperationBuilder minus(String sql) {
        operations.add(new SetOperation("MINUS", sql, false));
        return this;
    }
    
    /**
     * 添加ORDER BY子句
     */
    public SetOperationBuilder orderBy(String... fields) {
        this.orderBy.addAll(Arrays.asList(fields));
        return this;
    }
    
    /**
     * 添加ORDER BY ASC
     */
    public SetOperationBuilder orderByAsc(String field) {
        this.orderBy.add(field + " ASC");
        return this;
    }
    
    /**
     * 添加ORDER BY DESC
     */
    public SetOperationBuilder orderByDesc(String field) {
        this.orderBy.add(field + " DESC");
        return this;
    }
    
    /**
     * 设置LIMIT
     */
    public SetOperationBuilder limit(int limit) {
        this.limit = limit;
        return this;
    }
    
    /**
     * 设置OFFSET
     */
    public SetOperationBuilder offset(int offset) {
        this.offset = offset;
        return this;
    }
    
    /**
     * 构建完整的集合操作SQL
     */
    public String build() {
        if (operations.isEmpty()) {
            throw new IllegalStateException("至少需要一个查询");
        }
        
        StringBuilder sql = new StringBuilder();
        
        // 构建集合操作
        for (int i = 0; i < operations.size(); i++) {
            SetOperation operation = operations.get(i);
            
            if (i > 0) {
                sql.append(" ").append(operation.operator);
                if (operation.all) {
                    sql.append(" ALL");
                }
                sql.append(" ");
            }
            
            // 如果查询包含ORDER BY、LIMIT等，需要用括号包围
            if (needsParentheses(operation.sql)) {
                sql.append("(").append(operation.sql).append(")");
            } else {
                sql.append(operation.sql);
            }
        }
        
        // 添加ORDER BY（应用于整个集合操作结果）
        if (!orderBy.isEmpty()) {
            sql.append(" ORDER BY ").append(String.join(", ", orderBy));
        }
        
        // 添加LIMIT
        if (limit != null) {
            sql.append(" LIMIT ").append(limit);
        }
        
        // 添加OFFSET
        if (offset != null) {
            sql.append(" OFFSET ").append(offset);
        }
        
        return sql.toString();
    }
    
    /**
     * 判断查询是否需要括号
     */
    private boolean needsParentheses(String sql) {
        String upperSql = sql.toUpperCase().trim();
        return upperSql.contains(" ORDER BY ") || 
               upperSql.contains(" LIMIT ") || 
               upperSql.contains(" OFFSET ") ||
               upperSql.contains(" UNION ") ||
               upperSql.contains(" INTERSECT ") ||
               upperSql.contains(" EXCEPT ") ||
               upperSql.contains(" MINUS ");
    }
    
    /**
     * 获取所有参数（需要从各个查询中收集）
     */
    public List<Object> getParameters() {
        // 这里需要根据实际的查询构建器实现来收集参数
        // 暂时返回空列表，实际使用时需要扩展
        return new ArrayList<>();
    }
    
    // ========== 静态工厂方法 ==========
    
    /**
     * 创建新的集合操作构建器
     */
    public static SetOperationBuilder create() {
        return new SetOperationBuilder();
    }
    
    /**
     * 从查询开始构建
     */
    public static SetOperationBuilder from(String sql) {
        return new SetOperationBuilder().query(sql);
    }
    
    // ========== 便捷方法 ==========
    
    /**
     * 简单的UNION操作
     */
    public static String union(String sql1, String sql2) {
        return create().query(sql1).union(sql2).build();
    }
    
    /**
     * 简单的UNION ALL操作
     */
    public static String unionAll(String sql1, String sql2) {
        return create().query(sql1).unionAll(sql2).build();
    }
    
    /**
     * 简单的INTERSECT操作
     */
    public static String intersect(String sql1, String sql2) {
        return create().query(sql1).intersect(sql2).build();
    }
    
    /**
     * 简单的EXCEPT操作
     */
    public static String except(String sql1, String sql2) {
        return create().query(sql1).except(sql2).build();
    }
    
    // ========== 内部类 ==========
    
    /**
     * 集合操作定义
     */
    private static class SetOperation {
        private final String operator; // UNION, INTERSECT, EXCEPT, MINUS
        private final String sql;
        private final boolean all; // 是否使用ALL
        
        public SetOperation(String operator, String sql, boolean all) {
            this.operator = operator;
            this.sql = sql;
            this.all = all;
        }
    }
    
    // ========== 高级集合操作构建器 ==========
    
    /**
     * 高级集合操作构建器
     * 支持更复杂的集合操作组合
     */
    public static class AdvancedSetOperationBuilder {
        private List<SetOperationGroup> groups;
        private List<String> orderBy;
        private Integer limit;
        private Integer offset;
        
        public AdvancedSetOperationBuilder() {
            this.groups = new ArrayList<>();
            this.orderBy = new ArrayList<>();
        }
        
        /**
         * 添加集合操作组
         */
        public AdvancedSetOperationBuilder group(SetOperationBuilder builder) {
            groups.add(new SetOperationGroup(null, builder, false));
            return this;
        }
        
        /**
         * UNION操作组
         */
        public AdvancedSetOperationBuilder unionGroup(SetOperationBuilder builder) {
            groups.add(new SetOperationGroup("UNION", builder, false));
            return this;
        }
        
        /**
         * UNION ALL操作组
         */
        public AdvancedSetOperationBuilder unionAllGroup(SetOperationBuilder builder) {
            groups.add(new SetOperationGroup("UNION", builder, true));
            return this;
        }
        
        /**
         * INTERSECT操作组
         */
        public AdvancedSetOperationBuilder intersectGroup(SetOperationBuilder builder) {
            groups.add(new SetOperationGroup("INTERSECT", builder, false));
            return this;
        }
        
        /**
         * EXCEPT操作组
         */
        public AdvancedSetOperationBuilder exceptGroup(SetOperationBuilder builder) {
            groups.add(new SetOperationGroup("EXCEPT", builder, false));
            return this;
        }
        
        /**
         * 添加ORDER BY
         */
        public AdvancedSetOperationBuilder orderBy(String... fields) {
            this.orderBy.addAll(Arrays.asList(fields));
            return this;
        }
        
        /**
         * 设置LIMIT
         */
        public AdvancedSetOperationBuilder limit(int limit) {
            this.limit = limit;
            return this;
        }
        
        /**
         * 设置OFFSET
         */
        public AdvancedSetOperationBuilder offset(int offset) {
            this.offset = offset;
            return this;
        }
        
        /**
         * 构建SQL
         */
        public String build() {
            if (groups.isEmpty()) {
                throw new IllegalStateException("至少需要一个操作组");
            }
            
            StringBuilder sql = new StringBuilder();
            
            for (int i = 0; i < groups.size(); i++) {
                SetOperationGroup group = groups.get(i);
                
                if (i > 0) {
                    sql.append(" ").append(group.operator);
                    if (group.all) {
                        sql.append(" ALL");
                    }
                    sql.append(" ");
                }
                
                sql.append("(").append(group.builder.build()).append(")");
            }
            
            // 添加ORDER BY
            if (!orderBy.isEmpty()) {
                sql.append(" ORDER BY ").append(String.join(", ", orderBy));
            }
            
            // 添加LIMIT
            if (limit != null) {
                sql.append(" LIMIT ").append(limit);
            }
            
            // 添加OFFSET
            if (offset != null) {
                sql.append(" OFFSET ").append(offset);
            }
            
            return sql.toString();
        }
        
        /**
         * 集合操作组
         */
        private static class SetOperationGroup {
            private final String operator;
            private final SetOperationBuilder builder;
            private final boolean all;
            
            public SetOperationGroup(String operator, SetOperationBuilder builder, boolean all) {
                this.operator = operator;
                this.builder = builder;
                this.all = all;
            }
        }
    }
}