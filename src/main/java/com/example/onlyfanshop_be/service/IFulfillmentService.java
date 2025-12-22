package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.response.AvailabilityCheckResult;
import com.example.onlyfanshop_be.dto.response.FulfillmentResult;
import com.example.onlyfanshop_be.dto.response.SourceAllocation;
import com.example.onlyfanshop_be.entity.TransferRequest;

import java.util.List;

/**
 * Service interface for Transfer Request Fulfillment operations
 * Handles availability checking, source allocation, and inventory deduction
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 9.1, 9.2, 9.3, 9.4, 9.5
 */
public interface IFulfillmentService {
    
    /**
     * Check availability for a transfer request
     * Requirements: 5.1 - WHEN Admin reviews a Transfer_Request THEN the System SHALL display availability check results for each product
     * Requirements: 9.1 - WHEN Admin requests availability check for products THEN the System SHALL return quantity available in Main_Warehouse
     * Requirements: 9.2 - WHEN Main_Warehouse quantity is insufficient THEN the System SHALL return breakdown of available quantities from each Store_Warehouse
     * Requirements: 9.4 - WHEN availability check completes THEN the System SHALL return fulfillment recommendation with source allocation
     * Requirements: 9.5 - WHEN no sources can fulfill the request THEN the System SHALL return maximum fulfillable quantity and shortage amount
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
     * Requirements: 5.2 - WHEN checking availability THEN the System SHALL first check Main_Warehouse quantity for each product
     * Requirements: 5.3 - WHEN Main_Warehouse has insufficient quantity THEN the System SHALL check all other Store_Warehouses for available quantity
     * Requirements: 5.4 - WHEN multiple Store_Warehouses have available quantity THEN the System SHALL aggregate quantities to fulfill the shortage
     * 
     * @param productId The product ID to allocate
     * @param requiredQuantity The required quantity
     * @param excludeStoreId The store ID to exclude from allocation (the requesting store)
     * @return List of SourceAllocation representing where inventory should be sourced from
     */
    List<SourceAllocation> calculateSourceAllocations(Long productId, int requiredQuantity, Integer excludeStoreId);
    
    /**
     * Get available quantity for a product in Main Warehouse
     * Requirements: 9.3 - WHEN calculating available quantity in Store_Warehouse THEN the System SHALL exclude quantities reserved for pending Transfer_Requests
     * 
     * @param productId The product ID
     * @return Available quantity (total - reserved)
     */
    int getMainWarehouseAvailableQuantity(Long productId);
    
    /**
     * Get available quantity for a product in a specific Store Warehouse
     * Requirements: 9.3 - WHEN calculating available quantity in Store_Warehouse THEN the System SHALL exclude quantities reserved for pending Transfer_Requests
     * 
     * @param productId The product ID
     * @param warehouseId The warehouse ID
     * @return Available quantity (total - reserved)
     */
    int getStoreWarehouseAvailableQuantity(Long productId, Long warehouseId);
}
