package io.flexdata.spring.orm.routing.config;

import io.flexdata.spring.orm.routing.engine.RoutingEngine;
import io.flexdata.spring.orm.routing.selector.DataSourceSelector;
import io.flexdata.spring.orm.routing.selector.RuleBasedDataSourceSelector;
import io.flexdata.spring.orm.routing.engine.SpelExpressionEvaluator;
import io.flexdata.spring.orm.routing.aspect.RoutingAspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * 路由自动配置类
 * 自动装配路由引擎和相关组件
 * 
 * @author FlexData
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(RoutingRuleConfig.class)
@EnableAspectJAutoProxy
@ConditionalOnProperty(prefix = "flexdata.routing", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RoutingAutoConfiguration {

    /**
     * SpEL表达式评估器
     */
    @Bean
    @ConditionalOnMissingBean
    public SpelExpressionEvaluator spelExpressionEvaluator() {
        return new SpelExpressionEvaluator();
    }

    /**
     * 基于规则的数据源选择器
     */
    @Bean
    @ConditionalOnMissingBean
    public RuleBasedDataSourceSelector ruleBasedDataSourceSelector(
            SpelExpressionEvaluator spelEvaluator) {
        RuleBasedDataSourceSelector selector = new RuleBasedDataSourceSelector();
        // 通过反射设置SpelExpressionEvaluator
        try {
            java.lang.reflect.Field field = RuleBasedDataSourceSelector.class.getDeclaredField("spelEvaluator");
            field.setAccessible(true);
            field.set(selector, spelEvaluator);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set spelEvaluator", e);
        }
        return selector;
    }

    /**
     * 路由引擎
     */
    @Bean
    @ConditionalOnMissingBean
    public RoutingEngine routingEngine(
            RoutingRuleConfig routingConfig,
            SpelExpressionEvaluator spelEvaluator,
            List<DataSourceSelector> selectors,
            RoutingCacheManager cacheManager,
            RoutingMonitor routingMonitor) {
        RoutingEngine engine = new RoutingEngine(routingConfig, spelEvaluator, selectors);
        engine.setCacheManager(cacheManager);
        engine.setRoutingMonitor(routingMonitor);
        return engine;
    }

    /**
     * 路由切面
     */
    @Bean
    @ConditionalOnMissingBean
    public RoutingAspect routingAspect(
            RoutingEngine routingEngine,
            SpelExpressionEvaluator spelEvaluator) {
        return new RoutingAspect(routingEngine, spelEvaluator);
    }



    /**
     * 路由监控器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "flexdata.routing.monitoring", name = "enabled", havingValue = "true")
    public RoutingMonitor routingMonitor(RoutingEngine routingEngine) {
        return new RoutingMonitor(routingEngine);
    }

    /**
     * 路由缓存管理器
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "flexdata.routing.cache", name = "enabled", havingValue = "true")
    public RoutingCacheManager routingCacheManager(RoutingRuleConfig routingConfig) {
        return new RoutingCacheManager(routingConfig);
    }
}