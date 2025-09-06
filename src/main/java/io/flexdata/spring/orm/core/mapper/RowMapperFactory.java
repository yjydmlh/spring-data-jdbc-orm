package io.flexdata.spring.orm.core.mapper;

import io.flexdata.spring.orm.core.metadata.EntityMetadataRegistry;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * RowMapper工厂类
 * 根据配置选择使用通用RowMapper或Spring默认的BeanPropertyRowMapper
 */
@Component
public class RowMapperFactory {
    
    private final EntityMetadataRegistry metadataRegistry;
    private final ConcurrentMap<Class<?>, RowMapper<?>> mapperCache = new ConcurrentHashMap<>();
    
    // 配置项：是否启用通用RowMapper
    private boolean enableUniversalMapper = true;
    
    public RowMapperFactory(EntityMetadataRegistry metadataRegistry) {
        this.metadataRegistry = metadataRegistry;
    }
    
    /**
     * 获取指定实体类的RowMapper
     */
    @SuppressWarnings("unchecked")
    public <T> RowMapper<T> getRowMapper(Class<T> entityClass) {
        return (RowMapper<T>) mapperCache.computeIfAbsent(entityClass, this::createRowMapper);
    }
    
    /**
     * 创建RowMapper实例
     */
    private <T> RowMapper<T> createRowMapper(Class<T> entityClass) {
        if (enableUniversalMapper) {
            return new UniversalRowMapper<>(entityClass, metadataRegistry);
        } else {
            return new BeanPropertyRowMapper<>(entityClass);
        }
    }
    
    /**
     * 强制使用通用RowMapper
     */
    public <T> RowMapper<T> getUniversalRowMapper(Class<T> entityClass) {
        return new UniversalRowMapper<>(entityClass, metadataRegistry);
    }
    
    /**
     * 强制使用Spring默认RowMapper
     */
    public <T> RowMapper<T> getBeanPropertyRowMapper(Class<T> entityClass) {
        return new BeanPropertyRowMapper<>(entityClass);
    }
    
    /**
     * 设置是否启用通用RowMapper
     */
    public void setEnableUniversalMapper(boolean enableUniversalMapper) {
        this.enableUniversalMapper = enableUniversalMapper;
        // 清空缓存，强制重新创建
        mapperCache.clear();
    }
    
    /**
     * 检查是否启用了通用RowMapper
     */
    public boolean isUniversalMapperEnabled() {
        return enableUniversalMapper;
    }
    
    /**
     * 清空RowMapper缓存
     */
    public void clearCache() {
        mapperCache.clear();
    }
    
    /**
     * 获取缓存大小
     */
    public int getCacheSize() {
        return mapperCache.size();
    }
}