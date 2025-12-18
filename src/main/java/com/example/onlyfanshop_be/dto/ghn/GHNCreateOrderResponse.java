package com.example.onlyfanshop_be.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GHNCreateOrderResponse {
    @JsonProperty("order_code")
    private String orderCode;
    
    @JsonProperty("sort_code")
    private String sortCode;
    
    @JsonProperty("trans_type")
    private String transType;
    
    @JsonProperty("ward_encode")
    private String wardEncode;
    
    @JsonProperty("district_encode")
    private String districtEncode;
    
    @JsonProperty("fee")
    private GHNFee fee;
    
    @JsonProperty("total_fee")
    private Integer totalFee;
    
    @JsonProperty("expected_delivery_time")
    private String expectedDeliveryTime;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GHNFee {
        @JsonProperty("main_service")
        private Integer mainService;
        
        private Integer insurance;
        
        @JsonProperty("cod_fee")
        private Integer codFee;
        
        @JsonProperty("station_do")
        private Integer stationDo;
        
        @JsonProperty("station_pu")
        private Integer stationPu;
        
        @JsonProperty("return")
        private Integer returnFee;
        
        private Integer r2s;
        
        @JsonProperty("return_again")
        private Integer returnAgain;
        
        private Integer coupon;
        
        @JsonProperty("document_return")
        private Integer documentReturn;
        
        @JsonProperty("double_check")
        private Integer doubleCheck;
        
        @JsonProperty("double_check_deliver")
        private Integer doubleCheckDeliver;
        
        @JsonProperty("pick_remote_areas_fee")
        private Integer pickRemoteAreasFee;
        
        @JsonProperty("deliver_remote_areas_fee")
        private Integer deliverRemoteAreasFee;
        
        @JsonProperty("pick_remote_areas_fee_return")
        private Integer pickRemoteAreasFeeReturn;
        
        @JsonProperty("deliver_remote_areas_fee_return")
        private Integer deliverRemoteAreasFeeReturn;
        
        @JsonProperty("cod_failed_fee")
        private Integer codFailedFee;
    }
}
