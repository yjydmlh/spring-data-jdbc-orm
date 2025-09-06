# 增强多数据源使用指南

本指南详细介绍了Spring Data JDBC ORM框架的增强多数据源功能，包括任意多个数据源的配置、动态表名切换以及两者的组合使用。

## 目录

1. [功能概述](#功能概述)
2. [快速开始](#快速开始)
3. [数据源管理](#数据源管理)
4. [表名切换](#表名切换)
5. [注解使用](#注解使用)
6. [编程式API](#编程式api)
7. [增强Repository](#增强repository)
8. [配置说明](#配置说明)
9. [最佳实践](#最佳实践)
10. [常见问题](#常见问题)

## 功能概述

增强多数据源功能提供了以下核心能力：

- **任意多数据源支持**：不限于读写分离，支持配置和使用任意数量的数据源
- **动态表名切换**：运行时动态切换表名，支持分表、分库场景
- **灵活的切换方式**：支持注解式和编程式两种使用方式
- **组合操作**：数据源和表名可以独立或组合切换
- **线程安全**：基于ThreadLocal实现，确保多线程环境下的安全性
- **SpEL支持**：支持Spring表达式语言动态计算表名
- **SQL表名替换**：自动替换SQL中的表名，无需修改现有查询

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.spring</groupId>
    <artifactId>spring-data-jdbc-orm</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 配置数据源

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/main_db
    username: root
    password: password

multi-datasource:
  enhanced:
    enabled: true
  additional:
    user_db:
      url: jdbc:mysql://localhost:3306/user_db
      username: user_admin
      password: user_password
    order_db:
      url: jdbc:mysql://localhost:3306/order_db
      username: order_admin
      password: order_password
```

### 3. 启用增强配置

```java
@Configuration
@EnableEnhancedMultiDataSource
public class DataSourceConfig {
    // 配置类会自动扫描并注册所有配置的数据源
}
```

### 4. 使用示例

```java
@Service
public class UserService {
    
    @Autowired
    private EnhancedMultiRepository<User> userRepository;
    
    // 注解方式：指定数据源和表名
    @DataSource("user_db")
    @Table(mapping = {"user", "user_2024"})
    public List<User> getCurrentYearUsers() {
        return userRepository.findAll();
    }
    
    // 编程方式：动态切换
    public List<User> getUsersFromMultipleSources() {
        return DataSourceContext.executeWithDataSource("user_db", () -> {
            return TableContext.executeWithTable("user", "user_active", () -> {
                return userRepository.findAll();
            });
        });
    }
}
```

## 数据源管理

### DataSourceContext API

`DataSourceContext`提供了完整的数据源管理功能：

#### 基本操作

```java
// 设置当前线程的数据源
DataSourceContext.setDataSource("user_db");

// 获取当前数据源
String currentDataSource = DataSourceContext.getDataSource();

// 清除当前数据源设置
DataSourceContext.clearDataSource();
```

#### 作用域执行

```java
// 在指定数据源作用域内执行操作
List<User> users = DataSourceContext.executeWithDataSource("user_db", () -> {
    return userRepository.findAll();
});

// 支持返回值的操作
Integer count = DataSourceContext.executeWithDataSource("analytics_db", () -> {
    return userRepository.count();
});
```

#### 数据源映射

```java
// 设置数据源别名映射
DataSourceContext.setDataSourceMapping("users", "user_db");
DataSourceContext.setDataSourceMapping("orders", "order_db");

// 使用映射名称执行操作
List<User> users = DataSourceContext.executeWithDataSourceMapping("users", () -> {
    return userRepository.findAll();
});

// 清除所有映射
DataSourceContext.clearDataSourceMappings();
```

#### 多数据源操作

```java
// 在多个数据源上顺序执行相同操作
Map<String, List<User>> results = DataSourceContext.executeOnMultipleDataSources(
    Arrays.asList("main", "backup", "archive"),
    () -> userRepository.findAll()
);
```

#### 数据源管理

```java
// 注册新的数据源
DataSourceInfo dataSourceInfo = new DataSourceInfo(
    "new_db", 
    "jdbc:mysql://localhost:3306/new_db",
    "admin", 
    "password"
);
DataSourceContext.registerDataSource(dataSourceInfo);

// 检查数据源是否已注册
boolean isRegistered = DataSourceContext.isDataSourceRegistered("new_db");

// 获取所有已注册的数据源
Set<String> dataSources = DataSourceContext.getRegisteredDataSources();

// 验证数据源连接
boolean isAvailable = DataSourceContext.isDataSourceAvailable("user_db");
```

## 表名切换

### TableContext API

`TableContext`提供了灵活的表名切换功能：

#### 基本操作

```java
// 设置表名映射（逻辑表名 -> 物理表名）
TableContext.setTableMapping("user", "user_2024");

// 获取当前表名映射
String physicalTable = TableContext.getTableMapping("user");

// 移除特定映射
TableContext.removeTableMapping("user");

// 清除所有映射
TableContext.clearTableMappings();
```

#### 作用域执行

```java
// 在指定表名作用域内执行操作
List<User> users = TableContext.executeWithTable("user", "user_2024", () -> {
    return userRepository.findAll();
});

// 使用映射执行操作
TableContext.setTableMapping("current_user", "user_2024");
List<User> users = TableContext.executeWithTable("current_user", () -> {
    return userRepository.findAll();
});
```

#### 批量表名映射

```java
// 设置多个表名映射
Map<String, String> mappings = new HashMap<>();
mappings.put("user", "user_2024");
mappings.put("order", "order_current");
mappings.put("product", "product_active");
TableContext.setTableMappings(mappings);
```

## 注解使用

### @DataSource 注解

用于指定方法或类级别的数据源：

```java
// 方法级别
@DataSource("user_db")
public List<User> getUsers() {
    return userRepository.findAll();
}

// 类级别
@DataSource("analytics_db")
@Service
public class AnalyticsService {
    // 类中所有方法默认使用analytics_db数据源
    
    // 方法级别的注解会覆盖类级别的设置
    @DataSource("main")
    public void syncToMain() {
        // 使用main数据源
    }
}
```

### @Table 注解

用于指定动态表名映射：

```java
// 简单映射
@Table(mapping = {"user", "user_2024"})
public List<User> getCurrentYearUsers() {
    return userRepository.findAll();
}

// 多个映射
@Table(mapping = {
    "user", "user_2024",
    "order", "order_2024",
    "product", "product_active"
})
public void processData() {
    // 同时映射多个表
}

// 使用SpEL表达式
@Table(
    mapping = {"user", "#{T(java.time.LocalDate).now().getYear() + '_user'}"},
    useSpEL = true
)
public List<User> getUsersFromCurrentYearTable() {
    // 表名会根据当前年份动态生成
    return userRepository.findAll();
}

// 继承类级别映射
@Table(
    mapping = {"log", "log_error"},
    inheritClassMapping = true
)
public void logError() {
    // 会继承类级别的表名映射，并添加新的映射
}

// 设置优先级
@Table(
    mapping = {"user", "user_priority"},
    priority = 10
)
public void highPriorityOperation() {
    // 高优先级的表名映射
}
```

### 注解组合使用

```java
@DataSource("user_db")
@Table(mapping = {"user", "user_vip"})
public List<User> getVipUsers() {
    // 在user_db数据源的user_vip表中查询
    return userRepository.findAll();
}
```

## 编程式API

### 组合操作

```java
// 数据源和表名的嵌套切换
List<User> users = DataSourceContext.executeWithDataSource("user_db", () -> {
    return TableContext.executeWithTable("user", "user_2024", () -> {
        return userRepository.findAll();
    });
});

// 复杂的数据迁移场景
public void migrateData() {
    // 从旧系统读取数据
    List<User> oldUsers = DataSourceContext.executeWithDataSource("legacy_db", () -> {
        return TableContext.executeWithTable("user", "old_user_table", () -> {
            return userRepository.findAll();
        });
    });
    
    // 写入新系统
    DataSourceContext.executeWithDataSource("new_db", () -> {
        return TableContext.executeWithTable("user", "new_user_table", () -> {
            for (User user : oldUsers) {
                userRepository.save(user);
            }
            return null;
        });
    });
}
```

### 异常处理

```java
try {
    DataSourceContext.setDataSource("user_db");
    TableContext.setTableMapping("user", "user_2024");
    
    // 执行业务操作
    List<User> users = userRepository.findAll();
    
} catch (Exception e) {
    // 处理异常
    log.error("操作失败", e);
} finally {
    // 确保清理上下文
    DataSourceContext.clearDataSource();
    TableContext.clearTableMappings();
}
```

## 增强Repository

### EnhancedMultiRepository 接口

提供了便捷的多数据源和多表操作方法：

```java
public interface UserRepository extends EnhancedMultiRepository<User> {
    // 继承所有增强功能
}

@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    public void demonstrateEnhancedMethods() {
        // 在指定数据源中操作
        List<User> users1 = userRepository.findAllInDataSource("user_db");
        
        // 在指定表中操作
        List<User> users2 = userRepository.findAllInTable("user_2024");
        
        // 在指定数据源和表中操作
        List<User> users3 = userRepository.findAllInDataSourceAndTable("user_db", "user_2024");
        
        // 批量多数据源查询
        Map<String, List<User>> usersByDataSource = 
            userRepository.findAllInMultipleDataSources("main", "backup", "archive");
        
        // 批量多表查询
        Map<String, List<User>> usersByTable = 
            userRepository.findAllInMultipleTables("user_2024", "user_2023", "user_history");
        
        // 保存到指定数据源和表
        User newUser = new User();
        userRepository.saveInDataSourceAndTable(newUser, "user_db", "user_2024");
        
        // 在指定数据源中删除
        userRepository.deleteInDataSource(1L, "user_db");
        
        // 在指定表中更新
        userRepository.updateInTable(newUser, "user_2024");
    }
}
```

## 配置说明

### 完整配置示例

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/main_db
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver

multi-datasource:
  # 启用增强多数据源功能
  enhanced:
    enabled: true
    # 健康检查配置
    health-check:
      enabled: true
      timeout: 5000
      interval: 30000
    
  # 只读数据源
  read-only:
    url: jdbc:mysql://localhost:3306/readonly_db
    username: readonly_user
    password: readonly_password
    driver-class-name: com.mysql.cj.jdbc.Driver
    
  # 额外数据源
  additional:
    user_db:
      url: jdbc:mysql://localhost:3306/user_db
      username: user_admin
      password: user_password
      driver-class-name: com.mysql.cj.jdbc.Driver
    order_db:
      url: jdbc:mysql://localhost:3306/order_db
      username: order_admin
      password: order_password
      driver-class-name: com.mysql.cj.jdbc.Driver

# 数据源路由配置
datasource:
  routing:
    default: main
    aliases:
      primary: main
      secondary: readonly
      users: user_db
      orders: order_db

# 表名映射配置
table:
  mapping:
    user: user_2024
    order: order_current
    product: product_active
```

### Java配置

```java
@Configuration
@EnableEnhancedMultiDataSource
public class EnhancedDataSourceConfig {
    
    @Bean
    @Primary
    public DataSource primaryDataSource() {
        return DataSourceBuilder.create()
            .url("jdbc:mysql://localhost:3306/main_db")
            .username("root")
            .password("password")
            .build();
    }
    
    @Bean("userDataSource")
    public DataSource userDataSource() {
        return DataSourceBuilder.create()
            .url("jdbc:mysql://localhost:3306/user_db")
            .username("user_admin")
            .password("user_password")
            .build();
    }
    
    @Bean
    public DataSourceAspect dataSourceAspect() {
        return new DataSourceAspect();
    }
    
    @Bean
    public TableAspect tableAspect() {
        return new TableAspect();
    }
}
```

## 最佳实践

### 1. 上下文管理

```java
// 推荐：使用作用域执行，自动清理
List<User> users = DataSourceContext.executeWithDataSource("user_db", () -> {
    return userRepository.findAll();
});

// 不推荐：手动管理，容易忘记清理
DataSourceContext.setDataSource("user_db");
try {
    List<User> users = userRepository.findAll();
} finally {
    DataSourceContext.clearDataSource(); // 容易忘记
}
```

### 2. 异常处理

```java
@Service
public class UserService {
    
    public List<User> getUsers() {
        try {
            return DataSourceContext.executeWithDataSource("user_db", () -> {
                return userRepository.findAll();
            });
        } catch (DataAccessException e) {
            // 数据访问异常处理
            log.error("数据库访问失败", e);
            throw new ServiceException("获取用户数据失败", e);
        }
    }
}
```

### 3. 性能优化

```java
// 批量操作时复用上下文
public void batchProcess(List<User> users) {
    DataSourceContext.executeWithDataSource("user_db", () -> {
        TableContext.executeWithTable("user", "user_2024", () -> {
            // 在同一个上下文中执行多个操作
            for (User user : users) {
                userRepository.save(user);
            }
            return null;
        });
        return null;
    });
}
```

### 4. 事务管理

```java
@Service
@Transactional
public class UserService {
    
    // 注意：事务和数据源切换的配合使用
    @DataSource("user_db")
    @Transactional("userTransactionManager")
    public void updateUser(User user) {
        userRepository.save(user);
    }
}
```

### 5. 监控和日志

```java
@Component
public class DataSourceMonitor {
    
    @EventListener
    public void handleDataSourceSwitch(DataSourceSwitchEvent event) {
        log.info("数据源切换: {} -> {}", event.getFrom(), event.getTo());
    }
    
    @Scheduled(fixedRate = 60000)
    public void checkDataSourceHealth() {
        Set<String> dataSources = DataSourceContext.getRegisteredDataSources();
        for (String dataSource : dataSources) {
            boolean available = DataSourceContext.isDataSourceAvailable(dataSource);
            if (!available) {
                log.warn("数据源不可用: {}", dataSource);
            }
        }
    }
}
```

## 常见问题

### Q1: 如何处理数据源连接失败？

```java
// 使用健康检查和重试机制
public List<User> getUsersWithFallback() {
    try {
        return DataSourceContext.executeWithDataSource("user_db", () -> {
            return userRepository.findAll();
        });
    } catch (DataAccessException e) {
        log.warn("主数据源失败，切换到备份数据源", e);
        return DataSourceContext.executeWithDataSource("backup_db", () -> {
            return userRepository.findAll();
        });
    }
}
```

### Q2: 如何实现读写分离？

```java
@Service
public class UserService {
    
    @DataSource("main") // 写操作使用主数据源
    public void saveUser(User user) {
        userRepository.save(user);
    }
    
    @DataSource("readonly") // 读操作使用只读数据源
    public List<User> getUsers() {
        return userRepository.findAll();
    }
}
```

### Q3: 如何实现分表查询？

```java
public List<User> getUsersByDateRange(LocalDate start, LocalDate end) {
    List<User> allUsers = new ArrayList<>();
    
    // 根据日期范围确定需要查询的表
    List<String> tables = getTablesByDateRange(start, end);
    
    for (String table : tables) {
        List<User> users = TableContext.executeWithTable("user", table, () -> {
            return userRepository.findByDateRange(start, end);
        });
        allUsers.addAll(users);
    }
    
    return allUsers;
}
```

### Q4: 如何处理事务跨数据源？

```java
// 使用分布式事务管理器
@Configuration
public class TransactionConfig {
    
    @Bean
    public PlatformTransactionManager transactionManager() {
        JtaTransactionManager jtaTransactionManager = new JtaTransactionManager();
        // 配置分布式事务管理器
        return jtaTransactionManager;
    }
}

@Service
public class UserService {
    
    @Transactional
    public void transferUser(Long userId, String fromDb, String toDB) {
        // 从源数据库删除
        DataSourceContext.executeWithDataSource(fromDb, () -> {
            userRepository.deleteById(userId);
            return null;
        });
        
        // 在目标数据库创建
        DataSourceContext.executeWithDataSource(toDB, () -> {
            User user = getUserFromCache(userId);
            userRepository.save(user);
            return null;
        });
    }
}
```

### Q5: 如何优化性能？

1. **连接池配置**：为每个数据源配置合适的连接池大小
2. **缓存策略**：对频繁访问的数据使用缓存
3. **批量操作**：在同一个上下文中执行多个操作
4. **异步处理**：对于非关键路径的操作使用异步处理

```java
@Service
public class OptimizedUserService {
    
    @Cacheable("users")
    @DataSource("readonly")
    public List<User> getCachedUsers() {
        return userRepository.findAll();
    }
    
    @Async
    @DataSource("analytics_db")
    public CompletableFuture<Void> asyncAnalytics() {
        // 异步分析处理
        return CompletableFuture.completedFuture(null);
    }
}
```

## 总结

增强多数据源功能提供了灵活、强大的数据源和表名管理能力，支持复杂的业务场景。通过合理使用注解和编程式API，可以轻松实现多数据源切换、分表查询、数据迁移等功能。

关键要点：
- 优先使用作用域执行方法，确保上下文自动清理
- 合理配置数据源连接池和事务管理
- 注意异常处理和监控
- 根据业务需求选择合适的切换策略
- 考虑性能优化和缓存策略