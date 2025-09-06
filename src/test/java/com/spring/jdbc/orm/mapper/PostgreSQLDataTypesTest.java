package com.spring.jdbc.orm.mapper;

import com.spring.jdbc.orm.core.mapper.UniversalRowMapper;
import com.spring.jdbc.orm.core.metadata.EntityMetadataRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * PostgreSQL数据类型支持测试
 */
class PostgreSQLDataTypesTest {

    @Mock
    private EntityMetadataRegistry metadataRegistry;
    
    @Mock
    private ResultSet resultSet;
    
    @Mock
    private ResultSetMetaData resultSetMetaData;

    private UniversalRowMapper<PostgreSQLTestEntity> rowMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        rowMapper = new UniversalRowMapper<>(PostgreSQLTestEntity.class, metadataRegistry);
    }

    @Test
    void testPostgreSQLDataTypesSupport() {
        // 测试PostgreSQL数据类型支持的存在性
        assertNotNull(rowMapper);
        assertTrue(rowMapper instanceof UniversalRowMapper);
    }

    @Test
    void testInetAddressHandling() throws Exception {
        // 测试INET类型处理
        when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
        when(resultSetMetaData.getColumnCount()).thenReturn(1);
        when(resultSetMetaData.getColumnName(1)).thenReturn("ip_address");
        when(resultSet.getString("ip_address")).thenReturn("192.168.1.1/24");
        
        // 通过反射调用私有方法进行测试
        java.lang.reflect.Method method = UniversalRowMapper.class
            .getDeclaredMethod("handleInetType", ResultSet.class, String.class);
        method.setAccessible(true);
        
        InetAddress result = (InetAddress) method.invoke(rowMapper, resultSet, "ip_address");
        assertNotNull(result);
        assertEquals("192.168.1.1", result.getHostAddress());
    }

    @Test
    void testHstoreHandling() throws Exception {
        // 测试HSTORE类型处理
        when(resultSet.getString("metadata")).thenReturn("\"key1\"=>\"value1\", \"key2\"=>\"value2\"");
        
        java.lang.reflect.Method method = UniversalRowMapper.class
            .getDeclaredMethod("handleHstoreType", ResultSet.class, String.class);
        method.setAccessible(true);
        
        @SuppressWarnings("unchecked")
        Map<String, String> result = (Map<String, String>) method.invoke(rowMapper, resultSet, "metadata");
        assertNotNull(result);
        assertEquals("value1", result.get("key1"));
        assertEquals("value2", result.get("key2"));
    }

    @Test
    void testGeometricTypeHandling() throws Exception {
        // 测试几何类型处理（Point）
        when(resultSet.getString("coordinates")).thenReturn("(1.5,2.5)");
        
        java.lang.reflect.Method method = UniversalRowMapper.class
            .getDeclaredMethod("handleGeometricType", ResultSet.class, String.class, Class.class);
        method.setAccessible(true);
        
        double[] result = (double[]) method.invoke(rowMapper, resultSet, "coordinates", double[].class);
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals(1.5, result[0], 0.001);
        assertEquals(2.5, result[1], 0.001);
    }

    @Test
    void testXmlTypeHandling() throws Exception {
        // 测试XML类型处理
        String xmlData = "<root><item>test</item></root>";
        when(resultSet.getString("xml_data")).thenReturn(xmlData);
        
        java.lang.reflect.Method method = UniversalRowMapper.class
            .getDeclaredMethod("handleXmlType", ResultSet.class, String.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(rowMapper, resultSet, "xml_data");
        assertEquals(xmlData, result);
    }

    @Test
    void testNetworkTypeHandling() throws Exception {
        // 测试网络类型处理
        when(resultSet.getString("mac_address")).thenReturn("08:00:2b:01:02:03");
        
        java.lang.reflect.Method method = UniversalRowMapper.class
            .getDeclaredMethod("handleNetworkType", ResultSet.class, String.class, Class.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(rowMapper, resultSet, "mac_address", String.class);
        assertEquals("08:00:2b:01:02:03", result);
    }

    @Test
    void testRangeTypeHandling() throws Exception {
        // 测试范围类型处理
        when(resultSet.getString("date_range")).thenReturn("[2023-01-01,2023-12-31)");
        
        java.lang.reflect.Method method = UniversalRowMapper.class
            .getDeclaredMethod("handleRangeType", ResultSet.class, String.class, Class.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(rowMapper, resultSet, "date_range", String.class);
        assertEquals("[2023-01-01,2023-12-31)", result);
    }

    @Test
    void testTextSearchTypeHandling() throws Exception {
        // 测试全文搜索类型处理
        when(resultSet.getString("search_vector")).thenReturn("'hello':1 'world':2");
        
        java.lang.reflect.Method method = UniversalRowMapper.class
            .getDeclaredMethod("handleTextSearchType", ResultSet.class, String.class, Class.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(rowMapper, resultSet, "search_vector", String.class);
        assertEquals("'hello':1 'world':2", result);
    }

    @Test
    void testBitStringTypeHandling() throws Exception {
        // 测试位串类型处理
        when(resultSet.getString("bit_data")).thenReturn("101010");
        
        java.lang.reflect.Method method = UniversalRowMapper.class
            .getDeclaredMethod("handleBitStringType", ResultSet.class, String.class, Class.class);
        method.setAccessible(true);
        
        String result = (String) method.invoke(rowMapper, resultSet, "bit_data", String.class);
        assertEquals("101010", result);
    }

    @Test
    void testHstoreColumnDetection() throws Exception {
        // 测试hstore列检测
        when(resultSet.getMetaData()).thenReturn(resultSetMetaData);
        when(resultSetMetaData.getColumnCount()).thenReturn(1);
        when(resultSetMetaData.getColumnName(1)).thenReturn("metadata");
        when(resultSetMetaData.getColumnTypeName(1)).thenReturn("hstore");
        
        java.lang.reflect.Method method = UniversalRowMapper.class
            .getDeclaredMethod("isHstoreColumn", ResultSet.class, String.class);
        method.setAccessible(true);
        
        boolean result = (boolean) method.invoke(rowMapper, resultSet, "metadata");
        assertTrue(result);
    }

    /**
     * 测试用的PostgreSQL实体类
     */
    public static class PostgreSQLTestEntity {
        private Long id;
        private String name;
        private UUID uuid;
        private InetAddress ipAddress;
        private Map<String, String> metadata; // hstore
        private double[] coordinates; // point
        private String[] tags; // text[]
        private Map<String, Object> jsonData; // jsonb
        private String xmlData; // xml
        private String macAddress; // macaddr
        private String dateRange; // daterange
        private String searchVector; // tsvector
        private String bitData; // bit
        private BigDecimal money; // money
        private LocalDate createdDate;
        private LocalDateTime updatedAt;
        
        // 默认构造函数
        public PostgreSQLTestEntity() {}
        
        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public UUID getUuid() { return uuid; }
        public void setUuid(UUID uuid) { this.uuid = uuid; }
        
        public InetAddress getIpAddress() { return ipAddress; }
        public void setIpAddress(InetAddress ipAddress) { this.ipAddress = ipAddress; }
        
        public Map<String, String> getMetadata() { return metadata; }
        public void setMetadata(Map<String, String> metadata) { this.metadata = metadata; }
        
        public double[] getCoordinates() { return coordinates; }
        public void setCoordinates(double[] coordinates) { this.coordinates = coordinates; }
        
        public String[] getTags() { return tags; }
        public void setTags(String[] tags) { this.tags = tags; }
        
        public Map<String, Object> getJsonData() { return jsonData; }
        public void setJsonData(Map<String, Object> jsonData) { this.jsonData = jsonData; }
        
        public String getXmlData() { return xmlData; }
        public void setXmlData(String xmlData) { this.xmlData = xmlData; }
        
        public String getMacAddress() { return macAddress; }
        public void setMacAddress(String macAddress) { this.macAddress = macAddress; }
        
        public String getDateRange() { return dateRange; }
        public void setDateRange(String dateRange) { this.dateRange = dateRange; }
        
        public String getSearchVector() { return searchVector; }
        public void setSearchVector(String searchVector) { this.searchVector = searchVector; }
        
        public String getBitData() { return bitData; }
        public void setBitData(String bitData) { this.bitData = bitData; }
        
        public BigDecimal getMoney() { return money; }
        public void setMoney(BigDecimal money) { this.money = money; }
        
        public LocalDate getCreatedDate() { return createdDate; }
        public void setCreatedDate(LocalDate createdDate) { this.createdDate = createdDate; }
        
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }
}