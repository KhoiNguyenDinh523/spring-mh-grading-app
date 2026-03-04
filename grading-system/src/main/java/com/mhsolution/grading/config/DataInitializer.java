package com.mhsolution.grading.config;

import com.mhsolution.grading.entity.Role;
import com.mhsolution.grading.entity.User;
import com.mhsolution.grading.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * DataInitializer — seeds the database with a default Admin account on startup.
 *
 * CommandLineRunner: Spring Boot runs this method after the application context loads.
 * This ensures there's always at least one admin account to access the system.
 *
 * IMPORTANT: Change the default admin password immediately after first login!
 */
@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner seedAdmin(UserRepository userRepository,
                                       PasswordEncoder passwordEncoder) {
        return args -> {
            // Only create admin if it doesn't already exist
            if (!userRepository.existsByUsername("admin")) {
                User admin = new User(
                        "admin",
                        passwordEncoder.encode("Admin@123456"),  // Change this!
                        "admin@mhsolution.com",
                        Role.ROLE_ADMIN
                );
                userRepository.save(admin);
                System.out.println(">>> Default admin account created: admin / Admin@123456");
                System.out.println(">>> IMPORTANT: Change the admin password after first login!");
            }
        };
    }
}
