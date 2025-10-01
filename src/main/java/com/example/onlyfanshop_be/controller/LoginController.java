package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.UserDTO;
import com.example.onlyfanshop_be.dto.request.GoogleLoginRequest;
import com.example.onlyfanshop_be.dto.request.LoginRequest;
import com.example.onlyfanshop_be.dto.request.RegisterRequest;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.service.LoginService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("login")
public class LoginController {
  
    @Autowired
    private LoginService loginService;

    @PostMapping("/signin")
    @Operation(summary = "Đăng nhập", description = "-Nguyễn Hoàng Thiên")
    public ApiResponse<UserDTO> login(@RequestBody LoginRequest loginRequest) {
        ApiResponse<UserDTO> reponse = loginService.login(loginRequest);
        return reponse;
    }

    @PostMapping("/register")
    public ApiResponse register(@RequestBody RegisterRequest request) {
        ApiResponse response = loginService.register(request);
        response.setMessage("Đăng ký thành công");
        return response;
    }

    @PostMapping("/google")
    @Operation(summary = "Đăng nhập bằng Google")
    public ApiResponse<UserDTO> loginWithGoogle(@RequestBody GoogleLoginRequest request) {
        ApiResponse<UserDTO> response = loginService.loginWithGoogle(request.getIdToken());
        return response;
    }

    @PostMapping("/send-otp")
    public ApiResponse sendOtp(@RequestParam String email) {
        ApiResponse response = new ApiResponse();
        String otp = loginService.generateOTP(email);
        loginService.sendOTP(email, otp);
        response.setMessage("OTP đã được gửi qua email: " + email);
        return response;
    }

    @PostMapping("/verify-otp")
    public ApiResponse verifyOtp(@RequestParam String email, @RequestParam String otp) {
        ApiResponse response = new ApiResponse();
        if (loginService.validateOTP(email, otp)) {
            response.setMessage("Xác thực thành công");
            return response;
        }
        response.setMessage("OTP không hợp lệ");
        return response;
    }


    @GetMapping("/check-account")
    public ResponseEntity<Map<String, Boolean>> checkAccount(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email) {

        Map<String, Boolean> result = new HashMap<>();

        if (username != null && !username.isEmpty()) {
            result.put("usernameExists", userRepository.existsByUsername(username));
        }

        if (email != null && !email.isEmpty()) {
            result.put("emailExists", userRepository.existsByEmail(email));
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/reset-password")
    public ApiResponse resetPassword(
            @RequestParam String email,
            @RequestParam String newPassword) {
            ApiResponse apiResponse = loginService.resetPassword(email, newPassword);
        return apiResponse;
    }

}