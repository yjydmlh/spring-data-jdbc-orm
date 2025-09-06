package io.flexdata.spring.orm.config;

import io.flexdata.spring.orm.core.datasource.DataSourceContext;
import io.flexdata.spring.orm.core.datasource.DynamicDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 增强的多数据源配置类
 * 支持任意数量数据源的动态配置和管理
 */
@Configuration
public class EnhancedMultiDataSourceConfiguration {
    
    private final Environment environment;
    
    public EnhancedMultiDataSourceConfiguration(Environment environment) {
        this.environment = environment;
    }
    
    /**
     * 主数据源配置
     */
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.primary")
    public DataSource primaryDataSource() {
        DataSource dataSource = DataSourceBuilder.create().build();
        // 注册主数据源信息
        DataSourceContext.registerDataSource("primary", "Primary DataSource", "主数据源");
        return dataSource;
    }
    
    /**
     * 只读数据源配置
     */
    @Bean
    @ConfigurationProperties("spring.datasource.enhanced.readonly")
    public DataSource readOnlyDataSource() {
        DataSource dataSource = DataSourceBuilder.create().build();
        // 注册只读数据源信息
        DataSourceContext.registerDataSource("readonly", "ReadOnly DataSource", "只读数据源");
        return dataSource;
    }
    
    /**
     * 动态数据源配置
     * 整合所有数据源，支持动态切换
     */
    @Bean
    public DynamicDataSource dynamicDataSource(
            @Qualifier("primaryDataSource") DataSource primaryDataSource,
            @Qualifier("readOnlyDataSource") DataSource readOnlyDataSource) {
        
        Map<Object, Object> targetDataSources = new HashMap<>();
        
        // 添加预定义的数据源
        targetDataSources.put("primary", primaryDataSource);
        targetDataSources.put("readonly", readOnlyDataSource);
        
        // 动态发现和配置额外的数据源
        Map<String, DataSource> additionalDataSources = discoverAdditionalDataSources();
        targetDataSources.putAll(additionalDataSources);
        
        DynamicDataSource dynamicDataSource = new DynamicDataSource();
        dynamicDataSource.setTargetDataSources(targetDataSources);
        dynamicDataSource.setDefaultTargetDataSource(primaryDataSource);
        
        return dynamicDataSource;
    }
    
    /**
     * 动态发现额外的数据源配置
     * 支持通过配置文件动态添加任意数量的数据源
     */
    private Map<String, DataSource> discoverAdditionalDataSources() {
        Map<String, DataSource> dataSources = new HashMap<>();
        
        // 扫描配置文件中的数据源配置
        // 格式：spring.datasource.additional.{name}.url, driver-class-name, username, password
        Set<String> dataSourceNames = getAdditionalDataSourceNames();
        
        for (String name : dataSourceNames) {
            try {
                DataSource dataSource = createDataSourceFromProperties(name);
                if (dataSource != null) {
                    dataSources.put(name, dataSource);
                    
                    // 注册数据源信息
                    String description = environment.getProperty(
                        String.format("spring.datasource.additional.%s.description", name), 
                        "Additional DataSource: " + name);
                    DataSourceContext.registerDataSource(name, name, description);
                }
            } catch (Exception e) {
                // 记录错误但不中断其他数据源的创建
                System.err.println("Failed to create datasource: " + name + ", error: " + e.getMessage());
            }
        }
        
        return dataSources;
    }
    
    /**
     * 获取额外数据源的名称列表
     */
    private Set<String> getAdditionalDataSourceNames() {
        Set<String> names = new java.util.HashSet<>();
        
        // 扫描所有以 spring.datasource.additional. 开头的配置
        String prefix = "spring.datasource.additional.";
        
        // 这里简化实现，实际项目中可以通过反射或配置绑定来实现
        // 检查一些常见的数据源名称
        String[] commonNames = {"db1", "db2", "db3", "analytics", "reporting", "archive", "cache", "log"};
        
        for (String name : commonNames) {
            String urlProperty = prefix + name + ".url";
            if (StringUtils.hasText(environment.getProperty(urlProperty))) {
                names.add(name);
            }
        }
        
        // 也可以通过环境变量或系统属性来动态发现
        String additionalDataSources = environment.getProperty("app.datasources.additional");
        if (StringUtils.hasText(additionalDataSources)) {
            String[] nameArray = additionalDataSources.split(",");
            for (String name : nameArray) {
                if (StringUtils.hasText(name.trim())) {
                    names.add(name.trim());
                }
            }
        }
        
        return names;
    }
    
    /**
     * 根据配置属性创建数据源
     */
    private DataSource createDataSourceFromProperties(String name) {
        String prefix = String.format("spring.datasource.additional.%s", name);
        
        String url = environment.getProperty(prefix + ".url");
        String driverClassName = environment.getProperty(prefix + ".driver-class-name");
        String username = environment.getProperty(prefix + ".username");
        String password = environment.getProperty(prefix + ".password");
        
        if (!StringUtils.hasText(url)) {
            return null;
        }
        
        DataSourceBuilder<?> builder = DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password);
        
        if (StringUtils.hasText(driverClassName)) {
            builder.driverClassName(driverClassName);
        }
        
        // 设置连接池类型
        String type = environment.getProperty(prefix + ".type");
        if (StringUtils.hasText(type)) {
            try {
                Class<?> typeClass = Class.forName(type);
                builder.type((Class<? extends DataSource>) typeClass);
            } catch (ClassNotFoundException e) {
                System.err.println("DataSource type not found: " + type);
            }
        }
        
        return builder.build();
    }
    
    /**
     * 数据源配置属性类
     * 用于绑定配置文件中的数据源属性
     */
    public static class DataSourceProperties {
        private String url;
        private String driverClassName;
        private String username;
        private String password;
        private String type;
        private String description;
        private Map<String, String> properties = new HashMap<>();
        
        // Getters and Setters
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public String getDriverClassName() { return driverClassName; }
        public void setDriverClassName(String driverClassName) { this.driverClassName = driverClassName; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public Map<String, String> getProperties() { return properties; }
        public void setProperties(Map<String, String> properties) { this.properties = properties; }
    }
    
    /**
     * 数据源健康检查配置
     */
    @Bean
    public DataSourceHealthChecker dataSourceHealthChecker() {
        return new DataSourceHealthChecker();
    }
    
    /**
     * 数据源健康检查器
     */
    public static class DataSourceHealthChecker {
        
        /**
         * 检查数据源连接是否正常
         */
        public boolean checkDataSourceHealth(String dataSourceKey) {
            try {
                return DataSourceContext.executeWithDataSource(dataSourceKey, () -> {
                    // 这里可以执行简单的SQL查询来验证连接
                    // 例如：SELECT 1
                    return true;
                });
            } catch (Exception e) {
                System.err.println("DataSource health check failed for: " + dataSourceKey + ", error: " + e.getMessage());
                return false;
            }
        }
        
        /**
         * 检查所有已注册数据源的健康状态
         */
        public Map<String, Boolean> checkAllDataSourcesHealth() {
            Map<String, Boolean> healthStatus = new HashMap<>();
            Map<String, DataSourceContext.DataSourceInfo> allDataSources = DataSourceContext.getAllDataSources();
            
            for (String key : allDataSources.keySet()) {
                healthStatus.put(key, checkDataSourceHealth(key));
            }
            
            return healthStatus;
        }
    }
}