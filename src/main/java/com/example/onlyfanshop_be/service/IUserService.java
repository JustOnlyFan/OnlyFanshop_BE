package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.UserDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;

public interface IUserService {
    ApiResponse<UserDTO> getUserByID(int userID);
    ApiResponse<UserDTO> getUserByEmail(String email);
    ApiResponse<UserDTO> updateUser(UserDTO userDTO);
//    ApiResponse<Void> changePassword(String pass);
public void logout(String token);
}
