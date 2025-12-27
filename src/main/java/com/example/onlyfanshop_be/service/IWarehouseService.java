package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.InventoryItemDTO;
import com.example.onlyfanshop_be.dto.WarehouseDTO;

import java.util.List;

/**
 * Service interface for Warehouse management operations
 * Hệ thống chỉ hỗ trợ Store Warehouses - kho tổng (Main Warehouse) đã được loại bỏ
 * Requirements: 1.1, 2.1, 2.2, 2.3, 2.4, 7.2, 7.3
 */
public interface IWarehouseService {
    
    /**
     * Get a Store Warehouse by store ID
     * 
     * @param storeId The ID of the store
     * @return WarehouseDTO containing store warehouse details and inventory items
     */
    WarehouseDTO getStoreWarehouse(Integer storeId);
    
    /**
     * Get all active store warehouses
     * Requirements: 2.4 - THE System SHALL allow Admin to view inventory across all Store_Warehouses
     * Requirements: 7.4 - WHEN querying active warehouses THEN the System SHALL exclude inactive warehouses
     * 
     * @return List of WarehouseDTO containing all active store warehouses
     */
    List<WarehouseDTO> getAllActiveWarehouses();
    
    /**
     * Update quantity for a product in a Store Warehouse
     * Requirements: 2.1 - WHEN Admin updates inventory quantity THEN the System SHALL update the Inventory_Item in the specified Store_Warehouse
     * Requirements: 2.3 - WHEN inventory quantity changes THEN the System SHALL create an Inventory_Log entry recording the change
     * 
     * @param storeId The ID of the store
     * @param productId The ID of the product
     * @param quantity The new quantity
     * @param reason The reason for the quantity change
     * @return Updated InventoryItemDTO
     */
    InventoryItemDTO updateStoreWarehouseQuantity(Integer storeId, Long productId, Integer quantity, String reason);
    
    /**
     * Add a product to a Store Warehouse with specified quantity
     * Requirements: 2.2 - WHEN Admin adds a product to a store THEN the System SHALL create an Inventory_Item in that store's warehouse with the specified quantity
     * 
     * @param storeId The ID of the store
     * @param productId The ID of the product to add
     * @param quantity The initial quantity (default 0 if null)
     * @return Created InventoryItemDTO
     */
    InventoryItemDTO addProductToStoreWarehouse(Integer storeId, Long productId, Integer quantity);
    
    /**
     * Mark a warehouse as inactive (soft delete)
     * Requirements: 7.2 - WHEN System migrates THEN the System SHALL mark old Main_Warehouse records as inactive rather than deleting
     * Requirements: 7.3 - THE System SHALL not allow new operations on inactive warehouses
     * 
     * @param warehouseId The ID of the warehouse to deactivate
     */
    void deactivateWarehouse(Long warehouseId);
}
