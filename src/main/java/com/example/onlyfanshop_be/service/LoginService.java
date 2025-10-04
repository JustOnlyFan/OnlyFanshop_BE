package com.example.onlyfanshop_be.service;
import com.example.onlyfanshop_be.dto.UserDTO;
import com.example.onlyfanshop_be.dto.request.RegisterRequest;
import com.example.onlyfanshop_be.enums.AuthProvider;
import com.example.onlyfanshop_be.enums.Role;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.entity.Token;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.RoleRepository;
import com.example.onlyfanshop_be.repository.TokenRepository;
import com.example.onlyfanshop_be.security.JwtTokenProvider;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.example.onlyfanshop_be.dto.request.LoginRequest;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

                // 🔹 Sinh JWT mới
                String jwtToken = jwtTokenProvider.generateToken(user.getEmail());

                // 🔹 Lưu token vào DB
                Token tokenEntity = Token.builder()
                        .user(user)
                        .token(jwtToken)
                        .expired(false)
                        .revoked(false)
                        .build();
                tokenRepository.save(tokenEntity);

                // 🔹 Trả về UserDTO kèm token
                UserDTO userDTO = UserDTO.builder()
                        .userID(user.getUserID())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .phoneNumber(user.getPhoneNumber())
                        .address(user.getAddress())
                        .role(user.getRole())
                        .authProvider(user.getAuthProvider())
                        .token(jwtToken)
                        .build();
            }else throw new AppException(ErrorCode.WRONGPASS);

                return ApiResponse.<UserDTO>builder()
                        .statusCode(200)
                        .message("Đăng nhập thành công")
                        .data(userDTO)
                        .build();

            } else {
                throw new AppException(ErrorCode.WRONGPASS);
            }

        } else {
            throw new AppException(ErrorCode.USER_NOTEXISTED);
        }
    }

    @Override
    public ApiResponse<UserDTO> register(RegisterRequest registerRequest) {
        // Kiểm tra username đã tồn tại chưa
        if(userRepository.findByUsername(registerRequest.getUsername()).isPresent()){
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        if(userRepository.findByEmail(registerRequest.getEmail()).isPresent()){
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

    private final Map<String, OTPDetails> otpStorage = new HashMap<>();
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
        return ApiResponse.<Void>builder().statusCode(200).message("Đổi mật khẩu thành công, hãy đăng nhập").build();
    }


}