package com.example.onlyfanshop_be.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryLogDTO {
    private Long id;
    private Long warehouseId;
    private String warehouseName;
    private Long productId;
    private String productName;
    private Integer previousQuantity;
    private Integer newQuantity;
    private Integer quantityChange;
    private String reason;
    private Long userId;
    private String userName;
    private LocalDateTime createdAt;
}
