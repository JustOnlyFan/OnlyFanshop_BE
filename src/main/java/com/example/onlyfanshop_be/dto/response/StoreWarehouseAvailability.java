package com.example.onlyfanshop_be.dto.response;

import lombok.*;

/**
 * Represents availability information for a product in a specific store warehouse
 * Requirements: 9.2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreWarehouseAvailability {
    /**
     * The warehouse ID
     */
    private Long warehouseId;
    
    /**
     * The warehouse name
     */
    private String warehouseName;
    
    /**
     * The store ID
     */
    private Integer storeId;
    
    /**
     * The store name
     */
    private String storeName;
    
    /**
     * Total quantity in this warehouse
     */
    private Integer totalQuantity;
    
    /**
     * Reserved quantity (for pending transfer requests)
     */
    private Integer reservedQuantity;
    
    /**
     * Available quantity (total - reserved)
     */
    private Integer availableQuantity;
}
