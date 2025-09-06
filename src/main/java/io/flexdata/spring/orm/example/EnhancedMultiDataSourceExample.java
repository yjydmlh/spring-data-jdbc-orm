package io.flexdata.spring.orm.example;

import io.flexdata.spring.orm.annotation.DataSource;
import io.flexdata.spring.orm.core.table.Table;
import io.flexdata.spring.orm.core.datasource.DataSourceContext;
import io.flexdata.spring.orm.core.table.TableContext;
import io.flexdata.spring.orm.core.repository.EnhancedMultiRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 增强多数据源和多表切换使用示例
 * 
 * 本示例展示了如何使用增强的多数据源功能：
 * 1. 任意多个数据源的配置和切换
 * 2. 动态表名切换
 * 3. 数据源和表名的组合操作
 * 4. 编程式和注解式两种使用方式
 */
@Service
public class EnhancedMultiDataSourceExample {
    
    @Autowired
    private EnhancedMultiRepository<User, Long> userRepository;
    
    /**
     * 示例1：使用注解方式进行数据源和表切换
     */
    @DataSource("user_db")
    @Table(value = {"user:user_2024"})
    public List<User> getUsersFromSpecificSourceAndTable() {
        // 这个方法会在user_db数据源的user_2024表中查询数据
        return userRepository.findAll();
    }
    
    /**
     * 示例2：使用编程式方式进行数据源切换
     */
    public List<User> getUsersFromMultipleSources() {
        // 从主数据源查询
        List<User> mainUsers = DataSourceContext.executeWithDataSource("main", () -> {
            return userRepository.findAll();
        });
        
        // 从备份数据源查询
        List<User> backupUsers = DataSourceContext.executeWithDataSource("backup", () -> {
            return userRepository.findAll();
        });
        
        // 合并结果
        mainUsers.addAll(backupUsers);
        return mainUsers;
    }
    
    /**
     * 示例3：使用编程式方式进行表名切换
     */
    public List<User> getUsersFromDifferentTables() {
        // 从当前年份表查询
        List<User> currentYearUsers = TableContext.executeWithTableMapping("user", "user_2024", () -> {
            return userRepository.findAll();
        });
        
        // 从历史表查询
        List<User> historyUsers = TableContext.executeWithTableMapping("user", "user_history", () -> {
            return userRepository.findAll();
        });
        
        currentYearUsers.addAll(historyUsers);
        return currentYearUsers;
    }
    
    /**
     * 示例4：数据源和表名的组合操作
     */
    @DataSource("analytics_db")
    public void performAnalytics() {
        // 在analytics_db数据源中，分别从不同的表查询数据进行分析
        
        // 查询用户数据
        List<User> users = TableContext.executeWithTableMapping("user", "user_analytics", () -> {
            return userRepository.findAll();
        });
        
        // 查询订单数据（假设有OrderRepository）
        // List<Order> orders = TableContext.executeWithTable("order", "order_analytics", () -> {
        //     return orderRepository.findAll();
        // });
        
        // 执行分析逻辑...
    }
    
    /**
     * 示例5：使用增强Repository接口的便捷方法
     */
    public void useEnhancedRepositoryMethods() {
        // 在指定数据源中查询
        List<User> usersFromSpecificSource = userRepository.findAllOnDataSource("user_db");
        
        // 在指定表中查询
        List<User> usersFromSpecificTable = userRepository.findAllOnTable("user", "user_2024");
        
        // 在指定数据源和表中查询
        List<User> usersFromSpecificSourceAndTable = 
            userRepository.findAllOnDataSourceAndTable("user_db", "user", "user_2024");
        
        // 在多个数据源中批量查询
        List<List<User>> usersByDataSource = 
            userRepository.findAllOnMultipleDataSources(List.of("main", "backup", "archive"));
        
        // 在多个表中批量查询
        List<User> usersByTable = 
            userRepository.findAllOnMultipleTables(Map.of("user", "user_2024", "user_old", "user_2023"));
    }
    
    /**
     * 示例6：复杂的数据迁移场景
     */
    public void performDataMigration() {
        // 从旧系统数据源的历史表中读取数据
        List<User> oldUsers = DataSourceContext.executeWithDataSource("legacy_db", () -> {
            return TableContext.executeWithTableMapping("user", "old_user_table", () -> {
                return userRepository.findAll();
            });
        });
        
        // 将数据写入新系统数据源的当前表中
        DataSourceContext.executeWithDataSource("new_db", () -> {
            return TableContext.executeWithTableMapping("user", "new_user_table", () -> {
                for (User user : oldUsers) {
                    userRepository.save(user);
                }
                return null;
            });
        });
    }
    
    /**
     * 示例7：使用SpEL表达式动态确定表名
     */
    @Table(logicalName = "user", physicalName = "#{T(java.time.LocalDate).now().getYear() + '_user'}")
    public List<User> getUsersFromCurrentYearTable() {
        // 表名会根据当前年份动态生成，如：2024_user
        return userRepository.findAll();
    }
    
    /**
     * 示例8：数据源映射管理
     */
    public void manageDataSourceMappings() {
        // 设置数据源映射
        DataSourceContext.setDataSourceMapping("user_operations", "user_db");
        DataSourceContext.setDataSourceMapping("order_operations", "order_db");
        DataSourceContext.setDataSourceMapping("analytics_operations", "analytics_db");
        
        try {
            // 使用映射后的数据源名称
            List<User> users = DataSourceContext.executeWithDataSource("user_operations", () -> {
                return userRepository.findAll();
            });
            
            // 处理用户数据...
            
        } finally {
            // 清理映射
            DataSourceContext.clearDataSourceMappings();
        }
    }
    
    /**
     * 示例9：表名映射管理
     */
    public void manageTableMappings() {
        // 设置表名映射
        TableContext.setTableMapping("current_user", "user_2024");
        TableContext.setTableMapping("archive_user", "user_archive");
        
        try {
            // 使用映射后的表名
            List<User> currentUsers = TableContext.executeWithTableMapping("current_user", "user_2024", () -> {
                return userRepository.findAll();
            });
            
            List<User> archiveUsers = TableContext.executeWithTableMapping("archive_user", "user_archive", () -> {
                return userRepository.findAll();
            });
            
            // 处理数据...
            
        } finally {
            // 清理映射
            TableContext.clearTableMappings();
        }
    }
    
    /**
     * 用户实体类示例
     */
    public static class User {
        private Long id;
        private String name;
        private String email;
        
        // 构造函数、getter和setter省略...
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}