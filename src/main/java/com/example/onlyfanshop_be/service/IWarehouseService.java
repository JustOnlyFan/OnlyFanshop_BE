package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.InventoryItemDTO;
import com.example.onlyfanshop_be.dto.WarehouseDTO;

/**
 * Service interface for Warehouse management operations
 * Requirements: 2.3, 2.4, 2.5, 3.5
 */
public interface IWarehouseService {
    
    /**
     * Get the Main Warehouse with all inventory items
     * Requirements: 2.4 - WHEN Admin queries Main_Warehouse inventory THEN the System SHALL return all Inventory_Items with current quantities
     * 
     * @return WarehouseDTO containing main warehouse details and inventory items
     */
    WarehouseDTO getMainWarehouse();
    
    /**
     * Get a Store Warehouse by store ID
     * 
     * @param storeId The ID of the store
     * @return WarehouseDTO containing store warehouse details and inventory items
     */
    WarehouseDTO getStoreWarehouse(Integer storeId);
    
    /**
     * Update quantity for a product in Main Warehouse
     * Requirements: 2.3 - WHEN Admin updates quantity for a product in Main_Warehouse THEN the System SHALL persist the new quantity immediately
     * Requirements: 2.5 - WHEN quantity in Main_Warehouse changes THEN the System SHALL log the change
     * 
     * @param productId The ID of the product
     * @param quantity The new quantity
     * @param reason The reason for the quantity change
     * @return Updated InventoryItemDTO
     */
    InventoryItemDTO updateMainWarehouseQuantity(Long productId, Integer quantity, String reason);
    
    /**
     * Add a product to a Store Warehouse with zero quantity
     * Requirements: 3.5 - WHEN Admin adds a product to Store_Warehouse THEN the System SHALL create an Inventory_Item with zero quantity
     * 
     * @param storeId The ID of the store
     * @param productId The ID of the product to add
     * @return Created InventoryItemDTO
     */
    InventoryItemDTO addProductToStoreWarehouse(Integer storeId, Long productId);
    
    /**
     * Create an InventoryItem in Main Warehouse for a new product
     * Requirements: 2.2 - WHEN a new product is added to Product_Catalog THEN the System SHALL automatically create an InventoryItem in Main_Warehouse with zero quantity
     * 
     * @param productId The ID of the product
     */
    void createMainWarehouseInventoryItem(Long productId);
}
