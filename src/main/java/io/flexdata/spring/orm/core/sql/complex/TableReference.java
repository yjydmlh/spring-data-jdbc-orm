package io.flexdata.spring.orm.core.sql.complex;

/**
 * 表引用
 */
public class TableReference {
    private final String tableName;
    private final String alias;
    private final SubQuery subQuery;
    private final boolean isSubQuery;
    
    public TableReference(String tableName) {
        this.tableName = tableName;
        this.alias = null;
        this.subQuery = null;
        this.isSubQuery = false;
    }
    
    public TableReference(String tableName, String alias) {
        this.tableName = tableName;
        this.alias = alias;
        this.subQuery = null;
        this.isSubQuery = false;
    }
    
    public TableReference(SubQuery subQuery, String alias) {
        this.tableName = null;
        this.alias = alias;
        this.subQuery = subQuery;
        this.isSubQuery = true;
    }
    
    public String getTableName() { return tableName; }
    public String getAlias() { return alias; }
    public SubQuery getSubQuery() { return subQuery; }
    public boolean isSubQuery() { return isSubQuery; }
    
    public String getEffectiveName() {
        return alias != null ? alias : tableName;
    }
}