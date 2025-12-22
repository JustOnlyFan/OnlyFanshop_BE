package com.example.onlyfanshop_be.dto.response;

import com.example.onlyfanshop_be.enums.WarehouseType;
import lombok.*;

/**
 * Represents an allocation of inventory from a specific warehouse source
 * Used in fulfillment calculations to track where inventory will be sourced from
 * Requirements: 5.2, 5.3, 5.4
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceAllocation {
    /**
     * The warehouse ID from which inventory will be sourced
     */
    private Long warehouseId;
    
    /**
     * The warehouse name for display purposes
     */
    private String warehouseName;
    
    /**
     * The type of warehouse (MAIN or STORE)
     */
    private WarehouseType warehouseType;
    
    /**
     * The store ID if this is a store warehouse (null for main warehouse)
     */
    private Integer storeId;
    
    /**
     * The store name if this is a store warehouse
     */
    private String storeName;
    
    /**
     * The quantity to be allocated from this source
     */
    private Integer quantity;
}
