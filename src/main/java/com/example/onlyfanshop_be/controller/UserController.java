package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.UserDTO;
import com.example.onlyfanshop_be.dto.request.ChangePasswordRequest;
import com.example.onlyfanshop_be.dto.request.UpdateFCMTokenRequest;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.Token;
import com.example.onlyfanshop_be.repository.TokenRepository;
import com.example.onlyfanshop_be.security.JwtTokenProvider;
import com.example.onlyfanshop_be.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/users")
@Tag(name = "User Controller", description = "APIs for user management")
@Slf4j
public class UserController {

    @Autowired
    private IUserService userService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Autowired
    private TokenRepository tokenRepository;

    @GetMapping("/getUser")
    public ApiResponse<UserDTO> getUser(HttpServletRequest request) {
        String token = jwtTokenProvider.extractToken(request);
        int userid = jwtTokenProvider.getUserIdFromJWT(token);
        return userService.getUserByID(userid);
    }

    @PutMapping("/updateUser")
    public ApiResponse<UserDTO> updateUser(@RequestBody UserDTO userDTO, HttpServletRequest request) {
        String token = jwtTokenProvider.extractToken(request);
        int userId = jwtTokenProvider.getUserIdFromJWT(token);
        userDTO.setUserID(userId);
        return userService.updateUser(userDTO);
    }

    @PutMapping("/changePassword")
    public ApiResponse<Void> changePassword(@RequestBody ChangePasswordRequest request, HttpServletRequest httpRequest) {
        String token = jwtTokenProvider.extractToken(httpRequest);
        int userid = jwtTokenProvider.getUserIdFromJWT(token);
        userService.changePassword(userid, request.getOldPassword(), request.getNewPassword());
        return ApiResponse.<Void>builder().statusCode(200).message("Đổi mật khẩu thành công!").build();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String token = jwtTokenProvider.extractToken(request);
        userService.logout(token);
        return ApiResponse.<Void>builder().statusCode(200).message("Đăng xuất thành công!").build();
    }

    @PutMapping("/fcm-token")
    @Operation(summary = "Update FCM token", description = "Update FCM token for push notifications")
    public ApiResponse<Void> updateFCMToken(@RequestBody UpdateFCMTokenRequest request, HttpServletRequest httpRequest) {
        String token = jwtTokenProvider.extractToken(httpRequest);
        int userId = jwtTokenProvider.getUserIdFromJWT(token);
        userService.updateFCMToken(userId, request.getFcmToken());
        return ApiResponse.<Void>builder().statusCode(200).message("FCM token updated successfully!").build();
    }

    @PutMapping("/changeAddress")
    public ApiResponse<Void> changeAddress(@RequestParam String address, HttpServletRequest httpRequest) {
        String token = jwtTokenProvider.extractToken(httpRequest);
        int userid = jwtTokenProvider.getUserIdFromJWT(token);
        userService.changeAddress(userid, address);
        return ApiResponse.<Void>builder().statusCode(200).message("Cập nhật địa chỉ thành công").build();
    }
    @GetMapping("/getAllUsers")
    public ApiResponse<Page<UserDTO>> getAllUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "username") String sortField,
            @RequestParam(defaultValue = "ASC") String sortDirection) {

        return userService.getAllUsers(keyword, role, page, size, sortField, sortDirection);
    }


    
    @GetMapping("/token-status")
    @Operation(summary = "Check token status", description = "Debug endpoint to check if token is valid and in database")
    public ApiResponse<Map<String, Object>> checkTokenStatus(HttpServletRequest request) {
        Map<String, Object> status = new HashMap<>();
        
        try {
            String bearerToken = request.getHeader("Authorization");
            
            if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
                status.put("error", "No Bearer token found in Authorization header");
                return ApiResponse.<Map<String, Object>>builder()
                        .statusCode(400)
                        .message("No token provided")
                        .data(status)
                        .build();
            }
            
            String token = bearerToken.substring(7);
            status.put("tokenLength", token.length());
            
            // Check JWT validity
            boolean isJwtValid = jwtTokenProvider.validateToken(token);
            status.put("jwtValid", isJwtValid);
            
            if (isJwtValid) {
                // Extract claims
                try {
                    String email = jwtTokenProvider.getEmailFromJWT(token);
                    Integer userId = jwtTokenProvider.getUserIdFromJWT(token);
                    String role = jwtTokenProvider.getRoleFromJWT(token);
                    
                    status.put("email", email);
                    status.put("userId", userId);
                    status.put("role", role);
                } catch (Exception e) {
                    status.put("claimError", e.getMessage());
                }
            }
            
            // Check database
            Token dbToken = tokenRepository.findByToken(token).orElse(null);
            status.put("inDatabase", dbToken != null);
            
            if (dbToken != null) {
                status.put("revoked", dbToken.isRevoked());
                status.put("expired", dbToken.isExpired());
                status.put("userId", dbToken.getUser().getUserID());
                status.put("username", dbToken.getUser().getUsername());
            } else {
                status.put("dbError", "Token not found in database. User needs to re-login.");
            }
            
            return ApiResponse.<Map<String, Object>>builder()
                    .statusCode(200)
                    .message("Token status check completed")
                    .data(status)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error checking token status: {}", e.getMessage());
            status.put("error", e.getMessage());
            return ApiResponse.<Map<String, Object>>builder()
                    .statusCode(500)
                    .message("Error checking token status")
                    .data(status)
                    .build();
        }
    }
}
