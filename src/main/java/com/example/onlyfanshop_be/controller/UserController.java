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
}
