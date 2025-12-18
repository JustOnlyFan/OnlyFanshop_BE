package com.example.onlyfanshop_be.dto.ghn;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GHNOrderDetailResponse {
    @JsonProperty("order_code")
    private String orderCode;
    
    private String status;
    
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
    
    private Integer weight;
    
    @JsonProperty("leadtime")
    private String leadtime;
    
    @JsonProperty("finish_date")
    private String finishDate;
    
    @JsonProperty("log")
    private List<GHNLog> log;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GHNLog {
        private String status;
        
        @JsonProperty("updated_date")
        private String updatedDate;
    }
}
