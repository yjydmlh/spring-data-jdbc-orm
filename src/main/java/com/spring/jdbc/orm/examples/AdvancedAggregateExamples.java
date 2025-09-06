package com.spring.jdbc.orm.examples;

import com.spring.jdbc.orm.core.sql.advanced.AdvancedAggregateBuilder;
import com.spring.jdbc.orm.core.sql.advanced.WindowFunctionBuilder;
import com.spring.jdbc.orm.core.interfaces.EnhancedQueryBuilder;
import com.spring.jdbc.orm.repository.EnhancedQueryBuilderImpl;

import java.util.List;
import java.util.Map;

/**
 * 高级聚合函数和窗口函数使用示例
 * 展示如何使用AdvancedAggregateBuilder和WindowFunctionBuilder构建复杂的分析查询
 */
public class AdvancedAggregateExamples {
    
    private EnhancedQueryBuilder queryBuilder = new EnhancedQueryBuilderImpl<>(Object.class, null, null, null);
    
    /**
     * 示例1：基础聚合统计
     * 统计订单总数、总金额、平均金额等
     */
    public void example1_BasicAggregates() {
        String sql = queryBuilder
                .select(
                        AdvancedAggregateBuilder.countAll().as("total_orders").build(),
                        AdvancedAggregateBuilder.sum("amount").as("total_amount").build(),
                        AdvancedAggregateBuilder.avg("amount").as("avg_amount").build(),
                        AdvancedAggregateBuilder.max("amount").as("max_amount").build(),
                        AdvancedAggregateBuilder.min("amount").as("min_amount").build()
                )
                .from("orders")
                .where("status = 'COMPLETED'")
                .toSql();
        
        System.out.println("基础聚合统计SQL:");
        System.out.println(sql);
        // SELECT COUNT(*) AS total_orders, SUM(amount) AS total_amount, AVG(amount) AS avg_amount, 
        //        MAX(amount) AS max_amount, MIN(amount) AS min_amount 
        // FROM orders WHERE status = 'COMPLETED'
    }
    
    /**
     * 示例2：条件聚合
     * 使用FILTER WHERE进行条件聚合
     */
    public void example2_ConditionalAggregates() {
        String sql = queryBuilder
                .select(
                        "customer_id",
                        AdvancedAggregateBuilder.countWhere("status = 'COMPLETED'").as("completed_orders").build(),
                        AdvancedAggregateBuilder.countWhere("status = 'CANCELLED'").as("cancelled_orders").build(),
                        AdvancedAggregateBuilder.sumWhere("amount", "status = 'COMPLETED'").as("completed_amount").build(),
                        AdvancedAggregateBuilder.avgWhere("amount", "status = 'COMPLETED'").as("avg_completed_amount").build()
                )
                .from("orders")
                .groupBy("customer_id")
                .toSql();
        
        System.out.println("条件聚合SQL:");
        System.out.println(sql);
        // SELECT customer_id, 
        //        COUNT(*) FILTER (WHERE status = 'COMPLETED') AS completed_orders,
        //        COUNT(*) FILTER (WHERE status = 'CANCELLED') AS cancelled_orders,
        //        SUM(amount) FILTER (WHERE status = 'COMPLETED') AS completed_amount,
        //        AVG(amount) FILTER (WHERE status = 'COMPLETED') AS avg_completed_amount
        // FROM orders GROUP BY customer_id
    }
    
    /**
     * 示例3：CASE WHEN聚合
     * 使用CASE WHEN进行复杂条件聚合
     */
    public void example3_CaseWhenAggregates() {
        String sql = queryBuilder
                .select(
                        "category",
                        AdvancedAggregateBuilder.sumCase("price > 100", "price", "0").as("high_value_sales").build(),
                        AdvancedAggregateBuilder.countCase("price > 100").as("high_value_count").build(),
                        AdvancedAggregateBuilder.buildPercentage("price > 100", "high_value_percentage")
                )
                .from("products")
                .groupBy("category")
                .toSql();
        
        System.out.println("CASE WHEN聚合SQL:");
        System.out.println(sql);
        // SELECT category,
        //        SUM(CASE WHEN price > 100 THEN price ELSE 0 END) AS high_value_sales,
        //        COUNT(CASE WHEN price > 100 THEN 1 END) AS high_value_count,
        //        (COUNT(CASE WHEN price > 100 THEN 1 END) * 100.0 / COUNT(*)) AS high_value_percentage
        // FROM products GROUP BY category
    }
    
    /**
     * 示例4：字符串聚合
     * 使用STRING_AGG进行字符串聚合
     */
    public void example4_StringAggregation() {
        String sql = queryBuilder
                .select(
                        "customer_id",
                        AdvancedAggregateBuilder.stringAgg("product_name", ", ")
                                .orderBy("order_date DESC")
                                .as("recent_products").build(),
                        AdvancedAggregateBuilder.arrayAgg("order_id")
                                .orderBy("order_date")
                                .as("order_history").build()
                )
                .from("order_items oi")
                .join("orders", "o", "oi.order_id = o.id")
                .join("products", "p", "oi.product_id = p.id")
                .groupBy("customer_id")
                .toSql();
        
        System.out.println("字符串聚合SQL:");
        System.out.println(sql);
        // SELECT customer_id,
        //        STRING_AGG(product_name, ', ' ORDER BY order_date DESC) AS recent_products,
        //        ARRAY_AGG(order_id ORDER BY order_date) AS order_history
        // FROM order_items oi 
        // JOIN orders o ON oi.order_id = o.id 
        // JOIN products p ON oi.product_id = p.id 
        // GROUP BY customer_id
    }
    
    /**
     * 示例5：窗口函数 - 排名和行号
     * 使用ROW_NUMBER、RANK、DENSE_RANK进行排名
     */
    public void example5_WindowFunctionRanking() {
        String sql = queryBuilder
                .select(
                        "customer_id",
                        "order_date",
                        "amount",
                        WindowFunctionBuilder.rowNumber()
                                .partitionBy("customer_id")
                                .orderByDesc("amount")
                                .as("row_num").build(),
                        WindowFunctionBuilder.rank()
                                .partitionBy("customer_id")
                                .orderByDesc("amount")
                                .as("rank").build(),
                        WindowFunctionBuilder.denseRank()
                                .partitionBy("customer_id")
                                .orderByDesc("amount")
                                .as("dense_rank").build()
                )
                .from("orders")
                .toSql();
        
        System.out.println("窗口函数排名SQL:");
        System.out.println(sql);
        // SELECT customer_id, order_date, amount,
        //        ROW_NUMBER() OVER (PARTITION BY customer_id ORDER BY amount DESC) AS row_num,
        //        RANK() OVER (PARTITION BY customer_id ORDER BY amount DESC) AS rank,
        //        DENSE_RANK() OVER (PARTITION BY customer_id ORDER BY amount DESC) AS dense_rank
        // FROM orders
    }
    
    /**
     * 示例6：窗口函数 - LAG和LEAD
     * 使用LAG和LEAD获取前后行数据
     */
    public void example6_WindowFunctionLagLead() {
        String sql = queryBuilder
                .select(
                        "customer_id",
                        "order_date",
                        "amount",
                        WindowFunctionBuilder.lag("amount", 1, "0")
                                .partitionBy("customer_id")
                                .orderByAsc("order_date")
                                .as("prev_amount").build(),
                        WindowFunctionBuilder.lead("amount", 1, "0")
                                .partitionBy("customer_id")
                                .orderByAsc("order_date")
                                .as("next_amount").build(),
                        "amount - " + WindowFunctionBuilder.lag("amount", 1, "0")
                                .partitionBy("customer_id")
                                .orderByAsc("order_date").build() + " AS amount_diff"
                )
                .from("orders")
                .toSql();
        
        System.out.println("LAG/LEAD窗口函数SQL:");
        System.out.println(sql);
        // SELECT customer_id, order_date, amount,
        //        LAG(amount, 1, 0) OVER (PARTITION BY customer_id ORDER BY order_date ASC) AS prev_amount,
        //        LEAD(amount, 1, 0) OVER (PARTITION BY customer_id ORDER BY order_date ASC) AS next_amount,
        //        amount - LAG(amount, 1, 0) OVER (PARTITION BY customer_id ORDER BY order_date ASC) AS amount_diff
        // FROM orders
    }
    
    /**
     * 示例7：窗口函数 - 累计聚合
     * 使用窗口框架进行累计计算
     */
    public void example7_WindowFunctionCumulative() {
        String sql = queryBuilder
                .select(
                        "customer_id",
                        "order_date",
                        "amount",
                        WindowFunctionBuilder.sum("amount")
                                .partitionBy("customer_id")
                                .orderByAsc("order_date")
                                .frame(WindowFunctionBuilder.WindowFrame.betweenUnboundedPrecedingAndCurrentRow())
                                .as("cumulative_amount").build(),
                        WindowFunctionBuilder.avg("amount")
                                .partitionBy("customer_id")
                                .orderByAsc("order_date")
                                .rows("2 PRECEDING", "CURRENT ROW")
                                .as("moving_avg_3").build(),
                        WindowFunctionBuilder.count("*")
                                .partitionBy("customer_id")
                                .orderByAsc("order_date")
                                .frame(WindowFunctionBuilder.WindowFrame.betweenUnboundedPrecedingAndCurrentRow())
                                .as("order_sequence").build()
                )
                .from("orders")
                .toSql();
        
        System.out.println("累计聚合窗口函数SQL:");
        System.out.println(sql);
        // SELECT customer_id, order_date, amount,
        //        SUM(amount) OVER (PARTITION BY customer_id ORDER BY order_date ASC ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS cumulative_amount,
        //        AVG(amount) OVER (PARTITION BY customer_id ORDER BY order_date ASC ROWS BETWEEN 2 PRECEDING AND CURRENT ROW) AS moving_avg_3,
        //        COUNT(*) OVER (PARTITION BY customer_id ORDER BY order_date ASC ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS order_sequence
        // FROM orders
    }
    
    /**
     * 示例8：百分位数和统计函数
     * 使用PERCENTILE、NTILE等统计函数
     */
    public void example8_StatisticalFunctions() {
        String sql = queryBuilder
                .select(
                        "category",
                        AdvancedAggregateBuilder.percentileCont(0.5).as("median_price").build(),
                        AdvancedAggregateBuilder.percentileCont(0.25).as("q1_price").build(),
                        AdvancedAggregateBuilder.percentileCont(0.75).as("q3_price").build(),
                        AdvancedAggregateBuilder.stddev("price").as("price_stddev").build(),
                        AdvancedAggregateBuilder.variance("price").as("price_variance").build()
                )
                .from("products")
                .groupBy("category")
                .toSql();
        
        System.out.println("统计函数SQL:");
        System.out.println(sql);
        // SELECT category,
        //        PERCENTILE_CONT(0.5) AS median_price,
        //        PERCENTILE_CONT(0.25) AS q1_price,
        //        PERCENTILE_CONT(0.75) AS q3_price,
        //        STDDEV(price) AS price_stddev,
        //        VARIANCE(price) AS price_variance
        // FROM products GROUP BY category
    }
    
    /**
     * 示例9：NTILE分桶
     * 使用NTILE将数据分成若干桶
     */
    public void example9_NtileBuckets() {
        String sql = queryBuilder
                .select(
                        "customer_id",
                        "total_spent",
                        WindowFunctionBuilder.ntile(4)
                                .orderByDesc("total_spent")
                                .as("spending_quartile").build(),
                        WindowFunctionBuilder.percentRank()
                                .orderByDesc("total_spent")
                                .as("spending_percent_rank").build(),
                        WindowFunctionBuilder.cumeDist()
                                .orderByDesc("total_spent")
                                .as("spending_cume_dist").build()
                )
                .from("("
                        + queryBuilder.select("customer_id", "SUM(amount) AS total_spent")
                                .from("orders")
                                .where("status = 'COMPLETED'")
                                .groupBy("customer_id")
                                .toSql()
                        + ") customer_spending")
                .toSql();
        
        System.out.println("NTILE分桶SQL:");
        System.out.println(sql);
        // SELECT customer_id, total_spent,
        //        NTILE(4) OVER (ORDER BY total_spent DESC) AS spending_quartile,
        //        PERCENT_RANK() OVER (ORDER BY total_spent DESC) AS spending_percent_rank,
        //        CUME_DIST() OVER (ORDER BY total_spent DESC) AS spending_cume_dist
        // FROM (
        //     SELECT customer_id, SUM(amount) AS total_spent 
        //     FROM orders 
        //     WHERE status = 'COMPLETED' 
        //     GROUP BY customer_id
        // ) customer_spending
    }
    
    /**
     * 示例10：复合分析查询
     * 结合多种聚合和窗口函数进行复杂分析
     */
    public void example10_ComplexAnalytics() {
        String sql = queryBuilder
                .select(
                        "DATE_TRUNC('month', order_date) AS month",
                        AdvancedAggregateBuilder.sum("amount").as("monthly_revenue").build(),
                        AdvancedAggregateBuilder.countDistinct("customer_id").as("unique_customers").build(),
                        WindowFunctionBuilder.sum("amount")
                                .orderByAsc("DATE_TRUNC('month', order_date)")
                                .frame(WindowFunctionBuilder.WindowFrame.betweenUnboundedPrecedingAndCurrentRow())
                                .as("cumulative_revenue").build(),
                        WindowFunctionBuilder.lag("SUM(amount)", 1)
                                .orderByAsc("DATE_TRUNC('month', order_date)")
                                .as("prev_month_revenue").build(),
                        "(SUM(amount) - " + WindowFunctionBuilder.lag("SUM(amount)", 1, "0")
                                .orderByAsc("DATE_TRUNC('month', order_date)").build() 
                                + ") / NULLIF(" + WindowFunctionBuilder.lag("SUM(amount)", 1, "1")
                                .orderByAsc("DATE_TRUNC('month', order_date)").build() 
                                + ", 0) * 100 AS growth_rate"
                )
                .from("orders")
                .where("status = 'COMPLETED'")
                .groupBy("DATE_TRUNC('month', order_date)")
                .orderBy("month")
                .toSql();
        
        System.out.println("复合分析查询SQL:");
        System.out.println(sql);
        // SELECT DATE_TRUNC('month', order_date) AS month,
        //        SUM(amount) AS monthly_revenue,
        //        COUNT(DISTINCT customer_id) AS unique_customers,
        //        SUM(amount) OVER (ORDER BY DATE_TRUNC('month', order_date) ASC ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS cumulative_revenue,
        //        LAG(SUM(amount), 1) OVER (ORDER BY DATE_TRUNC('month', order_date) ASC) AS prev_month_revenue,
        //        (SUM(amount) - LAG(SUM(amount), 1, 0) OVER (ORDER BY DATE_TRUNC('month', order_date) ASC)) / NULLIF(LAG(SUM(amount), 1, 1) OVER (ORDER BY DATE_TRUNC('month', order_date) ASC), 0) * 100 AS growth_rate
        // FROM orders 
        // WHERE status = 'COMPLETED' 
        // GROUP BY DATE_TRUNC('month', order_date) 
        // ORDER BY month
    }
    
    /**
     * 运行所有示例
     */
    public static void main(String[] args) {
        AdvancedAggregateExamples examples = new AdvancedAggregateExamples();
        
        System.out.println("=== 高级聚合函数和窗口函数示例 ===");
        System.out.println();
        
        examples.example1_BasicAggregates();
        System.out.println();
        
        examples.example2_ConditionalAggregates();
        System.out.println();
        
        examples.example3_CaseWhenAggregates();
        System.out.println();
        
        examples.example4_StringAggregation();
        System.out.println();
        
        examples.example5_WindowFunctionRanking();
        System.out.println();
        
        examples.example6_WindowFunctionLagLead();
        System.out.println();
        
        examples.example7_WindowFunctionCumulative();
        System.out.println();
        
        examples.example8_StatisticalFunctions();
        System.out.println();
        
        examples.example9_NtileBuckets();
        System.out.println();
        
        examples.example10_ComplexAnalytics();
    }
}