package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.UserDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService implements  IUserController {
    @Autowired
    private UserRepository userRepository;
    @Override
    public ApiResponse<UserDTO> getUserByID(int userID) {
        Optional<User> userOtp = userRepository.findById(userID);
        if (userOtp.isPresent()) {
            User user = userOtp.get();
            return ApiResponse.<UserDTO>builder().data(UserDTO.builder()
                    .userID(user.getUserID())
                    .email(user.getEmail())
                    .phoneNumber(user.getPhoneNumber())
                    .address(user.getAddress())
                    .username(user.getUsername())
                    .role(user.getRole())
                    .authProvider(user.getAuthProvider())
                    .build()).build();
        }else throw new AppException(ErrorCode.USER_NOTEXISTED);
    }

    @Override
    public ApiResponse<UserDTO> getUserByEmail(String email) {
        Optional<User> userOtp = userRepository.findByEmail(email);
        if (userOtp.isPresent()) {
            User user = userOtp.get();
            return ApiResponse.<UserDTO>builder().data(UserDTO.builder()
                    .userID(user.getUserID())
                    .email(user.getEmail())
                    .phoneNumber(user.getPhoneNumber())
                    .address(user.getAddress())
                    .username(user.getUsername())
                    .role(user.getRole())
                    .authProvider(user.getAuthProvider())
                    .build()).build();
        }else throw new AppException(ErrorCode.USER_NOTEXISTED);
    }

}