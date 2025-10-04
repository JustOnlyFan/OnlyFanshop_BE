package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.UserDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.service.IUserController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {
    @Autowired
    private IUserController userService;
    @GetMapping("/getUserByID")
    public ApiResponse<UserDTO> getUserByID(@RequestParam int userID) {
        return userService.getUserByID(userID);
    }

    @GetMapping("/getUserByEmail")
    public ApiResponse<UserDTO> getUserByEmail(@RequestParam String  email) {
        return userService.getUserByEmail(email);
    }

    @PostMapping("/updateUser")
    public ApiResponse<UserDTO> updateUser(@RequestBody UserDTO userDTO) {
        return userController.updateUser(userDTO);
    }

    //    @PutMapping("/users/{id}/password")
//    public ApiResponse<Void> changePassword(
//            @PathVariable("id") int userID,
//            @RequestBody ChangePasswordRequest request,
//            @AuthenticationPrincipal CustomUserDetails userDetails) {
//        // check userID == userDetails.getId() để tránh đổi mật khẩu cho người khác
//        userService.changePassword(userID, request.getOldPassword(), request.getNewPassword());
//        return ApiResponse.success();
//    }
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            userController.logout(token);
            return ApiResponse.<Void>builder().message("Đăng xuất thành công!").build();
        } else {
            return ApiResponse.<Void>builder().message("Token không hợp lệ!").build();
        }
    }
}
