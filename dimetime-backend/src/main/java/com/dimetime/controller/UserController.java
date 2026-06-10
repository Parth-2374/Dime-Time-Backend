package com.dimetime.controller;

import com.dimetime.entity.User;
import com.dimetime.service.UserService;
import com.dimetime.dto.RegisterRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody RegisterRequest request) {
        try {
            userService.registerUser(request);
            return ResponseEntity.ok().body("{\"message\": \"User created successfully\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(
            @PathVariable Long id,
            @RequestBody User userDetails,
            @RequestParam("operator") String operator) {
        try {
            User result = userService.updateUser(id, userDetails, operator);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(
            @PathVariable Long id,
            @RequestParam("operator") String operator) {
        try {
            userService.deleteUser(id, operator);
            return ResponseEntity.ok().body("{\"message\": \"User deleted successfully\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}/reset-password")
    public ResponseEntity<?> resetPassword(
            @PathVariable Long id,
            @RequestParam("password") String password,
            @RequestParam("operator") String operator) {
        try {
            userService.resetPassword(id, password, operator);
            return ResponseEntity.ok().body("{\"message\": \"Password reset successfully\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{username}/role")
    public ResponseEntity<?> updateUserRole(
            @PathVariable String username,
            @RequestParam("role") String role,
            @RequestParam("operator") String operator) {
        try {
            User result = userService.updateUserRole(username, role, operator);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> request) {
        try {
            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            String oldPassword = request.get("oldPassword");
            String newPassword = request.get("newPassword");
            String confirmPassword = request.get("confirmPassword");

            userService.changePassword(currentUsername, oldPassword, newPassword, confirmPassword);
            return ResponseEntity.ok(Map.of("message", "Password changed successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/change-username")
    public ResponseEntity<?> changeUsername(@RequestBody Map<String, String> request) {
        try {
            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            String newUsername = request.get("newUsername");
            String confirmUsername = request.get("confirmUsername");

            userService.changeUsername(currentUsername, newUsername, confirmUsername);
            return ResponseEntity.ok(Map.of("message", "Username updated successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile() {
        try {
            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userService.getUserByUsername(currentUsername);
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("mobileNumber", user.getMobile());
            response.put("companyName", user.getCompanyName());
            response.put("role", user.getRole());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateUserProfile(@RequestBody Map<String, String> request) {
        try {
            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            String email = request.get("email");
            String mobileNumber = request.get("mobileNumber");
            
            if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
                throw new IllegalArgumentException("Invalid email format.");
            }
            if (mobileNumber == null || mobileNumber.replaceAll("\\D", "").length() < 10) {
                throw new IllegalArgumentException("Mobile number must be at least 10 digits.");
            }
            
            User updatedUser = userService.updateProfile(currentUsername, email, mobileNumber);
            
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("username", updatedUser.getUsername());
            response.put("email", updatedUser.getEmail());
            response.put("mobileNumber", updatedUser.getMobile());
            response.put("companyName", updatedUser.getCompanyName());
            response.put("role", updatedUser.getRole());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
