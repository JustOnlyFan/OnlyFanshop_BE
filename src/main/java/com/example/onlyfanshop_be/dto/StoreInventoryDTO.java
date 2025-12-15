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
public class StoreInventoryDTO {
    private Long id;
    private Integer storeId;
    private String storeName;
    private String storeAddress;
    private Long productId;
    private String productName;
    private String productImageUrl;
    private Double productPrice;
    private Boolean isAvailable;
    private Integer quantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

