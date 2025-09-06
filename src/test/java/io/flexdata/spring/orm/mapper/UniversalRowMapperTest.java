package io.flexdata.spring.orm.mapper;

import io.flexdata.spring.orm.core.mapper.RowMapperFactory;
import io.flexdata.spring.orm.core.mapper.UniversalRowMapper;
import io.flexdata.spring.orm.core.metadata.EntityMetadataRegistry;
import io.flexdata.spring.orm.example.entiry.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * UniversalRowMapper测试类
 * 验证新的数据类型支持功能
 */
class UniversalRowMapperTest {

    private RowMapperFactory rowMapperFactory;
    private EntityMetadataRegistry metadataRegistry;
    private ResultSet resultSet;

    @BeforeEach
    void setUp() {
        metadataRegistry = new EntityMetadataRegistry();
        rowMapperFactory = new RowMapperFactory(metadataRegistry);
        resultSet = mock(ResultSet.class);
    }

    @Test
    void testUniversalRowMapperCreation() {
        // 测试获取UniversalRowMapper
        RowMapper<Product> mapper = rowMapperFactory.getUniversalRowMapper(Product.class);
        assertNotNull(mapper);
        assertTrue(mapper instanceof UniversalRowMapper);
    }

    @Test
    void testBeanPropertyRowMapperCreation() {
        // 测试获取BeanPropertyRowMapper
        RowMapper<Product> mapper = rowMapperFactory.getBeanPropertyRowMapper(Product.class);
        assertNotNull(mapper);
    }

    @Test
    void testRowMapperCaching() {
        // 测试RowMapper缓存功能
        RowMapper<Product> mapper1 = rowMapperFactory.getRowMapper(Product.class);
        RowMapper<Product> mapper2 = rowMapperFactory.getRowMapper(Product.class);
        
        assertSame(mapper1, mapper2, "RowMapper应该被缓存");
        assertEquals(1, rowMapperFactory.getCacheSize());
    }

    @Test
    void testCacheClear() {
        // 测试缓存清理功能
        rowMapperFactory.getRowMapper(Product.class);
        assertEquals(1, rowMapperFactory.getCacheSize());
        
        rowMapperFactory.clearCache();
        assertEquals(0, rowMapperFactory.getCacheSize());
    }

    @Test
    void testUniversalMapperToggle() {
        // 测试通用RowMapper开关功能
        assertTrue(rowMapperFactory.isUniversalMapperEnabled());
        
        rowMapperFactory.setEnableUniversalMapper(false);
        assertFalse(rowMapperFactory.isUniversalMapperEnabled());
        
        // 禁用后应该返回BeanPropertyRowMapper
        RowMapper<Product> mapper = rowMapperFactory.getRowMapper(Product.class);
        assertNotNull(mapper);
        assertFalse(mapper instanceof UniversalRowMapper);
    }

    @Test
    void testUniversalRowMapperInstantiation() {
        // 测试UniversalRowMapper实例化
        assertDoesNotThrow(() -> {
            UniversalRowMapper<Product> mapper = new UniversalRowMapper<>(Product.class, metadataRegistry);
            assertNotNull(mapper);
        });
    }
}