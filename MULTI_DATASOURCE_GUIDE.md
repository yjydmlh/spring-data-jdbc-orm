# 多数据源配置指南

本指南介绍如何在Spring Data JDBC ORM框架中配置和使用多数据源功能。

## 功能特性

- ✅ **动态数据源切换**：运行时动态选择数据源
- ✅ **注解驱动**：通过`@DataSource`注解声明式切换
- ✅ **编程式切换**：通过`DataSourceContext`手动控制
- ✅ **读写分离**：支持主从库分离场景
- ✅ **事务安全**：与Spring事务管理完美集成
- ✅ **线程安全**：基于ThreadLocal实现线程隔离

## 快速开始

### 1. 配置多数据源

在`application.yml`中配置多个数据源：

```yaml
spring:
  datasource:
    # 主数据源（默认）
    primary:
      url: jdbc:mysql://localhost:3306/main_db
      username: root
      password: password
      driver-class-name: com.mysql.cj.jdbc.Driver
    
    # 只读数据源
    readonly:
      url: jdbc:mysql://localhost:3307/readonly_db
      username: readonly_user
      password: readonly_pass
      driver-class-name: com.mysql.cj.jdbc.Driver
    
    # 第二个数据源
    secondary:
      url: jdbc:postgresql://localhost:5432/secondary_db
      username: postgres
      password: postgres
      driver-class-name: org.postgresql.Driver
```

### 2. 启用多数据源配置

在主配置类上添加注解：

```java
@SpringBootApplication
@EnableAspectJAutoProxy // 启用AOP
@Import(MultiDataSourceConfiguration.class) // 导入多数据源配置
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

## 使用方式

### 方式1：注解驱动（推荐）

#### 方法级别注解

```java
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    // 从只读库查询
    @DataSource(type = DataSource.Type.READONLY)
    public List<User> getUsers() {
        return userRepository.findAll();
    }
    
    // 保存到主库
    @DataSource(type = DataSource.Type.MASTER)
    @Transactional
    public User saveUser(User user) {
        return userRepository.save(user);
    }
    
    // 使用自定义数据源
    @DataSource("secondary")
    public Optional<User> getUserFromSecondary(Long id) {
        return userRepository.findById(id);
    }
}
```

#### 类级别注解

```java
@Service
@DataSource(type = DataSource.Type.READONLY) // 类级别默认使用只读库
public class ReadOnlyUserService {
    
    // 继承类级别的数据源设置
    public List<User> getUsers() {
        return userRepository.findAll();
    }
    
    // 方法级别注解覆盖类级别设置
    @DataSource(type = DataSource.Type.MASTER)
    @Transactional
    public User saveUser(User user) {
        return userRepository.save(user);
    }
}
```

### 方式2：编程式切换

```java
@Service
public class UserService {
    
    public List<User> getUsers() {
        // 使用DataSourceContext手动切换
        return DataSourceContext.executeWithDataSource("readonly", () -> {
            return userRepository.findAll();
        });
    }
    
    public void complexOperation() {
        try {
            // 手动设置数据源
            DataSourceContext.setDataSource("secondary");
            
            // 执行操作
            userRepository.findAll();
            
        } finally {
            // 清理数据源设置
            DataSourceContext.clearDataSource();
        }
    }
}
```

### 方式3：Repository便捷方法

```java
public interface UserRepository extends MultiDataSourceRepository<User, Long> {
    // 继承所有多数据源便捷方法
}

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    public void demonstrateRepositoryMethods() {
        // 从只读库查询
        List<User> users = userRepository.findAllFromReadOnly();
        
        // 从只读库分页查询
        Page<User> userPage = userRepository.findAllFromReadOnly(PageRequest.of(0, 10));
        
        // 强制从主库查询
        Optional<User> user = userRepository.findByIdFromMaster(1L);
        
        // 保存到主库
        User newUser = new User();
        userRepository.saveToMaster(newUser);
        
        // 在第二个数据源上执行操作
        userRepository.withSecondaryDataSource(() -> {
            return userRepository.findAll();
        });
    }
}
```

## 高级用法

### 读写分离场景

```java
@Service
public class UserService {
    
    /**
     * 写后读场景：避免主从延迟问题
     */
    @Transactional
    public User createAndRead(User user) {
        // 1. 保存到主库
        User savedUser = DataSourceContext.executeWithDataSource("default", () -> {
            return userRepository.save(user);
        });
        
        // 2. 立即从主库读取（避免主从延迟）
        return DataSourceContext.executeWithDataSource("default", () -> {
            return userRepository.findById(savedUser.getId()).orElse(null);
        });
    }
    
    /**
     * 普通查询使用只读库
     */
    @DataSource(type = DataSource.Type.READONLY)
    public List<User> searchUsers(String keyword) {
        return userRepository.findByNameContaining(keyword);
    }
}
```

### 多数据源事务管理

```java
@Service
public class UserService {
    
    /**
     * 单数据源事务（推荐）
     */
    @DataSource(type = DataSource.Type.MASTER)
    @Transactional
    public void singleDataSourceTransaction() {
        // 所有操作都在主库的同一个事务中
        userRepository.save(new User());
        userRepository.save(new User());
    }
    
    /**
     * 多数据源操作（注意事务边界）
     */
    public void multiDataSourceOperation() {
        // 每个数据源使用独立的事务
        DataSourceContext.executeWithDataSource("default", () -> {
            return transactionTemplate.execute(status -> {
                userRepository.save(new User());
                return null;
            });
        });
        
        DataSourceContext.executeWithDataSource("secondary", () -> {
            return transactionTemplate.execute(status -> {
                userRepository.save(new User());
                return null;
            });
        });
    }
}
```

## 配置选项

### 数据源类型预定义

```java
@DataSource(type = DataSource.Type.DEFAULT)    // 默认数据源
@DataSource(type = DataSource.Type.MASTER)     // 主库
@DataSource(type = DataSource.Type.SLAVE)      // 从库
@DataSource(type = DataSource.Type.READONLY)   // 只读库
@DataSource(type = DataSource.Type.SECONDARY)  // 第二个数据源
```

### 自定义数据源标识

```java
@DataSource("custom-db")           // 使用自定义标识
@DataSource(value = "analytics")   // 分析数据库
@DataSource(value = "logging")     // 日志数据库
```

## 最佳实践

### 1. 数据源选择策略

- **写操作**：始终使用主库（`MASTER`或`DEFAULT`）
- **读操作**：优先使用只读库（`READONLY`或`SLAVE`）
- **实时性要求高的读操作**：使用主库避免主从延迟
- **分析查询**：使用专门的分析数据库

### 2. 事务管理

- **单数据源事务**：在方法上同时使用`@DataSource`和`@Transactional`
- **跨数据源操作**：避免跨数据源事务，使用补偿机制
- **读写分离**：写操作后的立即读取应使用主库

### 3. 性能优化

- **连接池配置**：为不同数据源配置合适的连接池大小
- **读写分离**：将查询操作分散到只读库减少主库压力
- **缓存策略**：对频繁查询的数据使用缓存

### 4. 监控和调试

```java
// 启用调试日志
logging:
  level:
    com.spring.jdbc.orm.aspect.DataSourceAspect: DEBUG
    com.spring.jdbc.orm.core.datasource: DEBUG
```

## 注意事项

1. **事务边界**：`@DataSource`注解应该在事务注解之外，确保数据源切换在事务开始之前
2. **线程安全**：数据源上下文基于ThreadLocal，在异步操作中需要手动传递
3. **主从延迟**：读写分离场景下注意主从同步延迟问题
4. **连接泄漏**：确保在finally块中清理数据源上下文
5. **嵌套调用**：内层方法的`@DataSource`注解会覆盖外层设置

## 故障排除

### 常见问题

1. **数据源未切换**
   - 检查AOP是否正确配置
   - 确认方法是通过Spring代理调用
   - 验证`@DataSource`注解是否正确

2. **事务问题**
   - 确保数据源切换在事务开始之前
   - 检查事务管理器配置
   - 避免跨数据源事务

3. **连接池问题**
   - 检查各数据源的连接池配置
   - 监控连接池使用情况
   - 调整连接池参数

### 调试技巧

```java
// 获取当前使用的数据源
String currentDataSource = DataSourceContext.getDataSource();
logger.info("Current datasource: {}", currentDataSource);

// 在切面中添加日志
@Around("dataSourcePointcut()")
public Object around(ProceedingJoinPoint point) throws Throwable {
    logger.info("Method: {}, DataSource: {}", 
               point.getSignature().toShortString(), 
               determineDataSourceKey(getDataSource(point)));
    return point.proceed();
}
```

## 扩展开发

### 自定义数据源路由策略

```java
@Component
public class CustomDataSourceRouter {
    
    public String routeDataSource(String operation, Object... params) {
        // 根据业务逻辑动态选择数据源
        if ("read".equals(operation)) {
            return selectReadOnlyDataSource();
        } else if ("write".equals(operation)) {
            return "default";
        }
        return "default";
    }
    
    private String selectReadOnlyDataSource() {
        // 负载均衡逻辑
        return ThreadLocalRandom.current().nextBoolean() ? "readonly1" : "readonly2";
    }
}
```

### 数据源健康检查

```java
@Component
public class DataSourceHealthChecker {
    
    @Autowired
    private Map<String, DataSource> dataSources;
    
    @Scheduled(fixedRate = 30000) // 每30秒检查一次
    public void checkDataSourceHealth() {
        dataSources.forEach((name, dataSource) -> {
            try (Connection conn = dataSource.getConnection()) {
                if (conn.isValid(5)) {
                    logger.info("DataSource {} is healthy", name);
                } else {
                    logger.warn("DataSource {} is not responding", name);
                }
            } catch (SQLException e) {
                logger.error("DataSource {} health check failed", name, e);
            }
        });
    }
}
```

通过以上配置和使用方式，您可以在Spring Data JDBC ORM框架中灵活地使用多数据源功能，实现读写分离、数据库分片等高级特性。