package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.request.LoginRequest;
import com.example.onlyfanshop_be.dto.request.RegisterRequest;
import com.example.onlyfanshop_be.dto.response.ApiResponse;

public interface ILoginService {
    public ApiResponse login(LoginRequest loginRequest);
    public ApiResponse register(RegisterRequest registerRequest);
    public String generateOTP(String email);
    public boolean validateOTP(String email, String otp);
    public void sendOTP(String to, String otp);
    public ApiResponse resetPassword(String email, String newPassword);
}
