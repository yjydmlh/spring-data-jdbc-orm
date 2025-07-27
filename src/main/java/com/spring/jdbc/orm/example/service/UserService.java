package com.spring.jdbc.orm.example.service;

import com.spring.jdbc.orm.core.interfaces.TypeSafeCriteria;
import com.spring.jdbc.orm.criteria.TypeSafeCriteriaBuilder;
import com.spring.jdbc.orm.example.entiry.User;
import com.spring.jdbc.orm.template.TypeSafeOrmTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 用户服务示例
 * 文件位置: src/main/java/com/example/orm/example/service/UserService.java
 */
@Service
@Transactional
public class UserService {

    @Autowired
    private TypeSafeOrmTemplate orm;

    public User createUser(String userName, String email, Integer age) {
        User user = new User(userName, email, age);
        return orm.save(user);
    }

    public Optional<User> findById(Long id) {
        return orm.findById(User.class, id);
    }

    public List<User> findAll() {
        return orm.findAll(User.class);
    }

    public List<User> findByUserName(String userName) {
        TypeSafeCriteriaBuilder<User> cb = orm.criteria(User.class);
        return orm.findByCriteria(User.class, cb.eq(User::getUserName, userName));
    }

    public List<User> findActiveAdults() {
        TypeSafeCriteriaBuilder<User> cb = orm.criteria(User.class);
        TypeSafeCriteria<User> criteria = cb.gte(User::getAge, 18)
                .and(cb.isNotNull(User::getEmail));

        return orm.findByCriteria(User.class, criteria);
    }

    public Page<User> searchUsers(String keyword, Pageable pageable) {
        TypeSafeCriteriaBuilder<User> cb = orm.criteria(User.class);

        TypeSafeCriteria<User> criteria = cb.like(User::getUserName, "%" + keyword + "%")
                .or(cb.like(User::getEmail, "%" + keyword + "%"));

        return orm.getRepository(User.class).findByCriteria(criteria, pageable);
    }

    public List<User> findUsersByAgeRange(Integer minAge, Integer maxAge) {
        TypeSafeCriteriaBuilder<User> cb = orm.criteria(User.class);
        return orm.findByCriteria(User.class, cb.between(User::getAge, minAge, maxAge));
    }

    public List<User> findUsersByNames(List<String> names) {
        TypeSafeCriteriaBuilder<User> cb = orm.criteria(User.class);
        return orm.findByCriteria(User.class, cb.in(User::getUserName, names));
    }

    public long countActiveUsers() {
        TypeSafeCriteriaBuilder<User> cb = orm.criteria(User.class);
        return orm.getRepository(User.class).countByCriteria(
                cb.isNotNull(User::getEmail).and(cb.gte(User::getAge, 18))
        );
    }

    public void deleteInactiveUsers() {
        TypeSafeCriteriaBuilder<User> cb = orm.criteria(User.class);
        orm.getRepository(User.class).deleteByCriteria(cb.isNull(User::getEmail));
    }

    public User updateUser(User user) {
        return orm.save(user);
    }

    public void deleteUser(Long id) {
        orm.deleteById(User.class, id);
    }

    public boolean userExists(String userName) {
        TypeSafeCriteriaBuilder<User> cb = orm.criteria(User.class);
        return orm.getRepository(User.class).exists(cb.eq(User::getUserName, userName));
    }

    // 复杂查询示例
    public List<User> findUsersForMarketing() {
        TypeSafeCriteriaBuilder<User> cb = orm.criteria(User.class);

        // 查找年龄在25-45之间，有邮箱，用户名包含特定关键词的用户
        TypeSafeCriteria<User> criteria = cb.between(User::getAge, 25, 45)
                .and(cb.isNotNull(User::getEmail))
                .and(cb.like(User::getUserName, "%premium%")
                        .or(cb.like(User::getUserName, "%vip%")));

        return orm.findByCriteria(User.class, criteria);
    }
}
