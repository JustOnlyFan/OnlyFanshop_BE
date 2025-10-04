package com.example.onlyfanshop_be.dto;

import com.example.onlyfanshop_be.enums.AuthProvider;
import com.example.onlyfanshop_be.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Integer userID;
    private String username;
    private String email;
    private String phoneNumber;
    private String address;
    private Role role;
    private AuthProvider authProvider;
    private String token;
}