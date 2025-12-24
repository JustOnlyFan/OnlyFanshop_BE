package com.example.onlyfanshop_be.dto.ghn;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * GHN Configuration Update Request - Request để cập nhật cấu hình GHN
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GHNConfigUpdateRequest {
    @NotBlank(message = "API Token is required")
    private String apiToken;
    
    @NotBlank(message = "Shop ID is required")
    private String shopId;
    
    private String defaultPickupTime;
}
