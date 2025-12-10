package com.example.onlyfanshop_be.dto.request;

import lombok.Data;

@Data
public class RegisterRequest {
    private String fullName;
    private String password;
    private String confirmPassword;
    private String email;
    private String phoneNumber;
    private String address;
}