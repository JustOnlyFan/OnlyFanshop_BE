package com.example.onlyfanshop_be.dto;

import lombok.*;

/**
 * DTO for DebtItem entity
 * Requirements: 6.1, 6.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebtItemDTO {
    private Long id;
    private Long debtOrderId;
    private Long productId;
    private String productName;
    private String productSku;
    private Integer owedQuantity;
    private Integer fulfilledQuantity;
    private Integer remainingQuantity;
    private Boolean fullyFulfilled;
}
