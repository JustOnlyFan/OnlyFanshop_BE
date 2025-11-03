package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.UserDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.Token;
import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.enums.AuthProvider;
import com.example.onlyfanshop_be.enums.Role;
import com.example.onlyfanshop_be.enums.TokenType;
import com.example.onlyfanshop_be.repository.TokenRepository;
import com.example.onlyfanshop_be.repository.UserRepository;
import com.example.onlyfanshop_be.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.time.Instant;

@Service
public class GoogleAuthService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TokenRepository tokenRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    public ApiResponse<UserDTO> handleGoogleLogin(String email, String username) {
        System.out.println("GoogleAuthService: Processing login for email: " + email + ", username: " + username);

        // Kiểm tra xem user đã tồn tại chưa
        Optional<User> existingUser = userRepository.findByEmail(email);
        System.out.println("GoogleAuthService: Existing user found: " + existingUser.isPresent());

        if (existingUser.isPresent()) {
            // User đã tồn tại, cập nhật thông tin nếu cần
            User user = existingUser.get();
            if (user.getAuthProvider() != AuthProvider.GOOGLE) {
                // Nếu user đã tồn tại với provider khác, trả về lỗi thân thiện
                System.out.println("GoogleAuthService: Email conflict - existing provider: " + user.getAuthProvider());
                ApiResponse<UserDTO> response = new ApiResponse<>();
                response.setStatusCode(400);
                response.setMessage("Email đã tồn tại với phương thức đăng nhập khác. Vui lòng sử dụng phương thức đăng nhập ban đầu.");
                return response;
            }
            // Revoke các token cũ
            tokenRepository.findAllByUser_UserIDAndExpiredFalseAndRevokedFalse(user.getUserID())
                    .forEach(t -> { t.setExpired(true); t.setRevoked(true); });
            // Tạo Access/Refresh token mới
            String access = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getUserID(), user.getRole(), user.getUsername());
            String refresh = jwtTokenProvider.generateRefreshToken(user.getEmail(), user.getUserID(), user.getRole());
            tokenRepository.save(Token.builder()
                    .user(user)
                    .token(access)
                    .expired(false)
                    .revoked(false)
                    .type(TokenType.ACCESS)
                    .expiresAt(Instant.now().plusSeconds(60L*30L))
                    .build());
            tokenRepository.save(Token.builder()
                    .user(user)
                    .token(refresh)
                    .expired(false)
                    .revoked(false)
                    .type(TokenType.REFRESH)
                    .expiresAt(Instant.now().plusSeconds(60L*60L*24L*7L))
                    .build());

            ApiResponse<UserDTO> response = new ApiResponse<>();
            response.setStatusCode(200);
            response.setMessage("Đăng nhập Google thành công");
            UserDTO userDTO = new UserDTO();
            userDTO.setUserID(user.getUserID());
            userDTO.setUsername(user.getUsername());
            userDTO.setEmail(user.getEmail());
            userDTO.setPhoneNumber(user.getPhoneNumber());
            userDTO.setAddress(user.getAddress());
            userDTO.setRole(user.getRole());
            userDTO.setAuthProvider(user.getAuthProvider());
            userDTO.setToken(access);
            userDTO.setRefreshToken(refresh);
            response.setData(userDTO);
            return response;
        } else {
            // Tạo user mới
            System.out.println("GoogleAuthService: Creating new user");
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setUsername(username);
            newUser.setRole(Role.CUSTOMER);
            newUser.setAuthProvider(AuthProvider.GOOGLE);
            // Không cần password cho Google login

            System.out.println("GoogleAuthService: Saving new user to database");
            User savedUser = userRepository.save(newUser);
            System.out.println("GoogleAuthService: User saved with ID: " + savedUser.getUserID());

            // Tạo Access/Refresh token cho user mới
            String access = jwtTokenProvider.generateAccessToken(savedUser.getEmail(), savedUser.getUserID(), savedUser.getRole(), savedUser.getUsername());
            String refresh = jwtTokenProvider.generateRefreshToken(savedUser.getEmail(), savedUser.getUserID(), savedUser.getRole());
            tokenRepository.save(Token.builder()
                    .user(savedUser)
                    .token(access)
                    .expired(false)
                    .revoked(false)
                    .type(TokenType.ACCESS)
                    .expiresAt(Instant.now().plusSeconds(60L*30L))
                    .build());
            tokenRepository.save(Token.builder()
                    .user(savedUser)
                    .token(refresh)
                    .expired(false)
                    .revoked(false)
                    .type(TokenType.REFRESH)
                    .expiresAt(Instant.now().plusSeconds(60L*60L*24L*7L))
                    .build());

            ApiResponse<UserDTO> response = new ApiResponse<>();
            response.setStatusCode(200);
            response.setMessage("Đăng ký và đăng nhập Google thành công");
            UserDTO userDTO = new UserDTO();
            userDTO.setUserID(newUser.getUserID());
            userDTO.setUsername(newUser.getUsername());
            userDTO.setEmail(newUser.getEmail());
            userDTO.setPhoneNumber(newUser.getPhoneNumber());
            userDTO.setAddress(newUser.getAddress());
            userDTO.setRole(newUser.getRole());
            userDTO.setAuthProvider(newUser.getAuthProvider());
            userDTO.setToken(access);
            userDTO.setRefreshToken(refresh);
            response.setData(userDTO);
            return response;
        }
    }
}
