package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.UserDTO;
import com.example.onlyfanshop_be.dto.request.LoginRequest;
import com.example.onlyfanshop_be.dto.request.RegisterRequest;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.service.ILoginService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

}
