package com.example.onlyfanshop_be.security;

import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional // ⚡ giữ session Hibernate mở để tránh LazyInitializationException
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + email));

        // Lấy tên role từ database (có thể là lowercase: "admin", "staff", "customer")
        String roleNameFromDB = user.getRole().getName();
        System.out.println("CustomUserDetailsService: Raw role name from DB: '" + roleNameFromDB + "'");
        
        // Normalize role name: 
        // 1. Remove ROLE_ prefix if exists
        // 2. Convert to UPPERCASE 
        // 3. Add ROLE_ prefix back
        // This ensures "admin" -> "ROLE_ADMIN", "ADMIN" -> "ROLE_ADMIN", "ROLE_admin" -> "ROLE_ADMIN"
        String normalizedRole = roleNameFromDB;
        if (normalizedRole.startsWith("ROLE_")) {
            normalizedRole = normalizedRole.substring(5); // Remove "ROLE_" prefix
        }
        normalizedRole = normalizedRole.toUpperCase().trim(); // Normalize to uppercase and trim
        normalizedRole = "ROLE_" + normalizedRole; // Add ROLE_ prefix back
        
        System.out.println("CustomUserDetailsService: Normalized role: '" + normalizedRole + "'");
        System.out.println("CustomUserDetailsService: Loading user " + user.getEmail() + " (ID: " + user.getId() + ") with role: " + normalizedRole);
        
        String safePassword = (user.getPasswordHash() == null || user.getPasswordHash().isEmpty())
                ? "N/A" : user.getPasswordHash();
        
        // Trả về đối tượng UserDetails cho Spring Security
        // Spring Security's hasAnyRole('ADMIN') will look for ROLE_ADMIN
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(normalizedRole);
        System.out.println("CustomUserDetailsService: Created authority: " + authority.getAuthority());
        
        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                safePassword,
                List.of(authority)
        );
    }
}
