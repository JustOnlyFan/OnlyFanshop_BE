package com.example.onlyfanshop_be.dto.request;

import com.example.onlyfanshop_be.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateStaffRequest {
    private String fullName;
    private String email;
    private String phoneNumber;
    private String address;
    private Integer storeLocationId;
    private UserStatus status;
}







