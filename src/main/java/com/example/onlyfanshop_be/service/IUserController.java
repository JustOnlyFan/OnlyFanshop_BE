package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.UserDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;

public interface IUserController {
    ApiResponse<UserDTO> getUserByID(int userID);
    ApiResponse<UserDTO> getUserByEmail(String email);

}
