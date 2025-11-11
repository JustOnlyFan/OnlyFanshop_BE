package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.UserDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IUserService {
    ApiResponse<UserDTO> getUserByID(int userID);
    ApiResponse<UserDTO> getUserByEmail(String email);
    ApiResponse<UserDTO> updateUser(UserDTO userDTO);
    void changePassword(int userID, String oldPassword, String newPassword);
    void changeAddress(int userID, String address);
    public void logout(String token);
    public ApiResponse<Page<UserDTO>> getAllUsers(
            String keyword,
            String role,
            int page,
            int size,
            String sortField,
            String sortDirection);
}
