# 多数据源多表切换使用示例

本目录包含了多数据源多表切换功能的完整使用示例，帮助您快速上手和理解各种使用场景。

## 文件说明

### 核心示例文件

- **`MultiDataSourceExample.java`** - 完整的使用示例，包含10个不同场景的演示
- **`MultiDataSourceConfiguration.java`** - 多数据源配置示例
- **`application-example.yml`** - 完整的配置文件示例

### 示例场景

#### 1. 基础功能示例
- 基本数据源切换
- 基本表名切换
- 组合使用数据源和表名切换
- 批量表名映射

#### 2. 注解使用示例
- `@Table` 注解的使用方法
- 方法级表名指定

#### 3. 实际业务场景
- **分片场景** - 根据用户ID路由到不同分片
- **多租户场景** - 根据租户ID切换数据源和表
- **读写分离场景** - 读操作使用从库，写操作使用主库
- **数据迁移场景** - 从旧系统迁移数据到新系统
- **性能监控场景** - 展示如何在切换过程中进行监控

## 快速开始

### 1. 配置数据源

```java
@Configuration
public class DataSourceConfig extends EnhancedMultiDataSourceConfig {
    // 参考 MultiDataSourceConfiguration.java
}
```

### 2. 配置文件

```yaml
# 参考 application-example.yml
spring:
  datasource:
    main:
      driver-class-name: com.mysql.cj.jdbc.Driver
      url: jdbc:mysql://localhost:3306/main_db
      username: root
      password: password
```

### 3. 使用示例

```java
@Service
public class YourService {
    
    @Autowired
    private EnhancedMultiDataSourceRepository repository;
    
    // 数据源切换
    public List<Map<String, Object>> getAnalyticsData() {
        return DataSourceContext.executeWithDataSource("analytics", () -> {
            return repository.findAll("SELECT * FROM user_stats");
        });
    }
    
    // 表名切换
    @Table("users_vip")
    public List<Map<String, Object>> getVipUsers() {
        return repository.findAll("SELECT * FROM users WHERE vip = 1");
    }
}
```

## 运行示例

### 1. 环境准备

- Java 8+
- Spring Boot 2.x+
- MySQL/PostgreSQL/Oracle 数据库

### 2. 配置数据库

根据 `application-example.yml` 配置您的数据库连接信息。

### 3. 运行测试

```bash
# 运行所有测试
mvn test

# 运行特定测试
mvn test -Dtest=EnhancedMultiDataSourceTest
```

## 核心API说明

### DataSourceContext - 数据源切换

```java
// 执行并返回结果
T result = DataSourceContext.executeWithDataSource("dataSourceName", () -> {
    // 您的业务逻辑
    return someOperation();
});

// 执行无返回值操作
DataSourceContext.executeWithDataSource("dataSourceName", () -> {
    // 您的业务逻辑
    someVoidOperation();
});
```

### TableContext - 表名切换

```java
// 单表映射
T result = TableContext.executeWithTableMapping("logicalTable", "physicalTable", () -> {
    // 您的业务逻辑
    return someOperation();
});

// 多表映射
Map<String, String> mappings = new HashMap<>();
mappings.put("users", "users_2024");
mappings.put("orders", "orders_2024");

T result = TableContext.executeWithTableMappings(mappings, () -> {
    // 您的业务逻辑
    return someOperation();
});
```

### @Table 注解

```java
@Table("physical_table_name")
public List<Map<String, Object>> someMethod() {
    // 方法内的SQL会自动使用指定的表名
    return repository.findAll("SELECT * FROM logical_table");
}
```

## 最佳实践

1. **资源管理** - 使用提供的作用域方法，避免手动管理ThreadLocal
2. **错误处理** - 实现适当的异常处理和重试机制
3. **性能优化** - 合理配置连接池，使用批量操作
4. **监控日志** - 添加适当的监控和日志记录
5. **测试覆盖** - 为不同场景编写完整的测试用例

## 常见问题

### Q: 如何处理事务？
A: 每个数据源的事务是独立的，跨数据源操作需要考虑分布式事务。

### Q: 如何实现动态配置？
A: 可以通过配置中心或数据库动态加载数据源配置。

### Q: 性能如何优化？
A: 合理配置连接池、使用缓存、批量操作等。

## 更多信息

- 完整使用指南：`MULTI_DATASOURCE_TABLE_GUIDE.md`
- 项目文档：`README.md`
- 测试用例：`src/test/java/com/spring/jdbc/orm/enhanced/`

## 联系支持

如有问题或建议，请提交Issue或联系项目维护者。