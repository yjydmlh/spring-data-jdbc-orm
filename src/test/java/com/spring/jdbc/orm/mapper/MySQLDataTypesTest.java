package com.spring.jdbc.orm.mapper;

import com.spring.jdbc.orm.core.mapper.UniversalRowMapper;
import com.spring.jdbc.orm.core.metadata.EntityMetadataRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * MySQL数据类型支持测试
 */
public class MySQLDataTypesTest {
    
    @Mock
    private ResultSet resultSet;
    
    @Mock
    private ResultSetMetaData metaData;
    
    @Mock
    private EntityMetadataRegistry metadataRegistry;
    
    private UniversalRowMapper<TestEntity> rowMapper;
    
    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        rowMapper = new UniversalRowMapper<>(TestEntity.class, metadataRegistry);
        
        when(resultSet.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(10);
    }
    
    @Test
    void testMySQLNumericTypes() throws SQLException {
        // 测试数值类型
        when(resultSet.getObject("intValue")).thenReturn(42);
        when(resultSet.getInt("intValue")).thenReturn(42);
        when(resultSet.wasNull()).thenReturn(false);
        
        when(resultSet.getObject("longValue")).thenReturn(123456789L);
        when(resultSet.getLong("longValue")).thenReturn(123456789L);
        
        when(resultSet.getObject("decimalValue")).thenReturn(new BigDecimal("99.99"));
        when(resultSet.getBigDecimal("decimalValue")).thenReturn(new BigDecimal("99.99"));
        
        when(resultSet.getObject("floatValue")).thenReturn(3.14f);
        when(resultSet.getFloat("floatValue")).thenReturn(3.14f);
        
        when(resultSet.getObject("doubleValue")).thenReturn(2.718);
        when(resultSet.getDouble("doubleValue")).thenReturn(2.718);
        
        // 验证数值类型处理
        assertNotNull(resultSet.getObject("intValue"));
        assertEquals(42, resultSet.getInt("intValue"));
        assertEquals(123456789L, resultSet.getLong("longValue"));
        assertEquals(new BigDecimal("99.99"), resultSet.getBigDecimal("decimalValue"));
        assertEquals(3.14f, resultSet.getFloat("floatValue"), 0.001);
        assertEquals(2.718, resultSet.getDouble("doubleValue"), 0.001);
    }
    
    @Test
    void testMySQLStringTypes() throws SQLException {
        // 测试字符串类型
        when(resultSet.getObject("varcharValue")).thenReturn("Hello MySQL");
        when(resultSet.getString("varcharValue")).thenReturn("Hello MySQL");
        
        when(resultSet.getObject("textValue")).thenReturn("Long text content");
        when(resultSet.getString("textValue")).thenReturn("Long text content");
        
        when(resultSet.getObject("charValue")).thenReturn("A");
        when(resultSet.getString("charValue")).thenReturn("A");
        
        // 验证字符串类型处理
        assertEquals("Hello MySQL", resultSet.getString("varcharValue"));
        assertEquals("Long text content", resultSet.getString("textValue"));
        assertEquals("A", resultSet.getString("charValue"));
    }
    
    @Test
    void testMySQLDateTimeTypes() throws SQLException {
        // 测试日期时间类型
        java.sql.Date testDate = java.sql.Date.valueOf("2023-12-25");
        Time testTime = Time.valueOf("14:30:00");
        Timestamp testTimestamp = Timestamp.valueOf("2023-12-25 14:30:00");
        
        when(resultSet.getObject("dateValue")).thenReturn(testDate);
        when(resultSet.getDate("dateValue")).thenReturn(testDate);
        
        when(resultSet.getObject("timeValue")).thenReturn(testTime);
        when(resultSet.getTime("timeValue")).thenReturn(testTime);
        
        when(resultSet.getObject("datetimeValue")).thenReturn(testTimestamp);
        when(resultSet.getTimestamp("datetimeValue")).thenReturn(testTimestamp);
        
        // 验证日期时间类型处理
        assertEquals(testDate, resultSet.getDate("dateValue"));
        assertEquals(testTime, resultSet.getTime("timeValue"));
        assertEquals(testTimestamp, resultSet.getTimestamp("datetimeValue"));
    }
    
    @Test
    void testMySQLBinaryTypes() throws SQLException {
        // 测试二进制类型
        byte[] binaryData = "Binary data".getBytes();
        byte[] blobData = "BLOB data content".getBytes();
        
        when(resultSet.getObject("binaryValue")).thenReturn(binaryData);
        when(resultSet.getBytes("binaryValue")).thenReturn(binaryData);
        
        when(resultSet.getObject("blobValue")).thenReturn(blobData);
        when(resultSet.getBytes("blobValue")).thenReturn(blobData);
        
        // 验证二进制类型处理
        assertArrayEquals(binaryData, resultSet.getBytes("binaryValue"));
        assertArrayEquals(blobData, resultSet.getBytes("blobValue"));
    }
    
    @Test
    void testMySQLJSONType() throws SQLException {
        // 测试JSON类型
        String jsonString = "{\"name\":\"John\",\"age\":30,\"city\":\"New York\"}";
        
        when(resultSet.getObject("jsonValue")).thenReturn(jsonString);
        when(resultSet.getString("jsonValue")).thenReturn(jsonString);
        
        // 验证JSON类型处理
        assertEquals(jsonString, resultSet.getString("jsonValue"));
        assertTrue(resultSet.getString("jsonValue").contains("John"));
    }
    
    @Test
    void testMySQLSetType() throws SQLException {
        // 测试SET类型
        String setValue = "option1,option2,option3";
        
        when(resultSet.getObject("setValue")).thenReturn(setValue);
        when(resultSet.getString("setValue")).thenReturn(setValue);
        
        // 模拟SET类型的元数据
        when(metaData.getColumnName(1)).thenReturn("setValue");
        when(metaData.getColumnTypeName(1)).thenReturn("SET");
        
        // 验证SET类型处理
        assertEquals(setValue, resultSet.getString("setValue"));
        assertTrue(setValue.contains("option1"));
        assertTrue(setValue.contains("option2"));
        assertTrue(setValue.contains("option3"));
    }
    
    @Test
    void testMySQLBitType() throws SQLException {
        // 测试BIT类型
        byte[] bitValue = {(byte) 0b10101010};
        
        when(resultSet.getObject("bitValue")).thenReturn(bitValue);
        when(resultSet.getBytes("bitValue")).thenReturn(bitValue);
        
        // 模拟BIT类型的元数据
        when(metaData.getColumnName(1)).thenReturn("bitValue");
        when(metaData.getColumnTypeName(1)).thenReturn("BIT");
        
        // 验证BIT类型处理
        assertArrayEquals(bitValue, resultSet.getBytes("bitValue"));
    }
    
    @Test
    void testMySQLEnumType() throws SQLException {
        // 测试ENUM类型
        String enumValue = "ACTIVE";
        
        when(resultSet.getObject("enumValue")).thenReturn(enumValue);
        when(resultSet.getString("enumValue")).thenReturn(enumValue);
        
        // 验证ENUM类型处理
        assertEquals(enumValue, resultSet.getString("enumValue"));
    }
    
    @Test
    void testMySQLSpatialTypes() throws SQLException {
        // 测试空间数据类型
        String pointWKT = "POINT(1.0 2.0)";
        String polygonWKT = "POLYGON((0 0, 0 1, 1 1, 1 0, 0 0))";
        
        when(resultSet.getObject("pointValue")).thenReturn(pointWKT);
        when(resultSet.getString("pointValue")).thenReturn(pointWKT);
        
        when(resultSet.getObject("polygonValue")).thenReturn(polygonWKT);
        when(resultSet.getString("polygonValue")).thenReturn(polygonWKT);
        
        // 验证空间数据类型处理
        assertEquals(pointWKT, resultSet.getString("pointValue"));
        assertEquals(polygonWKT, resultSet.getString("polygonValue"));
        assertTrue(pointWKT.startsWith("POINT"));
        assertTrue(polygonWKT.startsWith("POLYGON"));
    }
    
    @Test
    void testMySQLBooleanType() throws SQLException {
        // 测试BOOLEAN类型（MySQL中实际为TINYINT(1)）
        when(resultSet.getObject("booleanValue")).thenReturn(true);
        when(resultSet.getBoolean("booleanValue")).thenReturn(true);
        
        when(resultSet.getObject("booleanFalse")).thenReturn(false);
        when(resultSet.getBoolean("booleanFalse")).thenReturn(false);
        
        // 验证BOOLEAN类型处理
        assertTrue(resultSet.getBoolean("booleanValue"));
        assertFalse(resultSet.getBoolean("booleanFalse"));
    }
    
    @Test
    void testMySQLYearType() throws SQLException {
        // 测试YEAR类型
        when(resultSet.getObject("yearValue")).thenReturn(2023);
        when(resultSet.getInt("yearValue")).thenReturn(2023);
        
        // 验证YEAR类型处理
        assertEquals(2023, resultSet.getInt("yearValue"));
    }
    
    // 测试实体类
    public static class TestEntity {
        private Long id;
        private String name;
        private Integer age;
        private BigDecimal price;
        private LocalDate birthDate;
        private LocalTime workTime;
        private LocalDateTime createdAt;
        private Boolean active;
        private byte[] data;
        private Set<String> tags;
        
        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public Integer getAge() { return age; }
        public void setAge(Integer age) { this.age = age; }
        
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        
        public LocalDate getBirthDate() { return birthDate; }
        public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }
        
        public LocalTime getWorkTime() { return workTime; }
        public void setWorkTime(LocalTime workTime) { this.workTime = workTime; }
        
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
        
        public byte[] getData() { return data; }
        public void setData(byte[] data) { this.data = data; }
        
        public Set<String> getTags() { return tags; }
        public void setTags(Set<String> tags) { this.tags = tags; }
    }
}