package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.UserDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.repository.UserRepository;
import com.example.onlyfanshop_be.service.GoogleAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.time.Duration;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth/google")
@CrossOrigin(
        originPatterns = {
                "http://localhost:3000",
                "http://onlyfan.local:3000",
                "http://admin.onlyfan.local:3000",
                "http://staff.onlyfan.local:3000",
                "https://*.ngrok-free.dev",
                "https://*.ngrok-free.app"
        },
        allowCredentials = "true"
)
public class GoogleAuthController {

    @Autowired
    private GoogleAuthService googleAuthService;

    @Autowired
    private UserRepository userRepository;

    private static final String REFRESH_COOKIE_NAME = "refresh_token";

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserDTO>> googleLogin(
            @RequestBody GoogleLoginRequest request,
            HttpServletRequest httpServletRequest) {
        System.out.println("Google login request received: " + request.getEmail() + " - " + request.getUsername());
        try {
            ApiResponse<UserDTO> response = googleAuthService.handleGoogleLogin(request.getEmail(), request.getUsername());
            System.out.println("Google login response: " + response);
            if (response.getData() != null && response.getData().getRefreshToken() != null) {
                ResponseCookie refreshCookie = buildRefreshCookie(response.getData().getRefreshToken(), httpServletRequest);
                return ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                        .body(response);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error in Google login: " + e.getMessage());
            e.printStackTrace();

            // Trả về lỗi thân thiện thay vì throw exception
            return ResponseEntity.status(500).body(ApiResponse.<UserDTO>builder()
                    .statusCode(500)
                    .message("Lỗi server: " + e.getMessage())
                    .build());
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
                System.out.println("User: " + user.getEmail() + " - " + user.getFullname() + " - Role: " + roleName);
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
                              ", Fullname: " + existingUser.getFullname() +
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

    private ResponseCookie buildRefreshCookie(String refreshToken, HttpServletRequest request) {
        boolean secure = request.isSecure() ||
                "https".equalsIgnoreCase(request.getHeader("X-Forwarded-Proto"));
        return ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(googleAuthService.getRefreshTtlDays()))
                .build();
    }
}
