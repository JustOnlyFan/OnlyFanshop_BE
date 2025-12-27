package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.UserDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.Role;
import com.example.onlyfanshop_be.entity.Token;
import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.enums.TokenType;
import com.example.onlyfanshop_be.enums.UserStatus;
import com.example.onlyfanshop_be.repository.RoleRepository;
import com.example.onlyfanshop_be.repository.TokenRepository;
import com.example.onlyfanshop_be.repository.UserRepository;
import com.example.onlyfanshop_be.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class GoogleAuthService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TokenRepository tokenRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    public ApiResponse<UserDTO> handleGoogleLogin(String email, String username) {
        System.out.println("GoogleAuthService: Processing login for email: " + email + ", username: " + username);

        Optional<User> existingUser = userRepository.findByEmail(email);
        System.out.println("GoogleAuthService: Existing user found: " + existingUser.isPresent());

        if (existingUser.isPresent()) {
            User user = existingUser.get();

            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            List<Token> validTokens = tokenRepository.findAllByUserIdAndExpiredFalseAndRevokedFalse(user.getId());
            validTokens.forEach(t -> { t.setExpired(true); t.setRevoked(true); });
            tokenRepository.saveAll(validTokens);

            Role roleEntity = null;
            if (user.getRoleId() != null) {
                roleEntity = roleRepository.findById(user.getRoleId()).orElse(null);
            }

            String access = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getId(), roleEntity, user.getFullname());
            String refresh = jwtTokenProvider.generateRefreshToken(user.getEmail(), user.getId(), roleEntity);
            
            tokenRepository.save(Token.builder()
                    .userId(user.getId())
                    .token(access)
                    .expired(false)
                    .revoked(false)
                    .type(TokenType.ACCESS)
                    .expiresAt(Instant.now().plusSeconds(60L*30L))
                    .build());
            tokenRepository.save(Token.builder()
                    .userId(user.getId())
                    .token(refresh)
                    .expired(false)
                    .revoked(false)
                    .type(TokenType.REFRESH)
                    .expiresAt(Instant.now().plusSeconds(60L*60L*24L*7L))
                    .build());

            UserDTO userDTO = UserDTO.builder()
                    .userID(user.getId())
                    .fullName(user.getFullname())
                    .email(user.getEmail())
                    .phoneNumber(user.getPhone())
                    .phone(user.getPhone())
                    .status(user.getStatus())
                    .token(access)
                    .refreshToken(refresh)
                    .build();
            
            if (roleEntity != null) {
                userDTO.setRole(roleEntity);
                userDTO.setRoleName(roleEntity.getName());
            }

            ApiResponse<UserDTO> response = new ApiResponse<>();
            response.setStatusCode(200);
            response.setMessage("Đăng nhập Google thành công");
            response.setData(userDTO);
            return response;
        } else {
            System.out.println("GoogleAuthService: Creating new user");

            Role customerRole = roleRepository.findByName("customer")
                    .orElse(roleRepository.findById((byte) 1)
                            .orElseThrow(() -> new RuntimeException("Customer role not found")));

            String fullName = (username != null ? username : email).trim();
            
            User newUser = User.builder()
                    .email(email)
                    .fullname(fullName)
                    .roleId(customerRole.getId())
                    .status(UserStatus.active)
                    .createdAt(LocalDateTime.now())
                    .lastLoginAt(LocalDateTime.now())
                    .passwordHash("")
                    .build();

            System.out.println("GoogleAuthService: Saving new user to database");
            User savedUser = userRepository.save(newUser);
            System.out.println("GoogleAuthService: User saved with ID: " + savedUser.getId());

            String access = jwtTokenProvider.generateAccessToken(savedUser.getEmail(), savedUser.getId(), customerRole, savedUser.getFullname());
            String refresh = jwtTokenProvider.generateRefreshToken(savedUser.getEmail(), savedUser.getId(), customerRole);
            
            tokenRepository.save(Token.builder()
                    .userId(savedUser.getId())
                    .token(access)
                    .expired(false)
                    .revoked(false)
                    .type(TokenType.ACCESS)
                    .expiresAt(Instant.now().plusSeconds(60L*30L))
                    .build());
            tokenRepository.save(Token.builder()
                    .userId(savedUser.getId())
                    .token(refresh)
                    .expired(false)
                    .revoked(false)
                    .type(TokenType.REFRESH)
                    .expiresAt(Instant.now().plusSeconds(60L*60L*24L*7L))
                    .build());

            UserDTO userDTO = UserDTO.builder()
                    .userID(savedUser.getId())
                    .fullName(savedUser.getFullname())
                    .email(savedUser.getEmail())
                    .phoneNumber(savedUser.getPhone())
                    .phone(savedUser.getPhone())
                    .status(savedUser.getStatus())
                    .token(access)
                    .refreshToken(refresh)
                    .build();
            
            userDTO.setRole(customerRole);
            userDTO.setRoleName(customerRole.getName());

            ApiResponse<UserDTO> response = new ApiResponse<>();
            response.setStatusCode(200);
            response.setMessage("Đăng ký và đăng nhập Google thành công");
            response.setData(userDTO);
            return response;
        }
    }
}
