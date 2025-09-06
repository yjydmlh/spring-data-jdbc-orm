package io.flexdata.spring.orm.routing.aspect;

import io.flexdata.spring.orm.routing.annotation.AutoRouting;
import io.flexdata.spring.orm.routing.annotation.RouteDataSource;
import io.flexdata.spring.orm.routing.annotation.RouteTable;
import io.flexdata.spring.orm.routing.context.RoutingContext;
import io.flexdata.spring.orm.routing.engine.RoutingEngine;
import io.flexdata.spring.orm.routing.engine.SpelExpressionEvaluator;
import io.flexdata.spring.orm.core.datasource.DataSourceContext;
import io.flexdata.spring.orm.core.table.TableContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 路由切面
 * 实现基于注解的声明式路由功能
 */
@Aspect
@Component
@Order(100) // 确保在事务切面之前执行
public class RoutingAspect {

    private final RoutingEngine routingEngine;
    private final SpelExpressionEvaluator spelEvaluator;

    /**
     * 构造函数
     */
    public RoutingAspect(RoutingEngine routingEngine, SpelExpressionEvaluator spelEvaluator) {
        this.routingEngine = routingEngine;
        this.spelEvaluator = spelEvaluator;
    }

    /**
     * 默认构造函数（用于测试）
     */
    public RoutingAspect() {
        this.routingEngine = null;
        this.spelEvaluator = null;
    }

    /**
     * 路由结果缓存
     */
    private final Map<String, CachedRoutingResult> routingCache = new ConcurrentHashMap<>();

    /**
     * 缓存的路由结果
     */
    private static class CachedRoutingResult {
        private final String dataSource;
        private final String tableName;
        private final long expireTime;

        public CachedRoutingResult(String dataSource, String tableName, long expireTime) {
            this.dataSource = dataSource;
            this.tableName = tableName;
            this.expireTime = expireTime;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expireTime;
        }

        public String getDataSource() {
            return dataSource;
        }

        public String getTableName() {
            return tableName;
        }
    }

    /**
     * 自动路由切点
     */
    @Around("@annotation(autoRouting) || @within(autoRouting)")
    public Object aroundAutoRouting(ProceedingJoinPoint joinPoint, AutoRouting autoRouting) throws Throwable {
        if (autoRouting == null) {
            // 从类级别获取注解
            autoRouting = AnnotationUtils.findAnnotation(joinPoint.getTarget().getClass(), AutoRouting.class);
        }

        if (autoRouting == null || !autoRouting.enabled()) {
            return joinPoint.proceed();
        }

        return executeWithRouting(joinPoint, autoRouting, null, null);
    }

    /**
     * 数据源路由切点
     */
    @Around("@annotation(routeDataSource) || @within(routeDataSource)")
    public Object aroundDataSourceRouting(ProceedingJoinPoint joinPoint, RouteDataSource routeDataSource) throws Throwable {
        if (routeDataSource == null) {
            // 从类级别获取注解
            routeDataSource = AnnotationUtils.findAnnotation(joinPoint.getTarget().getClass(), RouteDataSource.class);
        }

        if (routeDataSource == null) {
            return joinPoint.proceed();
        }

        return executeWithRouting(joinPoint, null, routeDataSource, null);
    }

    /**
     * 表路由切点
     */
    @Around("@annotation(routeTable) || @within(routeTable)")
    public Object aroundTableRouting(ProceedingJoinPoint joinPoint, RouteTable routeTable) throws Throwable {
        if (routeTable == null) {
            // 从类级别获取注解
            routeTable = AnnotationUtils.findAnnotation(joinPoint.getTarget().getClass(), RouteTable.class);
        }

        if (routeTable == null) {
            return joinPoint.proceed();
        }

        return executeWithRouting(joinPoint, null, null, routeTable);
    }

    /**
     * 执行带路由的方法调用
     */
    private Object executeWithRouting(ProceedingJoinPoint joinPoint, 
                                    AutoRouting autoRouting,
                                    RouteDataSource routeDataSource, 
                                    RouteTable routeTable) throws Throwable {
        
        // 构建路由上下文
        RoutingContext context = buildRoutingContext(joinPoint);
        
        // 检查缓存
        String cacheKey = null;
        if (autoRouting != null && autoRouting.cache()) {
            cacheKey = buildCacheKey(autoRouting, context);
            CachedRoutingResult cached = routingCache.get(cacheKey);
            if (cached != null && !cached.isExpired()) {
                return executeWithSpecificRouting(joinPoint, cached.getDataSource(), cached.getTableName());
            }
        }
        
        String targetDataSource = null;
        String targetTable = null;
        
        // 处理自动路由
        if (autoRouting != null) {
            RoutingEngine.RoutingResult result = handleAutoRouting(autoRouting, context);
            if (result != null) {
                targetDataSource = result.getDataSource();
                targetTable = result.getTableName();
            }
        }
        
        // 处理数据源路由注解
        if (routeDataSource != null) {
            String dataSource = handleDataSourceRouting(routeDataSource, context);
            if (StringUtils.hasText(dataSource)) {
                targetDataSource = dataSource;
            }
        }
        
        // 处理表路由注解
        if (routeTable != null) {
            String table = handleTableRouting(routeTable, context);
            if (StringUtils.hasText(table)) {
                targetTable = table;
            }
        }
        
        // 缓存结果
        if (cacheKey != null && autoRouting != null) {
            long expireTime = System.currentTimeMillis() + (autoRouting.cacheExpire() * 1000L);
            routingCache.put(cacheKey, new CachedRoutingResult(targetDataSource, targetTable, expireTime));
        }
        
        return executeWithSpecificRouting(joinPoint, targetDataSource, targetTable);
    }

    /**
     * 处理自动路由
     */
    private RoutingEngine.RoutingResult handleAutoRouting(AutoRouting autoRouting, RoutingContext context) {
        try {
            // 检查依赖是否可用
            if (spelEvaluator == null || routingEngine == null) {
                return null;
            }
            
            // 检查条件
            if (StringUtils.hasText(autoRouting.condition())) {
                boolean conditionMet = spelEvaluator.evaluateCondition(autoRouting.condition(), context);
                if (!conditionMet) {
                    return null;
                }
            }
            
            // 如果指定了数据源或表名，直接使用
            if (StringUtils.hasText(autoRouting.dataSource()) || StringUtils.hasText(autoRouting.table())) {
                String dataSource = StringUtils.hasText(autoRouting.dataSource()) ? 
                    spelEvaluator.evaluateExpression(autoRouting.dataSource(), context, String.class) : null;
                String table = StringUtils.hasText(autoRouting.table()) ? 
                    spelEvaluator.evaluateExpression(autoRouting.table(), context, String.class) : context.getTableName();
                return new RoutingEngine.RoutingResult(dataSource, table, "Annotation routing");
            }
            
            // 使用路由引擎进行自动路由
            return routingEngine.route(context);
        } catch (Exception e) {
            System.err.println("Error in auto routing: " + e.getMessage());
            return null;
        }
    }

    /**
     * 处理数据源路由
     */
    private String handleDataSourceRouting(RouteDataSource routeDataSource, RoutingContext context) {
        try {
            // 检查依赖是否可用
            if (spelEvaluator == null) {
                return null;
            }
            
            // 检查条件
            if (StringUtils.hasText(routeDataSource.condition())) {
                boolean conditionMet = spelEvaluator.evaluateCondition(routeDataSource.condition(), context);
                if (!conditionMet) {
                    return null;
                }
            }
            
            // 评估数据源表达式
            String dataSource = spelEvaluator.evaluateExpression(routeDataSource.value(), context, String.class);
            
            // 如果主数据源为空且有备用数据源，使用备用数据源
            if (!StringUtils.hasText(dataSource) && StringUtils.hasText(routeDataSource.fallback())) {
                dataSource = spelEvaluator.evaluateExpression(routeDataSource.fallback(), context, String.class);
            }
            
            return dataSource;
        } catch (Exception e) {
            System.err.println("Error in dataSource routing: " + e.getMessage());
            
            // 尝试使用备用数据源
            if (StringUtils.hasText(routeDataSource.fallback()) && spelEvaluator != null) {
                try {
                    return spelEvaluator.evaluateExpression(routeDataSource.fallback(), context, String.class);
                } catch (Exception fallbackException) {
                    System.err.println("Error in fallback dataSource: " + fallbackException.getMessage());
                }
            }
            
            return null;
        }
    }

    /**
     * 处理表路由
     */
    private String handleTableRouting(RouteTable routeTable, RoutingContext context) {
        try {
            // 检查依赖是否可用
            if (spelEvaluator == null) {
                return null;
            }
            
            // 检查条件
            if (StringUtils.hasText(routeTable.condition())) {
                boolean conditionMet = spelEvaluator.evaluateCondition(routeTable.condition(), context);
                if (!conditionMet) {
                    return null;
                }
            }
            
            // 如果指定了分片策略，使用分片逻辑
            if (StringUtils.hasText(routeTable.shardingStrategy()) && StringUtils.hasText(routeTable.shardingKey())) {
                return handleTableSharding(routeTable, context);
            }
            
            // 评估表名表达式
            return spelEvaluator.evaluateExpression(routeTable.value(), context, String.class);
        } catch (Exception e) {
            System.err.println("Error in table routing: " + e.getMessage());
            return null;
        }
    }

    /**
     * 处理表分片
     */
    private String handleTableSharding(RouteTable routeTable, RoutingContext context) {
        Object shardingValue = context.getParameter(routeTable.shardingKey());
        if (shardingValue == null) {
            return null;
        }
        
        String template = StringUtils.hasText(routeTable.template()) ? 
            routeTable.template() : context.getTableName() + "_{0}";
        
        int shardIndex;
        switch (routeTable.shardingStrategy().toLowerCase()) {
            case "mod":
                shardIndex = Math.abs(shardingValue.hashCode()) % routeTable.shardCount();
                break;
            case "hash":
                shardIndex = Math.abs(shardingValue.toString().hashCode()) % routeTable.shardCount();
                break;
            default:
                // 对于其他策略，尝试使用SpEL表达式
                if (spelEvaluator != null) {
                    return spelEvaluator.evaluateExpression(routeTable.value(), context, String.class);
                }
                return null;
        }
        
        return String.format(template, shardIndex);
    }

    /**
     * 执行具体的路由调用
     */
    private Object executeWithSpecificRouting(ProceedingJoinPoint joinPoint, String dataSource, String tableName) throws Throwable {
        // 保存当前上下文
        String originalDataSource = DataSourceContext.getDataSource();
        String originalTable = TableContext.getCurrentTable();
        
        try {
            // 设置路由上下文
            if (StringUtils.hasText(dataSource)) {
                DataSourceContext.setDataSource(dataSource);
            }
            if (StringUtils.hasText(tableName)) {
                TableContext.setTable(tableName);
            }
            
            // 执行目标方法
            return joinPoint.proceed();
        } finally {
            // 恢复原始上下文
            if (originalDataSource != null) {
                DataSourceContext.setDataSource(originalDataSource);
            } else {
                DataSourceContext.clearDataSource();
            }
            
            if (originalTable != null) {
                TableContext.setTable(originalTable);
            } else {
                TableContext.clearTable();
            }
        }
    }

    /**
     * 构建路由上下文
     */
    public RoutingContext buildRoutingContext(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        Parameter[] parameters = method.getParameters();
        
        RoutingContext.Builder builder = RoutingContext.builder();
        
        // 设置操作类型
        RoutingContext.OperationType operationType = determineOperationType(method);
        builder.operationType(operationType);
        
        // 设置方法参数
        Map<String, Object> paramMap = new HashMap<>();
        for (int i = 0; i < parameters.length && i < args.length; i++) {
            paramMap.put(parameters[i].getName(), args[i]);
        }
        builder.parameters(paramMap);
        
        // 设置HTTP请求头（如果在Web环境中）
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                Map<String, String> headers = new HashMap<>();
                Collections.list(request.getHeaderNames()).forEach(name -> 
                    headers.put(name, request.getHeader(name)));
                builder.headers(headers);
            }
        } catch (Exception e) {
            // 忽略非Web环境的异常
        }
        
        // 尝试从注解中获取表名
        AutoRouting autoRouting = AnnotationUtils.findAnnotation(method, AutoRouting.class);
        if (autoRouting != null && StringUtils.hasText(autoRouting.table())) {
            builder.tableName(autoRouting.table());
        } else {
            // 尝试从当前上下文获取表名
            String currentTable = TableContext.getCurrentTable();
            if (StringUtils.hasText(currentTable)) {
                builder.tableName(currentTable);
            }
        }
        
        return builder.build();
    }

    /**
     * 确定操作类型
     */
    private RoutingContext.OperationType determineOperationType(Method method) {
        String methodName = method.getName().toLowerCase();
        
        if (methodName.startsWith("select") || methodName.startsWith("find") || 
            methodName.startsWith("get") || methodName.startsWith("query") ||
            methodName.startsWith("count") || methodName.startsWith("exists")) {
            return RoutingContext.OperationType.SELECT;
        } else if (methodName.startsWith("insert") || methodName.startsWith("save") || methodName.startsWith("add")) {
            if (methodName.contains("batch")) {
                return RoutingContext.OperationType.BATCH_INSERT;
            }
            return RoutingContext.OperationType.INSERT;
        } else if (methodName.startsWith("update") || methodName.startsWith("modify")) {
            if (methodName.contains("batch")) {
                return RoutingContext.OperationType.BATCH_UPDATE;
            }
            return RoutingContext.OperationType.UPDATE;
        } else if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
            if (methodName.contains("batch")) {
                return RoutingContext.OperationType.BATCH_DELETE;
            }
            return RoutingContext.OperationType.DELETE;
        }
        
        // 默认为查询操作
        return RoutingContext.OperationType.SELECT;
    }

    /**
     * 构建缓存键
     */
    private String buildCacheKey(AutoRouting autoRouting, RoutingContext context) {
        if (StringUtils.hasText(autoRouting.cacheKey()) && spelEvaluator != null) {
            try {
                return spelEvaluator.evaluateExpression(autoRouting.cacheKey(), context, String.class);
            } catch (Exception e) {
                System.err.println("Error building cache key: " + e.getMessage());
            }
        }
        
        // 默认缓存键：方法签名 + 参数哈希
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(context.getTableName()).append(":");
        keyBuilder.append(context.getOperationType()).append(":");
        keyBuilder.append(context.getParameters().hashCode());
        
        return keyBuilder.toString();
    }

    /**
     * 清理过期的缓存
     */
    public void cleanExpiredCache() {
        routingCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * 清空所有缓存
     */
    public void clearCache() {
        routingCache.clear();
    }

    /**
     * 获取缓存大小
     */
    public int getCacheSize() {
        return routingCache.size();
    }
}