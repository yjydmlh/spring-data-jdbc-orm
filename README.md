# Spring Data JDBC Type-Safe ORM

一个基于Spring Data JDBC的类型安全ORM框架，提供零SQL编写的开发体验。

## 🚀 核心特性

- **类型安全**：编译时类型检查，避免运行时SQL错误
- **零SQL编写**：通过Lambda表达式构建查询，无需手写SQL
- **多数据源支持**：灵活的多数据源配置和动态切换
- **智能路由**：基于规则的数据源自动选择
- **通用RowMapper**：自动处理复杂数据类型映射
- **Spring Boot集成**：开箱即用的自动配置

## 📋 系统要求

- Java 8+
- Spring Boot 2.7.x+
- Spring Data JDBC 2.4.x+

## 🎯 快速开始

### 添加依赖

```xml
<dependency>
    <groupId>io.flex.data</groupId>
    <artifactId>flex-data-orm</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 基础配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/testdb
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### 创建实体和Repository

```java
@Table("users")
public class User {
    @Id
    private Long id;
    private String userName;
    private String email;
    // getter和setter...
}

@Repository
public interface UserRepository extends TypeSafeRepository<User, Long> {
    // 继承所有基础CRUD方法
}
```

### 类型安全查询示例

```java
// 基础查询
public List<User> findActiveUsers() {
    return userRepository.findBy(
        criteria -> criteria.eq(User::getStatus, "ACTIVE")
                          .gt(User::getAge, 18)
    );
}
```

详细查询示例请参考：[类型安全查询指南](TYPE_SAFE_QUERY_GUIDE.md)

## 📚 高级功能

### 多数据源支持

框架支持动态多数据源切换，实现读写分离、数据库分片等企业级特性。

详细配置和使用方法请参考：[多数据源配置指南](MULTI_DATASOURCE_GUIDE.md)

### 手动指定库名和表名

框架完全支持开发者手动指定数据源（库名）和表名，提供灵活的数据路由能力。

详细配置和使用方法请参考：[多数据源多表切换完整指南](MULTI_DATASOURCE_TABLE_GUIDE.md)

### 高级查询功能

- **窗口函数查询** - 支持ROW_NUMBER、RANK、SUM等窗口函数
- **CTE（公共表表达式）** - 支持递归查询和复杂子查询
- **批量操作** - 高性能批量插入、更新、删除

## 📖 完整文档

### 多数据源
- [多数据源完整指南](docs/multi-datasource-complete-guide.md)
- [智能路由配置指南](docs/routing-guide.md)

### 数据库支持
- [数据库兼容性指南](docs/database-compatibility.md)
- [通用RowMapper使用指南](docs/rowmapper-guide.md)

## 🧪 测试

```bash
# 运行所有测试
mvn test

# 运行特定测试
mvn test -Dtest=ComplexSqlTest
```

## 🤝 贡献

欢迎提交Issue和Pull Request来帮助改进这个项目！

## 📄 许可证

本项目采用 Apache License 2.0 许可证。

---

**SQL支持程度**: 约 85% 的常用SQL功能 | **数据库支持**: MySQL, PostgreSQL | **Java版本**: 8+