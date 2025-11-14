package com.example.onlyfanshop_be.dto;

import com.example.onlyfanshop_be.enums.WarehouseType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseDTO {
    private Integer id;
    private String name;
    private String code;
    private WarehouseType type;
    private Integer parentWarehouseId;
    private String parentWarehouseName;
    private Integer storeLocationId;
    private String storeLocationName;
    private String addressLine1;
    private String addressLine2;
    private String ward;
    private String district;
    private String city;
    private String country;
    private String phone;
    private Boolean isActive;
    private LocalDateTime createdAt;
}








