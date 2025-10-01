package com.example.onlyfanshop_be.dto;

import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.enums.Role;
import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor    
public class UserDTO {

    private Long userId;
    private String username;
    private String email;
    private String phoneNumber;
    private String address;
    private String googleId;
    private Boolean isActive;
    private String role;
    private String roleDisplayName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserDTO(Long userId, String username, String email, String phoneNumber, String address, String googleId, Boolean isActive, Role role, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.googleId = googleId;
        this.isActive = isActive;
        this.role = role != null ? role.name() : null;
        this.roleDisplayName = role != null ? role.getDisplayName() : null;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static UserDTO toUserDTO(User user) {
        return UserDTO.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .googleId(user.getGoogleId())
                .isActive(user.getIsActive())
                .role(user.getRole() != null ? user.getRole().name() : null)
                .roleDisplayName(user.getRole() != null ? user.getRole().getDisplayName() : null)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

}
