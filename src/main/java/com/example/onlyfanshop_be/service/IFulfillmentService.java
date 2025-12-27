package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.response.AvailabilityCheckResult;
import com.example.onlyfanshop_be.dto.response.FulfillmentResult;
import com.example.onlyfanshop_be.dto.response.SourceAllocation;
import com.example.onlyfanshop_be.entity.TransferRequest;

import java.util.List;

/**
 * Service interface for Transfer Request Fulfillment operations
 * Handles availability checking, source allocation, and inventory deduction
 * Updated to only support Store Warehouses (Main Warehouse removed)
 * Requirements: 4.1, 4.2, 4.3, 4.4, 5.2, 5.5, 5.6
 */
public interface IFulfillmentService {
    
    /**
     * Check availability for a transfer request
     * Requirements: 4.1 - WHEN calculating source allocations THEN the System SHALL only consider Store_Warehouses
     * Requirements: 4.2 - WHEN a store needs inventory THEN the System SHALL search available quantity from other Store_Warehouses
     * 
     * @param request The transfer request to check availability for
     * @return AvailabilityCheckResult containing detailed availability information
     */
    AvailabilityCheckResult checkAvailability(TransferRequest request);
    
    /**
     * Check availability for a transfer request by ID
     * 
     * @param requestId The transfer request ID
     * @return AvailabilityCheckResult containing detailed availability information
     */
    AvailabilityCheckResult checkAvailabilityById(Long requestId);
    
    /**
     * Fulfill a transfer request by deducting inventory from source warehouses
     * Requirements: 5.5 - WHEN Admin approves a Transfer_Request THEN the System SHALL deduct quantities from source warehouses and create Shipments
     * Requirements: 5.6 - WHEN total available quantity across all sources is less than requested THEN the System SHALL allow partial fulfillment with Debt_Order creation
     * 
     * @param request The transfer request to fulfill
     * @return FulfillmentResult containing fulfillment details and any debt order created
     */
    FulfillmentResult fulfill(TransferRequest request);
    
    /**
     * Fulfill a transfer request by ID
     * 
     * @param requestId The transfer request ID
     * @return FulfillmentResult containing fulfillment details
     */
    FulfillmentResult fulfillById(Long requestId);
    
    /**
     * Calculate source allocations for a specific product and required quantity
     * Only considers Store Warehouses (Main Warehouse removed)
     * Requirements: 4.1 - WHEN calculating source allocations THEN the System SHALL only consider Store_Warehouses
     * Requirements: 4.3 - WHEN multiple Store_Warehouses have available stock THEN the System SHALL prioritize by available quantity (highest first)
     * Requirements: 4.4 - THE System SHALL exclude the requesting store's warehouse from source allocation
     * 
     * @param productId The product ID to allocate
     * @param requiredQuantity The required quantity
     * @param excludeStoreId The store ID to exclude from allocation (the requesting store)
     * @return List of SourceAllocation representing where inventory should be sourced from
     */
    List<SourceAllocation> calculateSourceAllocations(Long productId, int requiredQuantity, Integer excludeStoreId);
    
    /**
     * Get available quantity for a product in a specific Store Warehouse
     * Requirements: 4.2 - WHEN a store needs inventory THEN the System SHALL search available quantity from other Store_Warehouses
     * 
     * @param productId The product ID
     * @param warehouseId The warehouse ID
     * @return Available quantity (total - reserved)
     */
    int getStoreWarehouseAvailableQuantity(Long productId, Long warehouseId);

    /**
     * Calculate total available quantity for a product across all active store warehouses
     * Requirements: 6.1 - Calculate total available from all Store_Warehouses
     * 
     * @param productId The product ID
     * @param excludeStoreId Optional store ID to exclude (the requesting store)
     * @return Total available quantity across all eligible store warehouses
     */
    int calculateTotalAvailableQuantity(Long productId, Integer excludeStoreId);

    /**
     * Calculate shortage for a product
     * Requirements: 6.1 - WHEN total available quantity across all Store_Warehouses is less than requested 
     *               THEN the System SHALL calculate and report the shortage
     * 
     * Shortage = requested_quantity - total_available_quantity
     * 
     * @param productId The product ID
     * @param requestedQuantity The requested quantity
     * @param excludeStoreId Optional store ID to exclude (the requesting store)
     * @return Shortage quantity (0 if no shortage)
     */
    int calculateShortage(Long productId, int requestedQuantity, Integer excludeStoreId);
}
