package com.example.onlyfanshop_be.dto.request;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
}
