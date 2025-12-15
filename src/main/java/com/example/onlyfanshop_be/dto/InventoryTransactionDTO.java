package com.example.onlyfanshop_be.dto;

import com.example.onlyfanshop_be.enums.InventoryLocationType;
import com.example.onlyfanshop_be.enums.InventoryTransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryTransactionDTO {
    private Long id;
    private InventoryTransactionType transactionType;
    private Long productId;
    private String productName;
    private Integer quantity;
    private InventoryLocationType sourceType;
    private Integer sourceStoreId;
    private String sourceStoreName;
    private InventoryLocationType destinationType;
    private Integer destinationStoreId;
    private String destinationStoreName;
    private Long requestId;
    private Long orderId;
    private Long performedBy;
    private String performerName;
    private Integer quantityBefore;
    private Integer quantityAfter;
    private String note;
    private LocalDateTime createdAt;
}
