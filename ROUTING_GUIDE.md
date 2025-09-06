# 智能路由引擎使用指南

本指南介绍如何使用Spring Data JDBC ORM框架的智能路由引擎功能，实现基于规则的动态数据源路由。

## 概述

智能路由引擎提供了以下核心功能：

- **基于规则的数据源选择**：支持多种条件匹配规则
- **SpEL表达式支持**：动态表达式计算和路由决策
- **声明式路由注解**：通过注解简化路由配置
- **多租户支持**：基于租户信息的数据源路由
- **读写分离**：自动识别操作类型并路由到相应数据源
- **缓存和监控**：路由结果缓存和性能监控

## 快速开始

### 1. 启用路由功能

在配置文件中启用路由功能：

```yaml
flexdata:
  routing:
    enabled: true
    cache:
      enabled: true
      ttl: 300
    monitoring:
      enabled: true
    health-check:
      enabled: true
```

### 2. 配置数据源

```yaml
spring:
  datasource:
    primary:
      url: jdbc:mysql://localhost:3306/main_db
      username: root
      password: password
    read-replica:
      url: jdbc:mysql://localhost:3306/read_db
      username: readonly
      password: password
    tenant-a:
      url: jdbc:mysql://localhost:3306/tenant_a_db
      username: tenant_a
      password: password
```

### 3. 配置路由规则

```yaml
flexdata:
  routing:
    rules:
      # 读写分离规则
      - name: "read-write-split"
        priority: 100
        condition: "operationType == 'SELECT'"
        dataSource: "read-replica"
        enabled: true
      
      # 多租户路由规则
      - name: "tenant-routing"
        priority: 200
        condition: "headers['tenant-id'] != null"
        dataSource: "#{headers['tenant-id']}-db"
        enabled: true
      
      # 表级路由规则
      - name: "user-table-routing"
        priority: 150
        tableName: "user"
        condition: "parameters['userId'] > 1000"
        dataSource: "vip-db"
        enabled: true
    
    # 多租户配置
    multiTenant:
      enabled: true
      tenantHeader: "X-Tenant-ID"
      defaultTenant: "default"
      tenants:
        - tenantId: "tenant-a"
          dataSource: "tenant-a"
        - tenantId: "tenant-b"
          dataSource: "tenant-b"
    
    # 自定义规则
    customRules:
      - name: "vip-user-rule"
        expression: "parameters['userType'] == 'VIP'"
        dataSource: "vip-db"
        priority: 300
```

## 核心功能

### 1. 基于规则的路由

#### 条件表达式

路由引擎支持多种条件表达式：

```yaml
# 基于操作类型
condition: "operationType == 'SELECT'"

# 基于表名
condition: "tableName == 'user'"

# 基于参数
condition: "parameters['userId'] > 1000"

# 基于HTTP头
condition: "headers['tenant-id'] == 'tenant-a'"

# 复合条件
condition: "operationType == 'SELECT' && tableName == 'user'"
```

#### 动态数据源表达式

```yaml
# 静态数据源
dataSource: "read-replica"

# 基于参数的动态数据源
dataSource: "#{parameters['region']}-db"

# 基于头信息的动态数据源
dataSource: "#{headers['tenant-id']}-db"

# 复杂表达式
dataSource: "#{parameters['userId'] > 1000 ? 'vip-db' : 'normal-db'}"

# 使用SpEL工具类函数
dataSource: "#{T(io.flexdata.spring.orm.routing.SpelExpressionEvaluator.SpelUtils).isEmpty(parameters['region']) ? 'default-db' : parameters['region'] + '-db'}"

# 时间相关路由
dataSource: "#{T(io.flexdata.spring.orm.routing.SpelExpressionEvaluator.SpelUtils).currentHour() < 18 ? 'day-db' : 'night-db'}"
```

### 2. SpEL工具类函数

路由引擎提供了丰富的SpEL工具类函数，可以在路由表达式中使用：

#### 字符串操作函数

```yaml
# 检查字符串是否为空
condition: "T(io.flexdata.spring.orm.routing.SpelExpressionEvaluator.SpelUtils).isEmpty(parameters['region'])"

# 检查字符串是否不为空
condition: "T(io.flexdata.spring.orm.routing.SpelExpressionEvaluator.SpelUtils).isNotEmpty(parameters['userId'])"

# 字符串包含检查
condition: "T(io.flexdata.spring.orm.routing.SpelExpressionEvaluator.SpelUtils).contains(parameters['userType'], 'VIP')"

# 字符串开始/结束检查
condition: "T(io.flexdata.spring.orm.routing.SpelExpressionEvaluator.SpelUtils).startsWith(parameters['tableName'], 'user_')"
condition: "T(io.flexdata.spring.orm.routing.SpelExpressionEvaluator.SpelUtils).endsWith(parameters['tableName'], '_2024')"

# 正则表达式匹配
condition: "T(io.flexdata.spring.orm.routing.SpelExpressionEvaluator.SpelUtils).matches(parameters['phone'], '^1[3-9]\\d{9}$')"
```

#### 数值操作函数

```yaml
# 类型转换
condition: "T(io.flexdata.spring.orm.routing.SpelExpressionEvaluator.SpelUtils).toInt(parameters['userId']) > 1000"
condition: "T(io.flexdata.spring.orm.routing.SpelExpressionEvaluator.SpelUtils).toLong(parameters['timestamp']) > 1640995200000"

# 范围检查
condition: "T(io.flexdata.spring.orm.routing.SpelExpressionEvaluator.SpelUtils).inRange(T(io.flexdata.spring.orm.routing.SpelExpressionEvaluator.SpelUtils).toInt(parameters['userId']), 1000, 9999)"

# 哈希取模
dataSource: "shard_#{T(io.flexdata.spring.orm.routing.SpelExpressionEvaluator.SpelUtils).hashMod(parameters['userId'], 4)}"
dataSource: "shard_#{T(io.flexdata.spring.orm.routing.SpelExpressionEvaluator.SpelUtils).stringHashMod(parameters['username'], 4)}"
```

#### 时间相关函数

```yaml
# 获取当前时间戳
condition: "T(io.flexdata.spring.orm.routing.SpelExpressionEvaluator.SpelUtils).currentTimeMillis() > parameters['expireTime']"

# 获取当前日期
dataSource: "log_#{T(io.flexdata.spring.orm.routing.SpelExpressionEvaluator.SpelUtils).currentDate()}"

# 获取当前小时（0-23）
dataSource: "#{T(io.flexdata.spring.orm.routing.SpelExpressionEvaluator.SpelUtils).currentHour() < 12 ? 'morning-db' : 'afternoon-db'}"
```

#### 集合操作函数

```yaml
# 检查Map是否包含键
condition: "T(io.flexdata.spring.orm.routing.SpelExpressionEvaluator.SpelUtils).hasKey(parameters, 'tenantId')"

# 获取Map值
dataSource: "#{T(io.flexdata.spring.orm.routing.SpelExpressionEvaluator.SpelUtils).getStringValue(parameters, 'region', 'default')}-db"

# 检查集合是否包含任意元素
condition: "T(io.flexdata.spring.orm.routing.SpelExpressionEvaluator.SpelUtils).containsAny(parameters['roles'], {'admin', 'vip'})"
```

#### 其他工具函数

```yaml
# 对象相等比较
condition: "T(io.flexdata.spring.orm.routing.SpelExpressionEvaluator.SpelUtils).equals(parameters['status'], 'active')"

# 随机数生成
dataSource: "shard_#{T(io.flexdata.spring.orm.routing.SpelExpressionEvaluator.SpelUtils).random(4)}"
```

### 3. 声明式路由注解

#### @AutoRouting 注解

```java
@Service
public class UserService {
    
    @AutoRouting
    public List<User> findUsers() {
        // 自动根据配置的规则进行路由
        return userRepository.findAll();
    }
}
```

#### @RouteDataSource 注解

```java
@Service
public class UserService {
    
    // 指定固定数据源
    @RouteDataSource("read-replica")
    public List<User> findAllUsers() {
        return userRepository.findAll();
    }
    
    // 使用SpEL表达式
    @RouteDataSource("#{#userId > 1000 ? 'vip-db' : 'normal-db'}")
    public User findUser(@Param("userId") Long userId) {
        return userRepository.findById(userId);
    }
}
```

#### @RouteTable 注解

```java
@Service
public class UserService {
    
    // 指定表名路由
    @RouteTable("user_vip")
    public List<User> findVipUsers() {
        return userRepository.findAll();
    }
    
    // 动态表名
    @RouteTable("user_#{#year}")
    public List<User> findUsersByYear(@Param("year") int year) {
        return userRepository.findAll();
    }
}
```

### 4. 编程式路由

#### 使用RoutingContext

```java
@Service
public class UserService {
    
    @Autowired
    private RoutingEngine routingEngine;
    
    public List<User> findUsers(String tenantId) {
        RoutingContext context = RoutingContext.builder()
                .tableName("user")
                .operationType(RoutingContext.OperationType.SELECT)
                .header("tenant-id", tenantId)
                .build();
        
        String dataSource = routingEngine.route(context);
        
        return DataSourceContext.executeWithDataSource(dataSource, () -> {
            return userRepository.findAll();
        });
    }
}
```

#### 直接使用DataSourceContext

```java
@Service
public class UserService {
    
    public List<User> findUsersFromReadReplica() {
        return DataSourceContext.executeWithDataSource("read-replica", () -> {
            return userRepository.findAll();
        });
    }
}
```

## 高级功能

### 1. 多租户支持

#### 配置多租户

```yaml
flexdata:
  routing:
    multiTenant:
      enabled: true
      tenantHeader: "X-Tenant-ID"
      defaultTenant: "default"
      tenants:
        - tenantId: "tenant-a"
          dataSource: "tenant-a-db"
          description: "租户A数据库"
        - tenantId: "tenant-b"
          dataSource: "tenant-b-db"
          description: "租户B数据库"
```

#### 使用多租户路由

```java
@RestController
public class UserController {
    
    @GetMapping("/users")
    @AutoRouting
    public List<User> getUsers(@RequestHeader("X-Tenant-ID") String tenantId) {
        // 自动根据租户ID路由到对应数据源
        return userService.findAll();
    }
}
```

### 2. 读写分离

#### 配置读写分离规则

```yaml
flexdata:
  routing:
    rules:
      - name: "read-operations"
        priority: 100
        condition: "operationType in {'SELECT'}"
        dataSource: "read-replica"
        enabled: true
      
      - name: "write-operations"
        priority: 100
        condition: "operationType in {'INSERT', 'UPDATE', 'DELETE'}"
        dataSource: "primary"
        enabled: true
```

#### 自动读写分离

```java
@Service
public class UserService {
    
    @AutoRouting
    public List<User> findUsers() {
        // 自动路由到读库
        return userRepository.findAll();
    }
    
    @AutoRouting
    public User saveUser(User user) {
        // 自动路由到写库
        return userRepository.save(user);
    }
}
```

### 3. 缓存支持

#### 启用路由缓存

```yaml
flexdata:
  routing:
    cache:
      enabled: true
      ttl: 300  # 缓存时间（秒）
      maxSize: 1000  # 最大缓存条目数
```

#### 缓存管理

```java
@Service
public class RoutingService {
    
    @Autowired
    private RoutingCacheManager cacheManager;
    
    public void clearCache() {
        cacheManager.clearAll();
    }
    
    public void clearCacheForTable(String tableName) {
        cacheManager.clearByTable(tableName);
    }
}
```

### 4. 监控和健康检查

#### 启用监控

```yaml
flexdata:
  routing:
    monitoring:
      enabled: true
    health-check:
      enabled: true
```

#### 监控指标

路由引擎提供以下监控指标：

- `routing.requests.total`: 总路由请求数
- `routing.errors.total`: 路由错误数
- `routing.cache.hits.total`: 缓存命中数
- `routing.cache.misses.total`: 缓存未命中数
- `routing.duration`: 路由耗时

#### 健康检查端点

```bash
# 检查路由引擎状态
GET /actuator/health/routing

# 获取路由统计信息
GET /actuator/routing/stats
```

## 最佳实践

### 1. 规则优先级设计

```yaml
# 建议的优先级分配
flexdata:
  routing:
    rules:
      # 高优先级：特殊业务规则
      - name: "vip-user-rule"
        priority: 300
        condition: "parameters['userType'] == 'VIP'"
        dataSource: "vip-db"
      
      # 中优先级：多租户规则
      - name: "tenant-routing"
        priority: 200
        condition: "headers['tenant-id'] != null"
        dataSource: "#{headers['tenant-id']}-db"
      
      # 低优先级：读写分离
      - name: "read-write-split"
        priority: 100
        condition: "operationType == 'SELECT'"
        dataSource: "read-replica"
```

### 2. 性能优化

```yaml
# 启用缓存减少路由计算开销
flexdata:
  routing:
    cache:
      enabled: true
      ttl: 300
      maxSize: 1000
    
    # 预编译SpEL表达式
    precompileExpressions: true
```

### 3. 错误处理

```java
@Service
public class UserService {
    
    @Autowired
    private RoutingEngine routingEngine;
    
    public List<User> findUsersWithFallback(String tenantId) {
        try {
            RoutingContext context = RoutingContext.builder()
                    .tableName("user")
                    .operationType(RoutingContext.OperationType.SELECT)
                    .header("tenant-id", tenantId)
                    .build();
            
            String dataSource = routingEngine.route(context);
            
            return DataSourceContext.executeWithDataSource(dataSource, () -> {
                return userRepository.findAll();
            });
        } catch (RoutingException e) {
            // 降级到默认数据源
            log.warn("路由失败，使用默认数据源", e);
            return userRepository.findAll();
        }
    }
}
```

### 4. 测试建议

#### 基本路由测试

```java
@SpringBootTest
class RoutingTest {
    
    @Autowired
    private RoutingEngine routingEngine;
    
    @Test
    void testTenantRouting() {
        RoutingContext context = RoutingContext.builder()
                .tableName("user")
                .operationType(RoutingContext.OperationType.SELECT)
                .header("tenant-id", "tenant-a")
                .build();
        
        String dataSource = routingEngine.route(context);
        assertEquals("tenant-a-db", dataSource);
    }
}
```

#### SpEL表达式测试

```java
@SpringBootTest
class SpelExpressionTest {
    
    @Autowired
    private SpelExpressionEvaluator spelEvaluator;
    
    @Test
    void testSpelUtilsFunctions() {
        RoutingContext context = RoutingContext.builder()
                .tableName("user")
                .parameter("userId", "1001")
                .parameter("region", "")
                .build();
        
        // 测试isEmpty函数
        Boolean isEmpty = spelEvaluator.evaluate(
            "T(io.flexdata.spring.orm.routing.SpelExpressionEvaluator.SpelUtils).isEmpty(#region)", 
            context, Boolean.class);
        assertTrue(isEmpty);
        
        // 测试toInt函数
        Integer userId = spelEvaluator.evaluate(
            "T(io.flexdata.spring.orm.routing.SpelExpressionEvaluator.SpelUtils).toInt(#userId)", 
            context, Integer.class);
        assertEquals(1001, userId);
        
        // 测试inRange函数
        Boolean inRange = spelEvaluator.evaluate(
            "T(io.flexdata.spring.orm.routing.SpelExpressionEvaluator.SpelUtils).inRange(" +
            "T(io.flexdata.spring.orm.routing.SpelExpressionEvaluator.SpelUtils).toInt(#userId), 1000, 9999)", 
            context, Boolean.class);
        assertTrue(inRange);
    }
    
    @Test
    void testVariableAccess() {
        RoutingContext context = RoutingContext.builder()
                .tableName("user")
                .build();
        
        // 测试now变量（注意使用#now语法）
        Boolean isCurrentYear = spelEvaluator.evaluate(
            "#{#now.year > 2020}", context, Boolean.class);
        assertTrue(isCurrentYear);
        
        // 测试random变量（注意使用#random语法）
        Integer randomValue = spelEvaluator.evaluate(
            "#{#random.nextInt(100)}", context, Integer.class);
        assertTrue(randomValue >= 0 && randomValue < 100);
    }
}
```

## 故障排除

### 常见问题

1. **路由规则不生效**
   - 检查规则优先级设置
   - 验证条件表达式语法
   - 确认规则已启用

2. **SpEL表达式错误**
   - 检查表达式语法
   - 验证上下文变量是否存在
   - 查看详细错误日志
   - 注意变量访问语法：使用`#variableName`而不是`variableName`
   - 确保SpEL工具类方法为静态方法：使用`T(ClassName).methodName()`
   - 检查方法参数类型和数量是否正确

3. **数据源连接失败**
   - 检查数据源配置
   - 验证数据库连接
   - 查看健康检查状态

### 调试技巧

```yaml
# 启用调试日志
logging:
  level:
    io.flexdata.spring.orm.routing: DEBUG
```

```java
// 获取路由决策详情
RoutingContext context = RoutingContext.builder()
        .tableName("user")
        .operationType(RoutingContext.OperationType.SELECT)
        .build();

RoutingResult result = routingEngine.routeWithDetails(context);
log.info("路由结果: {}, 匹配规则: {}", result.getDataSource(), result.getMatchedRule());
```

## 总结

智能路由引擎为Spring Data JDBC ORM框架提供了强大的数据源路由能力，支持多种路由策略和使用方式。通过合理配置和使用，可以实现：

- 灵活的多数据源管理
- 高效的读写分离
- 完善的多租户支持
- 优秀的性能和监控

建议在实际使用中根据业务需求选择合适的路由策略，并充分利用缓存和监控功能来优化性能。