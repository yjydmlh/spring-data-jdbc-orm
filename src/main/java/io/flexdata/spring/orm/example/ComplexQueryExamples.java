package io.flexdata.spring.orm.example;

import io.flexdata.spring.orm.core.interfaces.Criteria;
import io.flexdata.spring.orm.core.sql.EnhancedCriteriaBuilder;
import io.flexdata.spring.orm.core.sql.SortDirection;
import io.flexdata.spring.orm.core.interfaces.EnhancedQueryBuilder;
import io.flexdata.spring.orm.core.sql.complex.ComplexSelectQuery;
import io.flexdata.spring.orm.core.sql.complex.SubQuery;

import java.util.Arrays;
import java.util.List;

/**
 * 复杂SQL查询示例
 * 演示如何使用增强的查询构建器构建复杂SQL语句
 */
public class ComplexQueryExamples {
    
    private EnhancedQueryBuilder<Object> queryBuilder;
    
    /**
     * 示例1: 用户询问的复杂条件
     * WHERE a.id = b.id AND (a.type = b.type OR a.ref = b.ref)
     */
    public void example1_ComplexWhereCondition() {
        // 构建复杂WHERE条件
        Criteria condition = EnhancedCriteriaBuilder.and(
            // a.id = b.id
            EnhancedCriteriaBuilder.tableFieldEq("a", "id", "b", "id"),
            // (a.type = b.type OR a.ref = b.ref)
            EnhancedCriteriaBuilder.or(
                EnhancedCriteriaBuilder.tableFieldEq("a", "type", "b", "type"),
                EnhancedCriteriaBuilder.tableFieldEq("a", "ref", "b", "ref")
            )
        );
        
        // 使用查询构建器
        List<Object> results = queryBuilder
            .select("a.*", "b.*")
            .from("table_a", "a")
            .join("table_b", "b", "a.id = b.id")  // JOIN条件
            .where(condition)  // 复杂WHERE条件
            .execute();
    }
    
    /**
     * 示例2: 多表JOIN查询
     */
    public void example2_MultiTableJoin() {
        List<Object> results = queryBuilder
            .select("u.name", "p.title", "c.name as category_name")
            .from("users", "u")
            .leftJoin("posts", "p", "u.id = p.user_id")
            .leftJoin("categories", "c", "p.category_id = c.id")
            .where(EnhancedCriteriaBuilder.and(
                EnhancedCriteriaBuilder.tableEq("u", "status", "active"),
                EnhancedCriteriaBuilder.tableIn("c", "type", Arrays.asList("tech", "business"))
            ))
            .orderByAsc("u.name")
            .execute();
    }
    
    /**
     * 示例3: 子查询IN条件
     */
    public void example3_SubQueryIn() {
        // 创建子查询：SELECT user_id FROM orders WHERE total > 1000
        ComplexSelectQuery selectQuery = new ComplexSelectQuery()
            .select("user_id")
            .from("orders")
            .where(EnhancedCriteriaBuilder.gt("total", 1000));
        SubQuery subQuery = new SubQuery(selectQuery);
        
        List<Object> results = queryBuilder
            .select("*")
            .from("users")
            .whereIn("id", subQuery)
            .execute();
    }
    
    /**
     * 示例4: EXISTS子查询
     */
    public void example4_ExistsSubQuery() {
        // 查找有订单的用户
        ComplexSelectQuery existsSelectQuery = new ComplexSelectQuery()
            .select("1")
            .from("orders")
            .where(EnhancedCriteriaBuilder.fieldEq("orders.user_id", "users.id"));
        SubQuery existsQuery = new SubQuery(existsSelectQuery);
        
        List<Object> results = queryBuilder
            .select("*")
            .from("users")
            .whereExists(existsQuery)
            .execute();
    }
    
    /**
     * 示例5: 聚合查询与GROUP BY
     */
    public void example5_AggregateQuery() {
        List<Object> results = queryBuilder
            .select("department")
            .selectCount("*", "employee_count")
            .selectAvg("salary", "avg_salary")
            .selectMax("salary", "max_salary")
            .from("employees")
            .where(EnhancedCriteriaBuilder.eq("status", "active"))
            .groupBy("department")
            .having(EnhancedCriteriaBuilder.gt("COUNT(*)", 5))
            .orderByDesc("avg_salary")
            .execute();
    }
    
    /**
     * 示例6: CTE (Common Table Expression)
     */
    public void example6_CteQuery() {
        // 创建CTE子查询
        ComplexSelectQuery cteSelectQuery = new ComplexSelectQuery()
            .select("department", "AVG(salary) as avg_salary")
            .from("employees")
            .groupBy("department");
        SubQuery cteQuery = new SubQuery(cteSelectQuery);
        
        List<Object> results = queryBuilder
            .withCte("dept_avg", cteQuery)
            .select("e.name", "e.salary", "da.avg_salary")
            .from("employees", "e")
            .join("dept_avg", "da", "e.department = da.department")
            .where(EnhancedCriteriaBuilder.fieldCompare("e.salary", ">", "da.avg_salary"))
            .execute();
    }
    
    /**
     * 示例7: UNION查询
     */
    public void example7_UnionQuery() {
        // 第一个查询：活跃用户
        EnhancedQueryBuilder<Object> query1 = queryBuilder
            .select("id", "name", "'active' as status")
            .from("users")
            .where(EnhancedCriteriaBuilder.eq("status", "active"));
        
        // 第二个查询：VIP用户
        EnhancedQueryBuilder<Object> query2 = queryBuilder
            .select("id", "name", "'vip' as status")
            .from("users")
            .where(EnhancedCriteriaBuilder.eq("vip_level", "gold"));
        
        // UNION查询
        List<Object> results = query1
            .union(query2)
            .orderBy("name")
            .execute();
    }
    
    /**
     * 示例8: 复杂的嵌套条件
     */
    public void example8_ComplexNestedConditions() {
        Criteria complexCondition = EnhancedCriteriaBuilder.and(
            // 基本条件
            EnhancedCriteriaBuilder.eq("status", "active"),
            
            // 复杂OR条件组
            EnhancedCriteriaBuilder.or(
                // 条件组1: 高级用户
                EnhancedCriteriaBuilder.and(
                    EnhancedCriteriaBuilder.eq("user_type", "premium"),
                    EnhancedCriteriaBuilder.gte("score", 80)
                ),
                
                // 条件组2: 活跃用户
                EnhancedCriteriaBuilder.and(
                    EnhancedCriteriaBuilder.eq("user_type", "regular"),
                    EnhancedCriteriaBuilder.gte("login_count", 30),
                    EnhancedCriteriaBuilder.isNotNull("last_login_date")
                ),
                
                // 条件组3: 特殊用户
                EnhancedCriteriaBuilder.in("special_flag", Arrays.asList("vip", "partner", "staff"))
            ),
            
            // 排除条件
            EnhancedCriteriaBuilder.not(
                EnhancedCriteriaBuilder.or(
                    EnhancedCriteriaBuilder.eq("banned", true),
                    EnhancedCriteriaBuilder.like("email", "%@spam.com")
                )
            )
        );
        
        List<Object> results = queryBuilder
            .select("*")
            .from("users")
            .where(complexCondition)
            .execute();
    }
    
    /**
     * 示例9: 分页查询
     */
    public void example9_PaginationQuery() {
        List<Object> results = queryBuilder
            .select("*")
            .from("products")
            .where(EnhancedCriteriaBuilder.and(
                EnhancedCriteriaBuilder.eq("status", "available"),
                EnhancedCriteriaBuilder.between("price", 100, 1000)
            ))
            .orderByDesc("created_date")
            .page(0, 20)  // 第1页，每页20条
            .execute();
    }
    
    /**
     * 示例10: 动态查询条件
     */
    public void example10_DynamicQuery(String name, String email, Integer minAge, Integer maxAge) {
        EnhancedQueryBuilder<Object> query = queryBuilder
            .select("*")
            .from("users");
        
        // 动态添加条件
        query.when(name != null && !name.isEmpty(), 
               q -> q.where(EnhancedCriteriaBuilder.like("name", "%" + name + "%")));
        
        query.when(email != null && !email.isEmpty(),
               q -> q.and(EnhancedCriteriaBuilder.eq("email", email)));
        
        query.when(minAge != null,
               q -> q.and(EnhancedCriteriaBuilder.gte("age", minAge)));
        
        query.when(maxAge != null,
               q -> q.and(EnhancedCriteriaBuilder.lte("age", maxAge)));
        
        List<Object> results = query.execute();
    }
    
    /**
     * 示例11: 原生SQL混合使用
     */
    public void example11_NativeSqlMixed() {
        List<Object> results = queryBuilder
            .select("u.*", "profile_score(u.id) as score")
            .from("users", "u")
            .where(EnhancedCriteriaBuilder.and(
                EnhancedCriteriaBuilder.eq("u.status", "active"),
                // 使用原生SQL条件
                EnhancedCriteriaBuilder.nativeSql("DATEDIFF(NOW(), u.last_login) <= 30")
            ))
            .orderBy("score", SortDirection.DESC)
            .execute();
    }
    
    /**
     * 获取SQL和参数（用于调试）
     */
    public void debugQuery() {
        EnhancedQueryBuilder<Object> query = queryBuilder
            .select("*")
            .from("users")
            .where(EnhancedCriteriaBuilder.eq("status", "active"));
        
        String sql = query.toSql();
        System.out.println("Generated SQL: " + sql);
        System.out.println("Parameters: " + query.getParameters());
    }
}