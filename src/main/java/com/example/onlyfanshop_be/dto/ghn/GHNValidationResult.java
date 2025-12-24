package com.example.onlyfanshop_be.dto.ghn;

import lombok.*;

/**
 * GHN Validation Result - Kết quả validate cấu hình GHN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GHNValidationResult {
    private boolean valid;
    private String message;
    private GHNShopInfo shopInfo;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class GHNShopInfo {
        private Integer shopId;
        private String name;
        private String phone;
        private String address;
        private Integer districtId;
        private String wardCode;
    }
    
    public static GHNValidationResult success(GHNShopInfo shopInfo) {
        return GHNValidationResult.builder()
            .valid(true)
            .message("Configuration is valid")
            .shopInfo(shopInfo)
            .build();
    }
    
    public static GHNValidationResult failure(String message) {
        return GHNValidationResult.builder()
            .valid(false)
            .message(message)
            .build();
    }
}
