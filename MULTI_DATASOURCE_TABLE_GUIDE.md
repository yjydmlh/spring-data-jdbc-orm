# 多数据源多表切换完整使用指南

本指南详细介绍如何使用 Spring Data JDBC ORM 的多数据源多表切换功能，包括配置、使用方法和最佳实践。

## 目录

1. [功能概述](#功能概述)
2. [快速开始](#快速开始)
3. [配置说明](#配置说明)
4. [核心功能](#核心功能)
5. [使用示例](#使用示例)
6. [最佳实践](#最佳实践)
7. [常见问题](#常见问题)
8. [性能优化](#性能优化)

## 功能概述

### 主要特性

- **多数据源支持**：支持任意数量的数据源配置和动态切换
- **动态表名切换**：支持运行时动态切换表名，适用于分表场景
- **注解驱动**：提供 `@Table` 注解支持方法级表名指定
- **线程安全**：基于 ThreadLocal 实现，确保多线程环境下的数据隔离
- **作用域管理**：支持嵌套作用域和自动清理
- **组合使用**：数据源切换和表名切换可以组合使用

### 适用场景

- **分库分表**：水平分片场景下的数据源和表名动态路由
- **多租户系统**：不同租户使用不同的数据库或表
- **读写分离**：读操作使用从库，写操作使用主库
- **数据迁移**：在不同系统间迁移数据
- **历史数据归档**：按时间分表存储历史数据
- **业务隔离**：不同业务模块使用独立的数据源

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

```java
@Configuration
public class DataSourceConfig extends EnhancedMultiDataSourceConfig {
    
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.main")
    public DataSource mainDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Bean
    @ConfigurationProperties("spring.datasource.analytics")
    public DataSource analyticsDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Override
    protected Map<String, DataSource> getDataSourceMap() {
        Map<String, DataSource> map = new HashMap<>();
        map.put("main", mainDataSource());
        map.put("analytics", analyticsDataSource());
        return map;
    }
    
    @Override
    protected String getDefaultDataSourceName() {
        return "main";
    }
}
```

### 3. 配置文件

```yaml
spring:
  datasource:
    main:
      driver-class-name: com.mysql.cj.jdbc.Driver
      url: jdbc:mysql://localhost:3306/main_db
      username: root
      password: password
    analytics:
      driver-class-name: com.mysql.cj.jdbc.Driver
      url: jdbc:mysql://localhost:3306/analytics_db
      username: analytics_user
      password: analytics_pass
```

### 4. 基本使用

```java
@Service
public class UserService {
    
    @Autowired
    private EnhancedMultiDataSourceRepository repository;
    
    // 数据源切换
    public List<Map<String, Object>> getAnalyticsData() {
        return DataSourceContext.executeWithDataSource("analytics", () -> {
            return repository.findAll("SELECT * FROM user_stats");
        });
    }
    
    // 表名切换
    public List<Map<String, Object>> getHistoryData() {
        return TableContext.executeWithTableMapping("users", "users_2023", () -> {
            return repository.findAll("SELECT * FROM users");
        });
    }
    
    // 注解方式
    @Table("users_vip")
    public List<Map<String, Object>> getVipUsers() {
        return repository.findAll("SELECT * FROM users WHERE vip = 1");
    }
}
```

## 配置说明

### 数据源配置类

继承 `EnhancedMultiDataSourceConfig` 并实现必要的方法：

```java
@Configuration
public class MultiDataSourceConfiguration extends EnhancedMultiDataSourceConfig {
    
    // 定义各个数据源Bean
    @Bean
    @Primary
    public DataSource mainDataSource() {
        // 数据源配置
    }
    
    // 配置数据源映射
    @Override
    protected Map<String, DataSource> getDataSourceMap() {
        // 返回数据源映射
    }
    
    // 指定默认数据源
    @Override
    protected String getDefaultDataSourceName() {
        return "main";
    }
}
```

### 连接池配置

推荐使用 HikariCP 连接池：

```yaml
spring:
  datasource:
    main:
      hikari:
        maximum-pool-size: 20
        minimum-idle: 5
        connection-timeout: 30000
        idle-timeout: 600000
        max-lifetime: 1800000
        pool-name: MainHikariCP
```

## 核心功能

### 1. DataSourceContext - 数据源切换

#### 基本用法

```java
// 执行并返回结果
List<Map<String, Object>> result = DataSourceContext.executeWithDataSource("analytics", () -> {
    return repository.findAll("SELECT * FROM reports");
});

// 执行无返回值操作
DataSourceContext.executeWithDataSource("backup", () -> {
    repository.execute("INSERT INTO logs (message) VALUES (?)", "backup completed");
});
```

#### 获取当前数据源

```java
String currentDataSource = DataSourceContext.getCurrentDataSource();
System.out.println("当前数据源: " + currentDataSource);
```

#### 手动设置和清理

```java
// 设置数据源
DataSourceContext.setCurrentDataSource("analytics");
try {
    // 执行操作
    List<Map<String, Object>> data = repository.findAll("SELECT * FROM stats");
} finally {
    // 清理数据源设置
    DataSourceContext.clearDataSource();
}
```

### 2. TableContext - 表名切换

#### 单表映射

```java
// 将逻辑表名 "users" 映射到物理表名 "users_2024"
List<Map<String, Object>> users = TableContext.executeWithTableMapping("users", "users_2024", () -> {
    return repository.findAll("SELECT * FROM users WHERE active = 1");
});
```

#### 多表映射

```java
Map<String, String> tableMappings = new HashMap<>();
tableMappings.put("users", "users_2024");
tableMappings.put("orders", "orders_2024");
tableMappings.put("products", "products_active");

List<Map<String, Object>> report = TableContext.executeWithTableMappings(tableMappings, () -> {
    return repository.findAll(
        "SELECT u.name, COUNT(o.id) as order_count " +
        "FROM users u LEFT JOIN orders o ON u.id = o.user_id " +
        "GROUP BY u.id"
    );
});
```

#### 手动管理表映射

```java
// 设置表映射
TableContext.setTableMapping("users", "users_vip");
TableContext.setTableMapping("orders", "orders_large");

try {
    // 执行操作
    List<Map<String, Object>> data = repository.findAll(
        "SELECT * FROM users u JOIN orders o ON u.id = o.user_id"
    );
} finally {
    // 清理所有映射
    TableContext.clearTableMappings();
}
```

### 3. @Table 注解

#### 方法级注解

```java
@Service
public class UserService {
    
    @Table("users_active")
    public List<Map<String, Object>> getActiveUsers() {
        return repository.findAll("SELECT * FROM users");
    }
    
    @Table("users_archived")
    public List<Map<String, Object>> getArchivedUsers() {
        return repository.findAll("SELECT * FROM users");
    }
}
```

#### 动态表名

```java
@Table("#{getTableName()}")
public List<Map<String, Object>> getDynamicUsers() {
    return repository.findAll("SELECT * FROM users");
}

private String getTableName() {
    // 根据业务逻辑返回表名
    return "users_" + getCurrentMonth();
}
```

### 4. 手动指定库名和表名

#### 手动指定数据源（库名）

**编程式API - 执行并返回结果**
```java
// 手动指定数据源执行查询操作
List<Map<String, Object>> analyticsData = DataSourceContext.executeWithDataSource("analytics", () -> {
    return repository.findAll("SELECT * FROM user_behavior WHERE date >= ?", "2024-01-01");
});

// 手动指定数据源执行更新操作
DataSourceContext.executeWithDataSource("backup", () -> {
    repository.execute("INSERT INTO backup_logs (message, created_at) VALUES (?, ?)", 
                      "数据备份完成", new Date());
});
```

**编程式API - 手动设置和清理**
```java
// 手动设置当前数据源
DataSourceContext.setCurrentDataSource("reporting");
try {
    // 在指定数据源上执行多个操作
    List<Map<String, Object>> users = repository.findAll("SELECT * FROM users");
    List<Map<String, Object>> orders = repository.findAll("SELECT * FROM orders");
    
    // 生成报表
    generateReport(users, orders);
} finally {
    // 必须手动清理数据源设置
    DataSourceContext.clearDataSource();
}
```

**配置映射方式**
```java
@Override
protected Map<String, DataSource> getDataSourceMap() {
    Map<String, DataSource> map = new HashMap<>();
    map.put("main", mainDataSource());           // 主业务库
    map.put("analytics", analyticsDataSource()); // 分析库
    map.put("backup", backupDataSource());       // 备份库
    map.put("shard_0", shard0DataSource());      // 分片库0
    map.put("shard_1", shard1DataSource());      // 分片库1
    map.put("legacy", legacyDataSource());       // 遗留系统库
    map.put("tenant_a", tenantADataSource());    // 租户A库
    map.put("tenant_b", tenantBDataSource());    // 租户B库
    return map;
}
```

#### 手动指定表名

**单表映射**
```java
// 将逻辑表名 "users" 手动映射到物理表名 "users_2024"
List<Map<String, Object>> currentUsers = TableContext.executeWithTableMapping("users", "users_2024", () -> {
    return repository.findAll("SELECT * FROM users WHERE status = 'active'");
});

// 查询历史表
List<Map<String, Object>> historyUsers = TableContext.executeWithTableMapping("users", "users_history", () -> {
    return repository.findAll("SELECT * FROM users WHERE deleted_at IS NOT NULL");
});
```

**批量表映射**
```java
// 手动指定多个表的映射关系
Map<String, String> tableMappings = new HashMap<>();
tableMappings.put("users", "users_2024_q1");      // 用户表映射到2024年Q1表
tableMappings.put("orders", "orders_2024_q1");    // 订单表映射到2024年Q1表
tableMappings.put("products", "products_active");  // 产品表映射到活跃产品表

// 执行跨表查询
List<Map<String, Object>> quarterlyReport = TableContext.executeWithTableMappings(tableMappings, () -> {
    return repository.findAll(
        "SELECT u.name, COUNT(o.id) as order_count, SUM(o.amount) as total_amount " +
        "FROM users u " +
        "LEFT JOIN orders o ON u.id = o.user_id " +
        "LEFT JOIN products p ON o.product_id = p.id " +
        "WHERE o.created_at >= ? " +
        "GROUP BY u.id, u.name",
        "2024-01-01"
    );
});
```

**手动设置表映射**
```java
// 手动设置多个表映射
TableContext.setTableMapping("users", "users_vip");
TableContext.setTableMapping("orders", "orders_large");
TableContext.setTableMapping("products", "products_premium");

try {
    // 执行复杂的业务查询
    List<Map<String, Object>> vipOrderData = repository.findAll(
        "SELECT u.name, u.vip_level, o.order_no, o.amount, p.name as product_name " +
        "FROM users u " +
        "JOIN orders o ON u.id = o.user_id " +
        "JOIN products p ON o.product_id = p.id " +
        "WHERE u.vip_level >= ? AND o.amount >= ?",
        3, 1000
    );
    
    // 处理VIP订单数据
    processVipOrders(vipOrderData);
} finally {
    // 清理所有表映射
    TableContext.clearTableMappings();
}
```

#### 组合使用 - 同时指定数据源和表名

**基础组合**
```java
// 同时手动指定数据源和表名
List<Map<String, Object>> shardData = DataSourceContext.executeWithDataSource("shard_1", () -> {
    return TableContext.executeWithTableMapping("users", "users_shard_1", () -> {
        return repository.findAll("SELECT * FROM users WHERE region = ?", "华东");
    });
});
```

**复杂业务场景组合**
```java
public class ManualRoutingService {
    
    /**
     * 手动分片路由 - 根据用户ID手动指定库和表
     */
    public Map<String, Object> getUserByIdManual(Long userId) {
        // 手动计算分片
        int shardIndex = (int) (userId % 4);
        String targetDataSource = "shard_" + shardIndex;  // 手动指定分片库
        String targetTable = "users_" + shardIndex;       // 手动指定分片表
        
        return DataSourceContext.executeWithDataSource(targetDataSource, () -> {
            return TableContext.executeWithTableMapping("users", targetTable, () -> {
                return repository.findOne("SELECT * FROM users WHERE id = ?", userId);
            });
        });
    }
    
    /**
     * 手动多租户路由 - 根据租户ID手动指定库和表
     */
    public List<Map<String, Object>> getTenantDataManual(String tenantId) {
        String targetDataSource = "tenant_" + tenantId;   // 手动指定租户库
        String targetTable = "data_" + tenantId;          // 手动指定租户表
        
        return DataSourceContext.executeWithDataSource(targetDataSource, () -> {
            return TableContext.executeWithTableMapping("tenant_data", targetTable, () -> {
                return repository.findAll("SELECT * FROM tenant_data WHERE active = 1");
            });
        });
    }
    
    /**
     * 手动读写分离 - 根据操作类型手动指定库
     */
    public void createUserManual(String name, String email) {
        // 写操作手动指定主库
        DataSourceContext.executeWithDataSource("master", () -> {
            repository.execute(
                "INSERT INTO users (name, email, created_at) VALUES (?, ?, ?)",
                name, email, new Date()
            );
        });
    }
    
    public List<Map<String, Object>> getUsersManual() {
        // 读操作手动指定从库
        return DataSourceContext.executeWithDataSource("slave", () -> {
            return repository.findAll("SELECT * FROM users ORDER BY created_at DESC LIMIT 100");
        });
    }
    
    /**
     * 手动历史数据查询 - 根据时间范围手动指定表
     */
    public List<Map<String, Object>> getHistoryDataManual(String yearMonth) {
        String targetTable = "orders_" + yearMonth;  // 手动指定历史表
        
        return TableContext.executeWithTableMapping("orders", targetTable, () -> {
            return repository.findAll(
                "SELECT order_no, amount, created_at FROM orders WHERE status = 'completed'"
            );
        });
    }
    
    /**
     * 手动数据迁移 - 在不同库表间迁移数据
     */
    public void migrateDataManual(String sourceDb, String sourceTable, String targetDb, String targetTable) {
        // 从源库源表读取数据
        List<Map<String, Object>> sourceData = DataSourceContext.executeWithDataSource(sourceDb, () -> {
            return TableContext.executeWithTableMapping("migration_table", sourceTable, () -> {
                return repository.findAll("SELECT * FROM migration_table WHERE migrated = 0");
            });
        });
        
        // 写入目标库目标表
        DataSourceContext.executeWithDataSource(targetDb, () -> {
            TableContext.executeWithTableMapping("migration_table", targetTable, () -> {
                for (Map<String, Object> row : sourceData) {
                    repository.execute(
                        "INSERT INTO migration_table (id, data, created_at) VALUES (?, ?, ?)",
                        row.get("id"), row.get("data"), row.get("created_at")
                    );
                }
            });
        });
        
        // 标记源数据为已迁移
        DataSourceContext.executeWithDataSource(sourceDb, () -> {
            TableContext.executeWithTableMapping("migration_table", sourceTable, () -> {
                repository.execute("UPDATE migration_table SET migrated = 1 WHERE migrated = 0");
            });
        });
    }
}
```

#### 手动指定的最佳实践

**1. 资源管理**
```java
// ✅ 推荐：使用executeWith*方法，自动管理资源
public List<Map<String, Object>> recommendedApproach(String dbName, String tableName) {
    return DataSourceContext.executeWithDataSource(dbName, () -> {
        return TableContext.executeWithTableMapping("logical_table", tableName, () -> {
            return repository.findAll("SELECT * FROM logical_table");
        });
    });
}

// ❌ 不推荐：手动管理，容易忘记清理
public List<Map<String, Object>> notRecommendedApproach(String dbName, String tableName) {
    DataSourceContext.setCurrentDataSource(dbName);
    TableContext.setTableMapping("logical_table", tableName);
    try {
        return repository.findAll("SELECT * FROM logical_table");
    } finally {
        DataSourceContext.clearDataSource();
        TableContext.clearTableMappings();
    }
}
```

**2. 错误处理**
```java
public List<Map<String, Object>> safeManualRouting(String dbName, String tableName) {
    try {
        return DataSourceContext.executeWithDataSource(dbName, () -> {
            return TableContext.executeWithTableMapping("users", tableName, () -> {
                return repository.findAll("SELECT * FROM users WHERE active = 1");
            });
        });
    } catch (DataAccessException e) {
        log.error("手动路由失败: db={}, table={}", dbName, tableName, e);
        // 降级到默认数据源和表
        return repository.findAll("SELECT * FROM users WHERE active = 1");
    }
}
```

**3. 参数验证**
```java
public List<Map<String, Object>> validatedManualRouting(String dbName, String tableName) {
    // 验证数据源名称
    if (!isValidDataSource(dbName)) {
        throw new IllegalArgumentException("无效的数据源名称: " + dbName);
    }
    
    // 验证表名
    if (!isValidTableName(tableName)) {
        throw new IllegalArgumentException("无效的表名: " + tableName);
    }
    
    return DataSourceContext.executeWithDataSource(dbName, () -> {
        return TableContext.executeWithTableMapping("users", tableName, () -> {
            return repository.findAll("SELECT * FROM users");
        });
    });
}

private boolean isValidDataSource(String dbName) {
    return Arrays.asList("main", "analytics", "backup", "shard_0", "shard_1").contains(dbName);
}

private boolean isValidTableName(String tableName) {
    return tableName != null && tableName.matches("^[a-zA-Z_][a-zA-Z0-9_]*$");
}
```

## 使用示例

### 1. 分库分表场景

```java
@Service
public class ShardingService {
    
    @Autowired
    private EnhancedMultiDataSourceRepository repository;
    
    public Map<String, Object> getUserById(Long userId) {
        // 根据用户ID计算分片
        int shardIndex = (int) (userId % 4);
        String dataSource = "shard_" + shardIndex;
        String tableName = "users_" + shardIndex;
        
        return DataSourceContext.executeWithDataSource(dataSource, () -> {
            return TableContext.executeWithTableMapping("users", tableName, () -> {
                return repository.findOne("SELECT * FROM users WHERE id = ?", userId);
            });
        });
    }
    
    public void saveUser(Long userId, String name, String email) {
        int shardIndex = (int) (userId % 4);
        String dataSource = "shard_" + shardIndex;
        String tableName = "users_" + shardIndex;
        
        DataSourceContext.executeWithDataSource(dataSource, () -> {
            TableContext.executeWithTableMapping("users", tableName, () -> {
                repository.execute(
                    "INSERT INTO users (id, name, email) VALUES (?, ?, ?)",
                    userId, name, email
                );
            });
        });
    }
}
```

### 2. 多租户场景

```java
@Service
public class MultiTenantService {
    
    public List<Map<String, Object>> getTenantData(String tenantId) {
        String dataSource = "tenant_" + tenantId;
        String tableName = "data_" + tenantId;
        
        return DataSourceContext.executeWithDataSource(dataSource, () -> {
            return TableContext.executeWithTableMapping("tenant_data", tableName, () -> {
                return repository.findAll("SELECT * FROM tenant_data WHERE active = 1");
            });
        });
    }
}
```

### 3. 读写分离场景

```java
@Service
public class ReadWriteService {
    
    // 写操作使用主库
    public void createUser(String name, String email) {
        DataSourceContext.executeWithDataSource("master", () -> {
            repository.execute(
                "INSERT INTO users (name, email) VALUES (?, ?)",
                name, email
            );
        });
    }
    
    // 读操作使用从库
    public List<Map<String, Object>> getUsers() {
        return DataSourceContext.executeWithDataSource("slave", () -> {
            return repository.findAll("SELECT * FROM users ORDER BY created_date DESC");
        });
    }
}
```

### 4. 历史数据查询

```java
@Service
public class HistoryDataService {
    
    public List<Map<String, Object>> getMonthlyData(String month) {
        String tableName = "data_" + month;
        
        return TableContext.executeWithTableMapping("monthly_data", tableName, () -> {
            return repository.findAll("SELECT * FROM monthly_data WHERE processed = 1");
        });
    }
    
    public List<Map<String, Object>> getYearlyReport(int year) {
        List<Map<String, Object>> allData = new ArrayList<>();
        
        for (int month = 1; month <= 12; month++) {
            String tableName = String.format("data_%d%02d", year, month);
            
            List<Map<String, Object>> monthData = TableContext.executeWithTableMapping(
                "monthly_data", tableName, () -> {
                    return repository.findAll(
                        "SELECT month, SUM(amount) as total FROM monthly_data GROUP BY month"
                    );
                }
            );
            
            allData.addAll(monthData);
        }
        
        return allData;
    }
}
```

## 最佳实践

### 1. 资源管理

- **使用 try-with-resources 模式**：确保资源正确释放
- **避免嵌套过深**：限制数据源和表名切换的嵌套层级
- **及时清理**：在适当的时机清理 ThreadLocal 变量

```java
// 推荐的资源管理方式
public void processData() {
    DataSourceContext.executeWithDataSource("analytics", () -> {
        TableContext.executeWithTableMapping("data", "data_2024", () -> {
            // 执行业务逻辑
            return repository.findAll("SELECT * FROM data");
        });
    });
    // 自动清理，无需手动管理
}
```

### 2. 错误处理

```java
public List<Map<String, Object>> safeDataAccess(String dataSource, String table) {
    try {
        return DataSourceContext.executeWithDataSource(dataSource, () -> {
            return TableContext.executeWithTableMapping("logical_table", table, () -> {
                return repository.findAll("SELECT * FROM logical_table");
            });
        });
    } catch (DataAccessException e) {
        log.error("数据访问失败: dataSource={}, table={}", dataSource, table, e);
        return Collections.emptyList();
    }
}
```

### 3. 性能优化

- **连接池配置**：合理配置连接池大小
- **缓存策略**：对频繁访问的数据进行缓存
- **批量操作**：使用批量操作减少数据库交互

```java
// 批量操作示例
public void batchInsert(List<User> users) {
    DataSourceContext.executeWithDataSource("main", () -> {
        TableContext.executeWithTableMapping("users", "users_batch", () -> {
            repository.batchUpdate(
                "INSERT INTO users (name, email) VALUES (?, ?)",
                users.stream()
                    .map(u -> new Object[]{u.getName(), u.getEmail()})
                    .collect(Collectors.toList())
            );
        });
    });
}
```

### 4. 监控和日志

```java
@Component
public class DataSourceMonitor {
    
    private static final Logger log = LoggerFactory.getLogger(DataSourceMonitor.class);
    
    public <T> T monitoredExecution(String operation, Supplier<T> supplier) {
        long startTime = System.currentTimeMillis();
        String currentDataSource = DataSourceContext.getCurrentDataSource();
        
        try {
            T result = supplier.get();
            long executionTime = System.currentTimeMillis() - startTime;
            
            log.info("操作完成: operation={}, dataSource={}, executionTime={}ms", 
                    operation, currentDataSource, executionTime);
            
            return result;
        } catch (Exception e) {
            log.error("操作失败: operation={}, dataSource={}", operation, currentDataSource, e);
            throw e;
        }
    }
}
```

## 常见问题

### Q1: 如何处理事务？

**A**: 每个数据源的事务是独立的，跨数据源操作需要使用分布式事务或补偿机制。

```java
@Transactional
public void singleDataSourceTransaction() {
    // 在同一个数据源内的操作会在同一个事务中
    DataSourceContext.executeWithDataSource("main", () -> {
        repository.execute("INSERT INTO users (name) VALUES (?)", "张三");
        repository.execute("INSERT INTO logs (message) VALUES (?)", "用户创建");
    });
}
```

### Q2: 如何处理数据源连接失败？

**A**: 实现重试机制和降级策略。

```java
@Retryable(value = {DataAccessException.class}, maxAttempts = 3)
public List<Map<String, Object>> resilientDataAccess(String dataSource) {
    try {
        return DataSourceContext.executeWithDataSource(dataSource, () -> {
            return repository.findAll("SELECT * FROM users");
        });
    } catch (DataAccessException e) {
        // 降级到备用数据源
        return DataSourceContext.executeWithDataSource("backup", () -> {
            return repository.findAll("SELECT * FROM users");
        });
    }
}
```

### Q3: 如何实现动态数据源配置？

**A**: 可以通过配置中心或数据库动态加载数据源配置。

```java
@Component
public class DynamicDataSourceManager {
    
    private final Map<String, DataSource> dynamicDataSources = new ConcurrentHashMap<>();
    
    public void addDataSource(String name, DataSourceProperties properties) {
        DataSource dataSource = DataSourceBuilder.create()
            .driverClassName(properties.getDriverClassName())
            .url(properties.getUrl())
            .username(properties.getUsername())
            .password(properties.getPassword())
            .build();
        
        dynamicDataSources.put(name, dataSource);
        // 更新数据源映射
    }
    
    public void removeDataSource(String name) {
        DataSource dataSource = dynamicDataSources.remove(name);
        if (dataSource instanceof Closeable) {
            try {
                ((Closeable) dataSource).close();
            } catch (IOException e) {
                log.error("关闭数据源失败: {}", name, e);
            }
        }
    }
}
```

## 性能优化

### 1. 连接池优化

```yaml
spring:
  datasource:
    main:
      hikari:
        # 核心配置
        maximum-pool-size: 20        # 最大连接数
        minimum-idle: 5              # 最小空闲连接数
        connection-timeout: 30000    # 连接超时时间
        idle-timeout: 600000         # 空闲超时时间
        max-lifetime: 1800000        # 连接最大生存时间
        
        # 性能优化
        leak-detection-threshold: 60000  # 连接泄漏检测阈值
        validation-timeout: 5000         # 连接验证超时
        initialization-fail-timeout: 1   # 初始化失败超时
```

### 2. 缓存策略

```java
@Service
public class CachedDataService {
    
    @Cacheable(value = "userData", key = "#dataSource + '_' + #userId")
    public Map<String, Object> getCachedUser(String dataSource, Long userId) {
        return DataSourceContext.executeWithDataSource(dataSource, () -> {
            return repository.findOne("SELECT * FROM users WHERE id = ?", userId);
        });
    }
    
    @CacheEvict(value = "userData", key = "#dataSource + '_' + #userId")
    public void evictUserCache(String dataSource, Long userId) {
        // 缓存清理
    }
}
```

### 3. 批量操作

```java
public void batchProcess(List<Long> userIds) {
    // 按分片分组
    Map<String, List<Long>> shardGroups = userIds.stream()
        .collect(Collectors.groupingBy(id -> "shard_" + (id % 4)));
    
    // 并行处理各分片
    shardGroups.entrySet().parallelStream().forEach(entry -> {
        String dataSource = entry.getKey();
        List<Long> ids = entry.getValue();
        
        DataSourceContext.executeWithDataSource(dataSource, () -> {
            String sql = "SELECT * FROM users WHERE id IN (" + 
                        ids.stream().map(id -> "?").collect(Collectors.joining(",")) + ")";
            return repository.findAll(sql, ids.toArray());
        });
    });
}
```

---

## 总结

多数据源多表切换功能为复杂的数据访问场景提供了灵活的解决方案。通过合理的配置和使用，可以有效支持分库分表、多租户、读写分离等各种业务需求。

关键要点：
- 正确配置数据源和连接池
- 合理使用作用域管理避免资源泄漏
- 实现适当的错误处理和监控
- 根据业务场景选择合适的使用模式
- 注意性能优化和资源管理

更多详细信息请参考项目文档和示例代码。