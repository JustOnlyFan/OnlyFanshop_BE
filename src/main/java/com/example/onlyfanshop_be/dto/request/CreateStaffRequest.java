package com.example.onlyfanshop_be.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateStaffRequest {
    // Username is optional - will be auto-generated from store name if not provided
    @Size(max = 100, message = "Username must be at most 100 characters")
    private String username;

    // Email is optional - will be auto-generated from store name if not provided
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    // Phone and address are optional - will be taken from store if not provided
    private String phoneNumber;
    private String address;
    
    // Store location is required
    private Integer storeLocationId;
}

