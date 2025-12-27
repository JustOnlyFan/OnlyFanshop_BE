package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.entity.InventoryTransaction;
import com.example.onlyfanshop_be.entity.StoreInventory;
import com.example.onlyfanshop_be.enums.InventoryLocationType;
import com.example.onlyfanshop_be.enums.InventoryTransactionType;
import com.example.onlyfanshop_be.repository.InventoryTransactionRepository;
import com.example.onlyfanshop_be.repository.ProductRepository;
import com.example.onlyfanshop_be.repository.StoreInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryTransactionService {
    private final InventoryTransactionRepository transactionRepository;
    private final ProductRepository productRepository;
    private final StoreInventoryRepository storeInventoryRepository;

    @Transactional
    public InventoryTransaction recordSale(Long productId, Integer storeId, Integer quantity,
                                           Long orderId, Long performedBy) {
        StoreInventory storeInventory = storeInventoryRepository.findByStoreIdAndProductId(storeId, productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tồn kho cửa hàng"));

        if (storeInventory.getQuantity() < quantity) {
            throw new RuntimeException("Kho cửa hàng không đủ hàng. Tồn kho: " + storeInventory.getQuantity());
        }

        Integer quantityBefore = storeInventory.getQuantity();

        storeInventory.setQuantity(storeInventory.getQuantity() - quantity);
        storeInventoryRepository.save(storeInventory);

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

    @Transactional
    public InventoryTransaction importToStore(Long productId, Integer storeId, Integer quantity, Long performedBy, String note) {
        StoreInventory storeInventory = storeInventoryRepository.findByStoreIdAndProductId(storeId, productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tồn kho cửa hàng"));

        Integer quantityBefore = storeInventory.getQuantity();

        storeInventory.setQuantity(storeInventory.getQuantity() + quantity);
        storeInventoryRepository.save(storeInventory);

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

    @Transactional
    public InventoryTransaction adjustInventory(Long productId, Integer storeId, Integer newQuantity,
                                                 Long performedBy, String note) {
        if (storeId == null) {
            throw new IllegalArgumentException("storeId is required for inventory adjustments.");
        }

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

    @Transactional
    public InventoryTransaction transferBetweenStores(Long productId, Integer sourceStoreId, Integer destStoreId,
                                                       Integer quantity, Long performedBy, String note) {
        StoreInventory sourceInventory = storeInventoryRepository.findByStoreIdAndProductId(sourceStoreId, productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tồn kho cửa hàng nguồn"));

        if (sourceInventory.getQuantity() < quantity) {
            throw new RuntimeException("Kho nguồn không đủ hàng. Tồn kho: " + sourceInventory.getQuantity());
        }

        StoreInventory destInventory = storeInventoryRepository.findByStoreIdAndProductId(destStoreId, productId)
                .orElseGet(() -> {
                    StoreInventory newInventory = new StoreInventory();
                    newInventory.setStoreId(destStoreId);
                    newInventory.setProductId(productId);
                    newInventory.setQuantity(0);
                    return storeInventoryRepository.save(newInventory);
                });

        Integer sourceQuantityBefore = sourceInventory.getQuantity();
        Integer destQuantityBefore = destInventory.getQuantity();

        sourceInventory.setQuantity(sourceInventory.getQuantity() - quantity);
        destInventory.setQuantity(destInventory.getQuantity() + quantity);

        storeInventoryRepository.save(sourceInventory);
        storeInventoryRepository.save(destInventory);

        return transactionRepository.save(InventoryTransaction.builder()
                .transactionType(InventoryTransactionType.TRANSFER)
                .productId(productId)
                .quantity(quantity)
                .sourceType(InventoryLocationType.STORE)
                .sourceStoreId(sourceStoreId)
                .destinationType(InventoryLocationType.STORE)
                .destinationStoreId(destStoreId)
                .performedBy(performedBy)
                .quantityBefore(sourceQuantityBefore)
                .quantityAfter(sourceInventory.getQuantity())
                .note(note != null ? note : "Chuyển kho từ cửa hàng #" + sourceStoreId + " đến cửa hàng #" + destStoreId)
                .build());
    }

    public List<InventoryTransaction> getProductTransactions(Long productId) {
        return transactionRepository.findByProductIdOrderByCreatedAtDesc(productId);
    }

    public List<InventoryTransaction> getStoreTransactions(Integer storeId) {
        return transactionRepository.findByStoreId(storeId);
    }

    public List<InventoryTransaction> getRequestTransactions(Long requestId) {
        return transactionRepository.findByRequestIdOrderByCreatedAtDesc(requestId);
    }
}
