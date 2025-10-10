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

    @PutMapping("/changeAddress")
    public ApiResponse<Void> changeAddress(@RequestParam String address, HttpServletRequest httpRequest) {
        String token = jwtTokenProvider.extractToken(httpRequest);
        int userid = jwtTokenProvider.getUserIdFromJWT(token);
        userService.changeAddress(userid, address);
        return ApiResponse.<Void>builder().statusCode(200).message("Cập nhật địa chỉ thành công").build();
    }
}
