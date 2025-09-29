package com.example.onlyfanshop_be.service;
import com.example.onlyfanshop_be.dto.UserDTO;
import com.example.onlyfanshop_be.dto.request.RegisterRequest;
import com.example.onlyfanshop_be.dto.response.RoleResponse;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.RoleRepository;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Service
public class LoginService implements ILoginService{
    @Autowired
    private UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    @Autowired
    private RoleRepository roleRepository;
    private final JavaMailSender mailSender;
    @Autowired
    public LoginService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public ApiResponse login(LoginRequest loginRequest) {
        Optional<User> userOpt = userRepository.findByUsername(loginRequest.getUsername());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            ApiResponse apiResponse = new ApiResponse();
            if(passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())){

                apiResponse.setMessage("Đăng nhập thành công");
                apiResponse.setData(UserDTO.builder()
                        .userID(user.getUserID())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .phoneNumber(user.getPhoneNumber())
                        .address(user.getAddress())
                        .role(user.getRole().getRoleName())
                        .build());
                return apiResponse;
            }else throw new AppException(ErrorCode.WRONGPASS);

        } else throw new AppException(ErrorCode.USER_NOTEXISTED);

    }
    @Override
    public ApiResponse register(RegisterRequest registerRequest) {
        // Kiểm tra username đã tồn tại chưa
        if(userRepository.findByUsername(registerRequest.getUsername()).isPresent()){
            throw new AppException(ErrorCode.USER_EXISTED); // Tạo ErrorCode phù hợp
        }

        // Tạo user mới
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setEmail(registerRequest.getEmail());
        user.setPhoneNumber(registerRequest.getPhoneNumber());
        user.setAddress(registerRequest.getAddress());
        user.setRole(roleRepository.getReferenceById(1)); // hoặc set role mặc định

        // Encode mật khẩu
        String hashedPassword = passwordEncoder.encode(registerRequest.getPassword());
        user.setPasswordHash(hashedPassword);

        // Lưu vào DB
        userRepository.save(user);

        ApiResponse response = new ApiResponse();
        response.setMessage("Đăng ký thành công");
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

        // Nếu đã hết hạn thì xóa luôn
        if (LocalDateTime.now().isAfter(details.getExpireTime())) {
            otpStorage.remove(email);
            return false;
        }

        // Nếu đúng OTP và còn hạn → xóa khỏi storage (dùng 1 lần thôi)
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

    // Inner class để lưu OTP + ExpireTime
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
}
