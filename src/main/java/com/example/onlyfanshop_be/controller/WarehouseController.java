package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.InventoryItemDTO;
import com.example.onlyfanshop_be.dto.WarehouseDTO;
import com.example.onlyfanshop_be.dto.request.AddProductToWarehouseRequest;
import com.example.onlyfanshop_be.dto.request.UpdateQuantityRequest;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.service.IWarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Warehouse Management", description = "APIs for managing store warehouses and inventory")
public class WarehouseController {
    
    private final IWarehouseService warehouseService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get All Active Warehouses", description = "Retrieves all active store warehouses")
    public ResponseEntity<ApiResponse<List<WarehouseDTO>>> getAllActiveWarehouses() {
        log.info("Getting all active warehouses");
        List<WarehouseDTO> warehouses = warehouseService.getAllActiveWarehouses();
        return ResponseEntity.ok(ApiResponse.<List<WarehouseDTO>>builder()
                .statusCode(200)
                .message("Active warehouses retrieved successfully")
                .data(warehouses)
                .build());
    }

    @GetMapping("/main")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get Main Warehouse (DEPRECATED)", description = "Main warehouse is no longer supported")
    public ResponseEntity<ApiResponse<WarehouseDTO>> getMainWarehouse() {
        log.warn("Attempted to access deprecated Main Warehouse endpoint");
        throw new AppException(ErrorCode.INVALID_WAREHOUSE_TYPE);
    }

    @PutMapping("/main/inventory/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update Main Warehouse Inventory (DEPRECATED)", description = "Main warehouse is no longer supported")
    public ResponseEntity<ApiResponse<InventoryItemDTO>> updateMainWarehouseQuantity(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateQuantityRequest request) {
        log.warn("Attempted to update deprecated Main Warehouse inventory");
        throw new AppException(ErrorCode.INVALID_WAREHOUSE_TYPE);
    }

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

    @PutMapping("/stores/{storeId}/inventory/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update Store Warehouse Inventory", description = "Updates the quantity of a product in a store warehouse")
    public ResponseEntity<ApiResponse<InventoryItemDTO>> updateStoreWarehouseQuantity(
            @PathVariable Integer storeId,
            @PathVariable Long productId,
            @Valid @RequestBody UpdateQuantityRequest request) {
        log.info("Updating Store Warehouse inventory for store {} product {}: quantity={}", 
                storeId, productId, request.getQuantity());
        InventoryItemDTO inventoryItem = warehouseService.updateStoreWarehouseQuantity(
                storeId, productId, request.getQuantity(), request.getReason());
        return ResponseEntity.ok(ApiResponse.<InventoryItemDTO>builder()
                .statusCode(200)
                .message("Inventory updated successfully")
                .data(inventoryItem)
                .build());
    }

    @PostMapping("/stores/{storeId}/products")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Add Product to Store Warehouse", description = "Adds a product to a store warehouse with specified quantity")
    public ResponseEntity<ApiResponse<InventoryItemDTO>> addProductToStoreWarehouse(
            @PathVariable Integer storeId,
            @Valid @RequestBody AddProductToWarehouseRequest request) {
        log.info("Adding product {} to Store Warehouse for store {} with quantity {}", 
                request.getProductId(), storeId, request.getQuantity());
        InventoryItemDTO inventoryItem = warehouseService.addProductToStoreWarehouse(
                storeId, request.getProductId(), request.getQuantity());
        return ResponseEntity.ok(ApiResponse.<InventoryItemDTO>builder()
                .statusCode(201)
                .message("Product added to store warehouse successfully")
                .data(inventoryItem)
                .build());
    }

    @DeleteMapping("/{warehouseId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate Warehouse", description = "Marks a warehouse as inactive (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deactivateWarehouse(@PathVariable Long warehouseId) {
        log.info("Deactivating warehouse {}", warehouseId);
        warehouseService.deactivateWarehouse(warehouseId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .statusCode(200)
                .message("Warehouse deactivated successfully")
                .build());
    }

    @PatchMapping("/stores/{storeId}/inventory/{productId}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Toggle Product Enabled Status", description = "Enable or disable a product in a store warehouse")
    public ResponseEntity<ApiResponse<InventoryItemDTO>> toggleProductEnabled(
            @PathVariable Integer storeId,
            @PathVariable Long productId,
            @RequestParam Boolean isEnabled) {
        log.info("Toggling product {} enabled status to {} in store {}", productId, isEnabled, storeId);
        InventoryItemDTO inventoryItem = warehouseService.toggleProductEnabled(storeId, productId, isEnabled);
        return ResponseEntity.ok(ApiResponse.<InventoryItemDTO>builder()
                .statusCode(200)
                .message(isEnabled ? "Product enabled successfully" : "Product disabled successfully")
                .data(inventoryItem)
                .build());
    }
}
