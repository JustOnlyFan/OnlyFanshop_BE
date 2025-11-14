package com.example.onlyfanshop_be.dto;

import com.example.onlyfanshop_be.enums.StoreStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreLocationSummaryDTO {
    private Integer locationID;
    private String name;
    private String address;
    private String ward;
    private String city;
    private String phone;
    private StoreStatus status;
}

