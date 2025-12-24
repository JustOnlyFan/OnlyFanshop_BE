package com.example.onlyfanshop_be.dto.ghn;

import lombok.*;

import java.time.LocalDateTime;

/**
 * GHN Configuration DTO - DTO cho cấu hình GHN API
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GHNConfigDTO {
    private Long id;
    private String apiToken;
    private String shopId;
    private String defaultPickupTime;
    private Boolean isActive;
    private LocalDateTime updatedAt;
    
    /**
     * Masked token for display (hide sensitive data)
     */
    public String getMaskedToken() {
        if (apiToken == null || apiToken.length() < 8) {
            return "****";
        }
        return apiToken.substring(0, 4) + "****" + apiToken.substring(apiToken.length() - 4);
    }
}
