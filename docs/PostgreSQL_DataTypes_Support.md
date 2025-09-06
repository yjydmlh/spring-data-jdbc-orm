# PostgreSQL 数据类型支持文档

本文档详细说明了 UniversalRowMapper 对 PostgreSQL 数据类型的支持情况。

## 完全支持的数据类型

### 1. 基础数据类型
- **数值类型**: `smallint`, `integer`, `bigint`, `decimal`, `numeric`, `real`, `double precision`, `smallserial`, `serial`, `bigserial`
- **字符串类型**: `character varying`, `varchar`, `character`, `char`, `text`
- **布尔类型**: `boolean`
- **二进制类型**: `bytea`

### 2. 日期/时间类型
- **日期**: `date`
- **时间**: `time [ (p) ] [ without time zone ]`, `time [ (p) ] with time zone`
- **时间戳**: `timestamp [ (p) ] [ without time zone ]`, `timestamp [ (p) ] with time zone`
- **间隔**: `interval [ fields ] [ (p) ]`

### 3. UUID 类型
- **UUID**: `uuid` - 支持字符串和二进制格式的自动转换

### 4. JSON 类型
- **JSON**: `json` - 自动解析为 Map 或 List
- **JSONB**: `jsonb` - 二进制 JSON，自动解析为 Map 或 List

### 5. 数组类型
- **数组**: 支持所有基础类型的数组，如 `integer[]`, `text[]`, `varchar[]`
- 自动转换为对应的 Java 数组类型

### 6. 枚举类型
- **枚举**: 用户定义的枚举类型
- 支持按名称和序号两种方式映射

## 新增支持的 PostgreSQL 特有类型

### 7. 网络地址类型
- **INET**: `inet` - IPv4 或 IPv6 主机地址，映射为 `InetAddress`
- **CIDR**: `cidr` - IPv4 或 IPv6 网络地址，作为字符串处理
- **MACADDR**: `macaddr` - MAC 地址，作为字符串处理
- **MACADDR8**: `macaddr8` - EUI-64 格式 MAC 地址，作为字符串处理

### 8. 几何类型
- **POINT**: `point` - 平面上的点，可映射为 `double[]` 或 `String`
- **LINE**: `line` - 平面上的无限直线，作为字符串处理
- **LSEG**: `lseg` - 平面上的线段，作为字符串处理
- **BOX**: `box` - 平面上的矩形框，作为字符串处理
- **PATH**: `path` - 平面上的几何路径，作为字符串处理
- **POLYGON**: `polygon` - 平面上的多边形，作为字符串处理
- **CIRCLE**: `circle` - 平面上的圆，作为字符串处理

### 9. 范围类型
- **INT4RANGE**: `int4range` - 整数范围，作为字符串处理
- **INT8RANGE**: `int8range` - 长整数范围，作为字符串处理
- **NUMRANGE**: `numrange` - 数值范围，作为字符串处理
- **TSRANGE**: `tsrange` - 时间戳范围，作为字符串处理
- **TSTZRANGE**: `tstzrange` - 带时区时间戳范围，作为字符串处理
- **DATERANGE**: `daterange` - 日期范围，作为字符串处理

### 10. 全文搜索类型
- **TSVECTOR**: `tsvector` - 文本搜索文档，作为字符串处理
- **TSQUERY**: `tsquery` - 文本搜索查询，作为字符串处理

### 11. 位串类型
- **BIT**: `bit [ (n) ]` - 固定长度位串，作为字符串处理
- **BIT VARYING**: `bit varying [ (n) ]` - 可变长度位串，作为字符串处理

### 12. XML 类型
- **XML**: `xml` - XML 数据，作为字符串处理

### 13. 键值存储类型
- **HSTORE**: `hstore` - 键值对存储，映射为 `Map<String, String>`

### 14. 其他特殊类型
- **MONEY**: `money` - 货币金额，映射为 `BigDecimal`
- **PG_LSN**: `pg_lsn` - PostgreSQL 日志序列号，作为字符串处理
- **PG_SNAPSHOT**: `pg_snapshot` - 用户级事务 ID 快照，作为字符串处理
- **TXID_SNAPSHOT**: `txid_snapshot` - 已弃用的事务 ID 快照，作为字符串处理

## 扩展类型支持

### 15. ltree 扩展
- **LTREE**: `ltree` - 层次树状数据类型，作为字符串处理
- **LQUERY**: `lquery` - ltree 查询类型，作为字符串处理
- **LTXTQUERY**: `ltxtquery` - ltree 全文搜索查询，作为字符串处理

## 使用示例

```java
// 实体类定义
public class PostgreSQLEntity {
    private Long id;
    private String name;
    private UUID uuid;
    private InetAddress ipAddress;
    private Map<String, String> metadata; // hstore
    private double[] coordinates; // point
    private String[] tags; // text[]
    private Map<String, Object> jsonData; // jsonb
    
    // getters and setters...
}

// 使用 UniversalRowMapper
RowMapper<PostgreSQLEntity> mapper = UniversalRowMapper.of(
    PostgreSQLEntity.class, 
    metadataRegistry
);

List<PostgreSQLEntity> results = jdbcTemplate.query(
    "SELECT * FROM postgresql_table", 
    mapper
);
```

## 注意事项

1. **几何类型**: 目前以字符串形式处理，如需要专门的几何对象，可以扩展相应的处理方法
2. **范围类型**: 以字符串形式保存原始表示，可以根据需要解析为具体的范围对象
3. **HSTORE**: 提供了基本的解析支持，复杂的 hstore 数据建议使用专门的解析库
4. **扩展类型**: 如 ltree 等扩展类型需要在数据库中启用相应扩展
5. **性能考虑**: 对于大型几何对象和复杂 JSON 数据，建议根据实际需求优化处理逻辑

## 总结

UniversalRowMapper 现在支持 PostgreSQL 的所有主要数据类型，包括：
- ✅ 所有基础数据类型（数值、字符串、布尔、日期时间）
- ✅ PostgreSQL 特有类型（UUID、JSON、数组、枚举）
- ✅ 网络地址类型（inet、cidr、macaddr）
- ✅ 几何类型（point、line、box、circle 等）
- ✅ 范围类型（int4range、daterange 等）
- ✅ 全文搜索类型（tsvector、tsquery）
- ✅ 位串类型（bit、varbit）
- ✅ XML 和 hstore 类型
- ✅ 扩展类型（ltree 等）

这使得 UniversalRowMapper 能够处理 PostgreSQL 数据库中的任何数据类型，为开发者提供了完整的类型支持。