package com.example.onlyfanshop_be.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GHNCalculateFeeRequest {
    @JsonProperty("from_district_id")
    private Integer fromDistrictId;
    
    @JsonProperty("from_ward_code")
    private String fromWardCode;
    
    @JsonProperty("service_type_id")
    private Integer serviceTypeId;
    
    @JsonProperty("to_district_id")
    private Integer toDistrictId;
    
    @JsonProperty("to_ward_code")
    private String toWardCode;
    
    private Integer weight; // gram
    private Integer length; // cm
    private Integer width;  // cm
    private Integer height; // cm
    
    @JsonProperty("insurance_value")
    private Integer insuranceValue;
    
    @JsonProperty("cod_value")
    private Integer codValue;
    
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
