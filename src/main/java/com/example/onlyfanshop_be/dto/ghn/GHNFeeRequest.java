package com.example.onlyfanshop_be.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

/**
 * GHN Fee Request - Request để tính phí vận chuyển GHN
 * Alias cho GHNCalculateFeeRequest với naming đơn giản hơn
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GHNFeeRequest {
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
    
    private List<GHNFeeItem> items;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GHNFeeItem {
        private String name;
        private String code;
        private Integer quantity;
        private Integer price;
        private Integer weight;
    }
    
    /**
     * Convert to GHNCalculateFeeRequest
     */
    public GHNCalculateFeeRequest toCalculateFeeRequest() {
        return GHNCalculateFeeRequest.builder()
            .fromDistrictId(this.fromDistrictId)
            .fromWardCode(this.fromWardCode)
            .serviceTypeId(this.serviceTypeId)
            .toDistrictId(this.toDistrictId)
            .toWardCode(this.toWardCode)
            .weight(this.weight)
            .length(this.length)
            .width(this.width)
            .height(this.height)
            .insuranceValue(this.insuranceValue)
            .codValue(this.codValue)
            .items(this.items != null ? this.items.stream()
                .map(item -> GHNCalculateFeeRequest.GHNItem.builder()
                    .name(item.getName())
                    .code(item.getCode())
                    .quantity(item.getQuantity())
                    .price(item.getPrice())
                    .weight(item.getWeight())
                    .build())
                .toList() : null)
            .build();
    }
}
