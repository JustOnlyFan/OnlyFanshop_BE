package com.example.onlyfanshop_be.service;
import com.example.onlyfanshop_be.dto.UserDTO;
import com.example.onlyfanshop_be.dto.request.RegisterRequest;
import com.example.onlyfanshop_be.dto.response.RoleResponse;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.RoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.example.onlyfanshop_be.dto.request.LoginRequest;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;
@Service
public class LoginService implements ILoginService{
    @Autowired
    private UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    @Autowired
    private RoleRepository roleRepository;

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


}
