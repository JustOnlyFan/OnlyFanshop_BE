package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.UserDTO;
import com.example.onlyfanshop_be.dto.request.LoginRequest;
import com.example.onlyfanshop_be.dto.request.RegisterRequest;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.service.ILoginService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("login")
public class LoginController {
    @Autowired
    private ILoginService loginService;
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



    @PostMapping("/send-otp")
    public ResponseEntity<String> sendOtp(@RequestParam String email) {
        String otp = loginService.generateOTP(email);
        loginService.sendOTP(email, otp);
        return ResponseEntity.ok("OTP đã gửi tới email: " + email);
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(@RequestParam String email, @RequestParam String otp) {
        if (loginService.validateOTP(email, otp)) {
            return ResponseEntity.ok("Xác thực thành công!");
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("OTP không hợp lệ!");
    }
}


