package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.UserDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.Role;
import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.entity.UserAddress;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.RoleRepository;
import com.example.onlyfanshop_be.repository.TokenRepository;
import com.example.onlyfanshop_be.repository.UserAddressRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UserService implements IUserService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TokenRepository tokenRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserAddressRepository userAddressRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    @Override
    public ApiResponse<UserDTO> getUserByID(int userID) {
        Optional<User> userOtp = userRepository.findById((long) userID);
        if (userOtp.isPresent()) {
            User user = userOtp.get();
            UserDTO dto = UserDTO.builder()
                    .userID(user.getId())
                    .email(user.getEmail())
                    .username(user.getUsername())
                    .fullName(user.getUsername()) // For backward compatibility
                    .phoneNumber(user.getPhone())
                    .phone(user.getPhone())
                    .status(user.getStatus())
                    .build();
            
            // Load role if needed
            if (user.getRoleId() != null) {
                roleRepository.findById(user.getRoleId()).ifPresent(role -> {
                    dto.setRole(role);
                    dto.setRoleName(role.getName());
                });
            }
            
            return ApiResponse.<UserDTO>builder().message("Thành công").statusCode(200).data(dto).build();
        }else throw new AppException(ErrorCode.USER_NOTEXISTED);
    }

    @Override
    public ApiResponse<UserDTO> getUserByEmail(String email) {
        Optional<User> userOtp = userRepository.findByEmail(email);
        if (userOtp.isPresent()) {
            User user = userOtp.get();
            UserDTO dto = UserDTO.builder()
                    .userID(user.getId())
                    .email(user.getEmail())
                    .username(user.getUsername())
                    .fullName(user.getUsername()) // For backward compatibility
                    .phoneNumber(user.getPhone())
                    .phone(user.getPhone())
                    .status(user.getStatus())
                    .build();
            
            // Load role if needed
            if (user.getRoleId() != null) {
                roleRepository.findById(user.getRoleId()).ifPresent(role -> {
                    dto.setRole(role);
                    dto.setRoleName(role.getName());
                });
            }
            
            return ApiResponse.<UserDTO>builder().message("Thành công").statusCode(200).data(dto).build();
        }else throw new AppException(ErrorCode.USER_NOTEXISTED);
    }

    @Override
    public ApiResponse<UserDTO> updateUser(UserDTO userDTO) {
        Optional<User> userOtp = userRepository.findById(userDTO.getUserID());
        if (userOtp.isPresent()) {
            User user = userOtp.get();
            
            // Update phone
            if (userDTO.getPhoneNumber() != null) {
                user.setPhone(userDTO.getPhoneNumber());
            } else if (userDTO.getPhone() != null) {
                user.setPhone(userDTO.getPhone());
            }
            
            // Update username
            if (userDTO.getUsername() != null) {
                user.setUsername(userDTO.getUsername());
            } else if (userDTO.getFullName() != null) {
                user.setUsername(userDTO.getFullName());
            }
            
            userRepository.save(user);
            
            UserDTO responseDto = UserDTO.builder()
                    .userID(user.getId())
                    .email(user.getEmail())
                    .username(user.getUsername())
                    .fullName(user.getUsername()) // For backward compatibility
                    .phoneNumber(user.getPhone())
                    .phone(user.getPhone())
                    .status(user.getStatus())
                    .build();
            
            // Load role
            if (user.getRoleId() != null) {
                roleRepository.findById(user.getRoleId()).ifPresent(role -> {
                    responseDto.setRole(role);
                    responseDto.setRoleName(role.getName());
                });
            }
            
            return ApiResponse.<UserDTO>builder().message("Cập nhật thành công").statusCode(200).data(responseDto).build();
        }
        else throw new AppException(ErrorCode.USER_NOTEXISTED);
    }

    @Override
    public void changePassword(int userID, String oldPassword, String newPassword) {
        User user = userRepository.findById((long) userID)
                .orElseThrow(() -> new AppException(ErrorCode.USER_EXISTED));

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new AppException(ErrorCode.CART_NOTFOUND);
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Revoke all tokens after password change
        List<Token> tokens = tokenRepository.findAllByUserIdAndExpiredFalseAndRevokedFalse(user.getId());
        tokens.forEach(t -> { t.setExpired(true); t.setRevoked(true); });
        tokenRepository.saveAll(tokens);
    }

    @Override
    public void changeAddress(int userID, String address) {
        User user = userRepository.findById((long) userID)
                .orElseThrow(() -> new AppException(ErrorCode.USER_EXISTED));
        
        // Update or create default address
        Optional<UserAddress> defaultAddress = userAddressRepository.findByUserIdAndIsDefault(user.getId(), true);
        if (defaultAddress.isPresent()) {
            UserAddress addr = defaultAddress.get();
            addr.setAddressLine1(address);
            userAddressRepository.save(addr);
        } else {
            // Create new default address
            UserAddress newAddress = UserAddress.builder()
                    .userId(user.getId())
                    .fullName(user.getUsername())
                    .phone(user.getPhone() != null ? user.getPhone() : "")
                    .addressLine1(address)
                    .isDefault(true)
                    .country("Vietnam")
                    .createdAt(LocalDateTime.now())
                    .build();
            userAddressRepository.save(newAddress);
        }
    }


    @Override
    @Transactional
    public void logout(String token) {
        tokenRepository.deleteByToken(token);
    }

    /**
     * Map frontend field names to entity field names for sorting
     * @param sortField Field name from frontend (e.g., "username", "userID")
     * @return Entity field name (e.g., "username", "id")
     */
    private String mapSortField(String sortField) {
        if (sortField == null || sortField.isBlank()) {
            return "id"; // Default sort field
        }
        
        String lowerField = sortField.toLowerCase().trim();
        
        // Map common frontend field names to entity field names
        // Valid entity fields: id, username, email, phone, status, createdAt, updatedAt, lastLoginAt, roleId
        switch (lowerField) {
            case "username":
            case "user_name":
            case "fullname":
            case "full_name":
            case "name":
                return "username";
            case "userid":
            case "user_id":
            case "id":
                return "id";
            case "phonenumber":
            case "phone_number":
            case "phone":
                return "phone";
            case "createdat":
            case "created_at":
            case "created":
                return "createdAt";
            case "updatedat":
            case "updated_at":
            case "updated":
                return "updatedAt";
            case "lastloginat":
            case "last_login_at":
            case "lastlogin":
                return "lastLoginAt";
            case "email":
                return "email";
            case "status":
                return "status";
            case "roleid":
            case "role_id":
            case "role":
                return "roleId";
            default:
                // If field doesn't match, default to id to avoid errors
                System.out.println("Warning: Unknown sort field '" + sortField + "', using default 'id'");
                return "id";
        }
    }

    @Override
    public ApiResponse<Page<UserDTO>> getAllUsers(
            String keyword, String role, int page, int size,
            String sortField, String sortDirection) {

        // Map frontend field names to entity field names
        String mappedSortField = mapSortField(sortField);
        
        Sort sort = sortDirection.equalsIgnoreCase("DESC")
                ? Sort.by(mappedSortField).descending()
                : Sort.by(mappedSortField).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Byte roleId = null;
        if (role != null && !role.isBlank()) {
            // Find role by name
            Optional<Role> roleEntity = roleRepository.findByName(role.toLowerCase());
            if (roleEntity.isPresent()) {
                roleId = roleEntity.get().getId();
            } else {
                throw new IllegalArgumentException("Invalid role: " + role);
            }
        }

        Page<User> userPage = userRepository.searchUsers(keyword, roleId, pageable);

        Page<UserDTO> dtoPage = userPage.map(user -> {
            UserDTO dto = new UserDTO();
            dto.setUserID(user.getId());
            dto.setUsername(user.getUsername());
            dto.setFullName(user.getUsername()); // For backward compatibility
            dto.setEmail(user.getEmail());
            dto.setPhoneNumber(user.getPhone());
            dto.setPhone(user.getPhone());
            dto.setStatus(user.getStatus());
            
            // Load role
            if (user.getRoleId() != null) {
                roleRepository.findById(user.getRoleId()).ifPresent(roleEntity -> {
                    dto.setRole(roleEntity);
                    dto.setRoleName(roleEntity.getName());
                });
            }
            
            return dto;
        });

        return ApiResponse.<Page<UserDTO>>builder()
                .data(dtoPage)
                .statusCode(200)
                .message("Lấy danh sách người dùng thành công")
                .build();
    }

}
