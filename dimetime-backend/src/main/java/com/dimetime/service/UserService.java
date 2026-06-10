package com.dimetime.service;

import com.dimetime.entity.User;
import com.dimetime.entity.Company;
import com.dimetime.entity.PasswordResetToken;
import com.dimetime.entity.UsernameRecoveryLog;
import com.dimetime.dto.RegisterRequest;
import com.dimetime.dto.LoginRequest;
import com.dimetime.dto.LoginResponse;
import com.dimetime.repository.UserRepository;
import com.dimetime.repository.CompanyRepository;
import com.dimetime.repository.PasswordResetTokenRepository;
import com.dimetime.repository.UsernameRecoveryLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private UsernameRecoveryLogRepository usernameRecoveryLogRepository;

    @Autowired
    private EmailService emailService;

    public void registerUser(RegisterRequest request) {
        if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (request.getPassword() == null || request.getPassword().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long");
        }
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Find or create Company Profile
        Company company = null;
        if (request.getCompanyName() != null && !request.getCompanyName().trim().isEmpty()) {
            String cName = request.getCompanyName().trim();
            Optional<Company> companyOpt = companyRepository.findByName(cName);
            if (companyOpt.isPresent()) {
                company = companyOpt.get();
            } else {
                String gst = (request.getGstNumber() != null && !request.getGstNumber().trim().isEmpty()) 
                        ? request.getGstNumber().trim() : "GST-" + System.currentTimeMillis();
                String addr = (request.getAddress() != null && !request.getAddress().trim().isEmpty()) 
                        ? request.getAddress().trim() : "Corporate Address";
                String contact = (request.getContactPerson() != null && !request.getContactPerson().trim().isEmpty()) 
                        ? request.getContactPerson().trim() : request.getFullName();
                String mail = request.getEmail() != null ? request.getEmail() : "info@" + cName.toLowerCase().replace(" ", "") + ".com";
                String ph = request.getMobile() != null ? request.getMobile() : "+15550000";
                
                company = new Company(
                        cName,
                        gst,
                        addr,
                        contact,
                        mail,
                        ph,
                        request.getRole() != null ? request.getRole() : "SUPPLIER"
                );
                company = companyRepository.save(company);
            }
        }

        User user = new User(
                request.getFullName(),
                request.getEmail(),
                request.getMobile(),
                request.getCompanyName(),
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()), // Encrypted using BCryptPasswordEncoder
                request.getRole() != null ? request.getRole() : "SUPPLIER"
        );
        user.setGstNumber(request.getGstNumber());
        user.setAddress(request.getAddress());
        user.setContactPerson(request.getContactPerson());
        user.setCompany(company);

        userRepository.save(user);
        auditLogService.logActivity("Registered User Account: " + user.getUsername(), user.getUsername());
    }

    public LoginResponse loginUser(LoginRequest request) {
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());
        
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        User user = userOpt.get();
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        auditLogService.logActivity("Logged in Successfully", user.getUsername());
        
        // Generate UserDetails for JWT token generation
        org.springframework.security.core.userdetails.UserDetails userDetails = 
                org.springframework.security.core.userdetails.User.builder()
                        .username(user.getUsername())
                        .password(user.getPassword())
                        .roles(user.getRole())
                        .build();
        
        String token = jwtService.generateToken(userDetails);
        
        return new LoginResponse(
                "Login Success",
                user.getUsername(),
                user.getRole(),
                user.getFullName(),
                user.getCompanyName(),
                token,
                user.getId()
        );
    }

    public java.util.List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User updateUserRole(String username, String role, String operator) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found: " + username);
        }
        User user = userOpt.get();
        user.setRole(role);
        User saved = userRepository.save(user);
        auditLogService.logActivity("Updated User role for " + username + " to " + role, operator);
        return saved;
    }

    public void deleteUser(Long id, String operator) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isPresent()) {
            String username = userOpt.get().getUsername();
            userRepository.deleteById(id);
            auditLogService.logActivity("Deleted User Account: " + username, operator);
        }
    }

    public User updateUser(Long id, User userDetails, String operator) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found with id: " + id);
        }
        User user = userOpt.get();
        user.setFullName(userDetails.getFullName());
        user.setEmail(userDetails.getEmail());
        user.setMobile(userDetails.getMobile());
        user.setCompanyName(userDetails.getCompanyName());
        user.setRole(userDetails.getRole());
        user.setGstNumber(userDetails.getGstNumber());
        user.setAddress(userDetails.getAddress());
        user.setContactPerson(userDetails.getContactPerson());
        
        if (userDetails.getPassword() != null && !userDetails.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(userDetails.getPassword().trim()));
        }
        
        User saved = userRepository.save(user);
        auditLogService.logActivity("Updated User Account: " + user.getUsername(), operator);
        return saved;
    }

    public void resetPassword(Long id, String newPassword, String operator) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found with id: " + id);
        }
        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        userRepository.save(user);
        auditLogService.logActivity("Reset password for user: " + user.getUsername(), operator);
    }

    public void changePassword(String currentUsername, String oldPassword, String newPassword, String confirmPassword) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!user.getRole().equals("SUPPLIER") && !user.getRole().equals("MANUFACTURER")) {
            throw new IllegalArgumentException("Only SUPPLIER and MANUFACTURER users are allowed to use this feature.");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Incorrect old password.");
        }

        // Validate password rules
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters long.");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Confirm password does not match.");
        }
        
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        for (char c : newPassword.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
        }
        if (!hasUpper || !hasLower || !hasDigit) {
            throw new IllegalArgumentException("New password must contain at least one uppercase letter, one lowercase letter, and one number.");
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("New password cannot be the same as your current password.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        auditLogService.logActivity("PASSWORD_CHANGED", user.getUsername());
    }

    public void changeUsername(String currentUsername, String newUsername, String confirmUsername) {
        User user = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!user.getRole().equals("SUPPLIER") && !user.getRole().equals("MANUFACTURER")) {
            throw new IllegalArgumentException("Only SUPPLIER and MANUFACTURER users are allowed to use this feature.");
        }

        if (newUsername == null || newUsername.trim().length() < 5) {
            throw new IllegalArgumentException("Username must be at least 5 characters long.");
        }

        if (!newUsername.equals(confirmUsername)) {
            throw new IllegalArgumentException("Confirm username does not match.");
        }

        if (newUsername.equalsIgnoreCase(currentUsername)) {
            throw new IllegalArgumentException("New username cannot be the same as your current username.");
        }

        if (userRepository.existsByUsername(newUsername)) {
            throw new IllegalArgumentException("Username already exists.");
        }

        user.setUsername(newUsername);
        userRepository.save(user);

        auditLogService.logActivity("USERNAME_CHANGED", newUsername);
    }

    public void requestPasswordResetOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No user found with the registered email address."));

        if (user.getRole().equals("ADMIN")) {
            throw new IllegalArgumentException("Password reset is not allowed for Admin users.");
        }

        // Generate 6-digit OTP
        String otp = String.format("%06d", new java.util.Random().nextInt(1000000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

        PasswordResetToken token = new PasswordResetToken(user, email, otp, expiresAt);
        passwordResetTokenRepository.save(token);

        // Simulation email log
        // System.out.println("\n==================================================");
        // System.out.println("[EMAIL SERVICE] SENDING PASSWORD RESET OTP");
        // System.out.println("To: " + email);
        // System.out.println("OTP Code: " + otp);
        // System.out.println("Expires At: " + expiresAt);
        // System.out.println("==================================================\n");

        emailService.sendOtpEmail(
            email,
            "Dime Time Password Reset OTP",
            "Your OTP is: " + otp + "\n\nValid for 10 minutes."
    );
    }

    public void verifyPasswordResetOtp(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No user found with the registered email address."));

        PasswordResetToken token = passwordResetTokenRepository
                .findFirstByUserAndVerifiedFalseOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new IllegalArgumentException("No active OTP request found. Request a new OTP."));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("OTP has expired. Request a new OTP.");
        }

        if (token.getAttempts() >= 3) {
            throw new IllegalArgumentException("OTP verification failed. Request a new OTP.");
        }

        if (token.getOtp().equals(otp)) {
            token.setVerified(true);
            passwordResetTokenRepository.save(token);
        } else {
            token.setAttempts(token.getAttempts() + 1);
            passwordResetTokenRepository.save(token);
            if (token.getAttempts() >= 3) {
                throw new IllegalArgumentException("OTP verification failed. Request a new OTP.");
            }
            throw new IllegalArgumentException("Invalid OTP. " + (3 - token.getAttempts()) + " attempts remaining.");
        }
    }

    public void resetPasswordWithOtp(String email, String otp, String newPassword, String confirmPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No user found with the registered email address."));

        PasswordResetToken token = passwordResetTokenRepository
                .findFirstByUserOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new IllegalArgumentException("No OTP verification record found."));

        if (!token.isVerified()) {
            throw new IllegalArgumentException("OTP has not been verified yet.");
        }

        if (!token.getOtp().equals(otp)) {
            throw new IllegalArgumentException("OTP mismatch.");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now().minusMinutes(30))) { // Allow 30 mins window after creation for reset
            throw new IllegalArgumentException("Reset session has expired. Request a new OTP.");
        }

        // Validate password rules
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters long.");
        }
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Confirm password does not match.");
        }
        
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        for (char c : newPassword.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
        }
        if (!hasUpper || !hasLower || !hasDigit) {
            throw new IllegalArgumentException("New password must contain at least one uppercase letter, one lowercase letter, and one number.");
        }

        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("New password cannot be the same as your current password.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        auditLogService.logActivity("PASSWORD_RESET", user.getUsername());
    }

    public void requestUsernameRecoveryOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No user found with the registered email address."));

        if (user.getRole().equals("ADMIN")) {
            throw new IllegalArgumentException("Username recovery is not allowed for Admin users.");
        }

        // Generate 6-digit OTP
        String otp = String.format("%06d", new java.util.Random().nextInt(1000000));
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(10);

        PasswordResetToken token = new PasswordResetToken(user, email, otp, expiresAt);
        passwordResetTokenRepository.save(token);

        // Simulation email log
        // System.out.println("\n==================================================");
        // System.out.println("[EMAIL SERVICE] SENDING USERNAME RECOVERY OTP");
        // System.out.println("To: " + email);
        // System.out.println("OTP Code: " + otp);
        // System.out.println("Expires At: " + expiresAt);
        // System.out.println("==================================================\n");

        emailService.sendOtpEmail(
            email,
            "Dime Time Username Recovery OTP",
            "Your OTP is: " + otp + "\n\nValid for 10 minutes."
    );
    }

    public String verifyUsernameRecoveryOtp(String email, String otp) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("No user found with the registered email address."));

        PasswordResetToken token = passwordResetTokenRepository
                .findFirstByUserAndVerifiedFalseOrderByCreatedAtDesc(user)
                .orElseThrow(() -> new IllegalArgumentException("No active OTP request found. Request a new OTP."));

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("OTP has expired. Request a new OTP.");
        }

        if (token.getAttempts() >= 3) {
            throw new IllegalArgumentException("OTP verification failed. Request a new OTP.");
        }

        if (token.getOtp().equals(otp)) {
            token.setVerified(true);
            passwordResetTokenRepository.save(token);

            // Log username recovery
            UsernameRecoveryLog log = new UsernameRecoveryLog(user, email);
            usernameRecoveryLogRepository.save(log);

            // Simulation email log sending recovered username
            System.out.println("\n==================================================");
            System.out.println("[EMAIL SERVICE] USERNAME RECOVERY LOG");
            System.out.println("To: " + email);
            System.out.println("emailService.sendOtpEmail(\n" + //
                                "        email,\n" + //
                                "        \"Dime Time Username Recovery\",\n" + //
                                "        \"Your Username is: \" + user.getUsername()\n" + //
                                "); " + user.getUsername());
            System.out.println("==================================================\n");

            auditLogService.logActivity("USERNAME_RECOVERY", user.getUsername());

            return user.getUsername();
        } else {
            token.setAttempts(token.getAttempts() + 1);
            passwordResetTokenRepository.save(token);
            if (token.getAttempts() >= 3) {
                throw new IllegalArgumentException("OTP verification failed. Request a new OTP.");
            }
            throw new IllegalArgumentException("Invalid OTP. " + (3 - token.getAttempts()) + " attempts remaining.");
        }
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    public User updateProfile(String username, String email, String mobileNumber) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
                
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email address cannot be empty.");
        }
        if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Mobile number cannot be empty.");
        }
        
        Optional<User> existingEmailUser = userRepository.findByEmail(email);
        if (existingEmailUser.isPresent() && !existingEmailUser.get().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Email address is already in use by another account.");
        }
        
        user.setEmail(email.trim());
        user.setMobile(mobileNumber.trim());
        User savedUser = userRepository.save(user);
        
        auditLogService.logActivity("PROFILE_UPDATED", username);
        return savedUser;
    }
}

