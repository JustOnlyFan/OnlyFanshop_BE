package com.example.onlyfanshop_be.dto;

import com.example.onlyfanshop_be.enums.DebtOrderStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for DebtOrder entity
 * Requirements: 6.1, 6.2, 6.3, 6.4, 6.5
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebtOrderDTO {
    private Long id;
    private Long transferRequestId;
    private Integer storeId;
    private String storeName;
    private DebtOrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime fulfilledAt;
    private List<DebtItemDTO> items;
    
    // Summary fields
    private Integer totalOwedQuantity;
    private Integer totalFulfilledQuantity;
    private Integer totalRemainingQuantity;
}
