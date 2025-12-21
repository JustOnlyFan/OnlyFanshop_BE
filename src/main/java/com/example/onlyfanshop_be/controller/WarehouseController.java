package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.InventoryItemDTO;
import com.example.onlyfanshop_be.dto.WarehouseDTO;
import com.example.onlyfanshop_be.dto.request.AddProductToWarehouseRequest;
import com.example.onlyfanshop_be.dto.request.UpdateQuantityRequest;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.service.IWarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Warehouse management operations
 * Requirements: 2.3, 2.4, 3.5
 */
@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Warehouse Management", description = "APIs for managing warehouses and inventory")
public class WarehouseController {
    
    private final IWarehouseService warehouseService;
    
    /**
     * GET /api/warehouses/main
     * Get the Main Warehouse with all inventory items
     * Requirements: 2.4 - WHEN Admin queries Main_Warehouse inventory THEN the System SHALL return all Inventory_Items with current quantities
     */
    @GetMapping("/main")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get Main Warehouse", description = "Retrieves the main warehouse with all inventory items")
    public ResponseEntity<ApiResponse<WarehouseDTO>> getMainWarehouse() {
        log.info("Getting Main Warehouse");
        WarehouseDTO warehouse = warehouseService.getMainWarehouse();
        return ResponseEntity.ok(ApiResponse.<WarehouseDTO>builder()
                .statusCode(200)
                .message("Main warehouse retrieved successfully")
                .data(warehouse)
                .build());
    }
    
    /**
     * PUT /api/warehouses/main/inventory/{productId}
     * Update quantity for a product in Main Warehouse
     * Requirements: 2.3 - WHEN Admin updates quantity for a product in Main_Warehouse THEN the System SHALL persist the new quantity immediately
     */
    @PutMapping("/main/inventory/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update Main Warehouse Inventory", description = "Updates the quantity of a product in the main warehouse")
    public ResponseEntity<ApiResponse<InventoryItemDTO>> updateMainWarehouseQuantity(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateQuantityRequest request) {
        log.info("Updating Main Warehouse inventory for product {}: quantity={}", productId, request.getQuantity());
        InventoryItemDTO inventoryItem = warehouseService.updateMainWarehouseQuantity(
                productId, request.getQuantity(), request.getReason());
        return ResponseEntity.ok(ApiResponse.<InventoryItemDTO>builder()
                .statusCode(200)
                .message("Inventory updated successfully")
                .data(inventoryItem)
                .build());
    }
    
    /**
     * GET /api/warehouses/stores/{storeId}
     * Get a Store Warehouse by store ID
     */
    @GetMapping("/stores/{storeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get Store Warehouse", description = "Retrieves a store warehouse with all inventory items")
    public ResponseEntity<ApiResponse<WarehouseDTO>> getStoreWarehouse(@PathVariable Integer storeId) {
        log.info("Getting Store Warehouse for store {}", storeId);
        WarehouseDTO warehouse = warehouseService.getStoreWarehouse(storeId);
        return ResponseEntity.ok(ApiResponse.<WarehouseDTO>builder()
                .statusCode(200)
                .message("Store warehouse retrieved successfully")
                .data(warehouse)
                .build());
    }
    
    /**
     * POST /api/warehouses/stores/{storeId}/products
     * Add a product to a Store Warehouse
     * Requirements: 3.5 - WHEN Admin adds a product to Store_Warehouse THEN the System SHALL create an Inventory_Item with zero quantity
     */
    @PostMapping("/stores/{storeId}/products")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add Product to Store Warehouse", description = "Adds a product to a store warehouse with zero quantity")
    public ResponseEntity<ApiResponse<InventoryItemDTO>> addProductToStoreWarehouse(
            @PathVariable Integer storeId,
            @Valid @RequestBody AddProductToWarehouseRequest request) {
        log.info("Adding product {} to Store Warehouse for store {}", request.getProductId(), storeId);
        InventoryItemDTO inventoryItem = warehouseService.addProductToStoreWarehouse(storeId, request.getProductId());
        return ResponseEntity.ok(ApiResponse.<InventoryItemDTO>builder()
                .statusCode(201)
                .message("Product added to store warehouse successfully")
                .data(inventoryItem)
                .build());
    }
}
