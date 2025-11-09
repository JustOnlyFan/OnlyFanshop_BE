package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.UserDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.repository.UserRepository;
import com.example.onlyfanshop_be.service.GoogleAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth/google")
@CrossOrigin(origins = "*")
public class GoogleAuthController {

    @Autowired
    private GoogleAuthService googleAuthService;

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public ApiResponse<UserDTO> googleLogin(@RequestBody GoogleLoginRequest request) {
        System.out.println("Google login request received: " + request.getEmail() + " - " + request.getUsername());
        try {
            ApiResponse<UserDTO> response = googleAuthService.handleGoogleLogin(request.getEmail(), request.getUsername());
            System.out.println("Google login response: " + response);
            return response;
        } catch (Exception e) {
            System.err.println("Error in Google login: " + e.getMessage());
            e.printStackTrace();

            // Trả về lỗi thân thiện thay vì throw exception
            return ApiResponse.<UserDTO>builder()
                    .statusCode(500)
                    .message("Lỗi server: " + e.getMessage())
                    .build();
        }
    }

    @GetMapping("/test")
    public String testEndpoint() {
        return "Google Auth Controller is working!";
    }

    @GetMapping("/debug/users")
    public String debugUsers() {
        try {
            List<User> users = userRepository.findAll();
            System.out.println("Total users in database: " + users.size());
            for (User user : users) {
                String roleName = user.getRole() != null ? user.getRole().getName() : "N/A";
                System.out.println("User: " + user.getEmail() + " - " + user.getUsername() + " - Role: " + roleName);
            }
            return "Total users: " + users.size() + " - Check console for details";
        } catch (Exception e) {
            System.err.println("Error getting users: " + e.getMessage());
            e.printStackTrace();
            return "Error: " + e.getMessage();
        }
    }

    @GetMapping("/check-email")
    public ApiResponse<String> checkEmail(@RequestParam String email) {
        try {
            Optional<User> user = userRepository.findByEmail(email);
            if (user.isPresent()) {
                User existingUser = user.get();
                String roleName = existingUser.getRole() != null ? existingUser.getRole().getName() : "N/A";
                return ApiResponse.<String>builder()
                        .statusCode(200)
                        .message("Email đã tồn tại")
                        .data("Email: " + existingUser.getEmail() +
                              ", Username: " + existingUser.getUsername() +
                              ", Role: " + roleName)
                        .build();
            } else {
                return ApiResponse.<String>builder()
                        .statusCode(404)
                        .message("Email chưa tồn tại")
                        .data("Email có thể sử dụng")
                        .build();
            }
        } catch (Exception e) {
            return ApiResponse.<String>builder()
                    .statusCode(500)
                    .message("Lỗi server: " + e.getMessage())
                    .build();
        }
    }

    // Inner class for request
    public static class GoogleLoginRequest {
        private String email;
        private String username;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }
}
