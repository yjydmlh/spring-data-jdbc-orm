package io.flexdata.spring.orm.example.controller;

import io.flexdata.spring.orm.example.entiry.User;
import io.flexdata.spring.orm.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * 用户控制器示例
 * 文件位置: src/main/java/com/example/orm/example/controller/UserController.java
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        User created = userService.createUser(user.getUserName(), user.getEmail(), user.getAge());
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        Optional<User> user = userService.findById(id);
        return user.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.findAll();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<User>> searchUsers(
            @RequestParam String keyword,
            Pageable pageable) {
        Page<User> users = userService.searchUsers(keyword, pageable);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/by-username/{userName}")
    public ResponseEntity<List<User>> getUsersByUserName(@PathVariable String userName) {
        List<User> users = userService.findByUserName(userName);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/adults")
    public ResponseEntity<List<User>> getActiveAdults() {
        List<User> users = userService.findActiveAdults();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/age-range")
    public ResponseEntity<List<User>> getUsersByAgeRange(
            @RequestParam Integer minAge,
            @RequestParam Integer maxAge) {
        List<User> users = userService.findUsersByAgeRange(minAge, maxAge);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> countActiveUsers() {
        long count = userService.countActiveUsers();
        return ResponseEntity.ok(count);
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        User updated = userService.updateUser(user);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/inactive")
    public ResponseEntity<Void> deleteInactiveUsers() {
        userService.deleteInactiveUsers();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/exists/{userName}")
    public ResponseEntity<Boolean> userExists(@PathVariable String userName) {
        boolean exists = userService.userExists(userName);
        return ResponseEntity.ok(exists);
    }
}
