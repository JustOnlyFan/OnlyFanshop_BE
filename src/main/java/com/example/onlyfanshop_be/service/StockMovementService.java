package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.entity.StockMovement;
import com.example.onlyfanshop_be.enums.StockMovementType;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StockMovementService {
    private final StockMovementRepository stockMovementRepository;
    private final WarehouseInventoryService warehouseInventoryService;

    /**
     * Create stock movement record
     */
    @Transactional
    public StockMovement createStockMovement(
            Integer warehouseId,
            Long productId,
            Long productVariantId,
            StockMovementType type,
            Integer quantity,
            String note,
            Long createdBy,
            Long orderId,
            Integer fromWarehouseId,
            Integer toWarehouseId) {
        
        StockMovement movement = StockMovement.builder()
                .warehouseId(warehouseId)
                .productId(productId)
                .productVariantId(productVariantId)
                .type(type.getDbValue())
                .quantity(quantity)
                .note(note)
                .createdBy(createdBy)
                .orderId(orderId)
                .fromWarehouseId(fromWarehouseId)
                .toWarehouseId(toWarehouseId)
                .createdAt(LocalDateTime.now())
                .build();
        
        return stockMovementRepository.save(movement);
    }

    /**
     * Record import (add stock)
     */
    @Transactional
    public StockMovement recordImport(
            Integer warehouseId,
            Long productId,
            Long productVariantId,
            Integer quantity,
            String note,
            Long createdBy) {
        
        // Add to inventory
        warehouseInventoryService.addQuantity(warehouseId, productId, productVariantId, quantity);
        
        // Create movement record
        return createStockMovement(
                warehouseId,
                productId,
                productVariantId,
                StockMovementType.IMPORT,
                quantity,
                note,
                createdBy,
                null,
                null,
                null
        );
    }

    /**
     * Record export (reduce stock)
     */
    @Transactional
    public StockMovement recordExport(
            Integer warehouseId,
            Long productId,
            Long productVariantId,
            Integer quantity,
            String note,
            Long createdBy,
            Long orderId) {
        
        // Reduce from inventory
        warehouseInventoryService.reduceQuantity(warehouseId, productId, productVariantId, quantity);
        
        // Create movement record
        return createStockMovement(
                warehouseId,
                productId,
                productVariantId,
                StockMovementType.EXPORT,
                quantity,
                note,
                createdBy,
                orderId,
                null,
                null
        );
    }

    /**
     * Record transfer between warehouses
     */
    @Transactional
    public StockMovement recordTransfer(
            Integer fromWarehouseId,
            Integer toWarehouseId,
            Long productId,
            Long productVariantId,
            Integer quantity,
            String note,
            Long createdBy) {
        
        // Validate warehouses are different
        if (fromWarehouseId.equals(toWarehouseId)) {
            throw new AppException(ErrorCode.INVALID_WAREHOUSE_TRANSFER);
        }
        
        // Reduce from source warehouse
        warehouseInventoryService.reduceQuantity(fromWarehouseId, productId, productVariantId, quantity);
        
        // Add to destination warehouse
        warehouseInventoryService.addQuantity(toWarehouseId, productId, productVariantId, quantity);
        
        // Create movement record (recorded at destination warehouse)
        return createStockMovement(
                toWarehouseId,
                productId,
                productVariantId,
                StockMovementType.TRANSFER,
                quantity,
                note,
                createdBy,
                null,
                fromWarehouseId,
                toWarehouseId
        );
    }

    /**
     * Record adjustment (manual correction)
     */
    @Transactional
    public StockMovement recordAdjustment(
            Integer warehouseId,
            Long productId,
            Long productVariantId,
            Integer quantity,
            String note,
            Long createdBy) {
        
        // Get current inventory
        var inventory = warehouseInventoryService.getInventory(warehouseId, productId, productVariantId);
        int currentQuantity = inventory.getQuantityInStock();
        
        // Set new quantity
        warehouseInventoryService.setQuantity(warehouseId, productId, productVariantId, quantity);
        
        // Calculate difference for movement record
        int difference = quantity - currentQuantity;
        
        // Create movement record
        return createStockMovement(
                warehouseId,
                productId,
                productVariantId,
                StockMovementType.ADJUSTMENT,
                difference,
                note,
                createdBy,
                null,
                null,
                null
        );
    }

    /**
     * Get movements by warehouse
     */
    public List<StockMovement> getMovementsByWarehouse(Integer warehouseId) {
        return stockMovementRepository.findByWarehouseId(warehouseId);
    }

    /**
     * Get movements by product
     */
    public List<StockMovement> getMovementsByProduct(Long productId) {
        return stockMovementRepository.findByProductId(productId);
    }

    /**
     * Get transfer movements
     */
    public List<StockMovement> getTransferMovements(Integer warehouseId) {
        return stockMovementRepository.findByTypeAndWarehouse(
                StockMovementType.TRANSFER.getDbValue(),
                warehouseId
        );
    }
}








