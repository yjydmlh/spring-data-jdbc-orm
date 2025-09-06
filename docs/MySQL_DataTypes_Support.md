# MySQL 数据类型支持文档

## 概述

UniversalRowMapper 为 MySQL 数据库提供了全面的数据类型支持，涵盖了 MySQL 5.7、8.0 和 8.4 版本的所有主要数据类型。

## 支持的 MySQL 版本

- **MySQL 5.7**: 完全支持
- **MySQL 8.0**: 完全支持
- **MySQL 8.4**: 完全支持

## 支持的数据类型

### 1. 数值类型 (Numeric Types)

#### 整数类型
- **TINYINT**: 映射到 `Integer/int`
- **SMALLINT**: 映射到 `Integer/int`
- **MEDIUMINT**: 映射到 `Integer/int`
- **INT/INTEGER**: 映射到 `Integer/int`
- **BIGINT**: 映射到 `Long/long`

#### 浮点类型
- **FLOAT**: 映射到 `Float/float`
- **DOUBLE**: 映射到 `Double/double`
- **DECIMAL/NUMERIC**: 映射到 `BigDecimal`

#### 位类型
- **BIT**: 映射到 `Boolean/boolean` 或 `byte[]`

### 2. 字符串类型 (String Types)

#### 字符类型
- **CHAR**: 映射到 `String`
- **VARCHAR**: 映射到 `String`
- **BINARY**: 映射到 `byte[]`
- **VARBINARY**: 映射到 `byte[]`

#### 文本类型
- **TINYTEXT**: 映射到 `String`
- **TEXT**: 映射到 `String`
- **MEDIUMTEXT**: 映射到 `String`
- **LONGTEXT**: 映射到 `String`

#### 二进制大对象类型
- **TINYBLOB**: 映射到 `byte[]`
- **BLOB**: 映射到 `byte[]`
- **MEDIUMBLOB**: 映射到 `byte[]`
- **LONGBLOB**: 映射到 `byte[]`

#### 枚举和集合类型
- **ENUM**: 映射到 Java `enum` 类型
- **SET**: 映射到 `Set<String>` 或 `String`

### 3. 日期和时间类型 (Date and Time Types)

- **DATE**: 映射到 `java.sql.Date` 或 `LocalDate`
- **TIME**: 映射到 `java.sql.Time` 或 `LocalTime`
- **DATETIME**: 映射到 `java.sql.Timestamp` 或 `LocalDateTime`
- **TIMESTAMP**: 映射到 `java.sql.Timestamp` 或 `LocalDateTime`
- **YEAR**: 映射到 `Integer/int`

### 4. JSON 类型 (MySQL 5.7+)

- **JSON**: 映射到 `Map<String, Object>` 或 `List<Object>`
  - 支持自动 JSON 解析
  - 支持嵌套对象和数组

### 5. 空间数据类型 (Spatial Types)

#### 单值几何类型
- **GEOMETRY**: 映射到 `String` (WKT格式)
- **POINT**: 映射到 `String` (WKT格式)
- **LINESTRING**: 映射到 `String` (WKT格式)
- **POLYGON**: 映射到 `String` (WKT格式)

#### 集合几何类型
- **MULTIPOINT**: 映射到 `String` (WKT格式)
- **MULTILINESTRING**: 映射到 `String` (WKT格式)
- **MULTIPOLYGON**: 映射到 `String` (WKT格式)
- **GEOMETRYCOLLECTION**: 映射到 `String` (WKT格式)

## 特殊功能支持

### 1. 自动类型转换
- 支持数值类型之间的自动转换
- 支持字符串到数值的安全转换
- 支持日期时间格式的自动识别

### 2. NULL 值处理
- 完全支持 NULL 值处理
- 自动跳过不存在的列

### 3. 字符集支持
- 支持 UTF-8、UTF-8MB4、Latin1 等字符集
- 自动处理字符编码转换

### 4. 大对象处理
- **CLOB**: 自动转换为 String
- **BLOB**: 自动转换为 byte[]
- 支持大文本和二进制数据的高效处理

## 使用示例

### 基本数据类型映射

```java
@Entity
@Table(name = "products")
public class Product {
    @Id
    private Long id;                    // BIGINT
    
    private String name;                // VARCHAR
    private BigDecimal price;           // DECIMAL
    private Integer stock;              // INT
    private Boolean active;             // BOOLEAN/TINYINT
    private LocalDateTime createdAt;    // DATETIME
    private byte[] image;               // BLOB
    
    // getters and setters
}
```

### JSON 类型映射

```java
@Entity
@Table(name = "user_profiles")
public class UserProfile {
    @Id
    private Long id;
    
    private String username;
    private Map<String, Object> preferences;  // JSON
    private List<String> tags;                // JSON Array
    
    // getters and setters
}
```

### 空间数据类型映射

```java
@Entity
@Table(name = "locations")
public class Location {
    @Id
    private Long id;
    
    private String name;
    private String coordinates;  // POINT -> WKT format
    private String boundary;     // POLYGON -> WKT format
    
    // getters and setters
}
```

### 枚举类型映射

```java
public enum Status {
    ACTIVE, INACTIVE, PENDING
}

@Entity
@Table(name = "orders")
public class Order {
    @Id
    private Long id;
    
    private Status status;  // ENUM('ACTIVE','INACTIVE','PENDING')
    
    // getters and setters
}
```

## 版本特性支持

### MySQL 5.7 新特性
- ✅ JSON 数据类型
- ✅ 生成列 (Generated Columns)
- ✅ 空间索引增强

### MySQL 8.0 新特性
- ✅ 窗口函数支持
- ✅ CTE (公用表表达式)
- ✅ 角色和权限增强
- ✅ 不可见索引

### MySQL 8.4 新特性
- ✅ 向量数据类型 (实验性)
- ✅ 多值索引
- ✅ 性能优化

## 性能优化

### 1. 类型映射缓存
- 自动缓存字段类型映射关系
- 减少反射调用开销

### 2. 批量处理优化
- 支持批量数据映射
- 优化大结果集处理

### 3. 内存管理
- 智能处理大对象
- 避免内存泄漏

## 注意事项

1. **字符集配置**: 确保数据库连接使用正确的字符集
2. **时区处理**: 注意 TIMESTAMP 类型的时区转换
3. **精度问题**: DECIMAL 类型注意精度和标度设置
4. **JSON 解析**: 复杂 JSON 结构可能需要自定义映射
5. **空间数据**: 空间数据类型需要相应的 MySQL 空间扩展支持

## 错误处理

- 类型转换失败时提供详细错误信息
- 支持部分字段映射失败的容错处理
- 提供调试模式用于问题排查

## 扩展性

- 支持自定义类型转换器
- 可扩展新的数据类型支持
- 插件化架构设计

---

**注意**: 本文档基于 UniversalRowMapper 当前实现，随着 MySQL 新版本发布，将持续更新支持的数据类型和特性。