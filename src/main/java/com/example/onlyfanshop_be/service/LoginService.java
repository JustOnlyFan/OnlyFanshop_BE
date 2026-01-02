package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.UserDTO;
import com.example.onlyfanshop_be.dto.request.LoginRequest;
import com.example.onlyfanshop_be.dto.request.RegisterRequest;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.Role;
import com.example.onlyfanshop_be.entity.Token;
import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.entity.UserAddress;
import com.example.onlyfanshop_be.enums.TokenType;
import com.example.onlyfanshop_be.enums.UserStatus;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.RoleRepository;
import com.example.onlyfanshop_be.repository.TokenRepository;
import com.example.onlyfanshop_be.repository.UserAddressRepository;
import com.example.onlyfanshop_be.repository.UserRepository;
import com.example.onlyfanshop_be.security.JwtTokenProvider;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class LoginService implements ILoginService{
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserAddressRepository userAddressRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JavaMailSender mailSender;

    @Autowired
    public LoginService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Autowired
    private TokenRepository tokenRepository;
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    @Override
    public ApiResponse<UserDTO> login(LoginRequest loginRequest) {
        if (loginRequest.getEmail() == null || loginRequest.getEmail().trim().isEmpty()) {
            throw new AppException(ErrorCode.USER_NOTEXISTED);
        }
        if (loginRequest.getPassword() == null || loginRequest.getPassword().isEmpty()) {
            throw new AppException(ErrorCode.WRONGPASS);
        }

        Optional<User> userOpt = userRepository.findByEmail(loginRequest.getEmail().trim().toLowerCase());
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            if (passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {

                user.setLastLoginAt(LocalDateTime.now());
                userRepository.save(user);

                List<Token> validUserTokens = tokenRepository.findAllByUserIdAndExpiredFalseAndRevokedFalse(user.getId());
                validUserTokens.forEach(t -> {
                    t.setExpired(true);
                    t.setRevoked(true);
                });
                tokenRepository.saveAll(validUserTokens);

                Role roleEntity = null;
                if (user.getRoleId() != null) {
                    roleEntity = roleRepository.findById(user.getRoleId()).orElse(null);
                }

                String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getId(), roleEntity, user.getFullname());
                String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail(), user.getId(), roleEntity);

                Token tokenEntity = Token.builder()
                        .userId(user.getId())
                        .token(accessToken)
                        .expired(false)
                        .revoked(false)
                        .type(TokenType.ACCESS)
                        .expiresAt(Instant.now().plusSeconds(60L*jwtAccessTtlMinutes()))
                        .build();
                tokenRepository.save(tokenEntity);

                Token refreshEntity = Token.builder()
                        .userId(user.getId())
                        .token(refreshToken)
                        .expired(false)
                        .revoked(false)
                        .type(TokenType.REFRESH)
                        .expiresAt(Instant.now().plusSeconds(60L*60L*24L*jwtRefreshTtlDays()))
                        .build();
                tokenRepository.save(refreshEntity);

                String roleName = "customer"; // Default role
                if (roleEntity != null) {
                    roleName = roleEntity.getName();
                }
                
                UserDTO userDTO = UserDTO.builder()
                        .userID(user.getId())
                        .fullName(user.getFullname())
                        .email(user.getEmail())
                        .phoneNumber(user.getPhone())
                        .phone(user.getPhone())
                        .status(user.getStatus())
                        .role(null)
                        .roleName(roleName)
                        .token(accessToken)
                        .refreshToken(refreshToken)
                        .build();

                return ApiResponse.<UserDTO>builder().data(userDTO).message("Đăng nhập thành công").statusCode(200).build();
            } else throw new AppException(ErrorCode.WRONGPASS);

        } else throw new AppException(ErrorCode.USER_NOTEXISTED);
    }

    @Override
    public ApiResponse<UserDTO> refreshToken(String refreshToken) {
        var tokenOpt = tokenRepository.findByToken(refreshToken);
        if (tokenOpt.isEmpty()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        var dbToken = tokenOpt.get();
        if (dbToken.isRevoked() || dbToken.isExpired()) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if (dbToken.getType() == null || dbToken.getType() != TokenType.REFRESH) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findById(dbToken.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOTEXISTED));

        List<Token> accessTokens = tokenRepository.findAllByUserIdAndTypeAndExpiredFalseAndRevokedFalse(user.getId(), TokenType.ACCESS);
        accessTokens.forEach(t -> { t.setExpired(true); t.setRevoked(true); });
        tokenRepository.saveAll(accessTokens);

        Role roleEntity = null;
        if (user.getRoleId() != null) {
            roleEntity = roleRepository.findById(user.getRoleId()).orElse(null);
        }

        String newAccess = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getId(), roleEntity, user.getFullname());
        Token accessEntity = Token.builder()
                .userId(user.getId())
                .token(newAccess)
                .expired(false)
                .revoked(false)
                .type(TokenType.ACCESS)
                .expiresAt(Instant.now().plusSeconds(60L*jwtAccessTtlMinutes()))
                .build();
        tokenRepository.save(accessEntity);

        String roleName = "customer"; // Default role
        if (roleEntity != null) {
            roleName = roleEntity.getName();
        }
        
        UserDTO userDTO = UserDTO.builder()
                .userID(user.getId())
                .fullName(user.getFullname())
                .email(user.getEmail())
                .phoneNumber(user.getPhone())
                .phone(user.getPhone())
                .status(user.getStatus())
                .role(null) // Explicitly set to null to avoid Hibernate proxy issues
                .roleName(roleName)
                .token(newAccess)
                .refreshToken(refreshToken)
                .build();

        return ApiResponse.<UserDTO>builder()
                .statusCode(200)
                .message("Refresh thành công")
                .data(userDTO)
                .build();
    }

    @Override
    public ApiResponse<Void> logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return ApiResponse.<Void>builder().statusCode(200).message("Đã đăng xuất").build();
        }
        try {
            if (jwtTokenProvider.validateToken(refreshToken)) {
                Long userId = jwtTokenProvider.getUserIdFromJWT(refreshToken);
                List<Token> tokens = tokenRepository.findAllByUserIdAndExpiredFalseAndRevokedFalse(userId);
                tokens.forEach(t -> {
                    t.setExpired(true);
                    t.setRevoked(true);
                });
                tokenRepository.saveAll(tokens);
            }
            tokenRepository.findByToken(refreshToken).ifPresent(t -> {
                t.setExpired(true);
                t.setRevoked(true);
                tokenRepository.save(t);
            });
        } catch (Exception e) {
            System.err.println("Logout: failed to revoke tokens: " + e.getMessage());
        }

        return ApiResponse.<Void>builder()
                .statusCode(200)
                .message("Đã đăng xuất")
                .build();
    }

    private long jwtAccessTtlMinutes() {
        try {
            var field = jwtTokenProvider.getClass().getDeclaredField("accessTtlMinutes");
            field.setAccessible(true);
            return (long) field.get(jwtTokenProvider);
        } catch (Exception e) { return 30L; }
    }

    private long jwtRefreshTtlDays() {
        try {
            var field = jwtTokenProvider.getClass().getDeclaredField("refreshTtlDays");
            field.setAccessible(true);
            return (long) field.get(jwtTokenProvider);
        } catch (Exception e) { return 7L; }
    }

    private final Map<String, OTPDetails> otpStorage = new HashMap<>();
    @Override
    public ApiResponse<UserDTO> register(RegisterRequest registerRequest) {
        try {
            System.out.println("=== REGISTER REQUEST ===");
            System.out.println("Email: " + registerRequest.getEmail());
            System.out.println("FullName: " + registerRequest.getFullName());
            System.out.println("Phone: " + registerRequest.getPhoneNumber());
            System.out.println("Has Password: " + (registerRequest.getPassword() != null && !registerRequest.getPassword().isEmpty()));

            if (registerRequest.getEmail() == null || registerRequest.getEmail().trim().isEmpty()) {
                throw new AppException(ErrorCode.USER_NOTEXISTED);
            }

            if (registerRequest.getFullName() == null || registerRequest.getFullName().trim().isEmpty()) {
                throw new AppException(ErrorCode.USER_NOTEXISTED);
            }

            String normalizedEmail = registerRequest.getEmail().trim().toLowerCase();
            if (userRepository.findByEmail(normalizedEmail).isPresent()) {
                System.out.println("ERROR: Email already exists: " + normalizedEmail);
                throw new AppException(ErrorCode.EMAIL_USED);
            }

            Role customerRole = roleRepository.findByName("customer")
                    .orElse(roleRepository.findById((byte) 1)
                            .orElseThrow(() -> new AppException(ErrorCode.USER_NOTEXISTED)));

            System.out.println("Customer role found: " + customerRole.getName() + " (ID: " + customerRole.getId() + ")");

            String fullName = registerRequest.getFullName().trim();

            User user = User.builder()
                    .fullname(fullName)
                    .email(normalizedEmail)
                    .phone(registerRequest.getPhoneNumber())
                    .roleId(customerRole.getId())
                    .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                    .status(UserStatus.active)
                    .createdAt(LocalDateTime.now())
                    .build();

            System.out.println("Saving user to database...");
            user = userRepository.save(user);
            System.out.println("User saved successfully with ID: " + user.getId());

            UserDTO userDTO = UserDTO.builder()
                    .userID(user.getId())
                    .fullName(user.getFullname())
                    .email(user.getEmail())
                    .phoneNumber(user.getPhone())
                    .phone(user.getPhone())
                    .status(user.getStatus())
                    .role(null) // Explicitly set to null to avoid Hibernate proxy issues
                    .roleName(customerRole != null ? customerRole.getName() : "customer")
                    .build();

            System.out.println("Registration successful for user: " + user.getEmail());
            return ApiResponse.<UserDTO>builder()
                    .statusCode(200)
                    .message("Đăng ký thành công, hãy đăng nhập")
                    .data(userDTO)
                    .build();
        } catch (AppException e) {
            System.out.println("AppException during registration: " + e.getMessage());
            throw e;
        } catch (Exception e) {
            System.out.println("Unexpected error during registration: " + e.getMessage());
            e.printStackTrace();
            throw new AppException(ErrorCode.UNCATEGORIZED_EXCEPTION);
        }
    }

    @Override
    public String generateOTP(String email) {
        String otp = String.format("%06d", new Random().nextInt(999999));
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(2);
        otpStorage.put(email, new OTPDetails(otp, expireTime));
        return otp;
    }

    @Override
    public boolean validateOTP(String email, String otp) {
        OTPDetails details = otpStorage.get(email);

        if (details == null) return false;

        if (LocalDateTime.now().isAfter(details.getExpireTime())) {
            otpStorage.remove(email);
            return false;
        }

        if (otp.equals(details.getOtp())) {
            otpStorage.remove(email);
            return true;
        }

        return false;
    }

    @Override
    public void sendOTP(String to, String otp) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(to);
            helper.setSubject("🔐 Xác thực email - Mã OTP OnlyFanShop");
            helper.setText(buildOTPEmailTemplate(otp), true);
            
            mailSender.send(message);
        } catch (Exception e) {
            throw new RuntimeException("Không thể gửi email OTP: " + e.getMessage(), e);
        }
    }

    private String buildOTPEmailTemplate(String otp) {
        try (InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("templates/OtpTemplate.html")) {
            
            if (inputStream == null) {
                throw new RuntimeException("Không tìm thấy file template: templates/OtpTemplate.html");
            }
            
            String template = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            
            // Thay thế placeholder {OTP} bằng mã OTP thực tế
            return template.replace("{OTP}", otp);
            
        } catch (IOException e) {
            throw new RuntimeException("Lỗi khi đọc file template: " + e.getMessage(), e);
        }
    }

    @Getter
    private static class OTPDetails {
        private final String otp;
        private final LocalDateTime expireTime;

        public OTPDetails(String otp, LocalDateTime expireTime) {
            this.otp = otp;
            this.expireTime = expireTime;
        }

    }

    public ApiResponse<Void> resetPassword(String email, String newPassword) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new AppException(ErrorCode.USER_NOTEXISTED);
        }
        User user = userOpt.get();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Revoke all tokens of this user
        List<Token> tokens = tokenRepository.findAllByUserIdAndExpiredFalseAndRevokedFalse(user.getId());
        tokens.forEach(t -> { t.setExpired(true); t.setRevoked(true); });
        tokenRepository.saveAll(tokens);
        return ApiResponse.<Void>builder().statusCode(200).message("Đổi mật khẩu thành công, hãy đăng nhập").build();
    }
}
