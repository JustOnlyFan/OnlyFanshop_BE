package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.request.LoginRequest;
import com.example.onlyfanshop_be.dto.request.RegisterRequest;
import com.example.onlyfanshop_be.dto.response.ApiResponse;

public interface ILoginService {
    public ApiResponse login(LoginRequest loginRequest);
    public ApiResponse register(RegisterRequest registerRequest);
}
