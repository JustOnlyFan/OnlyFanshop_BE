package com.example.onlyfanshop_be.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDTO {
    private Integer userID;
    private String username;
    private String email;
    private String phoneNumber;
    private String address;
    private String role;
}