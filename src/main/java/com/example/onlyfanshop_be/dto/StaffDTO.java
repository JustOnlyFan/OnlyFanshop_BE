package com.example.onlyfanshop_be.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.example.onlyfanshop_be.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffDTO {
    private Long userID;
    private String username;
    private String email;
    private String phoneNumber;
    private String address;
    private String role;
    private Integer storeLocationId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private StoreLocationSummaryDTO storeLocation;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
}




