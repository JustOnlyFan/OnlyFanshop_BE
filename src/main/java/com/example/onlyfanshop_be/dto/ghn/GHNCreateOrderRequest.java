package com.example.onlyfanshop_be.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GHNCreateOrderRequest {
    @JsonProperty("payment_type_id")
    private Integer paymentTypeId; // 1: Shop trả phí, 2: Khách trả phí
    
    private String note;
    
    @JsonProperty("required_note")
    private String requiredNote; // CHOTHUHANG, CHOXEMHANGKHONGTHU, KHONGCHOXEMHANG
    
    @JsonProperty("from_name")
    private String fromName;
    
    @JsonProperty("from_phone")
    private String fromPhone;
    
    @JsonProperty("from_address")
    private String fromAddress;
    
    @JsonProperty("from_ward_name")
    private String fromWardName;
    
    @JsonProperty("from_district_name")
    private String fromDistrictName;
    
    @JsonProperty("from_province_name")
    private String fromProvinceName;
    
    @JsonProperty("to_name")
    private String toName;
    
    @JsonProperty("to_phone")
    private String toPhone;
    
    @JsonProperty("to_address")
    private String toAddress;
    
    @JsonProperty("to_ward_code")
    private String toWardCode;
    
    @JsonProperty("to_district_id")
    private Integer toDistrictId;
    
    @JsonProperty("cod_amount")
    private Integer codAmount;
    
    private String content;
    
    private Integer weight; // gram
    private Integer length; // cm
    private Integer width;  // cm
    private Integer height; // cm
    
    @JsonProperty("insurance_value")
    private Integer insuranceValue;
    
    @JsonProperty("service_type_id")
    private Integer serviceTypeId; // 2: Standard, 5: Express
    
    private List<GHNItem> items;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GHNItem {
        private String name;
        private String code;
        private Integer quantity;
        private Integer price;
        private Integer weight;
    }
}
