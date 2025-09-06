package com.spring.jdbc.orm.examples;

import com.spring.jdbc.orm.core.sql.advanced.SetOperationBuilder;
import com.spring.jdbc.orm.core.interfaces.EnhancedQueryBuilder;
import com.spring.jdbc.orm.repository.EnhancedQueryBuilderImpl;

/**
 * 集合操作使用示例
 * 展示如何使用SetOperationBuilder构建UNION、INTERSECT、EXCEPT等集合操作
 */
public class SetOperationExamples {
    
    private EnhancedQueryBuilder queryBuilder = new EnhancedQueryBuilderImpl<>(Object.class, null, null, null);
    
    /**
     * 示例1：基础UNION操作
     * 合并活跃用户和VIP用户列表
     */
    public void example1_BasicUnion() {
        // 活跃用户查询
        String activeUsersQuery = queryBuilder
                .select("user_id", "username", "email", "'active' as user_type")
                .from("users")
                .where("last_login_date >= CURRENT_DATE - INTERVAL '30 days'")
                .toSql();
        
        // VIP用户查询
        String vipUsersQuery = queryBuilder
                .select("user_id", "username", "email", "'vip' as user_type")
                .from("users")
                .where("membership_level = 'VIP'")
                .toSql();
        
        // UNION操作（去重）
        String unionSql = SetOperationBuilder.create()
                .query(activeUsersQuery)
                .union(vipUsersQuery)
                .orderBy("username")
                .build();
        
        System.out.println("基础UNION操作SQL:");
        System.out.println(unionSql);
        // SELECT user_id, username, email, 'active' as user_type FROM users WHERE last_login_date >= CURRENT_DATE - INTERVAL '30 days'
        // UNION
        // SELECT user_id, username, email, 'vip' as user_type FROM users WHERE membership_level = 'VIP'
        // ORDER BY username
    }
    
    /**
     * 示例2：UNION ALL操作
     * 合并所有订单记录（包括重复）
     */
    public void example2_UnionAll() {
        // 在线订单
        String onlineOrdersQuery = queryBuilder
                .select("order_id", "customer_id", "amount", "'online' as channel")
                .from("online_orders")
                .where("order_date >= '2024-01-01'")
                .toSql();
        
        // 线下订单
        String offlineOrdersQuery = queryBuilder
                .select("order_id", "customer_id", "amount", "'offline' as channel")
                .from("offline_orders")
                .where("order_date >= '2024-01-01'")
                .toSql();
        
        // 电话订单
        String phoneOrdersQuery = queryBuilder
                .select("order_id", "customer_id", "amount", "'phone' as channel")
                .from("phone_orders")
                .where("order_date >= '2024-01-01'")
                .toSql();
        
        // UNION ALL操作（保留重复）
        String unionAllSql = SetOperationBuilder.create()
                .query(onlineOrdersQuery)
                .unionAll(offlineOrdersQuery)
                .unionAll(phoneOrdersQuery)
                .orderByDesc("amount")
                .limit(100)
                .build();
        
        System.out.println("UNION ALL操作SQL:");
        System.out.println(unionAllSql);
        // SELECT order_id, customer_id, amount, 'online' as channel FROM online_orders WHERE order_date >= '2024-01-01'
        // UNION ALL
        // SELECT order_id, customer_id, amount, 'offline' as channel FROM offline_orders WHERE order_date >= '2024-01-01'
        // UNION ALL
        // SELECT order_id, customer_id, amount, 'phone' as channel FROM phone_orders WHERE order_date >= '2024-01-01'
        // ORDER BY amount DESC
        // LIMIT 100
    }
    
    /**
     * 示例3：INTERSECT操作
     * 找出既是活跃用户又是VIP用户的用户
     */
    public void example3_Intersect() {
        // 活跃用户
        String activeUsersQuery = queryBuilder
                .select("user_id")
                .from("users")
                .where("last_login_date >= CURRENT_DATE - INTERVAL '30 days'")
                .toSql();
        
        // VIP用户
        String vipUsersQuery = queryBuilder
                .select("user_id")
                .from("users")
                .where("membership_level = 'VIP'")
                .toSql();
        
        // INTERSECT操作
        String intersectSql = SetOperationBuilder.create()
                .query(activeUsersQuery)
                .intersect(vipUsersQuery)
                .build();
        
        System.out.println("INTERSECT操作SQL:");
        System.out.println(intersectSql);
        // SELECT user_id FROM users WHERE last_login_date >= CURRENT_DATE - INTERVAL '30 days'
        // INTERSECT
        // SELECT user_id FROM users WHERE membership_level = 'VIP'
    }
    
    /**
     * 示例4：EXCEPT操作
     * 找出所有用户中不是VIP的用户
     */
    public void example4_Except() {
        // 所有用户
        String allUsersQuery = queryBuilder
                .select("user_id", "username", "email")
                .from("users")
                .where("status = 'active'")
                .toSql();
        
        // VIP用户
        String vipUsersQuery = queryBuilder
                .select("user_id", "username", "email")
                .from("users")
                .where("membership_level = 'VIP' AND status = 'active'")
                .toSql();
        
        // EXCEPT操作
        String exceptSql = SetOperationBuilder.create()
                .query(allUsersQuery)
                .except(vipUsersQuery)
                .orderBy("username")
                .build();
        
        System.out.println("EXCEPT操作SQL:");
        System.out.println(exceptSql);
        // SELECT user_id, username, email FROM users WHERE status = 'active'
        // EXCEPT
        // SELECT user_id, username, email FROM users WHERE membership_level = 'VIP' AND status = 'active'
        // ORDER BY username
    }
    
    /**
     * 示例5：复杂的多重集合操作
     * 组合多种集合操作
     */
    public void example5_ComplexSetOperations() {
        // 高价值客户（订单金额>1000）
        String highValueCustomersQuery = queryBuilder
                .select("customer_id", "'high_value' as segment")
                .from("orders")
                .where("amount > 1000")
                .groupBy("customer_id")
                .having("COUNT(*) >= 3")
                .toSql();
        
        // 频繁购买客户（订单数量>10）
        String frequentCustomersQuery = queryBuilder
                .select("customer_id", "'frequent' as segment")
                .from("orders")
                .groupBy("customer_id")
                .having("COUNT(*) > 10")
                .toSql();
        
        // 最近活跃客户（最近30天有订单）
        String recentCustomersQuery = queryBuilder
                .select("customer_id", "'recent' as segment")
                .from("orders")
                .where("order_date >= CURRENT_DATE - INTERVAL '30 days'")
                .groupBy("customer_id")
                .toSql();
        
        // VIP客户
        String vipCustomersQuery = queryBuilder
                .select("customer_id", "'vip' as segment")
                .from("customers")
                .where("membership_level = 'VIP'")
                .toSql();
        
        // 复杂集合操作：(高价值 UNION 频繁购买) INTERSECT (最近活跃 UNION VIP)
        String leftUnion = SetOperationBuilder.create()
                .query(highValueCustomersQuery)
                .union(frequentCustomersQuery)
                .build();
        
        String rightUnion = SetOperationBuilder.create()
                .query(recentCustomersQuery)
                .union(vipCustomersQuery)
                .build();
        
        String complexSql = SetOperationBuilder.create()
                .query("(" + leftUnion + ")")
                .intersect("(" + rightUnion + ")")
                .orderBy("customer_id")
                .build();
        
        System.out.println("复杂集合操作SQL:");
        System.out.println(complexSql);
    }
    
    /**
     * 示例6：分页的集合操作
     * 对集合操作结果进行分页
     */
    public void example6_PaginatedSetOperation() {
        // 产品搜索结果1（按名称搜索）
        String nameSearchQuery = queryBuilder
                .select("product_id", "product_name", "price", "'name_match' as match_type")
                .from("products")
                .where("product_name ILIKE '%laptop%'")
                .toSql();
        
        // 产品搜索结果2（按描述搜索）
        String descSearchQuery = queryBuilder
                .select("product_id", "product_name", "price", "'desc_match' as match_type")
                .from("products")
                .where("description ILIKE '%laptop%'")
                .toSql();
        
        // 产品搜索结果3（按标签搜索）
        String tagSearchQuery = queryBuilder
                .select("p.product_id", "p.product_name", "p.price", "'tag_match' as match_type")
                .from("products p")
                .join("product_tags", "pt", "p.product_id = pt.product_id")
                .join("tags", "t", "pt.tag_id = t.tag_id")
                .where("t.tag_name = 'laptop'")
                .toSql();
        
        // 合并搜索结果并分页
        String paginatedSearchSql = SetOperationBuilder.create()
                .query(nameSearchQuery)
                .unionAll(descSearchQuery)
                .unionAll(tagSearchQuery)
                .orderByDesc("price")
                .limit(20)
                .offset(40) // 第3页，每页20条
                .build();
        
        System.out.println("分页集合操作SQL:");
        System.out.println(paginatedSearchSql);
    }
    
    /**
     * 示例7：使用高级集合操作构建器
     * 构建更复杂的嵌套集合操作
     */
    public void example7_AdvancedSetOperations() {
        // 构建子查询1：活跃用户 UNION VIP用户
        SetOperationBuilder activeVipUsers = SetOperationBuilder.create()
                .query(queryBuilder.select("user_id").from("users").where("last_login_date >= CURRENT_DATE - INTERVAL '30 days'").toSql())
                .union(queryBuilder.select("user_id").from("users").where("membership_level = 'VIP'").toSql());
        
        // 构建子查询2：高消费用户 UNION 频繁购买用户
        SetOperationBuilder highSpendingUsers = SetOperationBuilder.create()
                .query(queryBuilder.select("customer_id as user_id").from("orders").groupBy("customer_id").having("SUM(amount) > 5000").toSql())
                .union(queryBuilder.select("customer_id as user_id").from("orders").groupBy("customer_id").having("COUNT(*) > 20").toSql());
        
        // 构建子查询3：问题用户（投诉或退款）
        SetOperationBuilder problemUsers = SetOperationBuilder.create()
                .query(queryBuilder.select("user_id").from("complaints").where("status = 'open'").toSql())
                .unionAll(queryBuilder.select("customer_id as user_id").from("refunds").where("refund_date >= CURRENT_DATE - INTERVAL '90 days'").toSql());
        
        // 使用高级构建器组合：(活跃VIP用户 INTERSECT 高消费用户) EXCEPT 问题用户
        SetOperationBuilder.AdvancedSetOperationBuilder advancedBuilder = 
                new SetOperationBuilder.AdvancedSetOperationBuilder();
        
        String advancedSql = advancedBuilder
                .group(activeVipUsers)
                .intersectGroup(highSpendingUsers)
                .exceptGroup(problemUsers)
                .orderBy("user_id")
                .limit(50)
                .build();
        
        System.out.println("高级集合操作SQL:");
        System.out.println(advancedSql);
    }
    
    /**
     * 示例8：数据仓库风格的集合操作
     * 用于数据分析和报表
     */
    public void example8_DataWarehouseSetOperations() {
        // 本月销售数据
        String currentMonthSales = queryBuilder
                .select(
                        "product_category",
                        "SUM(amount) as total_sales",
                        "COUNT(*) as order_count",
                        "'current_month' as period"
                )
                .from("orders o")
                .join("order_items", "oi", "o.order_id = oi.order_id")
                .join("products", "p", "oi.product_id = p.product_id")
                .where("DATE_TRUNC('month', o.order_date) = DATE_TRUNC('month', CURRENT_DATE)")
                .groupBy("product_category")
                .toSql();
        
        // 上月销售数据
        String lastMonthSales = queryBuilder
                .select(
                        "product_category",
                        "SUM(amount) as total_sales",
                        "COUNT(*) as order_count",
                        "'last_month' as period"
                )
                .from("orders o")
                .join("order_items", "oi", "o.order_id = oi.order_id")
                .join("products", "p", "oi.product_id = p.product_id")
                .where("DATE_TRUNC('month', o.order_date) = DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '1 month'")
                .groupBy("product_category")
                .toSql();
        
        // 去年同期销售数据
        String lastYearSales = queryBuilder
                .select(
                        "product_category",
                        "SUM(amount) as total_sales",
                        "COUNT(*) as order_count",
                        "'last_year' as period"
                )
                .from("orders o")
                .join("order_items", "oi", "o.order_id = oi.order_id")
                .join("products", "p", "oi.product_id = p.product_id")
                .where("DATE_TRUNC('month', o.order_date) = DATE_TRUNC('month', CURRENT_DATE) - INTERVAL '1 year'")
                .groupBy("product_category")
                .toSql();
        
        // 合并所有时期的数据用于对比分析
        String salesComparisonSql = SetOperationBuilder.create()
                .query(currentMonthSales)
                .unionAll(lastMonthSales)
                .unionAll(lastYearSales)
                .orderBy("product_category", "period")
                .build();
        
        System.out.println("数据仓库集合操作SQL:");
        System.out.println(salesComparisonSql);
    }
    
    /**
     * 示例9：便捷方法使用
     * 使用静态便捷方法快速构建简单集合操作
     */
    public void example9_ConvenienceMethods() {
        String query1 = "SELECT user_id FROM active_users";
        String query2 = "SELECT user_id FROM vip_users";
        String query3 = "SELECT user_id FROM premium_users";
        
        // 使用便捷方法
        String simpleUnion = SetOperationBuilder.union(query1, query2);
        String simpleUnionAll = SetOperationBuilder.unionAll(query1, query2);
        String simpleIntersect = SetOperationBuilder.intersect(query1, query2);
        String simpleExcept = SetOperationBuilder.except(query1, query2);
        
        System.out.println("便捷方法 - UNION:");
        System.out.println(simpleUnion);
        System.out.println();
        
        System.out.println("便捷方法 - UNION ALL:");
        System.out.println(simpleUnionAll);
        System.out.println();
        
        System.out.println("便捷方法 - INTERSECT:");
        System.out.println(simpleIntersect);
        System.out.println();
        
        System.out.println("便捷方法 - EXCEPT:");
        System.out.println(simpleExcept);
    }
    
    /**
     * 运行所有示例
     */
    public static void main(String[] args) {
        SetOperationExamples examples = new SetOperationExamples();
        
        System.out.println("=== SQL集合操作示例 ===");
        System.out.println();
        
        examples.example1_BasicUnion();
        System.out.println();
        
        examples.example2_UnionAll();
        System.out.println();
        
        examples.example3_Intersect();
        System.out.println();
        
        examples.example4_Except();
        System.out.println();
        
        examples.example5_ComplexSetOperations();
        System.out.println();
        
        examples.example6_PaginatedSetOperation();
        System.out.println();
        
        examples.example7_AdvancedSetOperations();
        System.out.println();
        
        examples.example8_DataWarehouseSetOperations();
        System.out.println();
        
        examples.example9_ConvenienceMethods();
    }
}