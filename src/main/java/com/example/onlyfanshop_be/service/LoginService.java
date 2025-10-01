package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.UserDTO;
import com.example.onlyfanshop_be.dto.request.RegisterRequest;
import com.example.onlyfanshop_be.enums.Role;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
public class LoginService implements ILoginService {

    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final JavaMailSender mailSender;

    @Autowired
    public LoginService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Value("${google.clientId}")

    private String clientId;

    @Override
    public ApiResponse login(LoginRequest loginRequest) {
        Optional<User> userOpt = userRepository.findByEmail(loginRequest.getEmail());
        System.out.println(loginRequest.getEmail());
        System.out.println(userOpt.isPresent());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            ApiResponse apiResponse = new ApiResponse();
            if (passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {

                apiResponse.setMessage("Đăng nhập thành công");
                apiResponse.setData(UserDTO.builder()
                        .userId(user.getUserId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .phoneNumber(user.getPhoneNumber())
                        .address(user.getAddress())
                        .role(user.getRole().name())
                        .roleDisplayName(user.getRole().getDisplayName())
                        .build());
                return apiResponse;
            } else throw new AppException(ErrorCode.WRONGPASS);

        } else throw new AppException(ErrorCode.USER_NOTEXISTED);

    }

    @Override
    public ApiResponse register(RegisterRequest registerRequest) {

        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setAddress(registerRequest.getAddress());
        user.setRole(Role.CUSTOMER);

        String hashedPassword = passwordEncoder.encode(registerRequest.getPassword());
        user.setPasswordHash(hashedPassword);

        userRepository.save(user);

        ApiResponse response = new ApiResponse();
        response.setMessage("Đăng ký thành công, hãy đăng nhập");
        response.setData(user);
        return response;
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

    private static class OTPDetails {
        private final String otp;
        private final LocalDateTime expireTime;

        public OTPDetails(String otp, LocalDateTime expireTime) {
            this.otp = otp;
            this.expireTime = expireTime;
        }

        public String getOtp() {
            return otp;
        }

        public LocalDateTime getExpireTime() {
            return expireTime;
        }
    }

    @Override
    public ApiResponse<UserDTO> loginWithGoogle(String idTokenString) {
        try {
            // Validate input token
            if (idTokenString == null || idTokenString.trim().isEmpty()) {
                return new ApiResponse<>(400, "ID token is required", null);
            }

            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    JacksonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(clientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();

                String userId = payload.getSubject();
                String email = payload.getEmail();
                String name = (String) payload.get("name");

                User user = userRepository.findByEmail(email)
                        .orElseGet(() -> {
                            User newUser = new User();
                            newUser.setEmail(email);
                            newUser.setUsername(name);
                            // Set default role for Google users
                            newUser.setRole(Role.CUSTOMER);
                            return userRepository.save(newUser);
                        });

                UserDTO userDTO = UserDTO.builder()
                        .userId(user.getUserId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .phoneNumber(user.getPhoneNumber())
                        .address(user.getAddress())
                        .role(user.getRole().name())
                        .roleDisplayName(user.getRole().getDisplayName())
                        .build();

                return new ApiResponse<>(200, "Login with Google success", userDTO);
            } else {
                return new ApiResponse<>(401, "Invalid ID token", null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return new ApiResponse<>(500, "Google verification failed", null);
        }

    public ApiResponse resetPassword(String email, String newPassword) {
        ApiResponse apiResponse = new ApiResponse();
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            throw new AppException(ErrorCode.USER_NOTEXISTED);
        }
        User user = userOpt.get();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        apiResponse.setMessage("Đổi mật khẩu thành công, hãy đăng nhập");
        return apiResponse;
    }
}
