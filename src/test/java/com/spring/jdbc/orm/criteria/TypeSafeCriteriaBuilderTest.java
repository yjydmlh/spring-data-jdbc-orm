package com.spring.jdbc.orm.criteria;

import com.spring.jdbc.orm.core.interfaces.TypeSafeCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TypeSafeCriteriaBuilderTest {

    private TypeSafeCriteriaBuilder<User> builder;

    // A simple entity for testing
    private static class User {
        private String username;
        private int age;
        private boolean active;

        public String getUsername() {
            return username;
        }

        public int getAge() {
            return age;
        }

        public boolean isActive() {
            return active;
        }
    }

    @BeforeEach
    void setUp() {
        builder = TypeSafeCriteriaBuilder.create();
    }

    @Test
    void testEq() {
        TypeSafeCriteria<User> criteria = builder.eq(User::getUsername, "testuser");
        assertEquals("username = :username", criteria.toSql());
        assertEquals("testuser", criteria.getParameters().get("username"));
    }

    @Test
    void testNe() {
        TypeSafeCriteria<User> criteria = builder.ne(User::getAge, 30);
        assertEquals("age != :age", criteria.toSql());
        assertEquals(30, criteria.getParameters().get("age"));
    }

    @Test
    void testGt() {
        TypeSafeCriteria<User> criteria = builder.gt(User::getAge, 25);
        assertEquals("age > :age", criteria.toSql());
        assertEquals(25, criteria.getParameters().get("age"));
    }

    @Test
    void testGte() {
        TypeSafeCriteria<User> criteria = builder.gte(User::getAge, 25);
        assertEquals("age >= :age", criteria.toSql());
        assertEquals(25, criteria.getParameters().get("age"));
    }

    @Test
    void testLt() {
        TypeSafeCriteria<User> criteria = builder.lt(User::getAge, 30);
        assertEquals("age < :age", criteria.toSql());
        assertEquals(30, criteria.getParameters().get("age"));
    }

    @Test
    void testLte() {
        TypeSafeCriteria<User> criteria = builder.lte(User::getAge, 30);
        assertEquals("age <= :age", criteria.toSql());
        assertEquals(30, criteria.getParameters().get("age"));
    }

    @Test
    void testLike() {
        TypeSafeCriteria<User> criteria = builder.like(User::getUsername, "user%");
        assertEquals("username LIKE :username", criteria.toSql());
        assertEquals("user%", criteria.getParameters().get("username"));
    }

    @Test
    void testNotLike() {
        TypeSafeCriteria<User> criteria = builder.notLike(User::getUsername, "admin%");
        assertEquals("username NOT LIKE :username", criteria.toSql());
        assertEquals("admin%", criteria.getParameters().get("username"));
    }

    @Test
    void testIn() {
        TypeSafeCriteria<User> criteria = builder.in(User::getUsername, Arrays.asList("user1", "user2"));
        assertEquals("username IN (:username_0, :username_1)", criteria.toSql());
        Map<String, Object> params = criteria.getParameters();
        assertEquals("user1", params.get("username_0"));
        assertEquals("user2", params.get("username_1"));
    }

    @Test
    void testNotIn() {
        TypeSafeCriteria<User> criteria = builder.notIn(User::getUsername, Collections.singletonList("admin"));
        assertEquals("username NOT IN (:username_0)", criteria.toSql());
        assertEquals(Collections.singletonList("admin").get(0), criteria.getParameters().get("username_0"));
    }

    @Test
    void testIsNull() {
        TypeSafeCriteria<User> criteria = builder.isNull(User::getUsername);
        assertEquals("username IS NULL", criteria.toSql());
        assertTrue(criteria.getParameters().isEmpty());
    }

    @Test
    void testIsNotNull() {
        TypeSafeCriteria<User> criteria = builder.isNotNull(User::getUsername);
        assertEquals("username IS NOT NULL", criteria.toSql());
        assertTrue(criteria.getParameters().isEmpty());
    }

    @Test
    void testBetween() {
        TypeSafeCriteria<User> criteria = builder.between(User::getAge, 20, 30);
        assertEquals("age BETWEEN :age_start AND :age_end", criteria.toSql());
        Map<String, Object> params = criteria.getParameters();
        assertEquals(20, params.get("age_start"));
        assertEquals(30, params.get("age_end"));
    }

    @Test
    void testAnd() {
        TypeSafeCriteria<User> criteria = builder.eq(User::getUsername, "test")
                .and(builder.gt(User::getAge, 25));
        assertEquals("(username = :username AND age > :age)", criteria.toSql());
        Map<String, Object> params = criteria.getParameters();
        assertEquals("test", params.get("username"));
        assertEquals(25, params.get("age"));
    }

    @Test
    void testOr() {
        TypeSafeCriteria<User> criteria = builder.eq(User::isActive, true)
                .or(builder.isNull(User::getUsername));
        assertEquals("(active = :active OR username IS NULL)", criteria.toSql());
        assertEquals(true, criteria.getParameters().get("active"));
    }

    @Test
    void testComplexCombination() {
        TypeSafeCriteria<User> criteria = builder.eq(User::isActive, true)
                .and(builder.gt(User::getAge, 18).or(builder.like(User::getUsername, "a%")));

        assertEquals("(active = :active AND (age > :age OR username LIKE :username))", criteria.toSql());
        Map<String, Object> params = criteria.getParameters();
        assertEquals(true, params.get("active"));
        assertEquals(18, params.get("age"));
        assertEquals("a%", params.get("username"));
    }
}
