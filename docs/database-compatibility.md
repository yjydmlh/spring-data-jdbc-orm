# 数据库兼容性指南

## 支持的数据库

### MySQL
- **MySQL 5.7** ✅ 完全支持
- **MySQL 8.0** ✅ 完全支持  
- **MySQL 8.4** ✅ 完全支持

### PostgreSQL
- **PostgreSQL 12+** ✅ 完全支持
- **PostgreSQL 13** ✅ 完全支持
- **PostgreSQL 14** ✅ 完全支持
- **PostgreSQL 15** ✅ 完全支持
- **PostgreSQL 16** ✅ 完全支持

### 其他数据库
- **H2** ✅ 测试环境支持
- **SQLite** ⚠️ 基础支持
- **Oracle** 🔄 计划支持
- **SQL Server** 🔄 计划支持

## 数据类型映射

### MySQL 数据类型支持

#### 数值类型

| MySQL类型 | Java类型 | 说明 |
|-----------|----------|------|
| `TINYINT` | `Byte`, `Boolean` | 1字节整数 |
| `SMALLINT` | `Short` | 2字节整数 |
| `MEDIUMINT` | `Integer` | 3字节整数 |
| `INT`, `INTEGER` | `Integer` | 4字节整数 |
| `BIGINT` | `Long` | 8字节整数 |
| `FLOAT` | `Float` | 单精度浮点 |
| `DOUBLE` | `Double` | 双精度浮点 |
| `DECIMAL`, `NUMERIC` | `BigDecimal` | 精确小数 |
| `BIT` | `Boolean`, `byte[]` | 位类型 |

#### 字符串类型

| MySQL类型 | Java类型 | 说明 |
|-----------|----------|------|
| `CHAR` | `String` | 定长字符串 |
| `VARCHAR` | `String` | 变长字符串 |
| `TINYTEXT` | `String` | 最大255字符 |
| `TEXT` | `String` | 最大65535字符 |
| `MEDIUMTEXT` | `String` | 最大16MB |
| `LONGTEXT` | `String` | 最大4GB |
| `BINARY` | `byte[]` | 定长二进制 |
| `VARBINARY` | `byte[]` | 变长二进制 |
| `TINYBLOB` | `byte[]` | 最大255字节 |
| `BLOB` | `byte[]` | 最大65KB |
| `MEDIUMBLOB` | `byte[]` | 最大16MB |
| `LONGBLOB` | `byte[]` | 最大4GB |

#### 日期时间类型

| MySQL类型 | Java类型 | 说明 |
|-----------|----------|------|
| `DATE` | `LocalDate` | 日期 |
| `TIME` | `LocalTime` | 时间 |
| `DATETIME` | `LocalDateTime` | 日期时间 |
| `TIMESTAMP` | `LocalDateTime`, `Instant` | 时间戳 |
| `YEAR` | `Year`, `Integer` | 年份 |

#### JSON类型

| MySQL类型 | Java类型 | 说明 |
|-----------|----------|------|
| `JSON` | `String`, `JsonNode`, 自定义对象 | JSON数据 |

#### 几何类型

| MySQL类型 | Java类型 | 说明 |
|-----------|----------|------|
| `GEOMETRY` | `byte[]`, `String` | 几何对象 |
| `POINT` | `Point`, `String` | 点 |
| `LINESTRING` | `LineString`, `String` | 线 |
| `POLYGON` | `Polygon`, `String` | 多边形 |

### PostgreSQL 数据类型支持

#### 基础数据类型

| PostgreSQL类型 | Java类型 | 说明 |
|----------------|----------|------|
| `BOOLEAN` | `Boolean` | 布尔值 |
| `SMALLINT` | `Short` | 2字节整数 |
| `INTEGER` | `Integer` | 4字节整数 |
| `BIGINT` | `Long` | 8字节整数 |
| `REAL` | `Float` | 单精度浮点 |
| `DOUBLE PRECISION` | `Double` | 双精度浮点 |
| `NUMERIC`, `DECIMAL` | `BigDecimal` | 精确小数 |
| `CHAR` | `String` | 定长字符串 |
| `VARCHAR` | `String` | 变长字符串 |
| `TEXT` | `String` | 无限长文本 |
| `BYTEA` | `byte[]` | 二进制数据 |

#### 日期/时间类型

| PostgreSQL类型 | Java类型 | 说明 |
|----------------|----------|------|
| `DATE` | `LocalDate` | 日期 |
| `TIME` | `LocalTime` | 时间 |
| `TIME WITH TIME ZONE` | `OffsetTime` | 带时区时间 |
| `TIMESTAMP` | `LocalDateTime` | 时间戳 |
| `TIMESTAMP WITH TIME ZONE` | `OffsetDateTime`, `ZonedDateTime` | 带时区时间戳 |
| `INTERVAL` | `Duration`, `Period` | 时间间隔 |

#### PostgreSQL特有类型

| PostgreSQL类型 | Java类型 | 说明 |
|----------------|----------|------|
| `UUID` | `UUID` | 通用唯一标识符 |
| `JSON` | `String`, `JsonNode` | JSON数据 |
| `JSONB` | `String`, `JsonNode` | 二进制JSON |
| `XML` | `String` | XML数据 |
| `INET` | `InetAddress`, `String` | IP地址 |
| `CIDR` | `String` | 网络地址 |
| `MACADDR` | `String` | MAC地址 |
| `POINT` | `Point`, `String` | 几何点 |
| `LINE` | `String` | 几何线 |
| `LSEG` | `String` | 线段 |
| `BOX` | `String` | 矩形 |
| `PATH` | `String` | 路径 |
| `POLYGON` | `String` | 多边形 |
| `CIRCLE` | `String` | 圆 |

#### 数组类型

| PostgreSQL类型 | Java类型 | 说明 |
|----------------|----------|------|
| `INTEGER[]` | `Integer[]`, `List<Integer>` | 整数数组 |
| `TEXT[]` | `String[]`, `List<String>` | 文本数组 |
| `NUMERIC[]` | `BigDecimal[]`, `List<BigDecimal>` | 数值数组 |

#### 枚举类型

```sql
-- PostgreSQL枚举定义
CREATE TYPE mood AS ENUM ('sad', 'ok', 'happy');
```

```java
// Java枚举映射
public enum Mood {
    SAD("sad"),
    OK("ok"), 
    HAPPY("happy");
    
    private final String value;
    
    Mood(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
}
```

## 通用映射规则

### 1. 自动类型推断

UniversalRowMapper会根据数据库元数据自动推断Java类型：

```java
@Entity
public class User {
    private Long id;              // BIGINT -> Long
    private String name;          // VARCHAR -> String
    private LocalDate birthDate;  // DATE -> LocalDate
    private Boolean active;       // BOOLEAN -> Boolean
    private BigDecimal salary;    // DECIMAL -> BigDecimal
    
    // getters and setters
}
```

### 2. 自定义类型转换

```java
@Configuration
public class TypeConverterConfig {
    
    @Bean
    public TypeConverter<String, CustomType> customTypeConverter() {
        return new TypeConverter<String, CustomType>() {
            @Override
            public CustomType convert(String source) {
                return CustomType.fromString(source);
            }
            
            @Override
            public String convertBack(CustomType target) {
                return target.toString();
            }
        };
    }
}
```

### 3. JSON类型处理

```java
@Entity
public class Product {
    private Long id;
    private String name;
    
    // JSON字段自动映射
    @JsonProperty
    private Map<String, Object> attributes;
    
    // 自定义JSON对象
    @JsonProperty
    private ProductSpec specification;
    
    // getters and setters
}

// 自定义JSON对象
public class ProductSpec {
    private String color;
    private String size;
    private List<String> features;
    
    // getters and setters
}
```

### 4. 数组类型处理

```java
// PostgreSQL数组支持
@Entity
public class Article {
    private Long id;
    private String title;
    
    // PostgreSQL数组字段
    @ArrayProperty
    private List<String> tags;
    
    @ArrayProperty
    private Integer[] categoryIds;
    
    // getters and setters
}
```

## 配置选项

### 启用UniversalRowMapper

```yaml
flex:
  data:
    universal-row-mapper:
      enabled: true  # 默认为true
      
      # 类型转换配置
      type-conversion:
        # 日期时间格式
        date-format: "yyyy-MM-dd"
        datetime-format: "yyyy-MM-dd HH:mm:ss"
        
        # 数值精度
        decimal-scale: 2
        
        # 字符串处理
        trim-strings: true
        empty-string-as-null: false
        
      # JSON处理
      json:
        # JSON库选择: jackson, gson, fastjson
        library: jackson
        # 未知属性处理
        fail-on-unknown-properties: false
        
      # 数组处理（PostgreSQL）
      array:
        # 数组分隔符
        delimiter: ","
        # 空数组处理
        empty-array-as-null: false
```

### 数据库特定配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/testdb?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC
    # MySQL特定参数
    hikari:
      connection-init-sql: "SET NAMES utf8mb4"
      
  # PostgreSQL配置
  datasource:
    url: jdbc:postgresql://localhost:5432/testdb?useUnicode=true&characterEncoding=UTF-8&stringtype=unspecified
    hikari:
      connection-init-sql: "SET search_path TO public"
```

## 性能优化

### 1. 批量操作优化

```java
@Repository
public class OptimizedRepository extends BaseRepository<User, Long> {
    
    // 批量插入优化
    @BatchSize(100)
    public void batchInsert(List<User> users) {
        saveAll(users);
    }
    
    // 流式查询大数据集
    @StreamQuery
    public Stream<User> streamAllUsers() {
        return query().stream();
    }
}
```

### 2. 连接池优化

```yaml
spring:
  datasource:
    hikari:
      # MySQL优化
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      
      # PostgreSQL优化
      data-source-properties:
        cachePrepStmts: true
        prepStmtCacheSize: 250
        prepStmtCacheSqlLimit: 2048
        useServerPrepStmts: true
```

### 3. 查询优化

```java
// 使用索引提示（MySQL）
@Query("SELECT /*+ USE_INDEX(users, idx_email) */ * FROM users WHERE email = ?")
List<User> findByEmailWithHint(String email);

// 使用EXPLAIN分析（PostgreSQL）
@Query(value = "EXPLAIN ANALYZE SELECT * FROM users WHERE age > ?", nativeQuery = true)
List<Object[]> explainQuery(int age);
```

## 迁移指南

### MySQL到PostgreSQL迁移

```java
@Service
public class DatabaseMigrationService {
    
    public void migrateUserData() {
        // 从MySQL读取
        List<User> users = DataSourceContext.executeWithDataSource("mysql", () -> {
            return userRepository.findAll();
        });
        
        // 转换数据类型
        List<User> convertedUsers = users.stream()
            .map(this::convertMysqlToPostgres)
            .collect(Collectors.toList());
        
        // 写入PostgreSQL
        DataSourceContext.executeWithDataSource("postgresql", () -> {
            return userRepository.saveAll(convertedUsers);
        });
    }
    
    private User convertMysqlToPostgres(User user) {
        // 处理数据类型差异
        // MySQL的TINYINT(1) -> PostgreSQL的BOOLEAN
        // MySQL的JSON -> PostgreSQL的JSONB
        return user;
    }
}
```

## 故障排除

### 常见问题

#### 1. 字符编码问题

**MySQL**:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/db?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=UTC
```

**PostgreSQL**:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/db?useUnicode=true&characterEncoding=UTF-8
```

#### 2. 时区问题

```java
// 统一使用UTC时区
@Configuration
public class TimeZoneConfig {
    
    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }
}
```

#### 3. 类型转换异常

```java
// 自定义异常处理
@ControllerAdvice
public class TypeConversionExceptionHandler {
    
    @ExceptionHandler(TypeConversionException.class)
    public ResponseEntity<String> handleTypeConversion(TypeConversionException e) {
        log.error("Type conversion failed: {}", e.getMessage());
        return ResponseEntity.badRequest().body("数据类型转换失败: " + e.getMessage());
    }
}
```

### 调试技巧

```yaml
# 启用SQL日志
logging:
  level:
    org.springframework.jdbc.core: DEBUG
    com.spring.jdbc.orm: DEBUG
    
# 显示SQL参数
flex:
  data:
    show-sql: true
    format-sql: true
    show-sql-parameters: true
```

---

通过本指南，您可以充分了解框架对各种数据库的支持情况，并根据实际需求选择合适的数据类型映射策略。