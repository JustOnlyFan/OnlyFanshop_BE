package com.example.onlyfanshop_be.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

/**
 * GHN Shop Info Response - Response từ GHN API khi lấy thông tin shop
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GHNShopInfoResponse {
    @JsonProperty("_id")
    private Integer id;
    
    private String name;
    
    private String phone;
    
    private String address;
    
    @JsonProperty("district_id")
    private Integer districtId;
    
    @JsonProperty("ward_code")
    private String wardCode;
    
    @JsonProperty("client_id")
    private Integer clientId;
    
    private Integer status;
}
