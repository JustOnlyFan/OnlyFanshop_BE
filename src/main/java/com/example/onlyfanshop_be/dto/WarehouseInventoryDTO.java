package com.example.onlyfanshop_be.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseInventoryDTO {
    private Long id;
    private Integer warehouseId;
    private String warehouseName;
    private String warehouseCode;
    private Long productId;
    private String productName;
    private Long productVariantId;
    private String productVariantName;
    private Integer quantityInStock;
    private LocalDateTime updatedAt;
}



