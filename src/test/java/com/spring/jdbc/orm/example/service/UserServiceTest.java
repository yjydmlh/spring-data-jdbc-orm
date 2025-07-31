package com.spring.jdbc.orm.example.service;

import com.spring.jdbc.orm.example.entiry.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class UserServiceTest {

    @Autowired
    private UserService userService;

    @Test
    void testCreateAndFindUser() {
        // Create a new user
        User newUser = userService.createUser("testuser", "test@example.com", 30);
        assertNotNull(newUser.getId(), "User ID should not be null after saving");

        // Find the user by ID
        Optional<User> foundUserOpt = userService.findById(newUser.getId());
        assertTrue(foundUserOpt.isPresent(), "User should be found by ID");

        User foundUser = foundUserOpt.get();
        assertEquals("testuser", foundUser.getUserName());
        assertEquals(30, foundUser.getAge());
    }

    @Test
    void testUpdateUser() {
        // Create a user first
        User user = userService.createUser("updateuser", "update@example.com", 40);
        Long userId = user.getId();

        // Update the user's age
        user.setAge(45);
        userService.updateUser(user);

        // Retrieve and verify the update
        Optional<User> updatedUserOpt = userService.findById(userId);
        assertTrue(updatedUserOpt.isPresent());
        assertEquals(45, updatedUserOpt.get().getAge());
    }

    @Test
    void testDeleteUser() {
        // Create a user
        User user = userService.createUser("deleteuser", "delete@example.com", 25);
        Long userId = user.getId();

        // Delete the user
        userService.deleteUser(userId);

        // Verify the user is deleted
        Optional<User> deletedUserOpt = userService.findById(userId);
        assertFalse(deletedUserOpt.isPresent(), "User should not be found after deletion");
    }

    @Test
    void testFindByUserName() {
        userService.createUser("findme", "findme@example.com", 33);
        assertFalse(userService.findByUserName("findme").isEmpty());
    }

    @Test
    void testUserExists() {
        userService.createUser("existinguser", "exists@example.com", 50);
        assertTrue(userService.userExists("existinguser"));
        assertFalse(userService.userExists("nonexistinguser"));
    }
}
