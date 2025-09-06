package io.flexdata.spring.orm.routing.engine;

import io.flexdata.spring.orm.routing.context.RoutingContext;
import io.flexdata.spring.orm.routing.engine.SpelExpressionEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.expression.EvaluationException;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SpEL表达式评估器单元测试
 * 
 * @author FlexData
 * @since 1.0.0
 */
class SpelExpressionEvaluatorTest {

    private SpelExpressionEvaluator evaluator;
    private RoutingContext context;

    @BeforeEach
    void setUp() {
        evaluator = new SpelExpressionEvaluator();
        
        // 创建测试上下文
        context = RoutingContext.builder()
                .tableName("user")
                .operationType(RoutingContext.OperationType.SELECT)
                .parameter("userId", 12345L)
                .parameter("userType", "VIP")
                .parameter("age", 25)
                .header("tenant-id", "tenant-a")
                .header("user-agent", "Mozilla/5.0")
                .build();
    }

    @Test
    void testEvaluateCondition_SimpleComparison() {
        // Given
        String expression = "#{parameters['userId'] > 10000}";
        
        // When
        boolean result = evaluator.evaluateCondition(expression, context);
        
        // Then
        assertTrue(result);
    }

    @Test
    void testEvaluateCondition_StringComparison() {
        // Given
        String expression = "#{parameters['userType'] == 'VIP'}";
        
        // When
        boolean result = evaluator.evaluateCondition(expression, context);
        
        // Then
        assertTrue(result);
    }

    @Test
    void testEvaluateCondition_HeaderAccess() {
        // Given
        String expression = "#{headers['tenant-id'] == 'tenant-a'}";
        
        // When
        boolean result = evaluator.evaluateCondition(expression, context);
        
        // Then
        assertTrue(result);
    }

    @Test
    void testEvaluateCondition_ComplexExpression() {
        // Given
        String expression = "#{parameters['userId'] > 10000 and parameters['userType'] == 'VIP' and parameters['age'] >= 18}";
        
        // When
        boolean result = evaluator.evaluateCondition(expression, context);
        
        // Then
        assertTrue(result);
    }

    @Test
    void testEvaluateCondition_FalseResult() {
        // Given
        String expression = "#{parameters['userId'] < 1000}";
        
        // When
        boolean result = evaluator.evaluateCondition(expression, context);
        
        // Then
        assertFalse(result);
    }

    @Test
    void testEvaluateExpression_StringResult() {
        // Given
        String expression = "#{parameters['userType'] + '_database'}";
        
        // When
        String result = evaluator.evaluateExpression(expression, context, String.class);
        
        // Then
        assertEquals("VIP_database", result);
    }

    @Test
    void testEvaluateExpression_NumericResult() {
        // Given
        String expression = "#{parameters['userId'] % 4}";
        
        // When
        Integer result = evaluator.evaluateExpression(expression, context, Integer.class);
        
        // Then
        assertEquals(1, result); // 12345 % 4 = 1
    }

    @Test
    void testEvaluateExpression_ConditionalResult() {
        // Given
        String expression = "#{parameters['userType'] == 'VIP' ? 'vip_db' : 'normal_db'}";
        
        // When
        String result = evaluator.evaluateExpression(expression, context, String.class);
        
        // Then
        assertEquals("vip_db", result);
    }

    @Test
    void testSpelUtils_StringOperations() {
        // Given
        String expression = "#{T(io.flexdata.spring.orm.routing.engine.SpelExpressionEvaluator.SpelUtils).isEmpty('')}";
        
        // When
        Boolean result = evaluator.evaluateExpression(expression, context, Boolean.class);
        
        // Then
        assertTrue(result);
    }

    @Test
    void testSpelUtils_HashOperations() {
        // Given
        String expression = "#{T(io.flexdata.spring.orm.routing.engine.SpelExpressionEvaluator.SpelUtils).hash('test', 4)}";
        
        // When
        Integer result = evaluator.evaluateExpression(expression, context, Integer.class);
        
        // Then
        assertNotNull(result);
        assertTrue(result >= 0 && result < 4);
    }

    @Test
    void testSpelUtils_RangeOperations() {
        // Given
        String expression = "#{T(io.flexdata.spring.orm.routing.engine.SpelExpressionEvaluator.SpelUtils).inRange(25, 18, 65)}";
        
        // When
        Boolean result = evaluator.evaluateExpression(expression, context, Boolean.class);
        
        // Then
        assertTrue(result);
    }

    @Test
    void testSpelUtils_CollectionOperations() {
        // Given
        String expression = "#{T(io.flexdata.spring.orm.routing.engine.SpelExpressionEvaluator.SpelUtils).contains({'VIP', 'PREMIUM'}, parameters['userType'])}";
        
        // When
        Boolean result = evaluator.evaluateExpression(expression, context, Boolean.class);
        
        // Then
        assertTrue(result);
    }

    @Test
    void testSpelUtils_TypeConversion() {
        // Given
        String expression = "#{T(io.flexdata.spring.orm.routing.engine.SpelExpressionEvaluator.SpelUtils).toInt('123')}";
        
        // When
        Integer result = evaluator.evaluateExpression(expression, context, Integer.class);
        
        // Then
        assertEquals(123, result);
    }

    @Test
    void testContextVariables() {
        // Given - 测试上下文变量访问
        String expression = "#{tableName + '_' + operationType.name().toLowerCase()}";
        
        // When
        String result = evaluator.evaluateExpression(expression, context, String.class);
        
        // Then
        assertEquals("user_select", result);
    }

    @Test
    void testNowVariable() {
        // Given - 测试now变量
        String expression = "#{#now.year > 2020}";
        
        // When
        Boolean result = evaluator.evaluateExpression(expression, context, Boolean.class);
        
        // Then
        assertTrue(result);
    }

    @Test
    void testRandomVariable() {
        // Given - 测试random变量
        String expression = "#{#random.nextInt(100)}";
        
        // When
        Integer result = evaluator.evaluateExpression(expression, context, Integer.class);
        
        // Then
        assertNotNull(result);
        assertTrue(result >= 0 && result < 100);
    }

    @Test
    void testExpressionCaching() {
        // Given
        String expression = "#{parameters['userId'] > 10000}";
        
        // When - 多次评估相同表达�?        boolean result1 = evaluator.evaluateCondition(expression, context);
        boolean result2 = evaluator.evaluateCondition(expression, context);
        boolean result3 = evaluator.evaluateCondition(expression, context);
        
        // Then - 结果应该一致（测试缓存功能�?        assertTrue(result1);
        assertTrue(result2);
        assertTrue(result3);
    }

    @Test
    void testInvalidExpression() {
        // Given
        String invalidExpression = "#{invalid.property.access}";
        
        // When & Then
        assertThrows(EvaluationException.class, () -> {
            evaluator.evaluateCondition(invalidExpression, context);
        });
    }

    @Test
    void testNullContext() {
        // Given
        String expression = "#{true}";
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            evaluator.evaluateCondition(expression, null);
        });
    }

    @Test
    void testEmptyExpression() {
        // Given
        String emptyExpression = "";
        
        // When & Then
        assertThrows(IllegalArgumentException.class, () -> {
            evaluator.evaluateCondition(emptyExpression, context);
        });
    }

    @Test
    void testNonBooleanConditionExpression() {
        // Given
        String expression = "#{parameters['userId']}";
        
        // When & Then - 条件表达式必须返回boolean
        assertThrows(ClassCastException.class, () -> {
            evaluator.evaluateCondition(expression, context);
        });
    }

    @Test
    void testComplexSpelUtilsOperations() {
        // Given - 测试复杂的SpelUtils操作组合
        String expression = "#{T(io.flexdata.spring.orm.routing.engine.SpelExpressionEvaluator.SpelUtils).hash(parameters['userType'], 4) == 0 ? 'shard0' : 'shard' + T(io.flexdata.spring.orm.routing.engine.SpelExpressionEvaluator.SpelUtils).hash(parameters['userType'], 4)}";
        
        // When
        String result = evaluator.evaluateExpression(expression, context, String.class);
        
        // Then
        assertNotNull(result);
        assertTrue(result.startsWith("shard"));
    }

    @Test
    void testMapOperations() {
        // Given - 测试Map操作
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("key1", "value1");
        testMap.put("key2", 42);
        
        RoutingContext mapContext = RoutingContext.builder()
                .tableName("test")
                .operationType(RoutingContext.OperationType.SELECT)
                .parameter("testMap", testMap)
                .build();
        
        String expression = "#{T(io.flexdata.spring.orm.routing.engine.SpelExpressionEvaluator.SpelUtils).getMapValue(parameters['testMap'], 'key1', 'default')}";
        
        // When
        String result = evaluator.evaluateExpression(expression, mapContext, String.class);
        
        // Then
        assertEquals("value1", result);
    }

    @Test
    void testListOperations() {
        // Given - 测试List操作
        List<String> testList = Arrays.asList("item1", "item2", "item3");
        
        RoutingContext listContext = RoutingContext.builder()
                .tableName("test")
                .operationType(RoutingContext.OperationType.SELECT)
                .parameter("testList", testList)
                .build();
        
        String expression = "#{T(io.flexdata.spring.orm.routing.engine.SpelExpressionEvaluator.SpelUtils).getListValue(parameters['testList'], 1, 'default')}";
        
        // When
        String result = evaluator.evaluateExpression(expression, listContext, String.class);
        
        // Then
        assertEquals("item2", result);
    }
}