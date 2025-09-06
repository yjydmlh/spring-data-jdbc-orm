package com.spring.jdbc.orm;

import com.spring.jdbc.orm.core.sql.EnhancedCriteriaBuilder;
import com.spring.jdbc.orm.core.sql.complex.*;
import com.spring.jdbc.orm.core.interfaces.Criteria;
import com.spring.jdbc.orm.repository.JoinType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

/**
 * 复杂SQL功能测试
 */
public class ComplexSqlTest {
    
    @BeforeEach
    void setUp() {
        // EnhancedCriteriaBuilder is a static utility class
    }
    
    @Test
    void testComplexSelectQuery() {
        ComplexSelectQuery query = new ComplexSelectQuery();
        query.select("u.name")
             .select("u.email")
             .from("users", "u")
             .where(EnhancedCriteriaBuilder.eq("u.active", true));
        
        // 验证复杂查询构建
        assertNotNull(query.getSelectFields());
        assertFalse(query.getSelectFields().isEmpty());
        assertNotNull(query.getFromTables());
        assertFalse(query.getFromTables().isEmpty());
        assertNotNull(query.getWhereClause());
    }
    
    @Test
    void testJoinQuery() {
        JoinQuery joinQuery = new JoinQuery();
        joinQuery.addTable("users", "u")
                 .addJoin(JoinType.INNER, "orders", "o", "u.id = o.user_id")
                 .where(EnhancedCriteriaBuilder.eq("u.active", true));
        
        // 验证JOIN查询构建
        assertNotNull(joinQuery.getTables());
        assertFalse(joinQuery.getTables().isEmpty());
        assertNotNull(joinQuery.getJoins());
        assertFalse(joinQuery.getJoins().isEmpty());
        assertNotNull(joinQuery.getWhereClause());
    }
    
    @Test
    void testSubQuery() {
        ComplexSelectQuery selectQuery = new ComplexSelectQuery();
        selectQuery.select("id").from("categories").where(EnhancedCriteriaBuilder.eq("active", true));
        SubQuery subQuery = new SubQuery(selectQuery);
        String sql = subQuery.toSql();
        assertNotNull(sql);
        assertTrue(sql.contains("SELECT"));
    }
    
    @Test
    void testExistsQuery() {
        ComplexSelectQuery selectQuery = new ComplexSelectQuery();
        selectQuery.select("1").from("orders").where(EnhancedCriteriaBuilder.eq("user_id", "u.id"));
        SubQuery subQuery = new SubQuery(selectQuery);
        ExistsQuery existsQuery = new ExistsQuery(subQuery, false);
        
        String sql = existsQuery.toSql(new HashMap<>());
        assertNotNull(sql);
        assertTrue(sql.contains("EXISTS"));
        assertFalse(sql.contains("NOT EXISTS"));
    }
    
    @Test
    void testNotExistsQuery() {
        ComplexSelectQuery selectQuery = new ComplexSelectQuery();
        selectQuery.select("1").from("orders").where(EnhancedCriteriaBuilder.eq("user_id", "u.id"));
        SubQuery subQuery = new SubQuery(selectQuery);
        ExistsQuery existsQuery = new ExistsQuery(subQuery, true);
        
        String sql = existsQuery.toSql(new HashMap<>());
        assertNotNull(sql);
        assertTrue(sql.contains("NOT EXISTS"));
    }
    
    @Test
    void testAggregateQuery() {
        TableReference table = new TableReference("employees", "e");
        AggregateQuery aggregateQuery = new AggregateQuery(table);
        aggregateQuery.addAggregate(new AggregateFunction("COUNT", "*", "total"));
        aggregateQuery.groupBy("department");
        aggregateQuery.having(EnhancedCriteriaBuilder.gt("COUNT(*)", 5));
        
        // 验证聚合查询构建
        assertNotNull(aggregateQuery.getAggregateFunctions());
        assertFalse(aggregateQuery.getAggregateFunctions().isEmpty());
        assertTrue(aggregateQuery.getGroupByFields().contains("department"));
        assertNotNull(aggregateQuery.getHavingClause());
    }
    
    @Test
    void testUnionQuery() {
        List<ComplexSelectQuery> queries = new ArrayList<>();
        
        ComplexSelectQuery query1 = new ComplexSelectQuery();
        query1.select("name").from("users");
        
        ComplexSelectQuery query2 = new ComplexSelectQuery();
        query2.select("name").from("customers");
        
        queries.add(query1);
        queries.add(query2);
        
        UnionQuery unionQuery = new UnionQuery(UnionQuery.UnionType.UNION);
        for (ComplexSelectQuery query : queries) {
            unionQuery.addQuery(query);
        }
        String sql = unionQuery.toSql(new HashMap<>());
        
        assertNotNull(sql);
        assertTrue(sql.contains("UNION"));
    }
    
    @Test
    void testWindowFunctionQuery() {
        WindowFunctionQuery windowQuery = new WindowFunctionQuery("ROW_NUMBER", "salary");
        windowQuery.partitionBy("department_id")
                  .orderBy("salary DESC");
        
        String sql = windowQuery.toSql(new HashMap<>());
        assertNotNull(sql);
        assertTrue(sql.contains("ROW_NUMBER"));
        assertTrue(sql.contains("OVER"));
    }
    
    @Test
    void testCaseWhenExpression() {
        List<WhenClause> whenClauses = Arrays.asList(
            new WhenClause("salary > 50000", "'High'"),
            new WhenClause("salary > 30000", "'Medium'")
        );
        
        CaseWhenExpression caseExpr = new CaseWhenExpression(whenClauses, "'Low'", "salary_level");
        String sql = caseExpr.toSql(new HashMap<>());
        
        assertNotNull(sql);
        assertTrue(sql.contains("CASE"));
        assertTrue(sql.contains("WHEN"));
        assertTrue(sql.contains("THEN"));
        assertTrue(sql.contains("ELSE"));
        assertTrue(sql.contains("END"));
        assertTrue(sql.contains("AS salary_level"));
    }
    
    @Test
    void testAggregateFunction() {
        AggregateFunction avgSalary = new AggregateFunction("AVG", "salary", "avg_salary", false);
        String sql = avgSalary.toSql();
        
        assertNotNull(sql);
        assertEquals("AVG(salary)", sql);
        assertEquals("avg_salary", avgSalary.getAlias());
    }
    
    @Test
    void testDistinctAggregateFunction() {
        AggregateFunction countDistinct = new AggregateFunction("COUNT", "department", "dept_count", true);
        String sql = countDistinct.toSql();
        
        assertNotNull(sql);
        assertEquals("COUNT(DISTINCT department)", sql);
        assertTrue(countDistinct.isDistinct());
    }
    
    @Test
    void testTableReference() {
        // 测试普通表引用
        TableReference tableRef1 = new TableReference("users", "u");
        assertEquals("users", tableRef1.getTableName());
        assertEquals("u", tableRef1.getAlias());
        assertEquals("u", tableRef1.getEffectiveName());
        assertFalse(tableRef1.isSubQuery());
        
        // 测试子查询表引用
        ComplexSelectQuery subSelectQuery = new ComplexSelectQuery();
        subSelectQuery.select("*").from("active_users");
        SubQuery subQuery = new SubQuery(subSelectQuery);
        TableReference tableRef2 = new TableReference(subQuery, "au");
        assertTrue(tableRef2.isSubQuery());
        assertEquals("au", tableRef2.getEffectiveName());
    }
    
    @Test
    void testSelectField() {
        // 测试简单字段
        SelectField field1 = new SelectField("name");
        assertEquals(SelectField.FieldType.SIMPLE, field1.getType());
        assertEquals("name", field1.getFieldExpression());
        
        // 测试带别名的表达式字段
        SelectField field2 = new SelectField("UPPER(name)", "upper_name");
        assertEquals(SelectField.FieldType.EXPRESSION, field2.getType());
        assertEquals("upper_name", field2.getAlias());
        
        // 测试聚合函数字段
        AggregateFunction avgFunc = new AggregateFunction("AVG", "salary", "avg_salary");
        SelectField field3 = new SelectField(avgFunc);
        assertEquals(SelectField.FieldType.AGGREGATE, field3.getType());
        assertEquals(avgFunc, field3.getAggregateFunction());
    }
    
    @Test
    void testCteDefinition() {
        ComplexSelectQuery cteSelectQuery = new ComplexSelectQuery();
        cteSelectQuery.select("*").from("users").where(EnhancedCriteriaBuilder.eq("active", true));
        SubQuery cteQuery = new SubQuery(cteSelectQuery);
        CteDefinition cte = new CteDefinition("active_users", cteQuery);
        
        assertEquals("active_users", cte.getName());
        assertEquals(cteQuery, cte.getQuery());
        assertNull(cte.getColumnNames());
        
        // 测试带列名的CTE
        List<String> columns = Arrays.asList("id", "name", "email");
        CteDefinition cteWithColumns = new CteDefinition("active_users", cteQuery, columns);
        assertEquals(columns, cteWithColumns.getColumnNames());
    }
    
    @Test
    void testEnhancedCriteriaBuilderExists() {
        ComplexSelectQuery existsSelectQuery = new ComplexSelectQuery();
        existsSelectQuery.select("1").from("orders").where(EnhancedCriteriaBuilder.eq("user_id", "u.id"));
        SubQuery subQuery = new SubQuery(existsSelectQuery);
        Criteria existsCriteria = EnhancedCriteriaBuilder.exists(subQuery);
        
        assertNotNull(existsCriteria);
        assertTrue(existsCriteria.toSql().contains("EXISTS"));
        assertNotNull(existsCriteria.getParameters());
    }
}