package com.spring.jdbc.orm.example;

import com.spring.jdbc.orm.core.datasource.DataSourceContext;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 多数据源配置示例
 * 
 * 展示如何配置多个数据源以支持多数据源多表切换功能
 */
@Configuration
public class MultiDataSourceConfiguration {
    
    /**
     * 主数据源配置
     */
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource.main")
    public DataSource mainDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    /**
     * 分析数据库配置
     */
    @Bean
    @ConfigurationProperties("spring.datasource.analytics")
    public DataSource analyticsDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    /**
     * 备份数据库配置
     */
    @Bean
    @ConfigurationProperties("spring.datasource.backup")
    public DataSource backupDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    /**
     * 分片数据源配置
     */
    @Bean
    @ConfigurationProperties("spring.datasource.shard0")
    public DataSource shard0DataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Bean
    @ConfigurationProperties("spring.datasource.shard1")
    public DataSource shard1DataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Bean
    @ConfigurationProperties("spring.datasource.shard2")
    public DataSource shard2DataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Bean
    @ConfigurationProperties("spring.datasource.shard3")
    public DataSource shard3DataSource() {
        return DataSourceBuilder.create().build();
    }
    
    /**
     * 读写分离数据源配置
     */
    @Bean
    @ConfigurationProperties("spring.datasource.master")
    public DataSource masterDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Bean
    @ConfigurationProperties("spring.datasource.slave")
    public DataSource slaveDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    /**
     * 遗留系统数据源配置
     */
    @Bean
    @ConfigurationProperties("spring.datasource.legacy")
    public DataSource legacyDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    /**
     * 新系统数据源配置
     */
    @Bean
    @ConfigurationProperties("spring.datasource.new-system")
    public DataSource newSystemDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    /**
     * 多租户数据源配置示例
     */
    @Bean
    @ConfigurationProperties("spring.datasource.tenant-a")
    public DataSource tenantADataSource() {
        return DataSourceBuilder.create().build();
    }
    
    @Bean
    @ConfigurationProperties("spring.datasource.tenant-b")
    public DataSource tenantBDataSource() {
        return DataSourceBuilder.create().build();
    }
    
    /**
     * 注册所有数据源到DataSourceContext
     * 这样就可以通过DataSourceContext.executeWithDataSource()方法使用这些数据源
     */
    @Bean
    public DataSourceRegistrar dataSourceRegistrar() {
        return new DataSourceRegistrar();
    }
    
    /**
     * 数据源注册器
     * 负责将所有配置的数据源注册到DataSourceContext中
     */
    public class DataSourceRegistrar {
        
        public DataSourceRegistrar() {
            // 注册所有数据源信息到DataSourceContext
            registerAllDataSources();
        }
        
        private void registerAllDataSources() {
            // 基础数据源
            DataSourceContext.registerDataSource("main", "Main DataSource", "主数据源");
            DataSourceContext.registerDataSource("analytics", "Analytics DataSource", "分析数据源");
            DataSourceContext.registerDataSource("backup", "Backup DataSource", "备份数据源");
            
            // 分片数据源
            DataSourceContext.registerDataSource("shard_0", "Shard 0 DataSource", "分片数据源0");
            DataSourceContext.registerDataSource("shard_1", "Shard 1 DataSource", "分片数据源1");
            DataSourceContext.registerDataSource("shard_2", "Shard 2 DataSource", "分片数据源2");
            DataSourceContext.registerDataSource("shard_3", "Shard 3 DataSource", "分片数据源3");
            
            // 读写分离数据源
            DataSourceContext.registerDataSource("master", "Master DataSource", "主库数据源");
            DataSourceContext.registerDataSource("slave", "Slave DataSource", "从库数据源");
            
            // 系统迁移数据源
            DataSourceContext.registerDataSource("legacy", "Legacy DataSource", "遗留系统数据源");
            DataSourceContext.registerDataSource("new_system", "New System DataSource", "新系统数据源");
            
            // 多租户数据源
            DataSourceContext.registerDataSource("tenant_a", "Tenant A DataSource", "租户A数据源");
            DataSourceContext.registerDataSource("tenant_b", "Tenant B DataSource", "租户B数据源");
        }
    }
}