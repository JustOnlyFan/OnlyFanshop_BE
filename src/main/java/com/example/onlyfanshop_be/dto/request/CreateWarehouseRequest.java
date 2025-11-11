package com.example.onlyfanshop_be.dto.request;

import com.example.onlyfanshop_be.enums.WarehouseType;
import lombok.Data;

@Data
public class CreateWarehouseRequest {
    private String name;
    private String code;
    private WarehouseType type;
    private Integer parentWarehouseId;
    private Integer storeLocationId;
    private String addressLine1;
    private String addressLine2;
    private String ward;
    private String district;
    private String city;
    private String country;
    private String phone;
}



