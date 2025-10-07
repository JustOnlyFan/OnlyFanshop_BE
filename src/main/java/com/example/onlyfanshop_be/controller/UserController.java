package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.UserDTO;
import com.example.onlyfanshop_be.dto.request.ChangePasswordRequest;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.security.JwtTokenProvider;
import com.example.onlyfanshop_be.service.IUserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private IUserService userService;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @GetMapping("/getUser")
    public ApiResponse<UserDTO> getUser(HttpServletRequest request) {
        String token = extractToken(request);
        int userid = jwtTokenProvider.getUserIdFromJWT(token);
        return userService.getUserByID(userid);
    }

    @PutMapping("/updateUser")
    public ApiResponse<UserDTO> updateUser(@RequestBody UserDTO userDTO, HttpServletRequest request) {
        String token = extractToken(request);
        int userId = jwtTokenProvider.getUserIdFromJWT(token);
        userDTO.setUserID(userId);
        return userService.updateUser(userDTO);
    }

    @PutMapping("/changePassword")
    public ApiResponse<Void> changePassword(@RequestBody ChangePasswordRequest request, HttpServletRequest httpRequest) {
        String token = extractToken(httpRequest);
        int userid = jwtTokenProvider.getUserIdFromJWT(token);
        userService.changePassword(userid, request.getOldPassword(), request.getNewPassword());
        return ApiResponse.<Void>builder().message("Đổi mật khẩu thành công!").build();
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String token = extractToken(request);
        userService.logout(token);
        return ApiResponse.<Void>builder().message("Đăng xuất thành công!").build();
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        throw new RuntimeException("Token không hợp lệ hoặc không được cung cấp!");
    }
}
