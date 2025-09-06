package io.flexdata.spring.orm.routing.selector;

import io.flexdata.spring.orm.routing.context.RoutingContext;
import io.flexdata.spring.orm.routing.engine.SpelExpressionEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 基于规则的数据源选择器单元测�? * 
 * @author FlexData
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class RuleBasedDataSourceSelectorTest {

    @Mock
    private SpelExpressionEvaluator spelEvaluator;
    
    private RuleBasedDataSourceSelector selector;

    @BeforeEach
    void setUp() {
        selector = new RuleBasedDataSourceSelector();
        // 通过反射设置spelEvaluator
        try {
            java.lang.reflect.Field field = RuleBasedDataSourceSelector.class.getDeclaredField("spelEvaluator");
            field.setAccessible(true);
            field.set(selector, spelEvaluator);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testSupports_WithMatchingRule() {
        // Given
        RoutingContext context = RoutingContext.builder()
                .tableName("user")
                .operationType(RoutingContext.OperationType.SELECT)
                .parameter("userType", "VIP")
                .build();
        
        RuleBasedDataSourceSelector.SelectionRule rule = new RuleBasedDataSourceSelector.SelectionRule();
        rule.setName("vip-rule");
        rule.setCondition("#{parameters['userType'] == 'VIP'}");
        rule.setDataSource("vip_db");
        rule.setPriority(100);
        rule.setType(RuleBasedDataSourceSelector.RuleType.SPEL);
        rule.setType(RuleBasedDataSourceSelector.RuleType.SPEL);
        
        selector.addRule("*", rule);
        
        // When
        boolean supports = selector.supports(context);
        
        // Then
        assertTrue(supports);
    }

    @Test
    void testSupports_WithoutMatchingRule() {
        // Given
        RoutingContext context = RoutingContext.builder()
                .tableName("user")
                .operationType(RoutingContext.OperationType.SELECT)
                .parameter("userType", "NORMAL")
                .build();
        
        RuleBasedDataSourceSelector.SelectionRule rule = new RuleBasedDataSourceSelector.SelectionRule();
        rule.setName("vip-rule");
        rule.setCondition("#{parameters['userType'] == 'VIP'}");
        rule.setDataSource("vip_db");
        rule.setPriority(100);
        
        selector.addRule("*", rule);
        
        // When
        boolean supports = selector.supports(context);
        
        // Then
        // supports方法只检查是否有规则存在，不检查规则是否匹配
        assertTrue(supports);
    }

    @Test
    void testSupports_EmptyRules() {
        // Given
        RoutingContext context = RoutingContext.builder()
                .tableName("user")
                .operationType(RoutingContext.OperationType.SELECT)
                .build();
        
        // When
        boolean supports = selector.supports(context);
        
        // Then
        assertFalse(supports);
    }

    @Test
    void testSelectDataSource_SingleRule() {
        // Given
        RoutingContext context = RoutingContext.builder()
                .tableName("user")
                .operationType(RoutingContext.OperationType.SELECT)
                .parameter("userType", "VIP")
                .build();
        
        RuleBasedDataSourceSelector.SelectionRule rule = new RuleBasedDataSourceSelector.SelectionRule();
        rule.setName("vip-rule");
        rule.setCondition("#{parameters['userType'] == 'VIP'}");
        rule.setDataSource("vip_db");
        rule.setPriority(100);
        
        selector.addRule("*", rule);
        
        when(spelEvaluator.evaluateCondition(eq("#{parameters['userType'] == 'VIP'}"), any(RoutingContext.class)))
                .thenReturn(true);
        
        // When
        String dataSource = selector.selectDataSource(context);
        
        // Then
        assertEquals("vip_db", dataSource);
    }

    @Test
    void testSelectDataSource_MultipleRules_PriorityOrder() {
        // Given
        RoutingContext context = RoutingContext.builder()
                .tableName("user")
                .operationType(RoutingContext.OperationType.SELECT)
                .parameter("userType", "VIP")
                .parameter("region", "US")
                .build();
        
        RuleBasedDataSourceSelector.SelectionRule lowPriorityRule = new RuleBasedDataSourceSelector.SelectionRule();
        lowPriorityRule.setName("region-rule");
        lowPriorityRule.setCondition("#{parameters['region'] == 'US'}");
        lowPriorityRule.setDataSource("us_db");
        lowPriorityRule.setPriority(50);
        lowPriorityRule.setType(RuleBasedDataSourceSelector.RuleType.SPEL);
        
        RuleBasedDataSourceSelector.SelectionRule highPriorityRule = new RuleBasedDataSourceSelector.SelectionRule();
        highPriorityRule.setName("vip-rule");
        highPriorityRule.setCondition("#{parameters['userType'] == 'VIP'}");
        highPriorityRule.setDataSource("vip_db");
        highPriorityRule.setPriority(100);
        highPriorityRule.setType(RuleBasedDataSourceSelector.RuleType.SPEL);
        
        selector.addRule("*", lowPriorityRule);
        selector.addRule("*", highPriorityRule);
        
        when(spelEvaluator.evaluateCondition(eq("#{parameters['userType'] == 'VIP'}"), any(RoutingContext.class)))
                .thenReturn(true);
        
        // When
        String dataSource = selector.selectDataSource(context);
        
        // Then
        assertEquals("vip_db", dataSource); // 高优先级规则应该被选中
    }

    @Test
    void testSelectDataSource_WithSpelExpression() {
        // Given
        RoutingContext context = RoutingContext.builder()
                .tableName("user")
                .operationType(RoutingContext.OperationType.SELECT)
                .parameter("userId", 12345L)
                .build();
        
        RuleBasedDataSourceSelector.SelectionRule rule = new RuleBasedDataSourceSelector.SelectionRule();
        rule.setName("shard-rule");
        rule.setCondition("#{parameters['userId'] != null}");
        rule.setDataSource("#{parameters['userId'] % 2 == 0 ? 'even_db' : 'odd_db'}");
        rule.setPriority(100);
        rule.setType(RuleBasedDataSourceSelector.RuleType.SPEL);
        
        selector.addRule("*", rule);
        
        when(spelEvaluator.evaluateCondition(eq("#{parameters['userId'] != null}"), any(RoutingContext.class)))
                .thenReturn(true);
        when(spelEvaluator.evaluateExpression(eq("#{parameters['userId'] % 2 == 0 ? 'even_db' : 'odd_db'}"), any(RoutingContext.class), eq(String.class)))
                .thenReturn("odd_db");
        
        // When
        String dataSource = selector.selectDataSource(context);
        
        // Then
        assertEquals("odd_db", dataSource);
    }

    @Test
    void testSelectDataSource_WithParameterRule() {
        // Given
        RoutingContext context = RoutingContext.builder()
                .tableName("user")
                .operationType(RoutingContext.OperationType.SELECT)
                .parameter("userId", 12345L)
                .parameter("userType", "VIP")
                .build();
        
        RuleBasedDataSourceSelector.SelectionRule rule = new RuleBasedDataSourceSelector.SelectionRule();
        rule.setName("param-rule");
        rule.setCondition("#{parameters['userType'] == 'VIP'}");
        rule.setDataSource("vip_db");
        rule.setPriority(100);
        rule.setType(RuleBasedDataSourceSelector.RuleType.SPEL);
        
        selector.addRule("*", rule);
        
        when(spelEvaluator.evaluateCondition(eq("#{parameters['userType'] == 'VIP'}"), any(RoutingContext.class)))
                .thenReturn(true);
        
        // When
        String dataSource = selector.selectDataSource(context);
        
        // Then
        assertEquals("vip_db", dataSource);
    }

    @Test
    void testSelectDataSource_WithHeaderRule() {
        // Given
        RoutingContext context = RoutingContext.builder()
                .tableName("user")
                .operationType(RoutingContext.OperationType.SELECT)
                .header("tenant-id", "tenant-a")
                .build();
        
        RuleBasedDataSourceSelector.SelectionRule rule = new RuleBasedDataSourceSelector.SelectionRule();
        rule.setName("header-rule");
        rule.setCondition("#{headers['tenant-id'] == 'tenant-a'}");
        rule.setDataSource("tenant_a_db");
        rule.setPriority(100);
        rule.setType(RuleBasedDataSourceSelector.RuleType.SPEL);
        
        selector.addRule("*", rule);
        
        when(spelEvaluator.evaluateCondition(eq("#{headers['tenant-id'] == 'tenant-a'}"), any(RoutingContext.class)))
                .thenReturn(true);
        
        // When
        String dataSource = selector.selectDataSource(context);
        
        // Then
        assertEquals("tenant_a_db", dataSource);
    }

    @Test
    void testSelectDataSource_WithTableNameRule() {
        // Given
        RoutingContext context = RoutingContext.builder()
                .tableName("user")
                .operationType(RoutingContext.OperationType.SELECT)
                .build();
        
        RuleBasedDataSourceSelector.SelectionRule rule = new RuleBasedDataSourceSelector.SelectionRule();
        rule.setName("table-rule");
        rule.setCondition("#{tableName == 'user'}");
        rule.setDataSource("user_db");
        rule.setPriority(100);
        rule.setType(RuleBasedDataSourceSelector.RuleType.SPEL);
        
        selector.addRule("*", rule);
        
        when(spelEvaluator.evaluateCondition(eq("#{tableName == 'user'}"), any(RoutingContext.class)))
                .thenReturn(true);
        
        // When
        String dataSource = selector.selectDataSource(context);
        
        // Then
        assertEquals("user_db", dataSource);
    }

    @Test
    void testSelectDataSource_WithOperationTypeRule() {
        // Given
        RoutingContext context = RoutingContext.builder()
                .tableName("user")
                .operationType(RoutingContext.OperationType.SELECT)
                .build();
        
        RuleBasedDataSourceSelector.SelectionRule rule = new RuleBasedDataSourceSelector.SelectionRule();
        rule.setName("operation-rule");
        rule.setCondition("#{operationType.name() == 'SELECT'}");
        rule.setDataSource("read_db");
        rule.setPriority(100);
        rule.setType(RuleBasedDataSourceSelector.RuleType.SPEL);
        
        selector.addRule("*", rule);
        
        when(spelEvaluator.evaluateCondition(eq("#{operationType.name() == 'SELECT'}"), any(RoutingContext.class)))
                .thenReturn(true);
        
        // When
        String dataSource = selector.selectDataSource(context);
        
        // Then
        assertEquals("read_db", dataSource);
    }

    @Test
    void testSelectDataSource_NoMatchingRule() {
        // Given
        RoutingContext context = RoutingContext.builder()
                .tableName("user")
                .operationType(RoutingContext.OperationType.SELECT)
                .parameter("userType", "NORMAL")
                .build();
        
        RuleBasedDataSourceSelector.SelectionRule rule = new RuleBasedDataSourceSelector.SelectionRule();
        rule.setName("vip-rule");
        rule.setCondition("#{parameters['userType'] == 'VIP'}");
        rule.setDataSource("vip_db");
        rule.setPriority(100);
        
        selector.addRule("*", rule);
        
        when(spelEvaluator.evaluateCondition(eq("#{parameters['userType'] == 'VIP'}"), any(RoutingContext.class)))
                .thenReturn(false);
        
        // When
        String dataSource = selector.selectDataSource(context);
        
        // Then
        assertNull(dataSource);
    }

    @Test
    void testSelectDataSource_SpelEvaluationError() {
        // Given
        RoutingContext context = RoutingContext.builder()
                .tableName("user")
                .operationType(RoutingContext.OperationType.SELECT)
                .parameter("userType", "VIP")
                .build();
        
        RuleBasedDataSourceSelector.SelectionRule rule = new RuleBasedDataSourceSelector.SelectionRule();
        rule.setName("error-rule");
        rule.setCondition("#{parameters['userType'] == 'VIP'}");
        rule.setDataSource("#{invalid.expression}");
        rule.setPriority(100);
        
        selector.addRule("*", rule);
        
        when(spelEvaluator.evaluateCondition(eq("#{parameters['userType'] == 'VIP'}"), any(RoutingContext.class)))
                .thenReturn(true);
        when(spelEvaluator.evaluateExpression(eq("#{invalid.expression}"), any(RoutingContext.class), eq(String.class)))
                .thenThrow(new RuntimeException("SpEL evaluation error"));
        
        // When
        String dataSource = selector.selectDataSource(context);
        
        // Then
        assertNull(dataSource); // 错误时应该返回null
    }

    @Test
    void testGetPriority() {
        // When
        int priority = selector.getPriority();
        
        // Then
        assertEquals(100, priority);
    }

    @Test
    void testGetName() {
        // When
        String name = selector.getName();
        
        // Then
        assertEquals("RuleBasedDataSourceSelector", name);
    }

    @Test
    void testAddRule() {
        // Given
        RuleBasedDataSourceSelector.SelectionRule rule1 = new RuleBasedDataSourceSelector.SelectionRule();
        rule1.setName("rule1");
        rule1.setCondition("#{true}");
        rule1.setDataSource("db1");
        rule1.setPriority(100);
        
        RuleBasedDataSourceSelector.SelectionRule rule2 = new RuleBasedDataSourceSelector.SelectionRule();
        rule2.setName("rule2");
        rule2.setCondition("#{true}");
        rule2.setDataSource("db2");
        rule2.setPriority(200);
        
        // When
        selector.addRule("*", rule1);
        selector.addRule("*", rule2);
        
        // Then
        assertEquals(2, selector.getRules("*").size());
        // getRules返回的是添加顺序，不是优先级排序
        assertEquals("rule1", selector.getRules("*").get(0).getName());
        assertEquals("rule2", selector.getRules("*").get(1).getName());
    }

    @Test
    void testRemoveRule() {
        // Given
        RuleBasedDataSourceSelector.SelectionRule rule = new RuleBasedDataSourceSelector.SelectionRule();
        rule.setName("test-rule");
        rule.setCondition("#{true}");
        rule.setDataSource("test_db");
        rule.setPriority(100);
        
        selector.addRule("*", rule);
        assertEquals(1, selector.getRules("*").size());
        
        // When
        selector.removeRule("*", "test-rule");
        
        // Then
        assertEquals(0, selector.getRules("*").size());
    }

    @Test
    void testRemoveRule_NotFound() {
        // When
        selector.removeRule("*", "non-existent-rule");
        
        // Then
        // 方法返回void，无需验证返回值
    }

    @Test
    void testClearRules() {
        // Given
        RuleBasedDataSourceSelector.SelectionRule rule1 = new RuleBasedDataSourceSelector.SelectionRule();
        rule1.setName("rule1");
        rule1.setCondition("#{true}");
        rule1.setDataSource("db1");
        rule1.setPriority(100);
        
        RuleBasedDataSourceSelector.SelectionRule rule2 = new RuleBasedDataSourceSelector.SelectionRule();
        rule2.setName("rule2");
        rule2.setCondition("#{true}");
        rule2.setDataSource("db2");
        rule2.setPriority(200);
        
        selector.addRule("*", rule1);
        selector.addRule("*", rule2);
        assertEquals(2, selector.getRules("*").size());
        
        // When
        selector.clearRules("*");
        
        // Then
        assertEquals(0, selector.getRules("*").size());
    }

    @Test
    void testRuleBuilder() {
        // When
        RuleBasedDataSourceSelector.SelectionRule rule = new RuleBasedDataSourceSelector.SelectionRule();
        rule.setName("test-rule");
        rule.setCondition("#{parameters['test'] == 'value'}");
        rule.setDataSource("test_db");
        rule.setPriority(150);
        
        // Then
        assertEquals("test-rule", rule.getName());
        assertEquals("#{parameters['test'] == 'value'}", rule.getCondition());
        assertEquals("test_db", rule.getDataSource());
        assertEquals(150, rule.getPriority());
    }
}