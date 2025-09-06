package io.flexdata.spring.orm.routing.engine;

import io.flexdata.spring.orm.routing.config.RoutingRuleConfig;
import io.flexdata.spring.orm.routing.context.RoutingContext;
import io.flexdata.spring.orm.routing.selector.DataSourceSelector;
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
import static org.mockito.Mockito.lenient;

/**
 * 路由引擎单元测试
 * 
 * @author FlexData
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class RoutingEngineTest {

    @Mock
    private RoutingRuleConfig routingConfig;
    
    @Mock
    private SpelExpressionEvaluator spelEvaluator;
    
    @Mock
    private DataSourceSelector dataSourceSelector;
    
    private RoutingEngine routingEngine;
    private List<DataSourceSelector> selectors;

    @BeforeEach
    void setUp() {
        selectors = Arrays.asList(dataSourceSelector);
        routingEngine = new RoutingEngine(routingConfig, spelEvaluator, selectors);
        
        // 设置默认配置
        lenient().when(routingConfig.getDefaultDataSource()).thenReturn("main");
    }

    @Test
    void testDefaultRouting() {
        // Given
        RoutingContext context = RoutingContext.builder()
                .tableName("user")
                .operationType(RoutingContext.OperationType.SELECT)
                .build();
        
        when(routingConfig.getCustomRules()).thenReturn(null);
        when(routingConfig.getReadWriteSplit()).thenReturn(null);
        when(routingConfig.getSharding()).thenReturn(null);
        when(routingConfig.getMultiTenant()).thenReturn(null);
        when(routingConfig.getTableMappings()).thenReturn(null);
        
        // When
        RoutingEngine.RoutingResult result = routingEngine.route(context);
        
        // Then
        assertNotNull(result);
        assertEquals("main", result.getDataSource());
        assertEquals("user", result.getTableName());
        assertEquals("Default routing", result.getReason());
    }

    @Test
    void testCustomRuleRouting() {
        // Given
        RoutingContext context = RoutingContext.builder()
                .tableName("user")
                .operationType(RoutingContext.OperationType.SELECT)
                .parameter("userType", "VIP")
                .build();
        
        RoutingRuleConfig.CustomRoutingRule customRule = new RoutingRuleConfig.CustomRoutingRule();
        customRule.setName("vip-user-routing");
        customRule.setCondition("#{parameters['userType'] == 'VIP'}");
        customRule.setDataSource("vip_db");
        customRule.setPriority(100);
        customRule.setEnabled(true);
        
        lenient().when(routingConfig.getCustomRules()).thenReturn(Arrays.asList(customRule));
        lenient().when(spelEvaluator.evaluateCondition(eq("#{parameters['userType'] == 'VIP'}"), any(RoutingContext.class)))
                .thenReturn(true);
        lenient().when(spelEvaluator.evaluateExpression(eq("vip_db"), any(RoutingContext.class), eq(String.class)))
                .thenReturn("vip_db");
        
        // When
        RoutingEngine.RoutingResult result = routingEngine.route(context);
        
        // Then
        assertNotNull(result);
        assertEquals("vip_db", result.getDataSource());
        assertEquals("user", result.getTableName());
        assertEquals("Custom rule: vip-user-routing", result.getReason());
    }

    @Test
    void testReadWriteSplitRouting() {
        // Given - 读操作
        RoutingContext readContext = RoutingContext.builder()
                .tableName("user")
                .operationType(RoutingContext.OperationType.SELECT)
                .build();
        
        RoutingRuleConfig.ReadWriteSplitConfig rwConfig = new RoutingRuleConfig.ReadWriteSplitConfig();
        rwConfig.setEnabled(true);
        rwConfig.setMasterDataSource("master");
        rwConfig.setSlaveDataSources(Arrays.asList("slave1", "slave2"));
        rwConfig.setStrategy("round_robin");
        
        when(routingConfig.getCustomRules()).thenReturn(null);
        when(routingConfig.getReadWriteSplit()).thenReturn(rwConfig);
        when(routingConfig.getSharding()).thenReturn(null);
        when(routingConfig.getMultiTenant()).thenReturn(null);
        
        // When
        RoutingEngine.RoutingResult result = routingEngine.route(readContext);
        
        // Then
        assertNotNull(result);
        assertTrue(Arrays.asList("slave1", "slave2").contains(result.getDataSource()));
        assertEquals("Read-write split: read", result.getReason());
        
        // Given - 写操作
        RoutingContext writeContext = RoutingContext.builder()
                .tableName("user")
                .operationType(RoutingContext.OperationType.INSERT)
                .build();
        
        // When
        RoutingEngine.RoutingResult writeResult = routingEngine.route(writeContext);
        
        // Then
        assertNotNull(writeResult);
        assertEquals("master", writeResult.getDataSource());
        assertEquals("Read-write split: write", writeResult.getReason());
    }

    @Test
    void testShardingRouting() {
        // Given
        RoutingContext context = RoutingContext.builder()
                .tableName("user")
                .operationType(RoutingContext.OperationType.SELECT)
                .parameter("userId", 123L)
                .build();
        
        RoutingRuleConfig.ShardingConfig shardingConfig = new RoutingRuleConfig.ShardingConfig();
        shardingConfig.setEnabled(true);
        shardingConfig.setStrategy("mod");
        shardingConfig.setShardingKey("userId");
        shardingConfig.setShardCount(4);
        shardingConfig.setTableTemplate("user_{0}");
        
        Map<String, String> dataSourceMapping = new HashMap<>();
        dataSourceMapping.put("0", "shard0");
        dataSourceMapping.put("1", "shard1");
        dataSourceMapping.put("2", "shard2");
        dataSourceMapping.put("3", "shard3");
        shardingConfig.setDataSourceMapping(dataSourceMapping);
        
        Map<String, RoutingRuleConfig.ShardingConfig> shardingMap = new HashMap<>();
        shardingMap.put("user", shardingConfig);
        
        lenient().when(routingConfig.getCustomRules()).thenReturn(null);
        lenient().when(routingConfig.getReadWriteSplit()).thenReturn(null);
        lenient().when(routingConfig.getSharding()).thenReturn(shardingMap);
        lenient().when(routingConfig.getMultiTenant()).thenReturn(null);
        
        // When
        RoutingEngine.RoutingResult result = routingEngine.route(context);
        
        // Then
        assertNotNull(result);
        assertEquals("shard3", result.getDataSource()); // 123L.hashCode() % 4 = 3
        assertEquals("user_3", result.getTableName());
        assertEquals("Sharding routing: mod", result.getReason());
    }

    @Test
    void testMultiTenantRouting() {
        // Given
        RoutingContext context = RoutingContext.builder()
                .tableName("user")
                .operationType(RoutingContext.OperationType.SELECT)
                .header("tenant-id", "tenant-a")
                .build();
        
        RoutingRuleConfig.MultiTenantConfig tenantConfig = new RoutingRuleConfig.MultiTenantConfig();
        tenantConfig.setEnabled(true);
        tenantConfig.setStrategy("datasource");
        tenantConfig.setTenantResolver("header");
        tenantConfig.setTenantKey("tenant-id");
        tenantConfig.setDefaultTenant("default");
        
        Map<String, String> tenantMappings = new HashMap<>();
        tenantMappings.put("tenant-a", "tenant_a_db");
        tenantMappings.put("tenant-b", "tenant_b_db");
        tenantConfig.setTenantMappings(tenantMappings);
        
        lenient().when(routingConfig.getCustomRules()).thenReturn(null);
        lenient().when(routingConfig.getReadWriteSplit()).thenReturn(null);
        lenient().when(routingConfig.getSharding()).thenReturn(null);
        lenient().when(routingConfig.getMultiTenant()).thenReturn(tenantConfig);
        
        // When
        RoutingEngine.RoutingResult result = routingEngine.route(context);
        
        // Then
        assertNotNull(result);
        assertEquals("tenant_a_db", result.getDataSource());
        assertEquals("user", result.getTableName());
        assertEquals("Multi-tenant routing: tenant-a", result.getReason());
    }

    @Test
    void testTableMapping() {
        // Given
        RoutingContext context = RoutingContext.builder()
                .tableName("user")
                .operationType(RoutingContext.OperationType.SELECT)
                .build();
        
        Map<String, String> tableMappings = new HashMap<>();
        tableMappings.put("user", "t_user");
        
        when(routingConfig.getCustomRules()).thenReturn(null);
        when(routingConfig.getReadWriteSplit()).thenReturn(null);
        when(routingConfig.getSharding()).thenReturn(null);
        when(routingConfig.getMultiTenant()).thenReturn(null);
        when(routingConfig.getTableMappings()).thenReturn(tableMappings);
        
        // When
        RoutingEngine.RoutingResult result = routingEngine.route(context);
        
        // Then
        assertNotNull(result);
        assertEquals("main", result.getDataSource());
        assertEquals("t_user", result.getTableName());
        assertEquals("Default routing", result.getReason());
    }

    @Test
    void testSpelExpressionError() {
        // Given
        RoutingContext context = RoutingContext.builder()
                .tableName("user")
                .operationType(RoutingContext.OperationType.SELECT)
                .parameter("userType", "VIP")
                .build();
        
        RoutingRuleConfig.CustomRoutingRule customRule = new RoutingRuleConfig.CustomRoutingRule();
        customRule.setName("error-rule");
        customRule.setCondition("#{invalid.expression}");
        customRule.setDataSource("vip_db");
        customRule.setPriority(100);
        customRule.setEnabled(true);
        
        when(routingConfig.getCustomRules()).thenReturn(Arrays.asList(customRule));
        when(spelEvaluator.evaluateCondition(eq("#{invalid.expression}"), any(RoutingContext.class)))
                .thenThrow(new RuntimeException("SpEL evaluation error"));
        
        // When
        RoutingEngine.RoutingResult result = routingEngine.route(context);
        
        // Then - 应该回退到默认路由
        assertNotNull(result);
        assertEquals("main", result.getDataSource());
        assertEquals("Default routing", result.getReason());
    }

    @Test
    void testGetAvailableDataSources() {
        // Given
        when(routingConfig.getDefaultDataSource()).thenReturn("main");
        
        Map<String, String> aliases = new HashMap<>();
        aliases.put("primary", "main");
        aliases.put("secondary", "backup");
        when(routingConfig.getAliases()).thenReturn(aliases);
        
        RoutingRuleConfig.ReadWriteSplitConfig rwConfig = new RoutingRuleConfig.ReadWriteSplitConfig();
        rwConfig.setMasterDataSource("master");
        rwConfig.setSlaveDataSources(Arrays.asList("slave1", "slave2"));
        when(routingConfig.getReadWriteSplit()).thenReturn(rwConfig);
        
        // When
        Set<String> dataSources = routingEngine.getAvailableDataSources();
        
        // Then
        assertNotNull(dataSources);
        assertTrue(dataSources.contains("main"));
        assertTrue(dataSources.contains("backup"));
        assertTrue(dataSources.contains("master"));
        assertTrue(dataSources.contains("slave1"));
        assertTrue(dataSources.contains("slave2"));
    }

    @Test
    void testEngineStatus() {
        // Given
        when(routingConfig.getCustomRules()).thenReturn(Arrays.asList(
                new RoutingRuleConfig.CustomRoutingRule(),
                new RoutingRuleConfig.CustomRoutingRule()
        ));
        
        RoutingRuleConfig.ReadWriteSplitConfig rwConfig = new RoutingRuleConfig.ReadWriteSplitConfig();
        rwConfig.setEnabled(true);
        when(routingConfig.getReadWriteSplit()).thenReturn(rwConfig);
        
        RoutingRuleConfig.MultiTenantConfig tenantConfig = new RoutingRuleConfig.MultiTenantConfig();
        tenantConfig.setEnabled(true);
        when(routingConfig.getMultiTenant()).thenReturn(tenantConfig);
        
        RoutingRuleConfig.ShardingConfig shardingConfig = new RoutingRuleConfig.ShardingConfig();
        shardingConfig.setEnabled(true);
        Map<String, RoutingRuleConfig.ShardingConfig> shardingMap = new HashMap<>();
        shardingMap.put("user", shardingConfig);
        when(routingConfig.getSharding()).thenReturn(shardingMap);
        
        // When & Then
        assertEquals(2, routingEngine.getCustomRulesCount());
        assertTrue(routingEngine.isReadWriteSplitEnabled());
        assertTrue(routingEngine.isMultiTenantEnabled());
        assertTrue(routingEngine.isShardingEnabled());
    }
}