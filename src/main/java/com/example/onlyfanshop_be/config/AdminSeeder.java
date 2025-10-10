package com.example.onlyfanshop_be.config;

import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.enums.AuthProvider;
import com.example.onlyfanshop_be.enums.Role;
import com.example.onlyfanshop_be.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class AdminSeeder implements CommandLineRunner {

    @Value("${ADMIN_EMAIL:}")
    private String ADMIN_EMAIL;
    @Value("${ADMIN_USERNAME:}")
    private String ADMIN_USERNAME;
    @Value("${ADMIN_PASSWORD:}")
    private String ADMIN_PASSWORD;
    @Value("${ADMIN_PHONE:}")
    private String ADMIN_PHONE;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        try {
            // Require admin env variables to be set to avoid leaking hardcoded secrets
            if (isBlank(ADMIN_EMAIL) || isBlank(ADMIN_USERNAME) || isBlank(ADMIN_PASSWORD)) {
                System.out.println("AdminSeeder: Missing ADMIN_* env variables. Skipping seeding.");
                return;
            }
            // Determine presence of any ADMIN and the configured email user
            List<User> allUsers = userRepository.findAll();
            boolean anyAdminExists = allUsers.stream().anyMatch(u -> u.getRole() == Role.ADMIN);
            Optional<User> existingByEmail = userRepository.findByEmail(ADMIN_EMAIL);

            if (existingByEmail.isPresent()) {
                User u = existingByEmail.get();
                if (u.getRole() == Role.ADMIN) {
                    System.out.println("AdminSeeder: ADMIN user already exists for email. Ensuring details are up-to-date.");
                    u.setUsername(ADMIN_USERNAME);
                    u.setPhoneNumber(ADMIN_PHONE);
                    if (u.getAuthProvider() != AuthProvider.LOCAL) u.setAuthProvider(AuthProvider.LOCAL);
                    if (u.getPasswordHash() == null || u.getPasswordHash().isBlank()) {
                        u.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
                    }
                    userRepository.save(u);
                    return;
                } else {
                    // Configured email exists but is not ADMIN
                    if (anyAdminExists) {
                        System.out.println("AdminSeeder: Another ADMIN exists; not upgrading configured email to maintain single ADMIN.");
                        return;
                    }
                    // No other ADMIN exists, upgrade configured email user to ADMIN
                    u.setRole(Role.ADMIN);
                    u.setAuthProvider(AuthProvider.LOCAL);
                    u.setUsername(ADMIN_USERNAME);
                    u.setPhoneNumber(ADMIN_PHONE);
                    u.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
                    userRepository.save(u);
                    System.out.println("AdminSeeder: Upgraded existing user to ADMIN: " + ADMIN_EMAIL);
                    return;
                }
            }

            // Configured email does not exist
            if (anyAdminExists) {
                System.out.println("AdminSeeder: Another ADMIN account already exists. Skipping creation to keep single ADMIN.");
                return;
            }

            // Create the ADMIN account as none exists
            User admin = new User();
            admin.setUsername(ADMIN_USERNAME);
            admin.setEmail(ADMIN_EMAIL);
            admin.setPhoneNumber(ADMIN_PHONE);
            admin.setRole(Role.ADMIN);
            admin.setAuthProvider(AuthProvider.LOCAL);
            admin.setPasswordHash(passwordEncoder.encode(ADMIN_PASSWORD));
            userRepository.save(admin);
            System.out.println("AdminSeeder: ADMIN user created successfully: " + ADMIN_EMAIL);
        } catch (Exception e) {
            System.err.println("AdminSeeder: Failed to seed ADMIN user - " + e.getMessage());
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}