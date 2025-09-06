package com.spring.jdbc.orm.core.sql;

import com.spring.jdbc.orm.core.interfaces.Criteria;
import com.spring.jdbc.orm.core.metadata.EntityMetadata;
import com.spring.jdbc.orm.core.sql.complex.*;

import java.util.List;
import java.util.Map;

/**
 * 增强的SQL生成器接口，支持复杂SQL语法
 * 文件位置: src/main/java/com/spring/jdbc/orm/core/sql/EnhancedSqlGenerator.java
 */
public interface EnhancedSqlGenerator {
    
    /**
     * 生成复杂SELECT查询
     */
    String generateComplexSelect(ComplexSelectQuery query);
    
    /**
     * 生成多表JOIN查询
     */
    String generateJoinQuery(JoinQuery joinQuery);
    
    /**
     * 生成子查询
     */
    String generateSubQuery(SubQuery subQuery);
    
    /**
     * 生成聚合查询
     */
    String generateAggregateQuery(AggregateQuery aggregateQuery);
    
    /**
     * 生成窗口函数查询
     */
    String generateWindowFunctionQuery(WindowFunctionQuery windowQuery);
    
    /**
     * 生成UNION查询
     */
    String generateUnionQuery(UnionQuery unionQuery);
    
    /**
     * 生成CTE（公共表表达式）查询
     */
    String generateCteQuery(CteQuery cteQuery);
    
    /**
     * 生成EXISTS子查询
     */
    String generateExistsQuery(ExistsQuery existsQuery);
    
    /**
     * 生成CASE WHEN表达式
     */
    String generateCaseWhenExpression(CaseWhenExpression caseWhen);
    
    /**
     * 构建表别名映射
     */
    Map<String, String> buildTableAliasMap(List<TableReference> tables);
    
    /**
     * 解析复杂WHERE条件（支持表别名）
     */
    String parseComplexWhere(Criteria criteria, Map<String, String> aliasMap);
    
    /**
     * 生成动态SQL片段
     */
    String generateDynamicFragment(SqlFragment fragment, Map<String, Object> context);
}