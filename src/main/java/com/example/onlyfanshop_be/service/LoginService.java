package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.UserDTO;
import com.example.onlyfanshop_be.dto.request.LoginRequest;
import com.example.onlyfanshop_be.dto.request.RegisterRequest;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.Token;
import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.enums.AuthProvider;
import com.example.onlyfanshop_be.enums.Role;
import com.example.onlyfanshop_be.enums.TokenType;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.TokenRepository;
import com.example.onlyfanshop_be.repository.UserRepository;
import com.example.onlyfanshop_be.security.JwtTokenProvider;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.*;

@Service
public class LoginService implements ILoginService{
    @Autowired
    private UserRepository userRepository;
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
        Optional<User> userOpt = userRepository.findByEmail(loginRequest.getEmail());
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            if (passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {

                // 🔹 Revoke các token cũ của user (nếu có)
                List<Token> validUserTokens = tokenRepository.findAllByUser_UserIDAndExpiredFalseAndRevokedFalse(user.getUserID());
                validUserTokens.forEach(t -> {
                    t.setExpired(true);
                    t.setRevoked(true);
                });
                tokenRepository.saveAll(validUserTokens);

                // 🔹 Sinh Access/Refresh token mới
                String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getUserID(), user.getRole(), user.getUsername());
                String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail(), user.getUserID(), user.getRole());

                // 🔹 Lưu token vào DB
                Token tokenEntity = Token.builder()
                        .user(user)
                        .token(accessToken)
                        .expired(false)
                        .revoked(false)
                        .type(TokenType.ACCESS)
                        .expiresAt(Instant.now().plusSeconds(60L*jwtAccessTtlMinutes()))
                        .build();
                tokenRepository.save(tokenEntity);

                Token refreshEntity = Token.builder()
                        .user(user)
                        .token(refreshToken)
                        .expired(false)
                        .revoked(false)
                        .type(TokenType.REFRESH)
                        .expiresAt(Instant.now().plusSeconds(60L*60L*24L*jwtRefreshTtlDays()))
                        .build();
                tokenRepository.save(refreshEntity);

                // 🔹 Trả về UserDTO kèm token
                 return ApiResponse.<UserDTO>builder().data(UserDTO.builder()
                        .userID(user.getUserID())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .phoneNumber(user.getPhoneNumber())
                        .address(user.getAddress())
                        .role(user.getRole())
                        .authProvider(user.getAuthProvider())
                        .token(accessToken)
                        .refreshToken(refreshToken)
                        .build()).message("Đăng nhập thành công").statusCode(200).build();
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

        User user = dbToken.getUser();

        // Revoke all existing access tokens
        List<Token> accessTokens = tokenRepository.findAllByUser_UserIDAndTypeAndExpiredFalseAndRevokedFalse(user.getUserID(), TokenType.ACCESS);
        accessTokens.forEach(t -> { t.setExpired(true); t.setRevoked(true); });
        tokenRepository.saveAll(accessTokens);

        String newAccess = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getUserID(), user.getRole(), user.getUsername());
        Token accessEntity = Token.builder()
                .user(user)
                .token(newAccess)
                .expired(false)
                .revoked(false)
                .type(TokenType.ACCESS)
                .expiresAt(Instant.now().plusSeconds(60L*jwtAccessTtlMinutes()))
                .build();
        tokenRepository.save(accessEntity);

        return ApiResponse.<UserDTO>builder()
                .statusCode(200)
                .message("Refresh thành công")
                .data(UserDTO.builder()
                        .userID(user.getUserID())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .phoneNumber(user.getPhoneNumber())
                        .address(user.getAddress())
                        .role(user.getRole())
                        .authProvider(user.getAuthProvider())
                        .token(newAccess)
                        .refreshToken(refreshToken)
                        .build())
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
        // Kiểm tra username đã tồn tại chưa
        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        if (userRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            throw new AppException(ErrorCode.EMAIL_USED);
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setAddress(registerRequest.getAddress());
        user.setRole(Role.CUSTOMER);
        user.setAuthProvider(AuthProvider.LOCAL);

        String hashedPassword = passwordEncoder.encode(registerRequest.getPassword());
        user.setPasswordHash(hashedPassword);

        userRepository.save(user);
        return ApiResponse.<UserDTO>builder().statusCode(200).message("Đăng ký thành công, hãy đăng nhập").data(UserDTO.builder()
                .userID(user.getUserID())
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .role(user.getRole())
                .authProvider(user.getAuthProvider())
                .build()).build();
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
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Xác thực email - OTP");
        message.setText("Mã OTP của bạn là: " + otp + " (hết hạn sau 5 phút)");
        mailSender.send(message);
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
        List<Token> tokens = tokenRepository.findAllByUser_UserIDAndExpiredFalseAndRevokedFalse(user.getUserID());
        tokens.forEach(t -> { t.setExpired(true); t.setRevoked(true); });
        tokenRepository.saveAll(tokens);
        return ApiResponse.<Void>builder().statusCode(200).message("Đổi mật khẩu thành công, hãy đăng nhập").build();
    }
}
