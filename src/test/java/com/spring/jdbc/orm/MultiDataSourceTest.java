package com.spring.jdbc.orm;

import com.spring.jdbc.orm.annotation.DataSource;
import com.spring.jdbc.orm.core.datasource.DataSourceContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 多数据源功能测试
 */
public class MultiDataSourceTest {
    
    @BeforeEach
    void setUp() {
        // 清理数据源上下文
        DataSourceContext.clearDataSource();
    }
    
    @AfterEach
    void tearDown() {
        // 清理数据源上下文
        DataSourceContext.clearDataSource();
    }
    
    /**
     * 测试DataSourceContext基本功能
     */
    @Test
    public void testDataSourceContext() {
        // 测试默认状态
        assertNull(DataSourceContext.getDataSource());
        
        // 测试设置数据源
        DataSourceContext.setDataSource("test");
        assertEquals("test", DataSourceContext.getDataSource());
        
        // 测试清理数据源
        DataSourceContext.clearDataSource();
        assertNull(DataSourceContext.getDataSource());
        
        // 测试空值处理
        assertThrows(IllegalArgumentException.class, () -> {
            DataSourceContext.setDataSource(null);
        });
        
        // 验证数据源已清理
        assertNull(DataSourceContext.getDataSource());
    }
    
    /**
     * 测试executeWithDataSource方法
     */
    @Test
    public void testExecuteWithDataSource() {
        // 初始状态
        assertNull(DataSourceContext.getDataSource());
        
        // 在指定数据源中执行操作
        String result = DataSourceContext.executeWithDataSource("secondary", () -> {
            // 验证数据源已切换
            assertEquals("secondary", DataSourceContext.getDataSource());
            return "executed in secondary";
        });
        
        // 验证返回值
        assertEquals("executed in secondary", result);
        
        // 验证数据源已恢复
        assertNull(DataSourceContext.getDataSource());
    }
    
    /**
     * 测试嵌套数据源切换
     */
    @Test
    public void testNestedDataSourceSwitch() {
        DataSourceContext.executeWithDataSource("primary", () -> {
            assertEquals("primary", DataSourceContext.getDataSource());
            
            // 嵌套切换到另一个数据源
            DataSourceContext.executeWithDataSource("readonly", () -> {
                assertEquals("readonly", DataSourceContext.getDataSource());
                return null;
            });
            
            // 验证恢复到外层数据源
            assertEquals("primary", DataSourceContext.getDataSource());
            return null;
        });
        
        // 验证完全恢复
        assertNull(DataSourceContext.getDataSource());
    }
    
    /**
     * 测试异常情况下的数据源恢复
     */
    @Test
    public void testDataSourceRecoveryOnException() {
        assertNull(DataSourceContext.getDataSource());
        
        try {
            DataSourceContext.executeWithDataSource("test", () -> {
                assertEquals("test", DataSourceContext.getDataSource());
                throw new RuntimeException("Test exception");
            });
        } catch (RuntimeException e) {
            assertEquals("Test exception", e.getMessage());
        }
        
        // 验证即使发生异常，数据源也能正确恢复
        assertNull(DataSourceContext.getDataSource());
    }
    
    /**
     * 测试基于注解的数据源切换（模拟AOP行为）
     */
    @Test
    public void testAnnotationBasedDataSourceSwitch() {
        // 模拟@DataSource(type = DataSource.Type.READONLY)的行为
        DataSourceContext.setDataSource("readonly");
        String result1 = DataSourceContext.getDataSource();
        assertEquals("readonly", result1);
        DataSourceContext.clearDataSource();
        
        // 模拟@DataSource(type = DataSource.Type.MASTER)的行为
        DataSourceContext.setDataSource("master");
        String result2 = DataSourceContext.getDataSource();
        assertEquals("master", result2);
        DataSourceContext.clearDataSource();
        
        // 模拟@DataSource("secondary")的行为
        DataSourceContext.setDataSource("secondary");
        String result3 = DataSourceContext.getDataSource();
        assertEquals("secondary", result3);
        DataSourceContext.clearDataSource();
    }
    
    /**
     * 测试线程隔离
     */
    @Test
    public void testThreadIsolation() throws InterruptedException {
        // 主线程设置数据源
        DataSourceContext.setDataSource("main-thread");
        assertEquals("main-thread", DataSourceContext.getDataSource());
        
        // 创建新线程
        Thread thread = new Thread(() -> {
            // 新线程应该看不到主线程的数据源设置
            assertNull(DataSourceContext.getDataSource());
            
            // 新线程设置自己的数据源
            DataSourceContext.setDataSource("sub-thread");
            assertEquals("sub-thread", DataSourceContext.getDataSource());
        });
        
        thread.start();
        thread.join();
        
        // 主线程的数据源设置应该不受影响
        assertEquals("main-thread", DataSourceContext.getDataSource());
        
        // 清理
        DataSourceContext.clearDataSource();
    }
    
    /**
     * 测试数据源类型枚举
     */
    @Test
    public void testDataSourceTypeEnum() {
        assertEquals("default", DataSource.Type.DEFAULT.getKey());
        assertEquals("default", DataSource.Type.MASTER.getKey());
        assertEquals("readonly", DataSource.Type.SLAVE.getKey());
        assertEquals("readonly", DataSource.Type.READONLY.getKey());
        assertEquals("secondary", DataSource.Type.SECONDARY.getKey());
    }
}