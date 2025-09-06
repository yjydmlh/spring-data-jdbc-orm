package com.spring.jdbc.orm.core.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spring.jdbc.orm.core.metadata.EntityMetadata;
import com.spring.jdbc.orm.core.metadata.EntityMetadataRegistry;
import com.spring.jdbc.orm.core.metadata.FieldMetadata;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 通用行映射器，支持多种数据库的常见数据类型
 * 支持MySQL、PostgreSQL、Oracle、SQL Server等主流数据库
 */
@Component
public class UniversalRowMapper<T> implements RowMapper<T> {
    
    private final Class<T> entityClass;
    private final EntityMetadataRegistry metadataRegistry;
    private final ObjectMapper objectMapper;
    
    public UniversalRowMapper(Class<T> entityClass, EntityMetadataRegistry metadataRegistry) {
        this.entityClass = entityClass;
        this.metadataRegistry = metadataRegistry;
        this.objectMapper = new ObjectMapper();
    }
    
    @Override
    public T mapRow(ResultSet rs, int rowNum) throws SQLException {
        try {
            T instance = entityClass.getDeclaredConstructor().newInstance();
            EntityMetadata metadata = metadataRegistry.getMetadata(entityClass);
            
            for (FieldMetadata field : metadata.getFields().values()) {
                Object value = extractValue(rs, field.getColumnName(), field.getFieldType());
                if (value != null) {
                    setFieldValue(instance, field.getFieldName(), value);
                }
            }
            
            return instance;
        } catch (Exception e) {
            throw new SQLException("Failed to map row to entity: " + entityClass.getSimpleName(), e);
        }
    }
    
    /**
     * 根据字段类型从ResultSet中提取值
     */
    private Object extractValue(ResultSet rs, String columnName, Class<?> fieldType) throws SQLException {
        // 检查列是否存在
        if (!hasColumn(rs, columnName)) {
            return null;
        }
        
        Object value = rs.getObject(columnName);
        if (value == null) {
            return null;
        }
        
        // 基础类型处理
        if (fieldType == String.class) {
            return handleStringType(rs, columnName, value);
        } else if (fieldType == Integer.class || fieldType == int.class) {
            return rs.getInt(columnName);
        } else if (fieldType == Long.class || fieldType == long.class) {
            return rs.getLong(columnName);
        } else if (fieldType == Double.class || fieldType == double.class) {
            return rs.getDouble(columnName);
        } else if (fieldType == Float.class || fieldType == float.class) {
            return rs.getFloat(columnName);
        } else if (fieldType == Boolean.class || fieldType == boolean.class) {
            return rs.getBoolean(columnName);
        } else if (fieldType == BigDecimal.class) {
            return rs.getBigDecimal(columnName);
        }
        
        // 时间类型处理
        else if (fieldType == java.sql.Date.class) {
            return rs.getDate(columnName);
        } else if (fieldType == Time.class) {
            return rs.getTime(columnName);
        } else if (fieldType == Timestamp.class) {
            return rs.getTimestamp(columnName);
        } else if (fieldType == LocalDate.class) {
            java.sql.Date date = rs.getDate(columnName);
            return date != null ? date.toLocalDate() : null;
        } else if (fieldType == LocalTime.class) {
            Time time = rs.getTime(columnName);
            return time != null ? time.toLocalTime() : null;
        } else if (fieldType == LocalDateTime.class) {
            Timestamp timestamp = rs.getTimestamp(columnName);
            return timestamp != null ? timestamp.toLocalDateTime() : null;
        } else if (fieldType == Instant.class) {
            Timestamp timestamp = rs.getTimestamp(columnName);
            return timestamp != null ? timestamp.toInstant() : null;
        }
        
        // UUID类型处理（PostgreSQL）
        else if (fieldType == UUID.class) {
            return handleUuidType(value);
        }
        
        // JSON类型处理（MySQL、PostgreSQL）
        else if (fieldType == Map.class || fieldType == List.class) {
            return handleJsonType(rs, columnName, fieldType);
        }
        
        // 数组类型处理（PostgreSQL）
        else if (fieldType.isArray()) {
            return handleArrayType(rs, columnName, fieldType);
        }
        
        // 枚举类型处理
        else if (fieldType.isEnum()) {
            return handleEnumType(rs, columnName, fieldType);
        }
        
        // 字节数组处理（BLOB）
        else if (fieldType == byte[].class) {
            return rs.getBytes(columnName);
        }
        
        // MySQL特有类型处理
        else if (fieldType == Set.class && isMySQLSetColumn(rs, columnName)) {
            return handleMySQLSetType(rs, columnName);
        } else if (fieldType == String.class && isMySQLBitColumn(rs, columnName)) {
            return handleMySQLBitType(rs, columnName);
        }
        
        // PostgreSQL特有类型处理
        else if (fieldType == InetAddress.class) {
            return handleInetType(rs, columnName);
        } else if (fieldType == Map.class && isHstoreColumn(rs, columnName)) {
            return handleHstoreType(rs, columnName);
        }
        
        // PostgreSQL几何类型处理
        else if (isGeometricType(fieldType)) {
            return handleGeometricType(rs, columnName, fieldType);
        }
        
        // PostgreSQL范围类型处理
        else if (isRangeType(fieldType)) {
            return handleRangeType(rs, columnName, fieldType);
        }
        
        // PostgreSQL全文搜索类型处理
        else if (isTextSearchType(fieldType)) {
            return handleTextSearchType(rs, columnName, fieldType);
        }
        
        // PostgreSQL网络类型处理
        else if (isNetworkType(fieldType)) {
            return handleNetworkType(rs, columnName, fieldType);
        }
        
        // PostgreSQL位串类型处理
        else if (isBitStringType(fieldType)) {
            return handleBitStringType(rs, columnName, fieldType);
        }
        
        // PostgreSQL XML类型处理
        else if (isXmlType(fieldType)) {
            return handleXmlType(rs, columnName);
        }
        
        // 默认处理
        return value;
    }
    
    /**
     * 处理字符串类型，支持CLOB等大文本
     */
    private String handleStringType(ResultSet rs, String columnName, Object value) throws SQLException {
        if (value instanceof Clob) {
            Clob clob = (Clob) value;
            return clob.getSubString(1, (int) clob.length());
        }
        return rs.getString(columnName);
    }
    
    /**
     * 处理UUID类型
     */
    private UUID handleUuidType(Object value) {
        if (value instanceof UUID) {
            return (UUID) value;
        } else if (value instanceof String) {
            return UUID.fromString((String) value);
        } else if (value instanceof byte[]) {
            // 处理二进制UUID
            byte[] bytes = (byte[]) value;
            if (bytes.length == 16) {
                return UUID.nameUUIDFromBytes(bytes);
            }
        }
        return null;
    }
    
    /**
     * 处理JSON类型
     */
    private Object handleJsonType(ResultSet rs, String columnName, Class<?> fieldType) throws SQLException {
        String jsonString = rs.getString(columnName);
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return null;
        }
        
        try {
            if (fieldType == Map.class) {
                return objectMapper.readValue(jsonString, Map.class);
            } else if (fieldType == List.class) {
                return objectMapper.readValue(jsonString, List.class);
            }
        } catch (Exception e) {
            // JSON解析失败，返回原始字符串
            return jsonString;
        }
        return null;
    }
    
    /**
     * 处理数组类型（PostgreSQL）
     */
    private Object handleArrayType(ResultSet rs, String columnName, Class<?> fieldType) throws SQLException {
        Array sqlArray = rs.getArray(columnName);
        if (sqlArray == null) {
            return null;
        }
        
        Object[] array = (Object[]) sqlArray.getArray();
        Class<?> componentType = fieldType.getComponentType();
        
        if (componentType == String.class) {
            return Arrays.copyOf(array, array.length, String[].class);
        } else if (componentType == Integer.class) {
            return Arrays.stream(array)
                    .map(obj -> obj != null ? Integer.valueOf(obj.toString()) : null)
                    .toArray(Integer[]::new);
        } else if (componentType == Long.class) {
            return Arrays.stream(array)
                    .map(obj -> obj != null ? Long.valueOf(obj.toString()) : null)
                    .toArray(Long[]::new);
        }
        
        return array;
    }
    
    /**
     * 处理枚举类型
     */
    @SuppressWarnings("unchecked")
    private Object handleEnumType(ResultSet rs, String columnName, Class<?> fieldType) throws SQLException {
        String enumValue = rs.getString(columnName);
        if (enumValue == null) {
            return null;
        }
        
        try {
            return Enum.valueOf((Class<Enum>) fieldType, enumValue);
        } catch (IllegalArgumentException e) {
            // 尝试按序号处理
            try {
                int ordinal = Integer.parseInt(enumValue);
                Object[] enumConstants = fieldType.getEnumConstants();
                if (ordinal >= 0 && ordinal < enumConstants.length) {
                    return enumConstants[ordinal];
                }
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
    
    /**
     * 检查ResultSet是否包含指定列
     */
    private boolean hasColumn(ResultSet rs, String columnName) {
        try {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                if (columnName.equalsIgnoreCase(metaData.getColumnName(i)) ||
                    columnName.equalsIgnoreCase(metaData.getColumnLabel(i))) {
                    return true;
                }
            }
            return false;
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * 设置字段值
     */
    private void setFieldValue(Object instance, String fieldName, Object value) throws Exception {
        java.lang.reflect.Field field = instance.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(instance, value);
    }
    
    /**
     * 处理PostgreSQL INET类型
     */
    private InetAddress handleInetType(ResultSet rs, String columnName) throws SQLException {
        String inetString = rs.getString(columnName);
        if (inetString == null) {
            return null;
        }
        try {
            // 移除CIDR后缀（如果存在）
            String address = inetString.split("/")[0];
            return InetAddress.getByName(address);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 处理PostgreSQL HSTORE类型
     */
    private Map<String, String> handleHstoreType(ResultSet rs, String columnName) throws SQLException {
        String hstoreString = rs.getString(columnName);
        if (hstoreString == null || hstoreString.trim().isEmpty()) {
            return new HashMap<>();
        }
        
        Map<String, String> result = new HashMap<>();
        // 简单的hstore解析（实际生产环境建议使用专门的hstore解析库）
        String[] pairs = hstoreString.split(",\\s*");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=>");
            if (keyValue.length == 2) {
                String key = keyValue[0].trim().replaceAll("^\"|\"$", "");
                String value = keyValue[1].trim().replaceAll("^\"|\"$", "");
                result.put(key, value);
            }
        }
        return result;
    }
    
    /**
     * 处理PostgreSQL几何类型
     */
    private Object handleGeometricType(ResultSet rs, String columnName, Class<?> fieldType) throws SQLException {
        String geometricString = rs.getString(columnName);
        if (geometricString == null) {
            return null;
        }
        
        // 返回几何类型的字符串表示，实际应用中可以创建专门的几何类型类
        if (fieldType == String.class) {
            return geometricString;
        }
        
        // 对于Point类型，可以解析为坐标数组
        if (geometricString.startsWith("(") && geometricString.endsWith(")")) {
            String coords = geometricString.substring(1, geometricString.length() - 1);
            String[] parts = coords.split(",");
            if (parts.length == 2 && fieldType == double[].class) {
                try {
                    return new double[]{Double.parseDouble(parts[0]), Double.parseDouble(parts[1])};
                } catch (NumberFormatException e) {
                    return geometricString;
                }
            }
        }
        
        return geometricString;
    }
    
    /**
     * 处理PostgreSQL范围类型
     */
    private Object handleRangeType(ResultSet rs, String columnName, Class<?> fieldType) throws SQLException {
        String rangeString = rs.getString(columnName);
        if (rangeString == null) {
            return null;
        }
        
        // 返回范围类型的字符串表示
        return rangeString;
    }
    
    /**
     * 处理PostgreSQL全文搜索类型（tsvector, tsquery）
     */
    private Object handleTextSearchType(ResultSet rs, String columnName, Class<?> fieldType) throws SQLException {
        return rs.getString(columnName);
    }
    
    /**
     * 处理PostgreSQL网络类型（cidr, macaddr等）
     */
    private Object handleNetworkType(ResultSet rs, String columnName, Class<?> fieldType) throws SQLException {
        return rs.getString(columnName);
    }
    
    /**
     * 处理PostgreSQL位串类型
     */
    private Object handleBitStringType(ResultSet rs, String columnName, Class<?> fieldType) throws SQLException {
        return rs.getString(columnName);
    }
    
    /**
     * 处理PostgreSQL XML类型
     */
    private String handleXmlType(ResultSet rs, String columnName) throws SQLException {
        return rs.getString(columnName);
    }
    
    /**
     * 检查是否为hstore列
     */
    private boolean isHstoreColumn(ResultSet rs, String columnName) {
        try {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                if (columnName.equalsIgnoreCase(metaData.getColumnName(i))) {
                    String typeName = metaData.getColumnTypeName(i);
                    return "hstore".equalsIgnoreCase(typeName);
                }
            }
        } catch (SQLException e) {
            // 忽略异常
        }
        return false;
    }
    
    /**
     * 检查是否为几何类型
     */
    private boolean isGeometricType(Class<?> fieldType) {
        return fieldType == String.class || fieldType == double[].class;
    }
    
    /**
     * 检查是否为范围类型
     */
    private boolean isRangeType(Class<?> fieldType) {
        return fieldType == String.class;
    }
    
    /**
     * 检查是否为全文搜索类型
     */
    private boolean isTextSearchType(Class<?> fieldType) {
        return fieldType == String.class;
    }
    
    /**
     * 检查是否为网络类型
     */
    private boolean isNetworkType(Class<?> fieldType) {
        return fieldType == String.class;
    }
    
    /**
     * 检查是否为位串类型
     */
    private boolean isBitStringType(Class<?> fieldType) {
        return fieldType == String.class;
    }
    
    /**
     * 检查是否为XML类型
     */
    private boolean isXmlType(Class<?> fieldType) {
        return fieldType == String.class;
    }
    
    /**
     * 创建RowMapper实例的工厂方法
     */
    /**
     * 处理MySQL SET类型
     */
    private Set<String> handleMySQLSetType(ResultSet rs, String columnName) throws SQLException {
        String setValue = rs.getString(columnName);
        if (setValue == null || setValue.isEmpty()) {
            return new HashSet<>();
        }
        
        Set<String> result = new HashSet<>();
        String[] values = setValue.split(",");
        for (String value : values) {
            result.add(value.trim());
        }
        return result;
    }
    
    /**
     * 处理MySQL BIT类型
     */
    private String handleMySQLBitType(ResultSet rs, String columnName) throws SQLException {
        byte[] bitValue = rs.getBytes(columnName);
        if (bitValue == null) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder();
        for (byte b : bitValue) {
            sb.append(String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0'));
        }
        return sb.toString();
    }
    
    /**
     * 检查是否为MySQL SET列
     */
    private boolean isMySQLSetColumn(ResultSet rs, String columnName) {
        try {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            for (int i = 1; i <= columnCount; i++) {
                if (metaData.getColumnName(i).equalsIgnoreCase(columnName)) {
                    String typeName = metaData.getColumnTypeName(i);
                    return typeName != null && typeName.toUpperCase().startsWith("SET");
                }
            }
        } catch (SQLException e) {
            // 忽略异常，返回false
        }
        return false;
    }
    
    /**
     * 检查是否为MySQL BIT列
     */
    private boolean isMySQLBitColumn(ResultSet rs, String columnName) {
        try {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            for (int i = 1; i <= columnCount; i++) {
                if (metaData.getColumnName(i).equalsIgnoreCase(columnName)) {
                    String typeName = metaData.getColumnTypeName(i);
                    return typeName != null && typeName.toUpperCase().equals("BIT");
                }
            }
        } catch (SQLException e) {
            // 忽略异常，返回false
        }
        return false;
    }
    
    public static <T> UniversalRowMapper<T> of(Class<T> entityClass, EntityMetadataRegistry metadataRegistry) {
        return new UniversalRowMapper<>(entityClass, metadataRegistry);
    }
}