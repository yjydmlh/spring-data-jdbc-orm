package io.flexdata.spring.orm.repository;

/**
 * SQL JOIN类型枚举
 */
public enum JoinType {
    INNER("INNER JOIN"),
    LEFT("LEFT JOIN"),
    RIGHT("RIGHT JOIN"),
    FULL("FULL OUTER JOIN"),
    CROSS("CROSS JOIN");
    
    private final String sql;
    
    JoinType(String sql) {
        this.sql = sql;
    }
    
    public String getSql() {
        return sql;
    }
    
    @Override
    public String toString() {
        return sql;
    }
}