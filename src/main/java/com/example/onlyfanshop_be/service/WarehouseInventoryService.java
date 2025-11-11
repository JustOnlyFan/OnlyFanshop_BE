package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.WarehouseInventoryDTO;
import com.example.onlyfanshop_be.entity.Product;
import com.example.onlyfanshop_be.entity.ProductVariant;
import com.example.onlyfanshop_be.entity.Warehouse;
import com.example.onlyfanshop_be.entity.WarehouseInventory;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.ProductRepository;
import com.example.onlyfanshop_be.repository.ProductVariantRepository;
import com.example.onlyfanshop_be.repository.WarehouseInventoryRepository;
import com.example.onlyfanshop_be.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseInventoryService {
    private final WarehouseInventoryRepository warehouseInventoryRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    /**
     * Get inventory for a specific warehouse, product, and variant
     */
    public WarehouseInventory getInventory(Integer warehouseId, Long productId, Long productVariantId) {
        Optional<WarehouseInventory> inventory = warehouseInventoryRepository
                .findByWarehouseIdAndProductIdAndProductVariantId(warehouseId, productId, productVariantId);
        
        if (inventory.isPresent()) {
            return inventory.get();
        }
        
        // Validate warehouse exists
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new AppException(ErrorCode.WAREHOUSE_NOT_FOUND);
        }
        
        // Validate product exists
        // Note: ProductRepository uses Integer but Product entity uses Long
        if (!productRepository.existsById(productId.intValue())) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }
        
        WarehouseInventory newInventory = WarehouseInventory.builder()
                .warehouseId(warehouseId)
                .productId(productId)
                .productVariantId(productVariantId)
                .quantityInStock(0)
                .updatedAt(LocalDateTime.now())
                .build();
        
        return warehouseInventoryRepository.save(newInventory);
    }

    /**
     * Add quantity to inventory
     */
    @Transactional
    public WarehouseInventory addQuantity(Integer warehouseId, Long productId, Long productVariantId, Integer quantity) {
        WarehouseInventory inventory = getInventory(warehouseId, productId, productVariantId);
        inventory.setQuantityInStock(inventory.getQuantityInStock() + quantity);
        inventory.setUpdatedAt(LocalDateTime.now());
        return warehouseInventoryRepository.save(inventory);
    }

    /**
     * Reduce quantity from inventory
     */
    @Transactional
    public WarehouseInventory reduceQuantity(Integer warehouseId, Long productId, Long productVariantId, Integer quantity) {
        WarehouseInventory inventory = getInventory(warehouseId, productId, productVariantId);
        
        if (inventory.getQuantityInStock() < quantity) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
        }
        
        inventory.setQuantityInStock(inventory.getQuantityInStock() - quantity);
        inventory.setUpdatedAt(LocalDateTime.now());
        return warehouseInventoryRepository.save(inventory);
    }

    /**
     * Set quantity in inventory
     */
    @Transactional
    public WarehouseInventory setQuantity(Integer warehouseId, Long productId, Long productVariantId, Integer quantity) {
        WarehouseInventory inventory = getInventory(warehouseId, productId, productVariantId);
        inventory.setQuantityInStock(quantity);
        inventory.setUpdatedAt(LocalDateTime.now());
        return warehouseInventoryRepository.save(inventory);
    }

    /**
     * Get all inventory for a warehouse
     */
    @Transactional(readOnly = true)
    public List<WarehouseInventoryDTO> getWarehouseInventory(Integer warehouseId) {
        List<WarehouseInventory> inventories = warehouseInventoryRepository.findByWarehouseId(warehouseId);
        return inventories.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get all inventory for a product
     */
    @Transactional(readOnly = true)
    public List<WarehouseInventoryDTO> getProductInventory(Long productId) {
        List<WarehouseInventory> inventories = warehouseInventoryRepository.findByProductId(productId);
        return inventories.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get total quantity across all warehouses for a product
     */
    public Integer getTotalQuantity(Long productId, Long productVariantId) {
        Integer total = warehouseInventoryRepository.getTotalQuantityByProductAndVariant(productId, productVariantId);
        return total != null ? total : 0;
    }

    /**
     * Check if warehouse has sufficient stock
     */
    public boolean hasSufficientStock(Integer warehouseId, Long productId, Long productVariantId, Integer quantity) {
        Optional<WarehouseInventory> inventory = warehouseInventoryRepository
                .findByWarehouseIdAndProductIdAndProductVariantId(warehouseId, productId, productVariantId);
        
        if (inventory.isEmpty()) {
            return false;
        }
        
        return inventory.get().getQuantityInStock() >= quantity;
    }

    private WarehouseInventoryDTO convertToDTO(WarehouseInventory inventory) {
        try {
            Warehouse warehouse = inventory.getWarehouse();
            Product product = inventory.getProduct();
            ProductVariant variant = inventory.getProductVariant();
            
            return WarehouseInventoryDTO.builder()
                    .id(inventory.getId())
                    .warehouseId(inventory.getWarehouseId())
                    .warehouseName(warehouse != null ? warehouse.getName() : null)
                    .warehouseCode(warehouse != null ? warehouse.getCode() : null)
                    .productId(inventory.getProductId())
                    .productName(product != null ? product.getName() : null)
                    .productVariantId(inventory.getProductVariantId())
                    .productVariantName(variant != null ? variant.getVariantName() : null)
                    .quantityInStock(inventory.getQuantityInStock())
                    .updatedAt(inventory.getUpdatedAt())
                    .build();
        } catch (Exception e) {
            // Fallback if lazy loading fails
            return WarehouseInventoryDTO.builder()
                    .id(inventory.getId())
                    .warehouseId(inventory.getWarehouseId())
                    .warehouseName(null)
                    .warehouseCode(null)
                    .productId(inventory.getProductId())
                    .productName(null)
                    .productVariantId(inventory.getProductVariantId())
                    .productVariantName(null)
                    .quantityInStock(inventory.getQuantityInStock())
                    .updatedAt(inventory.getUpdatedAt())
                    .build();
        }
    }
}

