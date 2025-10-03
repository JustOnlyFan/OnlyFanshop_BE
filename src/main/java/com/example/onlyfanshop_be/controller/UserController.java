package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.UserDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.service.IUserController;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {
    IUserController userController;
    @GetMapping("/getUserByID")
    public ApiResponse<UserDTO> getUserByID(@RequestParam int userID) {
        return userController.getUserByID(userID);
    }
    @GetMapping("/getUserByEmail")
    public ApiResponse<UserDTO> getUserByEmail(@RequestParam String  email) {
        return userController.getUserByEmail(email);
    }
}
