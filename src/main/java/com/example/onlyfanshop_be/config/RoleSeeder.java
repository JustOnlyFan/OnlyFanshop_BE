package com.example.onlyfanshop_be.config;

import com.example.onlyfanshop_be.entity.Role;
import com.example.onlyfanshop_be.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(1) // Run before AdminSeeder (which is Order 2)
public class RoleSeeder implements CommandLineRunner {

    @Autowired
    private RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        try {
            // Seed customer role (default role, id=1)
            if (!roleRepository.findByName("customer").isPresent()) {
                Role customerRole = Role.builder()
                        .name("customer")
                        .description("Khách hàng")
                        .build();
                roleRepository.save(customerRole);
                System.out.println("RoleSeeder: Created customer role");
            } else {
                System.out.println("RoleSeeder: Customer role already exists");
            }

            // Seed staff role
            if (!roleRepository.findByName("staff").isPresent()) {
                Role staffRole = Role.builder()
                        .name("staff")
                        .description("Nhân viên")
                        .build();
                roleRepository.save(staffRole);
                System.out.println("RoleSeeder: Created staff role");
            } else {
                System.out.println("RoleSeeder: Staff role already exists");
            }

            // Seed admin role
            if (!roleRepository.findByName("admin").isPresent()) {
                Role adminRole = Role.builder()
                        .name("admin")
                        .description("Quản trị viên")
                        .build();
                roleRepository.save(adminRole);
                System.out.println("RoleSeeder: Created admin role");
            } else {
                System.out.println("RoleSeeder: Admin role already exists");
            }

            System.out.println("RoleSeeder: Completed successfully");
        } catch (Exception e) {
            System.err.println("RoleSeeder: Failed to seed roles - " + e.getMessage());
            e.printStackTrace();
        }
    }
}


