# 多数据源完整指南

## 1. 功能概述

### 核心特性
- **动态数据源切换**：支持运行时动态切换数据源
- **注解驱动**：通过 `@DataSource` 注解轻松指定数据源
- **编程式切换**：提供 API 进行编程式数据源管理
- **表名动态映射**：支持动态指定表名和数据库名
- **智能路由**：基于规则的自动数据源选择
- **多租户支持**：完善的多租户数据隔离方案

### 适用场景
- 读写分离架构
- 多租户SaaS应用
- 数据分片场景
- 跨数据库数据整合
- 历史数据归档

## 2. 快速开始

### 2.1 添加依赖

```xml
<dependency>
    <groupId>io.flex.data</groupId>
    <artifactId>flex-data-orm</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2.2 配置多数据源

```yaml
spring:
  datasource:
    # 主数据源
    primary:
      url: jdbc:mysql://localhost:3306/primary_db
      username: root
      password: password
      driver-class-name: com.mysql.cj.jdbc.Driver
    
    # 从数据源
    secondary:
      url: jdbc:mysql://localhost:3306/secondary_db
      username: root
      password: password
      driver-class-name: com.mysql.cj.jdbc.Driver
    
    # 第三个数据源
    archive:
      url: jdbc:mysql://localhost:3306/archive_db
      username: root
      password: password
      driver-class-name: com.mysql.cj.jdbc.Driver

# 启用多数据源配置
flex:
  data:
    multi-datasource:
      enabled: true
      default-datasource: primary
```

### 2.3 启用多数据源

在主配置类上添加注解：

```java
@SpringBootApplication
@EnableMultiDataSource
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## 3. 基础配置

### 3.1 数据源配置详解

```yaml
spring:
  datasource:
    # 数据源名称可以自定义
    user-db:
      url: jdbc:mysql://localhost:3306/user_database
      username: ${DB_USER:root}
      password: ${DB_PASSWORD:password}
      driver-class-name: com.mysql.cj.jdbc.Driver
      # 连接池配置
      hikari:
        maximum-pool-size: 20
        minimum-idle: 5
        connection-timeout: 30000
        idle-timeout: 600000
        max-lifetime: 1800000
    
    order-db:
      url: jdbc:postgresql://localhost:5432/order_database
      username: ${DB_USER:postgres}
      password: ${DB_PASSWORD:password}
      driver-class-name: org.postgresql.Driver
      hikari:
        maximum-pool-size: 15
        minimum-idle: 3

flex:
  data:
    multi-datasource:
      enabled: true
      default-datasource: user-db
      # 数据源健康检查
      health-check:
        enabled: true
        interval: 30s
      # 连接池监控
      monitoring:
        enabled: true
```

### 3.2 实体类配置

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String username;
    private String email;
    
    // getters and setters
}

@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long userId;
    private BigDecimal amount;
    
    // getters and setters
}
```

## 4. 高级特性

### 4.1 注解驱动的数据源切换

#### 基础用法

```java
@Repository
public class UserRepository extends BaseRepository<User, Long> {
    
    // 使用指定数据源
    @DataSource("user-db")
    public List<User> findActiveUsers() {
        return query()
            .where(User::getStatus).eq("ACTIVE")
            .list();
    }
    
    // 使用从库进行只读查询
    @DataSource("user-db-slave")
    @Transactional(readOnly = true)
    public List<User> findUsersForReport() {
        return query()
            .select(User::getId, User::getUsername, User::getCreateTime)
            .list();
    }
}
```

#### 方法级别切换

```java
@Service
public class OrderService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    // 主库写入
    @DataSource("order-db")
    @Transactional
    public Order createOrder(Order order) {
        order.setCreateTime(LocalDateTime.now());
        return orderRepository.save(order);
    }
    
    // 从库查询
    @DataSource("order-db-slave")
    @Transactional(readOnly = true)
    public List<Order> getOrderHistory(Long userId) {
        return orderRepository.query()
            .where(Order::getUserId).eq(userId)
            .orderBy(Order::getCreateTime).desc()
            .list();
    }
    
    // 归档库查询历史数据
    @DataSource("archive-db")
    @Transactional(readOnly = true)
    public List<Order> getArchivedOrders(Long userId, LocalDate startDate) {
        return orderRepository.query()
            .where(Order::getUserId).eq(userId)
            .and(Order::getCreateTime).ge(startDate)
            .list();
    }
}
```

### 4.2 编程式数据源切换

#### 使用DataSourceContext

```java
@Service
public class ReportService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private OrderRepository orderRepository;
    
    public ReportData generateReport(String reportType) {
        try {
            // 切换到报表专用数据源
            DataSourceContext.setDataSource("report-db");
            
            List<User> users = userRepository.findAll();
            List<Order> orders = orderRepository.findAll();
            
            return new ReportData(users, orders);
        } finally {
            // 清理上下文
            DataSourceContext.clear();
        }
    }
    
    // 使用executeWithDataSource简化代码
    public List<Order> getOrdersFromArchive(Long userId) {
        return DataSourceContext.executeWithDataSource("archive-db", () -> {
            return orderRepository.query()
                .where(Order::getUserId).eq(userId)
                .list();
        });
    }
}
```

### 4.3 动态表名映射

#### 基础表名切换

```java
@Service
public class ShardingService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    public List<Order> getOrdersByMonth(int year, int month) {
        String tableName = String.format("orders_%d_%02d", year, month);
        
        return TableContext.executeWithTable(tableName, () -> {
            return orderRepository.findAll();
        });
    }
    
    // 组合使用数据源和表名
    public void migrateData(String sourceDb, String targetDb, String tableName) {
        // 从源数据库读取
        List<Order> orders = DataSourceContext.executeWithDataSource(sourceDb, () -> {
            return TableContext.executeWithTable(tableName, () -> {
                return orderRepository.findAll();
            });
        });
        
        // 写入目标数据库
        DataSourceContext.executeWithDataSource(targetDb, () -> {
            return TableContext.executeWithTable(tableName, () -> {
                return orderRepository.saveAll(orders);
            });
        });
    }
}
```

#### 注解方式指定表名

```java
@Repository
public class LogRepository extends BaseRepository<Log, Long> {
    
    @TableName("logs_#{T(java.time.LocalDate).now().getYear()}")
    public List<Log> getCurrentYearLogs() {
        return findAll();
    }
    
    @TableName("logs_#{#year}")
    public List<Log> getLogsByYear(int year) {
        return query()
            .where(Log::getLevel).eq("ERROR")
            .list();
    }
}
```

### 4.4 智能路由配置

#### 基于规则的路由

```yaml
flex:
  data:
    routing:
      enabled: true
      rules:
        # 读写分离
        - name: "read-write-split"
          condition: "@transactionManager.isReadOnly()"
          datasource: "slave-db"
          priority: 100
        
        # 大数据量查询路由到分析库
        - name: "analytics-routing"
          condition: "#methodName.contains('Report') || #methodName.contains('Analytics')"
          datasource: "analytics-db"
          priority: 90
        
        # 按用户ID分片
        - name: "user-sharding"
          condition: "#userId != null && #userId % 2 == 0"
          datasource: "shard-0"
          priority: 80
        
        - name: "user-sharding-odd"
          condition: "#userId != null && #userId % 2 == 1"
          datasource: "shard-1"
          priority: 80
```

#### 自定义路由策略

```java
@Component
public class CustomRoutingStrategy implements DataSourceRoutingStrategy {
    
    @Override
    public String determineDataSource(RoutingContext context) {
        // 获取方法参数
        Object[] args = context.getArgs();
        String methodName = context.getMethodName();
        
        // 基于时间的路由
        if (methodName.startsWith("findHistorical")) {
            return "archive-db";
        }
        
        // 基于参数的路由
        if (args.length > 0 && args[0] instanceof Long) {
            Long id = (Long) args[0];
            return id % 2 == 0 ? "shard-0" : "shard-1";
        }
        
        return "default";
    }
    
    @Override
    public int getOrder() {
        return 100;
    }
}
```

### 4.5 多租户支持

#### 基于数据库的多租户

```java
@Service
public class TenantService {
    
    @Autowired
    private UserRepository userRepository;
    
    public List<User> getTenantUsers(String tenantId) {
        String datasource = "tenant_" + tenantId;
        
        return DataSourceContext.executeWithDataSource(datasource, () -> {
            return userRepository.findAll();
        });
    }
    
    // 使用注解方式
    @DataSource("#{@tenantResolver.getCurrentTenantDataSource()}")
    public User createUser(User user) {
        return userRepository.save(user);
    }
}

@Component
public class TenantResolver {
    
    public String getCurrentTenantDataSource() {
        String tenantId = TenantContext.getCurrentTenant();
        return "tenant_" + tenantId;
    }
}
```

#### 基于表前缀的多租户

```java
@Service
public class MultiTenantTableService {
    
    @Autowired
    private OrderRepository orderRepository;
    
    public List<Order> getTenantOrders(String tenantId) {
        String tableName = tenantId + "_orders";
        
        return TableContext.executeWithTable(tableName, () -> {
            return orderRepository.findAll();
        });
    }
    
    // 组合租户数据源和表名
    public void processTenantData(String tenantId) {
        String datasource = "tenant_" + tenantId;
        String tableName = tenantId + "_orders";
        
        DataSourceContext.executeWithDataSource(datasource, () -> {
            return TableContext.executeWithTable(tableName, () -> {
                // 处理租户数据
                return orderRepository.query()
                    .where(Order::getStatus).eq("PENDING")
                    .list();
            });
        });
    }
}
```

## 5. 最佳实践

### 5.1 事务管理

```java
@Service
@Transactional
public class OrderService {
    
    // 正确：在同一个事务中使用相同数据源
    @DataSource("order-db")
    public void processOrder(Order order) {
        // 所有操作都在order-db上执行
        orderRepository.save(order);
        updateInventory(order);
        createOrderLog(order);
    }
    
    // 错误：在同一事务中切换数据源会导致问题
    // @DataSource("order-db")
    // public void badExample(Order order) {
    //     orderRepository.save(order);
    //     
    //     // 这里切换数据源会破坏事务一致性
    //     DataSourceContext.setDataSource("log-db");
    //     logRepository.save(new Log());
    // }
    
    // 正确：使用编程式事务管理多数据源
    public void processOrderWithLogs(Order order) {
        // 主业务事务
        transactionTemplate.execute(status -> {
            DataSourceContext.setDataSource("order-db");
            orderRepository.save(order);
            return null;
        });
        
        // 日志事务（独立）
        transactionTemplate.execute(status -> {
            DataSourceContext.setDataSource("log-db");
            logRepository.save(new Log("Order processed: " + order.getId()));
            return null;
        });
    }
}
```

### 5.2 连接池配置

```yaml
spring:
  datasource:
    primary:
      hikari:
        # 核心配置
        maximum-pool-size: 20
        minimum-idle: 5
        connection-timeout: 30000
        idle-timeout: 600000
        max-lifetime: 1800000
        
        # 性能优化
        leak-detection-threshold: 60000
        validation-timeout: 5000
        
        # 连接测试
        connection-test-query: SELECT 1
        
    secondary:
      hikari:
        # 从库可以设置更大的连接池
        maximum-pool-size: 30
        minimum-idle: 10
```

### 5.3 监控和诊断

```java
@Component
public class DataSourceMonitor {
    
    @EventListener
    public void handleDataSourceSwitch(DataSourceSwitchEvent event) {
        log.info("Data source switched from {} to {} for method {}", 
            event.getFromDataSource(), 
            event.getToDataSource(), 
            event.getMethodName());
    }
    
    @Scheduled(fixedRate = 60000)
    public void monitorConnectionPools() {
        DataSourceManager.getAllDataSources().forEach((name, dataSource) -> {
            if (dataSource instanceof HikariDataSource) {
                HikariDataSource hikari = (HikariDataSource) dataSource;
                log.info("DataSource {}: Active={}, Idle={}, Total={}", 
                    name,
                    hikari.getHikariPoolMXBean().getActiveConnections(),
                    hikari.getHikariPoolMXBean().getIdleConnections(),
                    hikari.getHikariPoolMXBean().getTotalConnections());
            }
        });
    }
}
```

### 5.4 错误处理

```java
@Service
public class RobustDataService {
    
    @Retryable(value = {DataAccessException.class}, maxAttempts = 3)
    @DataSource("primary")
    public List<User> findUsersWithRetry() {
        return userRepository.findAll();
    }
    
    @Recover
    public List<User> recoverFromDataSourceFailure(DataAccessException ex) {
        log.warn("Primary datasource failed, switching to backup", ex);
        
        return DataSourceContext.executeWithDataSource("backup", () -> {
            return userRepository.findAll();
        });
    }
    
    public Optional<User> findUserSafely(Long id) {
        try {
            return DataSourceContext.executeWithDataSource("primary", () -> {
                return userRepository.findById(id);
            });
        } catch (Exception e) {
            log.warn("Failed to query primary datasource, trying backup", e);
            
            try {
                return DataSourceContext.executeWithDataSource("backup", () -> {
                    return userRepository.findById(id);
                });
            } catch (Exception backupEx) {
                log.error("Both primary and backup datasources failed", backupEx);
                return Optional.empty();
            }
        }
    }
}
```

## 6. 故障排除

### 6.1 常见问题

#### 问题1：数据源切换不生效

**症状**：使用@DataSource注解后仍然使用默认数据源

**解决方案**：
1. 确认已添加`@EnableMultiDataSource`注解
2. 检查数据源名称是否正确配置
3. 验证AOP代理是否正常工作

```java
// 错误：在同一个类内部调用不会触发AOP
public class UserService {
    @DataSource("secondary")
    public void method1() {
        method2(); // 这里不会切换数据源
    }
    
    @DataSource("primary")
    public void method2() {
        // ...
    }
}

// 正确：通过Spring代理调用
@Autowired
private UserService userService;

public void correctUsage() {
    userService.method1();
    userService.method2(); // 会正确切换数据源
}
```

#### 问题2：事务回滚异常

**症状**：跨数据源操作时事务回滚失败

**解决方案**：
```java
// 使用JTA事务管理器
@Configuration
@EnableTransactionManagement
public class TransactionConfig {
    
    @Bean
    public PlatformTransactionManager transactionManager() {
        return new JtaTransactionManager();
    }
}
```

#### 问题3：连接池耗尽

**症状**：获取数据库连接超时

**解决方案**：
1. 调整连接池大小
2. 检查连接泄漏
3. 优化查询性能

```yaml
spring:
  datasource:
    primary:
      hikari:
        maximum-pool-size: 50  # 增加连接池大小
        leak-detection-threshold: 30000  # 启用连接泄漏检测
```

### 6.2 调试技巧

#### 启用调试日志

```yaml
logging:
  level:
    com.spring.jdbc.orm.datasource: DEBUG
    com.zaxxer.hikari: DEBUG
    org.springframework.transaction: DEBUG
```

#### 使用监控端点

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,datasource
  endpoint:
    health:
      show-details: always
```

访问 `/actuator/datasource` 查看数据源状态。

---

通过本指南，您应该能够完全掌握多数据源的配置和使用。如有问题，请参考项目的GitHub Issues或联系维护团队。