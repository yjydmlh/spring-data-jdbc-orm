package com.spring.jdbc.orm.examples;

import com.spring.jdbc.orm.annotation.DataSource;
import com.spring.jdbc.orm.core.datasource.DataSourceContext;
import com.spring.jdbc.orm.core.interfaces.MultiDataSourceRepository;
import com.spring.jdbc.orm.example.entiry.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 多数据源使用示例
 * 展示如何使用动态数据源切换功能
 */
@Service
public class MultiDataSourceExample {
    
    @Autowired
    private UserRepository userRepository;
    
    // ========== 方式1：注解驱动的数据源切换 ==========
    
    /**
     * 从只读库查询用户列表（注解方式）
     */
    @DataSource(type = DataSource.Type.READONLY)
    public List<User> getUsersFromReadOnly() {
        return userRepository.findAll();
    }
    
    /**
     * 从主库保存用户（注解方式）
     */
    @DataSource(type = DataSource.Type.MASTER)
    @Transactional
    public User saveUserToMaster(User user) {
        return userRepository.save(user);
    }
    
    /**
     * 从第二个数据源查询用户（注解方式）
     */
    @DataSource("secondary")
    public Optional<User> getUserFromSecondary(Long id) {
        return userRepository.findById(id);
    }
    
    // ========== 方式2：编程式数据源切换 ==========
    
    /**
     * 编程式切换到只读库查询
     */
    public List<User> getUsersFromReadOnlyProgrammatic() {
        return DataSourceContext.executeWithDataSource("readonly", () -> {
            return userRepository.findAll();
        });
    }
    
    /**
     * 编程式切换到主库保存
     */
    @Transactional
    public User saveUserToMasterProgrammatic(User user) {
        return DataSourceContext.executeWithDataSource("default", () -> {
            return userRepository.save(user);
        });
    }
    
    // ========== 方式3：Repository接口便捷方法 ==========
    
    /**
     * 使用Repository的便捷方法
     */
    public void demonstrateRepositoryMethods() {
        // 从只读库查询
        List<User> users = userRepository.findAllFromReadOnly();
        
        // 从只读库分页查询
        Page<User> userPage = userRepository.findAllFromReadOnly(PageRequest.of(0, 10));
        
        // 强制从主库查询（写后读场景）
        Optional<User> user = userRepository.findByIdFromMaster(1L);
        
        // 保存到主库
        User newUser = new User();
        newUser.setUserName("test");
        userRepository.saveToMaster(newUser);
    }
    
    // ========== 方式4：复杂业务场景 ==========
    
    /**
     * 读写分离场景：写操作后立即读取
     */
    @Transactional
    public User createAndRead(User user) {
        // 1. 保存到主库
        User savedUser = DataSourceContext.executeWithDataSource("default", () -> {
            return userRepository.save(user);
        });
        
        // 2. 立即从主库读取（避免主从延迟）
        return DataSourceContext.executeWithDataSource("default", () -> {
            return userRepository.findById(savedUser.getId()).orElse(null);
        });
    }
    
    /**
     * 多数据源聚合查询
     */
    public UserStatistics getUserStatistics() {
        // 从主库获取总用户数
        long totalUsers = DataSourceContext.executeWithDataSource("default", () -> {
            return userRepository.count();
        });
        
        // 从只读库获取活跃用户数（减少主库压力）
        long activeUsers = DataSourceContext.executeWithDataSource("readonly", () -> {
            // 假设有活跃用户查询方法
            return userRepository.count(); // 简化示例
        });
        
        // 从第二个数据源获取其他统计信息
        long otherStats = userRepository.withSecondaryDataSource(() -> {
            return userRepository.count();
        });
        
        return new UserStatistics(totalUsers, activeUsers, otherStats);
    }
    
    /**
     * 批量操作的数据源选择
     */
    @Transactional
    public void batchOperations(List<User> users) {
        // 批量保存到主库
        DataSourceContext.executeWithDataSource("default", () -> {
            userRepository.saveAll(users);
            return null;
        });
        
        // 从只读库验证保存结果（可能有延迟）
        DataSourceContext.executeWithDataSource("readonly", () -> {
            long count = userRepository.count();
            System.out.println("Current user count in readonly: " + count);
            return null;
        });
    }
    
    // ========== 内部类 ==========
    
    /**
     * 用户统计信息
     */
    public static class UserStatistics {
        private final long totalUsers;
        private final long activeUsers;
        private final long otherStats;
        
        public UserStatistics(long totalUsers, long activeUsers, long otherStats) {
            this.totalUsers = totalUsers;
            this.activeUsers = activeUsers;
            this.otherStats = otherStats;
        }
        
        // getters...
        public long getTotalUsers() { return totalUsers; }
        public long getActiveUsers() { return activeUsers; }
        public long getOtherStats() { return otherStats; }
    }
    
    /**
     * 用户Repository接口（继承多数据源功能）
     */
    public interface UserRepository extends MultiDataSourceRepository<User, Long> {
        // 继承所有基础CRUD方法和多数据源便捷方法
    }
}