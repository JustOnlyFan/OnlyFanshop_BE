package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.UserDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.enums.Role;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.TokenRepository;
import com.example.onlyfanshop_be.repository.UserRepository;
import com.example.onlyfanshop_be.entity.Token;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
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
                .orElseThrow(() -> new AppException(ErrorCode.USER_EXISTED));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new AppException(ErrorCode.CART_NOTFOUND);
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Revoke all tokens after password change
        List<Token> tokens = tokenRepository.findAllByUser_UserIDAndExpiredFalseAndRevokedFalse(userID);
        tokens.forEach(t -> { t.setExpired(true); t.setRevoked(true); });
        tokenRepository.saveAll(tokens);
    }

    @Override
    public void changeAddress(int userID, String address) {
        User user = userRepository.findById(userID)
                .orElseThrow(() -> new AppException(ErrorCode.USER_EXISTED));
        user.setAddress(address);
        userRepository.save(user);
    }

    @Override
    public void updateFCMToken(int userID, String fcmToken) {
        User user = userRepository.findById(userID)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOTEXISTED));
        user.setFcmToken(fcmToken);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void logout(String token) {
        tokenRepository.deleteByToken(token);
    }

    @Override
    public ApiResponse<Page<UserDTO>> getAllUsers(
            String keyword, String role, int page, int size,
            String sortField, String sortDirection) {

        Sort sort = sortDirection.equalsIgnoreCase("DESC")
                ? Sort.by(sortField).descending()
                : Sort.by(sortField).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Role enumRole = null;
        if (role != null && !role.isBlank()) {
            try {
                enumRole = Role.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid role: " + role);
            }
        }

        Page<User> userPage = userRepository.searchUsers(keyword, enumRole, pageable);

        Page<UserDTO> dtoPage = userPage.map(user -> {
            UserDTO dto = new UserDTO();
            dto.setUserID(user.getUserID());
            dto.setUsername(user.getUsername());
            dto.setEmail(user.getEmail());
            dto.setPhoneNumber(user.getPhoneNumber());
            dto.setAddress(user.getAddress());
            dto.setRole(user.getRole());
            return dto;
        });

        return ApiResponse.<Page<UserDTO>>builder()
                .data(dtoPage)
                .statusCode(200)
                .message("Lấy danh sách người dùng thành công")
                .build();
    }

}
