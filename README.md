# Spring Data JDBC Type-Safe ORM Framework

一个基于Spring Data JDBC的企业级类型安全ORM框架，提供强大的无SQL编写数据库操作能力，支持复杂查询、多数据库兼容和高性能数据访问。

[![Java Version](https://img.shields.io/badge/Java-8%2B-blue.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-2.7.18-green.svg)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

## 🚀 核心特性

### 基础能力
- ✅ **类型安全** - 使用Lambda表达式引用字段，编译时检查，避免字段名错误
- ✅ **零SQL编写** - 通过流畅的API构建查询，自动生成优化的SQL语句
- ✅ **IDE友好** - 完整的代码提示、自动补全和重构支持
- ✅ **高性能** - 元数据缓存、连接池优化、批量操作支持
- ✅ **易扩展** - 插件系统和模块化设计，支持自定义扩展

### 高级SQL功能
- 🔥 **复杂查询** - 支持多表JOIN、子查询、UNION、CTE（公共表表达式）
- 🔥 **聚合函数** - COUNT、SUM、AVG、MAX、MIN及高级聚合功能
- 🔥 **窗口函数** - ROW_NUMBER、RANK、LAG、LEAD等分析函数
- 🔥 **条件构建** - 支持复杂的WHERE条件组合和动态查询
- 🔥 **分页排序** - 内置分页支持和多字段排序

### 数据库兼容性
- 🗄️ **MySQL** - 完全支持，包括JSON、空间数据类型
- 🗄️ **PostgreSQL** - 完全支持，包括UUID、数组、JSONB、网络地址等特殊类型
- 🗄️ **Oracle** - 基础支持，支持常见数据类型和查询
- 🗄️ **SQL Server** - 基础支持，支持常见数据类型和查询

## 📋 系统要求

- **Java**: 8+ (推荐 Java 11 或 17 LTS)
- **Spring Boot**: 2.7.x+
- **数据库**: MySQL 5.7+, PostgreSQL 10+, Oracle 11g+, SQL Server 2012+

## 🚀 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>spring-data-jdbc-orm</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 配置数据源

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/testdb
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver
  
  jdbc:
    orm:
      # 启用类型安全ORM
      enable-type-safe: true
      # 启用性能监控
      enable-performance-monitor: true
      # 启用SQL日志
      enable-sql-logging: true
```

### 3. 定义实体类

```java
@Table("users")
public class User {
    @Id
    private Long id;
    
    private String userName;
    private String email;
    private Integer age;
    private LocalDateTime createdAt;
    
    // 构造函数、getter和setter...
}
```

### 4. 创建Repository

```java
@Repository
public interface UserRepository extends TypeSafeRepository<User, Long> {
    // 继承所有基础CRUD方法
}
```

### 5. 使用类型安全查询

```java
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    // 基础查询
    public List<User> findActiveUsers() {
        return userRepository.findBy(
            criteria -> criteria.eq(User::getStatus, "ACTIVE")
                              .gt(User::getAge, 18)
        );
    }
    
    // 复杂查询
    public List<User> findUsersWithOrders() {
        return userRepository.createQuery()
            .select(User::getUserName, User::getEmail)
            .leftJoin("orders", "o", "o.user_id = u.id")
            .where(criteria -> criteria.isNotNull("o.id"))
            .orderBy(User::getCreatedAt, DESC)
            .limit(10)
            .execute();
    }
    
    // 聚合查询
    public Map<String, Object> getUserStatistics() {
        return userRepository.createQuery()
            .selectCount(User::getId, "total_users")
            .selectAvg(User::getAge, "avg_age")
            .selectMax(User::getCreatedAt, "latest_registration")
            .executeForMap();
    }
}
```

## 📚 高级功能

### 窗口函数查询

```java
// 用户排名查询
List<Map<String, Object>> userRanking = userRepository.createQuery()
    .select(User::getUserName)
    .select(User::getScore)
    .selectWindowFunction(
        WindowFunctionBuilder.rowNumber()
            .partitionBy(User::getDepartment)
            .orderByDesc(User::getScore)
            .as("rank")
    )
    .executeForMaps();
```

### CTE（公共表表达式）

```java
// 递归查询组织结构
List<Department> departments = departmentRepository.createQuery()
    .withCte("dept_hierarchy", 
        subQuery -> subQuery
            .select("id", "name", "parent_id", "1 as level")
            .from("departments")
            .where(criteria -> criteria.isNull("parent_id"))
            .unionAll(
                subQuery2 -> subQuery2
                    .select("d.id", "d.name", "d.parent_id", "dh.level + 1")
                    .from("departments", "d")
                    .join("dept_hierarchy", "dh", "d.parent_id = dh.id")
            )
    )
    .select("*")
    .from("dept_hierarchy")
    .execute();
```

### 批量操作

```java
// 批量插入
List<User> users = Arrays.asList(/* 用户列表 */);
userRepository.saveAll(users);

// 批量更新
userRepository.updateBatch(
    users,
    criteria -> criteria.eq(User::getStatus, "INACTIVE")
);
```

## 🔧 配置选项

### application.yml 完整配置

```yaml
spring:
  jdbc:
    orm:
      # 基础配置
      enable-type-safe: true
      enable-enhanced-query: true
      
      # 性能配置
      enable-performance-monitor: true
      enable-sql-logging: true
      enable-metadata-cache: true
      
      # 数据映射配置
      rowmapper:
        enable-universal: true
        strict-mode: false
        json-failure-strategy: RETURN_STRING
        array-handling-strategy: AUTO_DETECT
        uuid-handling-strategy: AUTO
      
      # 插件配置
      plugins:
        validation:
          enabled: true
        audit:
          enabled: true
          created-by-field: "createdBy"
          updated-by-field: "updatedBy"
        cache:
          enabled: true
          ttl: 300
```

## 📖 文档

- [通用RowMapper使用指南](ROWMAPPER_GUIDE.md)
- [MySQL数据类型支持](docs/MySQL_DataTypes_Support.md)
- [PostgreSQL数据类型支持](docs/PostgreSQL_DataTypes_Support.md)

## 🧪 测试

```bash
# 运行所有测试
mvn test

# 运行特定测试
mvn test -Dtest=ComplexSqlTest

# 生成测试报告
mvn surefire-report:report
```

## 🤝 贡献

欢迎提交Issue和Pull Request来帮助改进这个项目！

1. Fork 项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 📄 许可证

本项目采用 Apache 2.0 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 🙏 致谢

- [Spring Data JDBC](https://spring.io/projects/spring-data-jdbc) - 核心数据访问框架
- [Spring Boot](https://spring.io/projects/spring-boot) - 应用框架
- 所有贡献者和使用者的支持

---

**SQL支持程度**: 约85% - 支持大部分企业级应用所需的SQL功能，包括复杂查询、聚合、窗口函数等，能够满足绝大多数业务场景需求。