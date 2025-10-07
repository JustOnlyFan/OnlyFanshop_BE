package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.UserDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.TokenRepository;
import com.example.onlyfanshop_be.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService implements IUserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TokenRepository tokenRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    @Override
    public ApiResponse<UserDTO> getUserByID(int userID) {
        Optional<User> userOtp = userRepository.findById(userID);
        if (userOtp.isPresent()) {
            User user = userOtp.get();
            return ApiResponse.<UserDTO>builder().message("Thành công").statusCode(200).data(UserDTO.builder()
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
            return ApiResponse.<UserDTO>builder().message("Thành công").statusCode(200).data(UserDTO.builder()
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
    public ApiResponse<UserDTO> updateUser(UserDTO userDTO) {
        Optional<User> userOtp = userRepository.findById(userDTO.getUserID());
        if (userOtp.isPresent()) {
            User user = userOtp.get();
            user.setAddress(userDTO.getAddress());
            user.setPhoneNumber(userDTO.getPhoneNumber());
            userRepository.save(user);
            return ApiResponse.<UserDTO>builder().message("Cập nhật thành công").statusCode(200).data(UserDTO.builder()
                    .userID(user.getUserID())
                    .email(user.getEmail())
                    .phoneNumber(user.getPhoneNumber())
                    .address(user.getAddress())
                    .username(user.getUsername())
                    .build()).build();
        }
        else throw new AppException(ErrorCode.USER_NOTEXISTED);
    }

    @Override
    public void changePassword(int userID, String oldPassword, String newPassword) {
        User user = userRepository.findById(userID)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy user!"));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new RuntimeException("Mật khẩu cũ không đúng!");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void logout(String token) {
        tokenRepository.deleteByToken(token);
    }
}
