package com.example.onlyfanshop_be.dto;

import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
