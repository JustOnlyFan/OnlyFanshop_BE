package com.example.onlyfanshop_be.dto.request;

import lombok.Data;

@Data
public class GoogleLoginRequest {

    private String email;
    private String idToken;
    private String googleId;
    private String displayName;
}
