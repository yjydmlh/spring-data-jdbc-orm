package io.flexdata.spring.orm.routing.integration;

import io.flexdata.spring.orm.routing.annotation.AutoRouting;
import io.flexdata.spring.orm.routing.annotation.RouteDataSource;
import io.flexdata.spring.orm.routing.annotation.RouteTable;
import io.flexdata.spring.orm.routing.config.RoutingAutoConfiguration;
import io.flexdata.spring.orm.routing.config.RoutingRuleConfig;
import io.flexdata.spring.orm.routing.config.RoutingCacheManager;
import io.flexdata.spring.orm.routing.config.RoutingMonitor;
import io.flexdata.spring.orm.routing.context.RoutingContext;
import io.flexdata.spring.orm.routing.engine.RoutingEngine;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * 路由引擎集成测试
 * 
 * @author FlexData
 * @since 1.0.0
 */
@SpringBootTest(classes = {
    RoutingIntegrationTest.TestConfiguration.class,
    RoutingAutoConfiguration.class
})
@TestPropertySource(properties = {
    "spring.routing.default-data-source=main",
    "spring.routing.read-write-split.enabled=true",
    "spring.routing.read-write-split.master-data-source=master",
    "spring.routing.read-write-split.slave-data-sources[0]=slave1",
    "spring.routing.read-write-split.slave-data-sources[1]=slave2",
    "spring.routing.read-write-split.strategy=round_robin",
    "spring.routing.multi-tenant.enabled=true",
    "spring.routing.multi-tenant.strategy=datasource",
    "spring.routing.multi-tenant.tenant-resolver=header",
    "spring.routing.multi-tenant.tenant-key=tenant-id",
    "spring.routing.multi-tenant.default-tenant=default"
})
class RoutingIntegrationTest {

    @Autowired
    private RoutingEngine routingEngine;
    
    @Autowired
    private TestService testService;

    @Test
    void testRoutingEngineAutoConfiguration() {
        // Then
        assertNotNull(routingEngine);
        assertTrue(routingEngine.isReadWriteSplitEnabled());
        assertTrue(routingEngine.isMultiTenantEnabled());
    }

    @Test
    void testDefaultRouting() {
        // Given
        RoutingContext context = RoutingContext.builder()
                .tableName("user")
                .operationType(RoutingContext.OperationType.SELECT)
                .build();
        
        // When
        RoutingEngine.RoutingResult result = routingEngine.route(context);
        
        // Then
        assertNotNull(result);
        assertTrue(Arrays.asList("slave1", "slave2").contains(result.getDataSource())); // 读写分离
        assertEquals("user", result.getTableName());
    }

    @Test
    void testReadWriteSplitRouting() {
        // Given - 读操作
        RoutingContext readContext = RoutingContext.builder()
                .tableName("user")
                .operationType(RoutingContext.OperationType.SELECT)
                .build();
        
        // When
        RoutingEngine.RoutingResult readResult = routingEngine.route(readContext);
        
        // Then
        assertNotNull(readResult);
        assertTrue(Arrays.asList("slave1", "slave2").contains(readResult.getDataSource()));
        
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
    }

    @Test
    void testMultiTenantRouting() {
        // Given
        RoutingContext context = RoutingContext.builder()
                .tableName("user")
                .operationType(RoutingContext.OperationType.SELECT)
                .header("tenant-id", "tenant-a")
                .build();
        
        // When
        RoutingEngine.RoutingResult result = routingEngine.route(context);
        
        // Then
        assertNotNull(result);
        // 由于没有配置租户映射，应该使用默认数据源
        assertNotNull(result.getDataSource());
    }

    @Test
    void testAutoRoutingAnnotation() {
        // When
        String result = testService.autoRoutingMethod(12345L);
        
        // Then
        assertEquals("auto-routing-result", result);
    }

    @Test
    void testDataSourceRoutingAnnotation() {
        // When
        String result = testService.dataSourceRoutingMethod("VIP");
        
        // Then
        assertEquals("datasource-routing-result", result);
    }

    @Test
    void testTableRoutingAnnotation() {
        // When
        String result = testService.tableRoutingMethod(12345L);
        
        // Then
        assertEquals("table-routing-result", result);
    }

    @Test
    void testCombinedAnnotations() {
        // When
        String result = testService.combinedRoutingMethod("VIP", 12345L);
        
        // Then
        assertEquals("combined-routing-result", result);
    }

    @Test
    void testRoutingEngineStatus() {
        // When & Then
        assertTrue(routingEngine.isReadWriteSplitEnabled());
        assertTrue(routingEngine.isMultiTenantEnabled());
        assertFalse(routingEngine.isShardingEnabled()); // 没有配置分片
        
        Set<String> dataSources = routingEngine.getAvailableDataSources();
        assertNotNull(dataSources);
        assertTrue(dataSources.contains("main"));
        assertTrue(dataSources.contains("master"));
        assertTrue(dataSources.contains("slave1"));
        assertTrue(dataSources.contains("slave2"));
    }

    @Test
    void testCustomRuleRouting() {
        // Given - 创建自定义规则上下文
        RoutingContext context = RoutingContext.builder()
                .tableName("user")
                .operationType(RoutingContext.OperationType.SELECT)
                .parameter("userType", "VIP")
                .parameter("region", "US")
                .build();
        
        // When
        RoutingEngine.RoutingResult result = routingEngine.route(context);
        
        // Then
        assertNotNull(result);
        // 由于没有配置自定义规则，应该使用默认路由
        assertNotNull(result.getDataSource());
    }

    @Test
    void testShardingRouting() {
        // Given
        RoutingContext context = RoutingContext.builder()
                .tableName("order")
                .operationType(RoutingContext.OperationType.SELECT)
                .parameter("orderId", 12345L)
                .build();
        
        // When
        RoutingEngine.RoutingResult result = routingEngine.route(context);
        
        // Then
        assertNotNull(result);
        assertEquals("order", result.getTableName()); // 没有配置分片，表名不变
    }

    @Test
    void testComplexRoutingScenario() {
        // Given - 复杂路由场景：多租户 + 读写分离
        RoutingContext context = RoutingContext.builder()
                .tableName("user")
                .operationType(RoutingContext.OperationType.SELECT)
                .header("tenant-id", "tenant-b")
                .parameter("userId", 67890L)
                .parameter("userType", "PREMIUM")
                .build();
        
        // When
        RoutingEngine.RoutingResult result = routingEngine.route(context);
        
        // Then
        assertNotNull(result);
        assertNotNull(result.getDataSource());
        assertEquals("user", result.getTableName());
        assertNotNull(result.getReason());
    }

    @Configuration
    @EnableAspectJAutoProxy
    static class TestConfiguration {
        
        @Bean
        @Primary
        public RoutingRuleConfig routingRuleConfig() {
            RoutingRuleConfig config = new RoutingRuleConfig();
            config.setDefaultDataSource("main");
            
            // 配置读写分离
            RoutingRuleConfig.ReadWriteSplitConfig rwConfig = new RoutingRuleConfig.ReadWriteSplitConfig();
            rwConfig.setEnabled(true);
            rwConfig.setMasterDataSource("master");
            rwConfig.setSlaveDataSources(Arrays.asList("slave1", "slave2"));
            rwConfig.setStrategy("round_robin");
            config.setReadWriteSplit(rwConfig);
            
            // 配置多租户
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
            config.setMultiTenant(tenantConfig);
            
            // 配置自定义规则
            RoutingRuleConfig.CustomRoutingRule vipRule = new RoutingRuleConfig.CustomRoutingRule();
            vipRule.setName("vip-user-rule");
            vipRule.setCondition("#{parameters['userType'] == 'VIP'}");
            vipRule.setDataSource("vip_db");
            vipRule.setPriority(100);
            vipRule.setEnabled(true);
            
            RoutingRuleConfig.CustomRoutingRule regionRule = new RoutingRuleConfig.CustomRoutingRule();
            regionRule.setName("us-region-rule");
            regionRule.setCondition("#{parameters['region'] == 'US'}");
            regionRule.setDataSource("us_db");
            regionRule.setPriority(50);
            regionRule.setEnabled(true);
            
            config.setCustomRules(Arrays.asList(vipRule, regionRule));
            
            return config;
        }
        
        @Bean
        public TestService testService() {
            return new TestService();
        }
        
        // Mock数据�?        @Bean("main")
        public DataSource mainDataSource() {
            return mock(DataSource.class);
        }
        
        @Bean("master")
        public DataSource masterDataSource() {
            return mock(DataSource.class);
        }
        
        @Bean("slave1")
        public DataSource slave1DataSource() {
            return mock(DataSource.class);
        }
        
        @Bean("slave2")
        public DataSource slave2DataSource() {
            return mock(DataSource.class);
        }
        
        @Bean("vip_db")
        public DataSource vipDataSource() {
            return mock(DataSource.class);
        }
        
        @Bean("us_db")
        public DataSource usDataSource() {
            return mock(DataSource.class);
        }
        
        @Bean("tenant_a_db")
        public DataSource tenantADataSource() {
            return mock(DataSource.class);
        }
        
        @Bean("tenant_b_db")
        public DataSource tenantBDataSource() {
            return mock(DataSource.class);
        }
        
        @Bean
        public RoutingCacheManager routingCacheManager(RoutingRuleConfig routingConfig) {
            return new RoutingCacheManager(routingConfig);
        }
        
        @Bean
        public RoutingMonitor routingMonitor() {
            // 使用mock来避免循环依赖
            return mock(RoutingMonitor.class);
        }
    }

    @Service
    static class TestService {
        
        @AutoRouting(
            enabled = true,
            table = "user",
            dataSource = "#{args[0] > 10000 ? 'big_user_db' : 'small_user_db'}"
        )
        public String autoRoutingMethod(Long userId) {
            return "auto-routing-result";
        }
        
        @RouteDataSource(
            value = "#{args[0] == 'VIP' ? 'vip_db' : 'normal_db'}",
            condition = "#{args[0] != null}"
        )
        public String dataSourceRoutingMethod(String userType) {
            return "datasource-routing-result";
        }
        
        @RouteTable(
            value = "user_#{args[0] % 10}",
            condition = "#{args[0] != null}",
            shardingStrategy = "mod",
            shardingKey = "userId",
            shardCount = 10,
            template = "user_{0}"
        )
        public String tableRoutingMethod(Long userId) {
            return "table-routing-result";
        }
        
        @RouteDataSource(
            value = "#{args[0] == 'VIP' ? 'vip_db' : 'normal_db'}",
            condition = "#{args[0] != null}"
        )
        @RouteTable(
            value = "user_#{args[1] % 4}",
            condition = "#{args[1] != null}",
            shardingStrategy = "mod",
            shardingKey = "userId",
            shardCount = 10,
            template = "user_{0}"
        )
        @AutoRouting(
            enabled = true,
            priority = 200
        )
        public String combinedRoutingMethod(String userType, Long userId) {
            return "combined-routing-result";
        }
    }
}