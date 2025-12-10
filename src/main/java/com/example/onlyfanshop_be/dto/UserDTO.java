package com.example.onlyfanshop_be.dto;

import com.example.onlyfanshop_be.entity.Role;
import com.example.onlyfanshop_be.enums.AuthProvider;
import com.example.onlyfanshop_be.enums.UserStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Long userID;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String phone;
    private String address;
    @JsonIgnore
    private Role role;
    private String roleName;
    private AuthProvider authProvider;
    private UserStatus status;
    private String token;
    private String refreshToken;
    private Integer storeLocationId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private StoreLocationSummaryDTO storeLocation;
}