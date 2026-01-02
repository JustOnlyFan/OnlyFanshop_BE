package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.UserDTO;
import com.example.onlyfanshop_be.dto.request.LoginRequest;
import com.example.onlyfanshop_be.dto.request.RegisterRequest;
import com.example.onlyfanshop_be.dto.response.ApiResponse;

public interface ILoginService {
    ApiResponse<UserDTO> login(LoginRequest loginRequest);
    ApiResponse<UserDTO> register(RegisterRequest request);
    String generateOTP(String email);
    boolean validateOTP(String email, String otp);
    void sendOTP(String to, String otp);
    ApiResponse<Void> resetPassword(String email, String newPassword);
    ApiResponse<UserDTO> refreshToken(String refreshToken);
    ApiResponse<Void> logout(String refreshToken);
}
