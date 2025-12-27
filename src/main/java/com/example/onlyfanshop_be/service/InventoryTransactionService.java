package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.entity.InventoryTransaction;
import com.example.onlyfanshop_be.entity.Product;
import com.example.onlyfanshop_be.entity.StoreInventory;
import com.example.onlyfanshop_be.enums.InventoryLocationType;
import com.example.onlyfanshop_be.enums.InventoryTransactionType;
import com.example.onlyfanshop_be.repository.InventoryTransactionRepository;
import com.example.onlyfanshop_be.repository.ProductRepository;
import com.example.onlyfanshop_be.repository.StoreInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * InventoryTransactionService - Handles inventory transaction logging
 * Updated to only support Store Warehouses (Main Warehouse/CENTRAL removed)
 * Requirements: 1.1 - THE System SHALL only support Store_Warehouse type for all warehouses
 */
@Service
@RequiredArgsConstructor
public class InventoryTransactionService {
    private final InventoryTransactionRepository transactionRepository;
    private final ProductRepository productRepository;
    private final StoreInventoryRepository storeInventoryRepository;

    /**
     * Transfer inventory between store warehouses
     * @deprecated Use FulfillmentService for transfer operations
     * Main Warehouse has been removed - transfers now happen between Store Warehouses
     */
    @Deprecated
    @Transactional
    public InventoryTransaction transferToStore(Long productId, Integer storeId, Integer quantity, 
                                                 Long requestId, Long performedBy, String note) {
        throw new UnsupportedOperationException(
            "transferToStore is deprecated. Main Warehouse has been removed. " +
            "Use FulfillmentService for store-to-store transfers.");
    }

    /**
     * @deprecated Main Warehouse has been removed from the system.
     * Use store-to-store transfers instead.
     */
    @Deprecated
    @Transactional
    public InventoryTransaction returnToCentral(Long productId, Integer storeId, Integer quantity,
                                                 Long performedBy, String note) {
        throw new UnsupportedOperationException(
            "returnToCentral is deprecated. Main Warehouse has been removed. " +
            "Use store-to-store transfers instead.");
    }

    /**
     * Ghi nhận bán hàng (trừ kho cửa hàng)
     */
    @Transactional
    public InventoryTransaction recordSale(Long productId, Integer storeId, Integer quantity,
                                           Long orderId, Long performedBy) {
        StoreInventory storeInventory = storeInventoryRepository.findByStoreIdAndProductId(storeId, productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tồn kho cửa hàng"));

        if (storeInventory.getQuantity() < quantity) {
            throw new RuntimeException("Kho cửa hàng không đủ hàng. Tồn kho: " + storeInventory.getQuantity());
        }

        Integer quantityBefore = storeInventory.getQuantity();

        // Trừ kho cửa hàng
        storeInventory.setQuantity(storeInventory.getQuantity() - quantity);
        storeInventoryRepository.save(storeInventory);

        // Ghi transaction
        InventoryTransaction transaction = InventoryTransaction.builder()
                .transactionType(InventoryTransactionType.SALE)
                .productId(productId)
                .quantity(quantity)
                .sourceType(InventoryLocationType.STORE)
                .sourceStoreId(storeId)
                .destinationType(InventoryLocationType.STORE)
                .destinationStoreId(storeId)
                .orderId(orderId)
                .performedBy(performedBy)
                .quantityBefore(quantityBefore)
                .quantityAfter(storeInventory.getQuantity())
                .note("Bán hàng - Order #" + orderId)
                .build();

        return transactionRepository.save(transaction);
    }

    /**
     * Import inventory to a store warehouse
     * Updated to import directly to store warehouses (Main Warehouse removed)
     */
    @Transactional
    public InventoryTransaction importToStore(Long productId, Integer storeId, Integer quantity, Long performedBy, String note) {
        StoreInventory storeInventory = storeInventoryRepository.findByStoreIdAndProductId(storeId, productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tồn kho cửa hàng"));

        Integer quantityBefore = storeInventory.getQuantity();

        // Cộng kho cửa hàng
        storeInventory.setQuantity(storeInventory.getQuantity() + quantity);
        storeInventoryRepository.save(storeInventory);

        // Ghi transaction
        InventoryTransaction transaction = InventoryTransaction.builder()
                .transactionType(InventoryTransactionType.IMPORT)
                .productId(productId)
                .quantity(quantity)
                .sourceType(InventoryLocationType.STORE)
                .sourceStoreId(storeId)
                .destinationType(InventoryLocationType.STORE)
                .destinationStoreId(storeId)
                .performedBy(performedBy)
                .quantityBefore(quantityBefore)
                .quantityAfter(storeInventory.getQuantity())
                .note(note)
                .build();

        return transactionRepository.save(transaction);
    }

    /**
     * @deprecated Use importToStore instead. Main Warehouse has been removed.
     */
    @Deprecated
    @Transactional
    public InventoryTransaction importToCentral(Long productId, Integer quantity, Long performedBy, String note) {
        throw new UnsupportedOperationException(
            "importToCentral is deprecated. Main Warehouse has been removed. " +
            "Use importToStore() to import directly to store warehouses.");
    }

    /**
     * Điều chỉnh tồn kho (kiểm kê) - Only for store warehouses
     * Updated: storeId is now required (Main Warehouse removed)
     */
    @Transactional
    public InventoryTransaction adjustInventory(Long productId, Integer storeId, Integer newQuantity,
                                                 Long performedBy, String note) {
        if (storeId == null) {
            throw new UnsupportedOperationException(
                "storeId is required. Main Warehouse has been removed. " +
                "Inventory adjustments must be done on store warehouses.");
        }
        
        // Điều chỉnh kho cửa hàng
        StoreInventory storeInventory = storeInventoryRepository.findByStoreIdAndProductId(storeId, productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tồn kho cửa hàng"));

        Integer quantityBefore = storeInventory.getQuantity();
        int difference = newQuantity - quantityBefore;

        storeInventory.setQuantity(newQuantity);
        storeInventoryRepository.save(storeInventory);

        return transactionRepository.save(InventoryTransaction.builder()
                .transactionType(InventoryTransactionType.ADJUSTMENT)
                .productId(productId)
                .quantity(Math.abs(difference))
                .sourceType(InventoryLocationType.STORE)
                .sourceStoreId(storeId)
                .destinationType(InventoryLocationType.STORE)
                .destinationStoreId(storeId)
                .performedBy(performedBy)
                .quantityBefore(quantityBefore)
                .quantityAfter(newQuantity)
                .note(note + " (Chênh lệch: " + (difference >= 0 ? "+" : "") + difference + ")")
                .build());
    }

    /**
     * Lấy lịch sử giao dịch của product
     */
    public List<InventoryTransaction> getProductTransactions(Long productId) {
        return transactionRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    /**
     * Lấy lịch sử giao dịch của store
     */
    public List<InventoryTransaction> getStoreTransactions(Integer storeId) {
        return transactionRepository.findByStoreId(storeId);
    }

    /**
     * Lấy lịch sử giao dịch theo request
     */
    public List<InventoryTransaction> getRequestTransactions(Long requestId) {
        return transactionRepository.findByRequestIdOrderByCreatedAtDesc(requestId);
    }
}
