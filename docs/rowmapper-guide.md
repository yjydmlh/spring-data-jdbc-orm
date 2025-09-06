# 通用RowMapper使用指南

## 概述

本框架提供了一个强大的通用RowMapper，支持多种数据库的常见数据类型，包括PostgreSQL、MySQL、Oracle、SQL Server等主流数据库的特殊数据类型。

## 支持的数据类型

### 基础数据类型
- `String` - 字符串类型，支持CLOB大文本
- `Integer/int` - 整数类型
- `Long/long` - 长整数类型
- `Double/double` - 双精度浮点数
- `Float/float` - 单精度浮点数
- `Boolean/boolean` - 布尔类型
- `BigDecimal` - 高精度数值类型
- `byte[]` - 字节数组，支持BLOB类型

### 时间日期类型
- `java.sql.Date` - SQL日期类型
- `java.sql.Time` - SQL时间类型
- `java.sql.Timestamp` - SQL时间戳类型
- `LocalDate` - Java 8本地日期
- `LocalTime` - Java 8本地时间
- `LocalDateTime` - Java 8本地日期时间
- `Instant` - Java 8瞬时时间

### PostgreSQL特殊类型
- `UUID` - 通用唯一标识符
- `Array Types` - 数组类型（如`String[]`、`Integer[]`等）
- `JSON/JSONB` - JSON数据类型（映射为`Map`或`List`）

### MySQL特殊类型
- `JSON` - JSON数据类型（映射为`Map`或`List`）

### 通用特殊类型
- `Enum` - 枚举类型（支持字符串和序号两种方式）
- `Map<String, Object>` - JSON对象映射
- `List<?>` - JSON数组映射

## 配置选项

### application.yml配置

```yaml
spring:
  jdbc:
    orm:
      rowmapper:
        # 是否启用通用RowMapper（默认：true）
        enable-universal: true
        # 是否启用严格模式（默认：false）
        strict-mode: false
        # JSON解析失败策略（RETURN_NULL, RETURN_STRING, THROW_EXCEPTION）
        json-failure-strategy: RETURN_STRING
        # 数组处理策略（AUTO_DETECT, STRING_SPLIT, JDBC_ARRAY）
        array-handling-strategy: AUTO_DETECT
        # UUID处理策略（STRING, BINARY, AUTO）
        uuid-handling-strategy: AUTO
```

### 程序化配置

```java
@Autowired
private RowMapperFactory rowMapperFactory;

// 启用/禁用通用RowMapper
rowMapperFactory.setEnableUniversalMapper(true);

// 强制使用通用RowMapper
RowMapper<User> universalMapper = rowMapperFactory.getUniversalRowMapper(User.class);

// 强制使用Spring默认RowMapper
RowMapper<User> beanMapper = rowMapperFactory.getBeanPropertyRowMapper(User.class);
```

## 使用示例

### 1. 基础实体类

```java
@Table("users")
public class User {
    @Id
    private Long id;
    
    private String userName;
    private String email;
    private Integer age;
    private LocalDateTime createdAt;
    
    // getters and setters...
}
```

### 2. PostgreSQL特殊类型实体

```java
@Table("products")
public class Product {
    @Id
    private Long id;
    
    @Column("product_uuid")
    private UUID productUuid;  // PostgreSQL UUID类型
    
    @Column("tag_list")
    private String[] tags;  // PostgreSQL 数组类型
    
    @Column("attributes")
    private Map<String, Object> attributes;  // JSON类型
    
    @Column("specifications")
    private List<String> specifications;  // JSON数组类型
    
    @Column("status")
    private ProductStatus status;  // 枚举类型
    
    // getters and setters...
}
```

### 3. 对应的数据库表结构

#### PostgreSQL表结构

```sql
CREATE TABLE products (
    id BIGSERIAL PRIMARY KEY,
    product_uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2),
    tag_list TEXT[],  -- 数组类型
    attributes JSONB,  -- JSON类型
    specifications JSON,  -- JSON数组
    status VARCHAR(20),  -- 枚举
    image_data BYTEA,  -- 二进制数据
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### MySQL表结构

```sql
CREATE TABLE products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_uuid VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(10,2),
    tag_list JSON,  -- 使用JSON存储数组
    attributes JSON,  -- JSON类型
    specifications JSON,  -- JSON数组
    status ENUM('ACTIVE', 'INACTIVE', 'DISCONTINUED', 'OUT_OF_STOCK'),
    image_data LONGBLOB,  -- 二进制数据
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

## 数据库特定配置

### PostgreSQL配置

```xml
<!-- pom.xml -->
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
```

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mydb
    username: postgres
    password: password
    driver-class-name: org.postgresql.Driver
```

### MySQL配置

```xml
<!-- pom.xml -->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
</dependency>
```

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb?useSSL=false&serverTimezone=UTC
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver
```

## 最佳实践

### 1. 类型选择建议

- **UUID类型**：PostgreSQL使用`UUID`，MySQL使用`VARCHAR(36)`
- **JSON类型**：优先使用`Map<String, Object>`表示JSON对象，`List<?>`表示JSON数组
- **数组类型**：PostgreSQL使用原生数组，MySQL使用JSON数组
- **枚举类型**：PostgreSQL使用字符串枚举，MySQL使用ENUM类型

### 2. 性能优化

- 启用RowMapper缓存（默认已启用）
- 对于简单实体，可以禁用通用RowMapper使用Spring默认实现
- 大量数据处理时，考虑使用批处理操作

### 3. 错误处理

- 启用严格模式进行开发调试
- 生产环境建议使用非严格模式，避免因数据类型问题导致系统崩溃
- 合理配置JSON解析失败策略

## 扩展支持

如需支持其他数据库特殊类型，可以扩展`UniversalRowMapper`类：

```java
public class CustomRowMapper<T> extends UniversalRowMapper<T> {
    
    public CustomRowMapper(Class<T> entityClass, EntityMetadataRegistry metadataRegistry) {
        super(entityClass, metadataRegistry);
    }
    
    @Override
    protected Object extractValue(ResultSet rs, String columnName, Class<?> fieldType) throws SQLException {
        // 添加自定义类型处理逻辑
        if (fieldType == MyCustomType.class) {
            return handleCustomType(rs, columnName);
        }
        
        return super.extractValue(rs, columnName, fieldType);
    }
    
    private MyCustomType handleCustomType(ResultSet rs, String columnName) throws SQLException {
        // 自定义类型处理逻辑
        return null;
    }
}
```

## 注意事项

1. **数据库兼容性**：不同数据库对相同数据类型的实现可能有差异
2. **性能影响**：通用RowMapper比Spring默认实现稍慢，但提供更强的类型支持
3. **JSON处理**：需要Jackson依赖，确保项目中已包含相关依赖
4. **数组类型**：主要支持PostgreSQL，其他数据库需要特殊处理
5. **枚举类型**：支持字符串和序号两种映射方式，建议使用字符串方式