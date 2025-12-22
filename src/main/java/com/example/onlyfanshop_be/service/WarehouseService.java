package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.InventoryItemDTO;
import com.example.onlyfanshop_be.dto.WarehouseDTO;
import com.example.onlyfanshop_be.entity.InventoryItem;
import com.example.onlyfanshop_be.entity.InventoryLog;
import com.example.onlyfanshop_be.entity.Product;
import com.example.onlyfanshop_be.entity.Warehouse;
import com.example.onlyfanshop_be.enums.WarehouseType;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.InventoryItemRepository;
import com.example.onlyfanshop_be.repository.InventoryLogRepository;
import com.example.onlyfanshop_be.repository.ProductRepository;
import com.example.onlyfanshop_be.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of WarehouseService
 * Handles warehouse management operations including inventory tracking and logging
 * Requirements: 2.3, 2.4, 2.5, 3.5
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WarehouseService implements IWarehouseService {
    
    private final WarehouseRepository warehouseRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final ProductRepository productRepository;
    private final IDebtOrderService debtOrderService;

    /**
     * Get the Main Warehouse with all inventory items
     * Requirements: 2.4 - WHEN Admin queries Main_Warehouse inventory THEN the System SHALL return all Inventory_Items with current quantities
     */
    @Override
    public WarehouseDTO getMainWarehouse() {
        Warehouse mainWarehouse = warehouseRepository.findFirstByType(WarehouseType.MAIN)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        
        return convertToWarehouseDTO(mainWarehouse);
    }
    
    /**
     * Get a Store Warehouse by store ID
     */
    @Override
    public WarehouseDTO getStoreWarehouse(Integer storeId) {
        if (storeId == null) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
        
        Warehouse storeWarehouse = warehouseRepository.findByStoreId(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        
        return convertToWarehouseDTO(storeWarehouse);
    }
    
    /**
     * Update quantity for a product in Main Warehouse
     * Requirements: 2.3 - WHEN Admin updates quantity for a product in Main_Warehouse THEN the System SHALL persist the new quantity immediately
     * Requirements: 2.5 - WHEN quantity in Main_Warehouse changes THEN the System SHALL log the change
     */
    @Override
    @Transactional
    public InventoryItemDTO updateMainWarehouseQuantity(Long productId, Integer quantity, String reason) {
        if (productId == null || quantity == null) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
        
        if (quantity < 0) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
        
        // Get Main Warehouse
        Warehouse mainWarehouse = warehouseRepository.findFirstByType(WarehouseType.MAIN)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        
        // Verify product exists
        Product product = findProductById(productId);
        
        // Get or create inventory item
        InventoryItem inventoryItem = inventoryItemRepository
                .findByWarehouseIdAndProductId(mainWarehouse.getId(), productId)
                .orElseGet(() -> createNewInventoryItem(mainWarehouse.getId(), productId));
        
        // Store previous quantity for logging
        Integer previousQuantity = inventoryItem.getQuantity();
        
        // Update quantity
        inventoryItem.setQuantity(quantity);
        InventoryItem savedItem = inventoryItemRepository.save(inventoryItem);
        
        // Create inventory log entry (Requirements 2.5)
        createInventoryLog(mainWarehouse.getId(), productId, previousQuantity, quantity, reason);
        
        log.info("Updated Main Warehouse inventory for product {}: {} -> {}", 
                productId, previousQuantity, quantity);
        
        // Check for fulfillable debt orders when quantity increases (Requirements 6.3, 6.4)
        if (quantity > previousQuantity) {
            try {
                var fulfillableOrders = debtOrderService.checkFulfillableDebtOrders();
                if (!fulfillableOrders.isEmpty()) {
                    log.info("Found {} fulfillable debt orders after inventory update", fulfillableOrders.size());
                }
            } catch (Exception e) {
                log.warn("Error checking fulfillable debt orders: {}", e.getMessage());
            }
        }
        
        return convertToInventoryItemDTO(savedItem, product, mainWarehouse);
    }

    /**
     * Add a product to a Store Warehouse with zero quantity
     * Requirements: 3.5 - WHEN Admin adds a product to Store_Warehouse THEN the System SHALL create an Inventory_Item with zero quantity
     */
    @Override
    @Transactional
    public InventoryItemDTO addProductToStoreWarehouse(Integer storeId, Long productId) {
        if (storeId == null || productId == null) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
        
        // Get Store Warehouse
        Warehouse storeWarehouse = warehouseRepository.findByStoreId(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        
        // Verify it's a store warehouse
        if (storeWarehouse.getType() != WarehouseType.STORE) {
            throw new AppException(ErrorCode.INVALID_WAREHOUSE_TYPE);
        }
        
        // Verify product exists
        Product product = findProductById(productId);
        
        // Check if inventory item already exists
        if (inventoryItemRepository.existsByWarehouseIdAndProductId(storeWarehouse.getId(), productId)) {
            // Return existing item
            InventoryItem existingItem = inventoryItemRepository
                    .findByWarehouseIdAndProductId(storeWarehouse.getId(), productId)
                    .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_INVENTORY_NOT_FOUND));
            return convertToInventoryItemDTO(existingItem, product, storeWarehouse);
        }
        
        // Create new inventory item with zero quantity
        InventoryItem inventoryItem = InventoryItem.builder()
                .warehouseId(storeWarehouse.getId())
                .productId(productId)
                .quantity(0)
                .reservedQuantity(0)
                .build();
        
        InventoryItem savedItem = inventoryItemRepository.save(inventoryItem);
        
        log.info("Added product {} to Store Warehouse {} (Store ID: {}) with quantity 0", 
                productId, storeWarehouse.getId(), storeId);
        
        return convertToInventoryItemDTO(savedItem, product, storeWarehouse);
    }
    
    /**
     * Create an InventoryItem in Main Warehouse for a new product
     * Requirements: 2.2 - WHEN a new product is added to Product_Catalog THEN the System SHALL automatically create an InventoryItem in Main_Warehouse with zero quantity
     */
    @Override
    @Transactional
    public void createMainWarehouseInventoryItem(Long productId) {
        if (productId == null) {
            log.warn("Cannot create inventory item - productId is null");
            return;
        }
        
        try {
            // Find Main Warehouse
            Warehouse mainWarehouse = warehouseRepository.findFirstByType(WarehouseType.MAIN)
                    .orElse(null);
            
            if (mainWarehouse == null) {
                log.warn("Main Warehouse not found - cannot create inventory item for product {}", productId);
                return;
            }
            
            // Check if inventory item already exists
            if (inventoryItemRepository.existsByWarehouseIdAndProductId(mainWarehouse.getId(), productId)) {
                log.info("InventoryItem already exists for product {} in Main Warehouse", productId);
                return;
            }
            
            // Create new inventory item with zero quantity
            InventoryItem inventoryItem = InventoryItem.builder()
                    .warehouseId(mainWarehouse.getId())
                    .productId(productId)
                    .quantity(0)
                    .reservedQuantity(0)
                    .build();
            
            inventoryItemRepository.save(inventoryItem);
            
            log.info("Created InventoryItem for product {} in Main Warehouse (ID: {}) with quantity 0", 
                    productId, mainWarehouse.getId());
        } catch (Exception e) {
            log.error("Error creating inventory item for product {}: {}", productId, e.getMessage());
            // Don't throw exception to not affect product creation
        }
    }

    // ==================== Private Helper Methods ====================
    
    /**
     * Find product by ID (handles Long to Integer conversion)
     */
    private Product findProductById(Long productId) {
        if (productId == null) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }
        return productRepository.findById(productId.intValue())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOTEXISTED));
    }
    
    /**
     * Create a new InventoryItem with zero quantity
     */
    private InventoryItem createNewInventoryItem(Long warehouseId, Long productId) {
        InventoryItem item = InventoryItem.builder()
                .warehouseId(warehouseId)
                .productId(productId)
                .quantity(0)
                .reservedQuantity(0)
                .build();
        return inventoryItemRepository.save(item);
    }
    
    /**
     * Create an inventory log entry for quantity changes
     * Requirements: 2.5 - WHEN quantity in Main_Warehouse changes THEN the System SHALL log the change
     */
    private void createInventoryLog(Long warehouseId, Long productId, 
                                    Integer previousQuantity, Integer newQuantity, String reason) {
        Long userId = getCurrentUserId();
        
        InventoryLog log = InventoryLog.builder()
                .warehouseId(warehouseId)
                .productId(productId)
                .previousQuantity(previousQuantity)
                .newQuantity(newQuantity)
                .reason(reason != null ? reason : "Manual update")
                .userId(userId)
                .build();
        
        inventoryLogRepository.save(log);
        
        WarehouseService.log.debug("Created inventory log: warehouse={}, product={}, {} -> {}, reason={}", 
                warehouseId, productId, previousQuantity, newQuantity, reason);
    }
    
    /**
     * Get current user ID from security context
     */
    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
                org.springframework.security.core.userdetails.UserDetails userDetails = 
                        (org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal();
                // Try to extract user ID from username or other means
                // This depends on your UserDetails implementation
                return null; // Return null if cannot determine user ID
            }
        } catch (Exception e) {
            log.warn("Could not get current user ID: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Convert Warehouse entity to WarehouseDTO
     */
    private WarehouseDTO convertToWarehouseDTO(Warehouse warehouse) {
        List<InventoryItem> inventoryItems = inventoryItemRepository.findByWarehouseId(warehouse.getId());
        
        List<InventoryItemDTO> inventoryItemDTOs = inventoryItems.stream()
                .map(item -> {
                    Product product = null;
                    try {
                        product = productRepository.findById(item.getProductId().intValue()).orElse(null);
                    } catch (Exception e) {
                        log.warn("Could not load product {}: {}", item.getProductId(), e.getMessage());
                    }
                    return convertToInventoryItemDTO(item, product, warehouse);
                })
                .collect(Collectors.toList());
        
        String storeName = null;
        if (warehouse.getStore() != null) {
            storeName = warehouse.getStore().getName();
        }
        
        return WarehouseDTO.builder()
                .id(warehouse.getId())
                .name(warehouse.getName())
                .type(warehouse.getType())
                .storeId(warehouse.getStoreId())
                .storeName(storeName)
                .address(warehouse.getAddress())
                .phone(warehouse.getPhone())
                .createdAt(warehouse.getCreatedAt())
                .inventoryItems(inventoryItemDTOs)
                .build();
    }
    
    /**
     * Convert InventoryItem entity to InventoryItemDTO
     */
    private InventoryItemDTO convertToInventoryItemDTO(InventoryItem item, Product product, Warehouse warehouse) {
        return InventoryItemDTO.builder()
                .id(item.getId())
                .warehouseId(item.getWarehouseId())
                .warehouseName(warehouse != null ? warehouse.getName() : null)
                .productId(item.getProductId())
                .productName(product != null ? product.getName() : null)
                .productSku(product != null ? product.getSku() : null)
                .quantity(item.getQuantity())
                .reservedQuantity(item.getReservedQuantity())
                .availableQuantity(item.getAvailableQuantity())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
