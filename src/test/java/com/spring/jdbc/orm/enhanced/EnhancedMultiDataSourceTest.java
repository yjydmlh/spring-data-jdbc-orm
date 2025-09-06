package com.spring.jdbc.orm.enhanced;

import com.spring.jdbc.orm.core.datasource.DataSourceContext;
import com.spring.jdbc.orm.core.table.TableContext;
import com.spring.jdbc.orm.core.sql.SqlTableReplacer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 增强多数据源功能测试类
 * 测试DataSourceContext、TableContext和SqlTableReplacer的功能
 */
public class EnhancedMultiDataSourceTest {
    
    @BeforeEach
    void setUp() {
        // 测试前清理
        DataSourceContext.clearDataSource();
        DataSourceContext.clearDataSourceMappings();
        TableContext.clearTableMappings();
    }
    
    @AfterEach
    void tearDown() {
        // 测试后清理
        DataSourceContext.clearDataSource();
        DataSourceContext.clearDataSourceMappings();
        TableContext.clearTableMappings();
    }
    
    @Test
    @DisplayName("测试DataSourceContext基本功能")
    void testDataSourceContextBasicOperations() {
        // 测试设置和获取数据源
        assertNull(DataSourceContext.getDataSource());
        
        DataSourceContext.setDataSource("user_db");
        assertEquals("user_db", DataSourceContext.getDataSource());
        
        DataSourceContext.clearDataSource();
        assertNull(DataSourceContext.getDataSource());
    }
    
    @Test
    @DisplayName("测试DataSourceContext作用域执行")
    void testDataSourceContextScopeExecution() {
        // 测试作用域内执行
        String result = DataSourceContext.executeWithDataSource("test_db", () -> {
            assertEquals("test_db", DataSourceContext.getDataSource());
            return "success";
        });
        
        assertEquals("success", result);
        // 作用域外应该自动清理
        assertNull(DataSourceContext.getDataSource());
    }
    
    @Test
    @DisplayName("测试DataSourceContext数据源映射")
    void testDataSourceContextMapping() {
        // 测试数据源映射
        DataSourceContext.setDataSourceMapping("users", "user_db");
        DataSourceContext.setDataSourceMapping("orders", "order_db");
        
        // 测试映射执行
        String result = DataSourceContext.executeWithDataSource("users", () -> {
            assertEquals("users", DataSourceContext.getDataSource());
            return "mapped_success";
        });
        
        assertEquals("mapped_success", result);
        assertNull(DataSourceContext.getDataSource());
        
        // 测试批量映射设置
        Map<String, String> mappings = new HashMap<>();
        mappings.put("analytics", "analytics_db");
        mappings.put("backup", "backup_db");
        for (Map.Entry<String, String> entry : mappings.entrySet()) {
            DataSourceContext.setDataSourceMapping(entry.getKey(), entry.getValue());
        }
        
        String analyticsResult = DataSourceContext.executeWithDataSource("analytics", () -> {
            return DataSourceContext.getDataSource();
        });
        assertEquals("analytics", analyticsResult);
        
        // 测试清理映射
        DataSourceContext.clearDataSourceMappings();
        // 清理后映射应该不存在，但不会抛异常，会使用原始名称
    }
    
    @Test
    @DisplayName("测试DataSourceContext数据源注册")
    void testDataSourceContextRegistration() {
        // 测试数据源注册
        DataSourceContext.DataSourceInfo info = new DataSourceContext.DataSourceInfo(
            "test_db",
            "Test Database",
            "Test database for unit testing"
        );
        
        DataSourceContext.registerDataSource(info);
        assertTrue(DataSourceContext.isDataSourceRegistered("test_db"));
        
        Map<String, DataSourceContext.DataSourceInfo> allDataSources = DataSourceContext.getAllDataSources();
        assertTrue(allDataSources.containsKey("test_db"));
        
        // 测试获取数据源信息
        DataSourceContext.DataSourceInfo retrieved = DataSourceContext.getDataSourceInfo("test_db");
        assertNotNull(retrieved);
        assertEquals("test_db", retrieved.getKey());
        assertEquals("Test Database", retrieved.getName());
        assertEquals("Test database for unit testing", retrieved.getDescription());
    }
    
    @Test
    @DisplayName("测试TableContext基本功能")
    void testTableContextBasicOperations() {
        // 测试表名映射
        assertEquals("user", TableContext.getTableMapping("user"));
        
        TableContext.setTableMapping("user", "user_2024");
        assertEquals("user_2024", TableContext.getTableMapping("user"));
        
        TableContext.removeTableMapping("user");
        assertEquals("user", TableContext.getTableMapping("user"));
    }
    
    @Test
    @DisplayName("测试TableContext作用域执行")
    void testTableContextScopeExecution() {
        // 测试单表映射作用域执行
        String result = TableContext.executeWithTableMapping("user", "user_2024", () -> {
            assertEquals("user_2024", TableContext.getTableMapping("user"));
            return "table_success";
        });
        
        assertEquals("table_success", result);
        // 作用域外应该自动清理
        assertEquals("user", TableContext.getTableMapping("user"));
        
        // 测试使用现有映射的作用域执行
        TableContext.setTableMapping("current_user", "user_active");
        String mappingResult = TableContext.executeWithTableMapping("current_user", "user_active", () -> {
            return TableContext.getTableMapping("current_user");
        });
        assertEquals("user_active", mappingResult);
    }
    
    @Test
    @DisplayName("测试TableContext批量映射")
    void testTableContextBatchMapping() {
        // 测试批量设置映射
        Map<String, String> mappings = new HashMap<>();
        mappings.put("user", "user_2024");
        mappings.put("order", "order_current");
        mappings.put("product", "product_active");
        
        // 批量设置映射
        for (Map.Entry<String, String> entry : mappings.entrySet()) {
            TableContext.setTableMapping(entry.getKey(), entry.getValue());
        }
        
        assertEquals("user_2024", TableContext.getTableMapping("user"));
        assertEquals("order_current", TableContext.getTableMapping("order"));
        assertEquals("product_active", TableContext.getTableMapping("product"));
        
        // 测试获取所有映射
        Map<String, String> allMappings = TableContext.getAllTableMappings();
        assertEquals(3, allMappings.size());
        assertTrue(allMappings.containsKey("user"));
        assertTrue(allMappings.containsKey("order"));
        assertTrue(allMappings.containsKey("product"));
        
        // 测试清理所有映射
        TableContext.clearTableMappings();
        assertTrue(TableContext.getAllTableMappings().isEmpty());
    }
    
    @Test
    @DisplayName("测试DataSourceContext和TableContext组合使用")
    void testCombinedContextUsage() {
        // 测试嵌套作用域执行
        String result = DataSourceContext.executeWithDataSource("user_db", () -> {
            return TableContext.executeWithTableMapping("user", "user_2024", () -> {
                assertEquals("user_db", DataSourceContext.getDataSource());
                assertEquals("user_2024", TableContext.getTableMapping("user"));
                return "combined_success";
            });
        });
        
        assertEquals("combined_success", result);
        // 两个上下文都应该被清理
        assertNull(DataSourceContext.getDataSource());
        assertEquals("user", TableContext.getTableMapping("user"));
    }
    
    @Test
    @DisplayName("测试SqlTableReplacer表名替换功能")
    void testSqlTableReplacer() {
        // 设置表名映射
        TableContext.setTableMapping("user", "user_2024");
        TableContext.setTableMapping("order", "order_current");
        
        // 测试简单SELECT语句
        String selectSql = "SELECT * FROM user WHERE id = 1";
        String replacedSelect = SqlTableReplacer.replaceTableNames(selectSql);
        assertEquals("SELECT * FROM user_2024 WHERE id = 1", replacedSelect);
        
        // 测试带别名的SELECT语句
        String aliasSql = "SELECT u.name FROM user u JOIN order o ON u.id = o.user_id";
        String replacedAlias = SqlTableReplacer.replaceTableNames(aliasSql);
        assertEquals("SELECT u.name FROM user_2024 u JOIN order_current o ON u.id = o.user_id", replacedAlias);
        
        // 测试INSERT语句
        String insertSql = "INSERT INTO user (name, email) VALUES ('test', 'test@example.com')";
        String replacedInsert = SqlTableReplacer.replaceTableNames(insertSql);
        assertEquals("INSERT INTO user_2024 (name, email) VALUES ('test', 'test@example.com')", replacedInsert);
        
        // 测试UPDATE语句
        String updateSql = "UPDATE user SET name = 'updated' WHERE id = 1";
        String replacedUpdate = SqlTableReplacer.replaceTableNames(updateSql);
        assertEquals("UPDATE user_2024 SET name = 'updated' WHERE id = 1", replacedUpdate);
        
        // 测试DELETE语句
        String deleteSql = "DELETE FROM user WHERE id = 1";
        String replacedDelete = SqlTableReplacer.replaceTableNames(deleteSql);
        assertEquals("DELETE FROM user_2024 WHERE id = 1", replacedDelete);
    }
    
    @Test
    @DisplayName("测试SqlTableReplacer表名提取功能")
    void testSqlTableExtraction() {
        String complexSql = "SELECT u.name, o.total FROM user u " +
                           "JOIN order o ON u.id = o.user_id " +
                           "LEFT JOIN product p ON o.product_id = p.id " +
                           "WHERE u.status = 'active'";
        
        Set<String> tableNames = SqlTableReplacer.extractTableNames(complexSql);
        
        assertEquals(3, tableNames.size());
        assertTrue(tableNames.contains("user"));
        assertTrue(tableNames.contains("order"));
        assertTrue(tableNames.contains("product"));
    }
    
    @Test
    @DisplayName("测试SqlTableReplacer映射检查功能")
    void testSqlTableMappingCheck() {
        // 设置部分映射
        TableContext.setTableMapping("user", "user_2024");
        // order表没有映射
        
        String sql = "SELECT * FROM user u JOIN order o ON u.id = o.user_id";
        
        // 检查是否包含表名
        assertTrue(SqlTableReplacer.containsTable(sql, "user"));
        assertTrue(SqlTableReplacer.containsTable(sql, "order"));
        
        // 检查SQL中的表名
        Set<String> extractedTables = SqlTableReplacer.extractTableNames(sql);
        assertEquals(2, extractedTables.size());
        assertTrue(extractedTables.contains("user"));
        assertTrue(extractedTables.contains("order"));
        
        // 验证映射
        Map<String, String> tableMappings = Map.of("user", "user_2024");
        SqlTableReplacer.ValidationResult result = SqlTableReplacer.validateTableMappings(sql, tableMappings);
        assertFalse(result.isValid());
        assertEquals(1, result.getMissingMappings().size());
        assertTrue(result.getMissingMappings().contains("order"));
    }
    
    @Test
    @DisplayName("测试异常情况处理")
    void testExceptionHandling() {
        // 测试空SQL
        assertDoesNotThrow(() -> {
            String result = SqlTableReplacer.replaceTableNames("");
            assertEquals("", result);
        });
        
        // 测试null SQL
        assertDoesNotThrow(() -> {
            String result = SqlTableReplacer.replaceTableNames(null);
            assertNull(result);
        });
        
        // 测试没有表名的SQL
        String noTableSql = "SELECT 1";
        String result = SqlTableReplacer.replaceTableNames(noTableSql);
        assertEquals(noTableSql, result);
        
        // 测试作用域执行中的异常
        assertThrows(RuntimeException.class, () -> {
            DataSourceContext.executeWithDataSource("test_db", () -> {
                throw new RuntimeException("测试异常");
            });
        });
        
        // 异常后上下文应该被清理
        assertNull(DataSourceContext.getDataSource());
    }
    
    @Test
    @DisplayName("测试线程安全性")
    void testThreadSafety() throws InterruptedException {
        final int threadCount = 10;
        final List<String> results = Collections.synchronizedList(new ArrayList<>());
        final List<Thread> threads = new ArrayList<>();
        
        // 创建多个线程同时操作上下文
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            Thread thread = new Thread(() -> {
                String dataSource = "db_" + threadId;
                String table = "table_" + threadId;
                
                DataSourceContext.executeWithDataSource(dataSource, () -> {
                    return TableContext.executeWithTableMappings(Map.of("test", table), () -> {
                        // 验证当前线程的上下文
                        String currentDs = DataSourceContext.getDataSource();
                        String currentTable = TableContext.getTableMapping("test");
                        
                        results.add(currentDs + "|" + currentTable);
                        
                        // 模拟一些处理时间
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        
                        return null;
                    });
                });
            });
            
            threads.add(thread);
            thread.start();
        }
        
        // 等待所有线程完成
        for (Thread thread : threads) {
            thread.join();
        }
        
        // 验证结果
        assertEquals(threadCount, results.size());
        
        // 验证每个线程都有正确的上下文
        for (int i = 0; i < threadCount; i++) {
            String expected = "db_" + i + "|table_" + i;
            assertTrue(results.contains(expected), "Missing result: " + expected);
        }
        
        // 主线程的上下文应该是干净的
        assertNull(DataSourceContext.getDataSource());
        assertEquals("test", TableContext.getTableMapping("test"));
    }
}