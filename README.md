# Spring Data JDBC Type-Safe ORM Framework

一个基于Spring Data JDBC的类型安全ORM框架，提供无SQL编写的数据库操作能力。

## 特性

- ✅ **类型安全** - 使用Lambda表达式引用字段，编译时检查
- ✅ **零SQL编写** - 通过API构建查询，自动生成SQL
- ✅ **IDE友好** - 完整的代码提示和重构支持
- ✅ **高性能** - 元数据缓存、批量操作支持
- ✅ **易扩展** - 插件系统和模块化设计

## 快速开始

### 1. 添加依赖

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>spring-data-jdbc-orm</artifactId>
    <version>1.0.0</version>
</dependency>