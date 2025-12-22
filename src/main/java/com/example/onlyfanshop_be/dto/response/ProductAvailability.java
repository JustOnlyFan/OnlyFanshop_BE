package com.example.onlyfanshop_be.dto.response;

import lombok.*;

import java.util.List;

/**
 * Represents availability information for a single product across all warehouses
 * Requirements: 9.1, 9.2, 9.4, 9.5
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductAvailability {
    /**
     * The product ID
     */
    private Long productId;
    
    /**
     * The product name
     */
    private String productName;
    
    /**
     * The product SKU
     */
    private String productSku;
    
    /**
     * The requested quantity for this product
     */
    private Integer requestedQuantity;
    
    /**
     * Available quantity in Main Warehouse
     * Requirements: 9.1
     */
    private Integer mainWarehouseAvailable;
    
    /**
     * Breakdown of available quantities from each Store Warehouse
     * Only populated when main warehouse is insufficient
     * Requirements: 9.2
     */
    private List<StoreWarehouseAvailability> storeAvailabilities;
    
    /**
     * Total available quantity across all sources
     */
    private Integer totalAvailable;
    
    /**
     * Shortage amount (requested - total available), 0 if fully available
     * Requirements: 9.5
     */
    private Integer shortage;
    
    /**
     * Recommended source allocations for fulfillment
     * Requirements: 9.4
     */
    private List<SourceAllocation> recommendedAllocations;
    
    /**
     * Whether this product can be fully fulfilled
     */
    public boolean canFullyFulfill() {
        return shortage == null || shortage <= 0;
    }
}
