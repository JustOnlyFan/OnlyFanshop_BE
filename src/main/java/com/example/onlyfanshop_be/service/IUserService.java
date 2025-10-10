package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.UserDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;

public interface IUserService {
    ApiResponse<UserDTO> getUserByID(int userID);
    ApiResponse<UserDTO> getUserByEmail(String email);
    ApiResponse<UserDTO> updateUser(UserDTO userDTO);
    void changePassword(int userID, String oldPassword, String newPassword);
    void changeAddress(int userID, String address);
    public void logout(String token);
}
