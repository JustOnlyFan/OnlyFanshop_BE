package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.UserDTO;
import com.example.onlyfanshop_be.dto.request.LoginRequest;
import com.example.onlyfanshop_be.dto.request.RegisterRequest;
import com.example.onlyfanshop_be.dto.request.RefreshTokenRequest;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.repository.UserRepository;
import com.example.onlyfanshop_be.security.JwtTokenProvider;
import com.example.onlyfanshop_be.service.ILoginService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("login")
public class LoginController {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public LoginController(AuthenticationManager authenticationManager, JwtTokenProvider tokenProvider) {
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }
    @Autowired
    private ILoginService loginService;
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/signin")
    @Operation(summary = "Đăng nhập", description = "-Nguyễn Hoàng Thiên")
    public ApiResponse<UserDTO> login(@RequestBody LoginRequest loginRequest) {
        return loginService.login(loginRequest);
    }

    @PostMapping("/register")
    public ApiResponse<UserDTO> register(@RequestBody RegisterRequest request) {
        return  loginService.register(request);
    }



    @PostMapping("/send-otp")
    public ApiResponse<Void> sendOtp(@RequestParam String email) {
        String otp = loginService.generateOTP(email);
        loginService.sendOTP(email, otp);
        return ApiResponse.<Void>builder().statusCode(200).message("OTP đã được gửi qua email: " + email).build();
    }

    @PostMapping("/verify-otp")
    public ApiResponse<Void> verifyOtp(@RequestParam String email, @RequestParam String otp) {
        if (loginService.validateOTP(email, otp)) {
            return ApiResponse.<Void>builder().message("Xác thực thành công").build();
        }
        return ApiResponse.<Void>builder().statusCode(200).message("OTP không hợp lệ").build();
    }


    @GetMapping("/check-account")
    public ResponseEntity<Map<String, Boolean>> checkAccount(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email) {

        Map<String, Boolean> result = new HashMap<>();

        // Check if username (fullName) exists
        if (username != null && !username.isEmpty()) {
            result.put("usernameAvailable", !userRepository.existsByFullName(username));
        }

        // Check if email exists
        if (email != null && !email.isEmpty()) {
            result.put("emailAvailable", !userRepository.existsByEmail(email));
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(
            @RequestParam String email,
            @RequestParam String newPassword) {
        return loginService.resetPassword(email, newPassword);
    }

    @PostMapping("/refresh")
    public ApiResponse<UserDTO> refresh(@RequestBody RefreshTokenRequest request) {
        return loginService.refreshToken(request.getRefreshToken());
    }

}

