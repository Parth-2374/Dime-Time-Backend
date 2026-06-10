package com.dimetime.controller;

import com.dimetime.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class AccountSecurityController {

    @Autowired
    private UserService userService;

    // 3. FORGOT PASSWORD - STEP 1 & 2: REQUEST OTP (Public)
    @PostMapping("/api/auth/forgot-password/request")
    public ResponseEntity<?> requestPasswordResetOtp(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            userService.requestPasswordResetOtp(email);
            return ResponseEntity.ok(Map.of("message", "OTP sent successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 4. FORGOT PASSWORD - STEP 3: VERIFY OTP (Public)
    @PostMapping("/api/auth/forgot-password/verify")
    public ResponseEntity<?> verifyPasswordResetOtp(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String otp = request.get("otp");
            userService.verifyPasswordResetOtp(email, otp);
            return ResponseEntity.ok(Map.of("message", "OTP verified successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 5. FORGOT PASSWORD - STEP 4: RESET PASSWORD (Public)
    @PostMapping("/api/auth/forgot-password/reset")
    public ResponseEntity<?> resetPasswordWithOtp(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String otp = request.get("otp");
            String newPassword = request.get("newPassword");
            String confirmPassword = request.get("confirmPassword");

            userService.resetPasswordWithOtp(email, otp, newPassword, confirmPassword);
            return ResponseEntity.ok(Map.of("message", "Password reset successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 6. FORGOT USERNAME - STEP 1 & 2: REQUEST OTP (Public)
    @PostMapping("/api/auth/forgot-username/request")
    public ResponseEntity<?> requestUsernameRecoveryOtp(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            userService.requestUsernameRecoveryOtp(email);
            return ResponseEntity.ok(Map.of("message", "OTP sent successfully."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // 7. FORGOT USERNAME - STEP 3 & 4: VERIFY & DISPLAY (Public)
    @PostMapping("/api/auth/forgot-username/verify")
    public ResponseEntity<?> verifyUsernameRecoveryOtp(@RequestBody Map<String, String> request) {
        try {
            String email = request.get("email");
            String otp = request.get("otp");
            String username = userService.verifyUsernameRecoveryOtp(email, otp);
            return ResponseEntity.ok(Map.of(
                "message", "Username recovered successfully.",
                "username", username
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
