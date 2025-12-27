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

@Service
@RequiredArgsConstructor
@Slf4j
public class WarehouseService implements IWarehouseService {
    
    private final WarehouseRepository warehouseRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final ProductRepository productRepository;

    @Override
    public WarehouseDTO getStoreWarehouse(Integer storeId) {
        if (storeId == null) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        Warehouse storeWarehouse = warehouseRepository.findByStoreIdAndIsActiveTrue(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));

        return convertToWarehouseDTO(storeWarehouse);
    }

    @Override
    public List<WarehouseDTO> getAllActiveWarehouses() {
        List<Warehouse> activeWarehouses = warehouseRepository.findByIsActiveTrue();
        
        return activeWarehouses.stream()
                .map(this::convertToWarehouseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public InventoryItemDTO updateStoreWarehouseQuantity(Integer storeId, Long productId, Integer quantity, String reason) {
        if (storeId == null || productId == null || quantity == null) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
        
        if (quantity < 0) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        Warehouse storeWarehouse = warehouseRepository.findByStoreIdAndIsActiveTrue(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));

        Product product = findProductById(productId);

        InventoryItem inventoryItem = inventoryItemRepository
                .findByWarehouseIdAndProductId(storeWarehouse.getId(), productId)
                .orElseGet(() -> createNewInventoryItem(storeWarehouse.getId(), productId));

        Integer previousQuantity = inventoryItem.getQuantity();

        inventoryItem.setQuantity(quantity);
        InventoryItem savedItem = inventoryItemRepository.save(inventoryItem);

        createInventoryLog(storeWarehouse.getId(), productId, previousQuantity, quantity, reason);
        
        log.info("Updated Store Warehouse inventory for store {} product {}: {} -> {}", 
                storeId, productId, previousQuantity, quantity);
        
        return convertToInventoryItemDTO(savedItem, product, storeWarehouse);
    }

    @Override
    @Transactional
    public InventoryItemDTO addProductToStoreWarehouse(Integer storeId, Long productId, Integer quantity) {
        if (storeId == null || productId == null) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        int initialQuantity = (quantity != null && quantity >= 0) ? quantity : 0;

        Warehouse storeWarehouse = warehouseRepository.findByStoreIdAndIsActiveTrue(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));

        if (storeWarehouse.getType() != WarehouseType.STORE) {
            throw new AppException(ErrorCode.INVALID_WAREHOUSE_TYPE);
        }

        Product product = findProductById(productId);

        if (inventoryItemRepository.existsByWarehouseIdAndProductId(storeWarehouse.getId(), productId)) {
            InventoryItem existingItem = inventoryItemRepository
                    .findByWarehouseIdAndProductId(storeWarehouse.getId(), productId)
                    .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_INVENTORY_NOT_FOUND));
            return convertToInventoryItemDTO(existingItem, product, storeWarehouse);
        }

        InventoryItem inventoryItem = InventoryItem.builder()
                .warehouseId(storeWarehouse.getId())
                .productId(productId)
                .quantity(initialQuantity)
                .reservedQuantity(0)
                .build();
        
        InventoryItem savedItem = inventoryItemRepository.save(inventoryItem);

        if (initialQuantity > 0) {
            createInventoryLog(storeWarehouse.getId(), productId, 0, initialQuantity, "Initial stock added");
        }
        
        log.info("Added product {} to Store Warehouse {} (Store ID: {}) with quantity {}", 
                productId, storeWarehouse.getId(), storeId, initialQuantity);
        
        return convertToInventoryItemDTO(savedItem, product, storeWarehouse);
    }

    @Override
    @Transactional
    public void deactivateWarehouse(Long warehouseId) {
        if (warehouseId == null) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }
        
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        
        if (!warehouse.getIsActive()) {
            log.info("Warehouse {} is already inactive", warehouseId);
            return;
        }
        
        warehouse.setIsActive(false);
        warehouseRepository.save(warehouse);
        
        log.info("Deactivated warehouse {} ({})", warehouseId, warehouse.getName());
    }

    private Product findProductById(Long productId) {
        if (productId == null) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }
        return productRepository.findById(productId.intValue())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOTEXISTED));
    }

    private InventoryItem createNewInventoryItem(Long warehouseId, Long productId) {
        InventoryItem item = InventoryItem.builder()
                .warehouseId(warehouseId)
                .productId(productId)
                .quantity(0)
                .reservedQuantity(0)
                .build();
        return inventoryItemRepository.save(item);
    }

    private void createInventoryLog(Long warehouseId, Long productId, 
                                    Integer previousQuantity, Integer newQuantity, String reason) {
        Long userId = getCurrentUserId();
        
        InventoryLog inventoryLog = InventoryLog.builder()
                .warehouseId(warehouseId)
                .productId(productId)
                .previousQuantity(previousQuantity)
                .newQuantity(newQuantity)
                .reason(reason != null ? reason : "Manual update")
                .userId(userId)
                .build();
        
        inventoryLogRepository.save(inventoryLog);
        
        log.debug("Created inventory log: warehouse={}, product={}, {} -> {}, reason={}", 
                warehouseId, productId, previousQuantity, newQuantity, reason);
    }

    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
                org.springframework.security.core.userdetails.UserDetails userDetails = 
                        (org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal();
                return null;
            }
        } catch (Exception e) {
            log.warn("Could not get current user ID: {}", e.getMessage());
        }
        return null;
    }

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
                .isActive(warehouse.getIsActive())
                .address(warehouse.getAddress())
                .phone(warehouse.getPhone())
                .createdAt(warehouse.getCreatedAt())
                .inventoryItems(inventoryItemDTOs)
                .build();
    }

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
