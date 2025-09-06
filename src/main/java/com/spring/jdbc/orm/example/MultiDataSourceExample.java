package com.spring.jdbc.orm.example;

import com.spring.jdbc.orm.core.datasource.DataSourceContext;
import com.spring.jdbc.orm.core.table.TableContext;
import com.spring.jdbc.orm.core.table.Table;
import com.spring.jdbc.orm.core.repository.EnhancedMultiRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 多数据源多表切换使用示例
 * 
 * 本示例展示了如何在实际业务场景中使用多数据源和动态表名切换功能
 */
@Service
public class MultiDataSourceExample {
    
    @Autowired
    private EnhancedMultiRepository<Map<String, Object>, Long> repository;
    
    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;
    
    /**
     * 示例1：基本的数据源切换
     * 在不同数据源之间切换查询用户数据
     */
    public void basicDataSourceSwitching() {
        // 从主数据库查询用户
        List<Map<String, Object>> mainUsers = DataSourceContext.executeWithDataSource("main", () -> {
            return jdbcTemplate.queryForList("SELECT * FROM users WHERE status = 'active'", new HashMap<>());
        });
        
        // 从分析数据库查询用户统计
        Map<String, Object> userStats = DataSourceContext.executeWithDataSource("analytics", () -> {
            return jdbcTemplate.queryForObject("SELECT COUNT(*) as total, AVG(age) as avg_age FROM users", new HashMap<>(), (rs, rowNum) -> {
                Map<String, Object> result = new HashMap<>();
                result.put("total", rs.getLong("total"));
                result.put("avg_age", rs.getDouble("avg_age"));
                return result;
            });
        });
        
        System.out.println("主数据库活跃用户数: " + mainUsers.size());
        System.out.println("分析数据库用户统计: " + userStats);
    }
    
    /**
     * 示例2：基本的表名切换
     * 在同一数据源的不同表之间切换
     */
    public void basicTableSwitching() {
        // 查询当前月份的用户表
        List<Map<String, Object>> currentUsers = TableContext.executeWithTableMapping("users", "users_202401", () -> {
            return jdbcTemplate.queryForList("SELECT * FROM users WHERE created_date >= '2024-01-01'", new HashMap<>());
        });
        
        // 查询上个月份的用户表
        List<Map<String, Object>> lastMonthUsers = TableContext.executeWithTableMapping("users", "users_202312", () -> {
            return jdbcTemplate.queryForList("SELECT * FROM users WHERE created_date >= '2023-12-01'", new HashMap<>());
        });
        
        System.out.println("当前月用户数: " + currentUsers.size());
        System.out.println("上月用户数: " + lastMonthUsers.size());
    }
    
    /**
     * 示例3：组合使用数据源和表名切换
     * 同时切换数据源和表名
     */
    public void combinedSwitching() {
        // 在备份数据库的历史表中查询数据
        List<Map<String, Object>> backupData = DataSourceContext.executeWithDataSource("backup", () -> {
            return TableContext.executeWithTableMapping("users", "users_history_2023", () -> {
                return jdbcTemplate.queryForList("SELECT * FROM users WHERE deleted = 1", new HashMap<>());
            });
        });
        
        System.out.println("备份数据库历史表中的删除用户数: " + backupData.size());
    }
    
    /**
     * 示例4：批量表名映射
     * 同时映射多个表名
     */
    public void batchTableMapping() {
        Map<String, String> tableMappings = new HashMap<>();
        tableMappings.put("users", "users_202401");
        tableMappings.put("orders", "orders_202401");
        tableMappings.put("products", "products_active");
        
        // 在多个表映射下执行复杂查询
        List<Map<String, Object>> monthlyReport = TableContext.executeWithTableMappings(tableMappings, () -> {
            return jdbcTemplate.queryForList(
                "SELECT u.name, COUNT(o.id) as order_count, SUM(o.amount) as total_amount " +
                "FROM users u " +
                "LEFT JOIN orders o ON u.id = o.user_id " +
                "LEFT JOIN products p ON o.product_id = p.id " +
                "WHERE u.status = 'active' AND p.status = 'available' " +
                "GROUP BY u.id, u.name", new HashMap<>()
            );
        });
        
        System.out.println("月度报告数据条数: " + monthlyReport.size());
    }
    
    /**
     * 示例5：使用注解方式进行表名切换
     * 通过@Table注解指定表名
     */
    @Table("users_vip")
    public List<Map<String, Object>> getVipUsers() {
        // 这个方法会自动使用users_vip表
        return jdbcTemplate.queryForList("SELECT * FROM users WHERE vip_level > 0", new HashMap<>());
    }
    
    @Table("orders_large")
    public List<Map<String, Object>> getLargeOrders() {
        // 这个方法会自动使用orders_large表
        return jdbcTemplate.queryForList("SELECT * FROM orders WHERE amount > 10000", new HashMap<>());
    }
    
    /**
     * 示例5.1：手动指定数据源（库名）
     * 展示各种手动指定数据源的方式
     */
    public void manualDataSourceSpecification() {
        // 方式1：executeWithDataSource - 推荐方式
        List<Map<String, Object>> analyticsUsers = DataSourceContext.executeWithDataSource("analytics", () -> {
            Map<String, Object> params = new HashMap<>();
            params.put("loginDate", "2024-01-01");
            return jdbcTemplate.queryForList("SELECT * FROM user_behavior WHERE login_date >= :loginDate", params);
        });
        
        // 方式2：手动设置和清理 - 需要谨慎使用
        // DataSourceContext.setCurrentDataSource("reporting"); // 此方法已移除
        try {
            List<Map<String, Object>> users = jdbcTemplate.queryForList("SELECT * FROM users WHERE active = 1", new HashMap<>());
            List<Map<String, Object>> orders = jdbcTemplate.queryForList("SELECT * FROM orders WHERE status = 'completed'", new HashMap<>());
            
            // 生成报表逻辑
            generateReport(users, orders);
        } finally {
            DataSourceContext.clearDataSource(); // 必须清理
        }
        
        // 方式3：多个数据源的顺序操作
        performMultiDataSourceOperations();
        
        System.out.println("手动数据源指定示例完成");
    }
    
    /**
     * 示例5.2：手动指定表名
     * 展示各种手动指定表名的方式
     */
    public void manualTableSpecification() {
        // 方式1：单表映射
        List<Map<String, Object>> currentUsers = TableContext.executeWithTableMapping("users", "users_2024_q1", () -> {
            return jdbcTemplate.queryForList("SELECT * FROM users WHERE status = 'active'", new HashMap<>());
        });
        
        // 方式2：批量表映射
        Map<String, String> tableMappings = new HashMap<>();
        tableMappings.put("users", "users_premium");
        tableMappings.put("orders", "orders_high_value");
        tableMappings.put("products", "products_featured");
        
        List<Map<String, Object>> premiumReport = TableContext.executeWithTableMappings(tableMappings, () -> {
            return jdbcTemplate.queryForList(
                "SELECT u.name, COUNT(o.id) as order_count, SUM(o.amount) as total " +
                "FROM users u " +
                "LEFT JOIN orders o ON u.id = o.user_id " +
                "LEFT JOIN products p ON o.product_id = p.id " +
                "WHERE u.premium = 1 " +
                "GROUP BY u.id, u.name", new HashMap<>()
            );
        });
        
        // 方式3：手动设置表映射
        TableContext.setTableMapping("users", "users_vip");
        TableContext.setTableMapping("orders", "orders_large");
        try {
            List<Map<String, Object>> vipData = jdbcTemplate.queryForList(
                "SELECT u.name, o.amount FROM users u JOIN orders o ON u.id = o.user_id", new HashMap<>()
            );
            processVipData(vipData);
        } finally {
            TableContext.clearTableMappings(); // 必须清理
        }
        
        System.out.println("手动表名指定示例完成");
    }
    
    /**
     * 示例5.3：手动组合指定（数据源 + 表名）
     * 同时手动指定数据源和表名的复杂场景
     */
    public void manualCombinedSpecification() {
        // 场景1：分片路由
        Long userId = 12345L;
        Map<String, Object> userInfo = manualShardRouting(userId);
        
        // 场景2：多租户数据访问
        String tenantId = "tenant_001";
        List<Map<String, Object>> tenantData = manualTenantRouting(tenantId);
        
        // 场景3：历史数据查询
        String yearMonth = "202312";
        List<Map<String, Object>> historyData = manualHistoryQuery(yearMonth);
        
        // 场景4：数据迁移
        manualDataMigration("legacy_db", "old_users", "main_db", "users_migrated");
        
        System.out.println("手动组合指定示例完成");
    }
    
    /**
     * 手动分片路由实现
     */
    private Map<String, Object> manualShardRouting(Long userId) {
        // 手动计算分片索引
        int shardIndex = (int) (userId % 4);
        String targetDataSource = "shard_" + shardIndex;  // 手动指定分片库
        String targetTable = "users_" + shardIndex;       // 手动指定分片表
        
        return DataSourceContext.executeWithDataSource(targetDataSource, () -> {
            return TableContext.executeWithTableMapping("users", targetTable, () -> {
                Map<String, Object> params = new HashMap<>();
                params.put("id", userId);
                return jdbcTemplate.queryForObject("SELECT * FROM users WHERE id = :id", params, (rs, rowNum) -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", rs.getLong("id"));
                    result.put("name", rs.getString("name"));
                    result.put("email", rs.getString("email"));
                    return result;
                });
            });
        });
    }
    
    /**
     * 手动多租户路由实现
     */
    private List<Map<String, Object>> manualTenantRouting(String tenantId) {
        String targetDataSource = "tenant_" + tenantId;   // 手动指定租户库
        String targetTable = "data_" + tenantId;          // 手动指定租户表
        
        return DataSourceContext.executeWithDataSource(targetDataSource, () -> {
            return TableContext.executeWithTableMapping("tenant_data", targetTable, () -> {
                return jdbcTemplate.queryForList("SELECT * FROM tenant_data WHERE active = 1", new HashMap<>());
            });
        });
    }
    
    /**
     * 手动历史数据查询实现
     */
    private List<Map<String, Object>> manualHistoryQuery(String yearMonth) {
        String targetTable = "orders_" + yearMonth;  // 手动指定历史表
        
        return TableContext.executeWithTableMapping("orders", targetTable, () -> {
            return jdbcTemplate.queryForList(
                "SELECT order_no, amount, created_at FROM orders WHERE status = 'completed'", new HashMap<>()
            );
        });
    }
    
    /**
     * 手动数据迁移实现
     */
    private void manualDataMigration(String sourceDb, String sourceTable, String targetDb, String targetTable) {
        // 从源库源表读取数据
        List<Map<String, Object>> sourceData = DataSourceContext.executeWithDataSource(sourceDb, () -> {
            return TableContext.executeWithTableMapping("migration_table", sourceTable, () -> {
                return jdbcTemplate.queryForList("SELECT * FROM migration_table WHERE migrated = 0 LIMIT 1000", new HashMap<>());
            });
        });
        
        if (!sourceData.isEmpty()) {
            // 写入目标库目标表
            DataSourceContext.executeWithDataSource(targetDb, () -> {
                TableContext.executeWithTableMapping("migration_table", targetTable, () -> {
                    for (Map<String, Object> row : sourceData) {
                        Map<String, Object> params = new HashMap<>();
                        params.put("id", row.get("id"));
                        params.put("data", row.get("data"));
                        params.put("created_at", row.get("created_at"));
                        jdbcTemplate.update(
                            "INSERT INTO migration_table (id, data, created_at) VALUES (:id, :data, :created_at)",
                            params
                        );
                    }
                    return null;
                });
                return null;
            });
            
            // 标记源数据为已迁移
            DataSourceContext.executeWithDataSource(sourceDb, () -> {
                return TableContext.executeWithTableMapping("migration_table", sourceTable, () -> {
                    String ids = sourceData.stream()
                        .map(row -> row.get("id").toString())
                        .reduce((a, b) -> a + "," + b)
                        .orElse("");
                    jdbcTemplate.update("UPDATE migration_table SET migrated = 1 WHERE id IN (" + ids + ")", new HashMap<>());
                    return null;
                });
            });
        }
    }
    
    /**
     * 多数据源顺序操作
     */
    private void performMultiDataSourceOperations() {
        // 操作1：从主库读取用户
        List<Map<String, Object>> users = DataSourceContext.executeWithDataSource("main", () -> {
            return jdbcTemplate.queryForList("SELECT id, name, email FROM users WHERE active = 1", new HashMap<>());
        });
        
        // 操作2：写入分析库
        DataSourceContext.executeWithDataSource("analytics", () -> {
            for (Map<String, Object> user : users) {
                Map<String, Object> params = new HashMap<>();
                params.put("user_id", user.get("id"));
                params.put("name", user.get("name"));
                params.put("email", user.get("email"));
                jdbcTemplate.update(
                    "INSERT INTO user_analytics (user_id, name, email, sync_time) VALUES (:user_id, :name, :email, NOW())",
                    params
                );
            }
            return null;
        });
        
        // 操作3：记录到备份库
        DataSourceContext.executeWithDataSource("backup", () -> {
            Map<String, Object> params = new HashMap<>();
            params.put("operation", "user_sync");
            params.put("record_count", users.size());
            jdbcTemplate.update(
                "INSERT INTO sync_logs (operation, record_count, sync_time) VALUES (:operation, :record_count, NOW())",
                params
            );
            return null;
        });
    }
    
    /**
     * 生成报表的辅助方法
     */
    private void generateReport(List<Map<String, Object>> users, List<Map<String, Object>> orders) {
        System.out.println("生成报表: 用户数=" + users.size() + ", 订单数=" + orders.size());
    }
    
    /**
     * 处理VIP数据的辅助方法
     */
    private void processVipData(List<Map<String, Object>> vipData) {
        System.out.println("处理VIP数据: " + vipData.size() + " 条记录");
    }
    
    /**
     * 示例6：分片场景 - 根据用户ID路由到不同分片
     * 模拟水平分片的使用场景
     */
    public Map<String, Object> getUserFromShard(Long userId) {
        // 根据用户ID计算分片
        int shardIndex = (int) (userId % 4); // 假设有4个分片
        String shardDataSource = "shard_" + shardIndex;
        String shardTable = "users_" + shardIndex;
        
        return DataSourceContext.executeWithDataSource(shardDataSource, () -> {
            return TableContext.executeWithTableMapping("users", shardTable, () -> {
                Map<String, Object> params = new HashMap<>();
                params.put("id", userId);
                return jdbcTemplate.queryForObject("SELECT * FROM users WHERE id = :id", params, (rs, rowNum) -> {
                    Map<String, Object> result = new HashMap<>();
                    result.put("id", rs.getLong("id"));
                    result.put("name", rs.getString("name"));
                    result.put("email", rs.getString("email"));
                    return result;
                });
            });
        });
    }
    
    /**
     * 示例7：多租户场景 - 根据租户ID切换数据源和表
     * 模拟SaaS多租户的使用场景
     */
    public List<Map<String, Object>> getTenantData(String tenantId) {
        String tenantDataSource = "tenant_" + tenantId;
        String tenantTable = "data_" + tenantId;
        
        return DataSourceContext.executeWithDataSource(tenantDataSource, () -> {
            return TableContext.executeWithTableMapping("tenant_data", tenantTable, () -> {
                return jdbcTemplate.queryForList("SELECT * FROM tenant_data WHERE active = 1", new HashMap<>());
            });
        });
    }
    
    /**
     * 示例8：读写分离场景
     * 读操作使用从库，写操作使用主库
     */
    public void readWriteSeparation() {
        // 写操作 - 使用主库
        DataSourceContext.executeWithDataSource("master", () -> {
            Map<String, Object> params = new HashMap<>();
            params.put("name", "张三");
            params.put("email", "zhangsan@example.com");
            jdbcTemplate.update("INSERT INTO users (name, email) VALUES (:name, :email)", params);
            return null;
        });
        
        // 读操作 - 使用从库
        List<Map<String, Object>> users = DataSourceContext.executeWithDataSource("slave", () -> {
            return jdbcTemplate.queryForList("SELECT * FROM users ORDER BY created_date DESC LIMIT 10", new HashMap<>());
        });
        
        System.out.println("最新用户数据: " + users.size());
    }
    
    /**
     * 示例9：数据迁移场景
     * 从旧系统迁移数据到新系统
     */
    public void dataMigration() {
        // 从旧系统读取数据
        List<Map<String, Object>> oldData = DataSourceContext.executeWithDataSource("legacy", () -> {
            return TableContext.executeWithTableMapping("users", "old_users", () -> {
                return jdbcTemplate.queryForList("SELECT * FROM users WHERE migrated = 0", new HashMap<>());
            });
        });
        
        // 将数据写入新系统
        DataSourceContext.executeWithDataSource("new_system", () -> {
            for (Map<String, Object> record : oldData) {
                Map<String, Object> params = new HashMap<>();
                params.put("name", record.get("name"));
                params.put("email", record.get("email"));
                params.put("phone", record.get("phone"));
                jdbcTemplate.update(
                    "INSERT INTO users (name, email, phone) VALUES (:name, :email, :phone)",
                    params
                );
            }
            return null;
        });
        
        // 标记旧数据为已迁移
        DataSourceContext.executeWithDataSource("legacy", () -> {
            TableContext.executeWithTableMapping("users", "old_users", () -> {
                // 使用jdbcTemplate直接执行SQL更新
                jdbcTemplate.update("UPDATE users SET migrated = 1 WHERE migrated = 0", new HashMap<>());
                return null;
            });
        });
        
        System.out.println("迁移完成，共迁移 " + oldData.size() + " 条记录");
    }
    
    /**
     * 示例10：性能监控和日志记录
     * 展示如何在切换过程中进行监控
     */
    public void performanceMonitoring() {
        long startTime = System.currentTimeMillis();
        
        try {
            List<Map<String, Object>> result = DataSourceContext.executeWithDataSource("analytics", () -> {
                return TableContext.executeWithTableMapping("performance_log", "performance_log_" + getCurrentMonth(), () -> {
                    // 使用jdbcTemplate执行自定义查询
                    return jdbcTemplate.queryForList("SELECT * FROM performance_log WHERE response_time > 1000", new HashMap<>());
                });
            });
            
            long endTime = System.currentTimeMillis();
            long executionTime = endTime - startTime;
            
            System.out.println("查询执行时间: " + executionTime + "ms");
            System.out.println("慢查询记录数: " + result.size());
            
            // 记录性能日志
            logPerformance("performance_monitoring", executionTime, result.size());
            
        } catch (Exception e) {
            System.err.println("查询执行失败: " + e.getMessage());
            logError("performance_monitoring", e);
        }
    }
    
    private String getCurrentMonth() {
        return "202401"; // 简化实现
    }
    
    private void logPerformance(String operation, long executionTime, int resultCount) {
        // 记录性能日志的实现
        System.out.println("[PERF] " + operation + ": " + executionTime + "ms, results: " + resultCount);
    }
    
    private void logError(String operation, Exception e) {
        // 记录错误日志的实现
        System.err.println("[ERROR] " + operation + ": " + e.getMessage());
    }
}