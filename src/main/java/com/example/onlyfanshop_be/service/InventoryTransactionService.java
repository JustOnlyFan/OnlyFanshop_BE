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

@Service
@RequiredArgsConstructor
public class InventoryTransactionService {
    private final InventoryTransactionRepository transactionRepository;
    private final ProductRepository productRepository;
    private final StoreInventoryRepository storeInventoryRepository;

    /**
     * Chuyển hàng từ kho tổng sang kho cửa hàng
     */
    @Transactional
    public InventoryTransaction transferToStore(Long productId, Integer storeId, Integer quantity, 
                                                 Long requestId, Long performedBy, String note) {
        // Lấy thông tin product (kho tổng)
        Product product = productRepository.findById(productId.intValue())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        if (product.getQuantity() < quantity) {
            throw new RuntimeException("Kho tổng không đủ hàng. Tồn kho: " + product.getQuantity());
        }

        // Lấy thông tin store inventory
        StoreInventory storeInventory = storeInventoryRepository.findByStoreIdAndProductId(storeId, productId)
                .orElseThrow(() -> new RuntimeException("Cửa hàng chưa được cấu hình cho sản phẩm này"));

        Integer centralBefore = product.getQuantity();
        Integer storeBefore = storeInventory.getQuantity();

        // Trừ kho tổng
        product.setQuantity(product.getQuantity() - quantity);
        productRepository.save(product);

        // Cộng kho cửa hàng
        storeInventory.setQuantity(storeInventory.getQuantity() + quantity);
        storeInventoryRepository.save(storeInventory);


        // Ghi transaction
        InventoryTransaction transaction = InventoryTransaction.builder()
                .transactionType(InventoryTransactionType.TRANSFER_TO_STORE)
                .productId(productId)
                .quantity(quantity)
                .sourceType(InventoryLocationType.CENTRAL)
                .sourceStoreId(null)
                .destinationType(InventoryLocationType.STORE)
                .destinationStoreId(storeId)
                .requestId(requestId)
                .performedBy(performedBy)
                .quantityBefore(centralBefore)
                .quantityAfter(product.getQuantity())
                .note(note)
                .build();

        return transactionRepository.save(transaction);
    }

    /**
     * Trả hàng từ cửa hàng về kho tổng
     */
    @Transactional
    public InventoryTransaction returnToCentral(Long productId, Integer storeId, Integer quantity,
                                                 Long performedBy, String note) {
        // Lấy thông tin store inventory
        StoreInventory storeInventory = storeInventoryRepository.findByStoreIdAndProductId(storeId, productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tồn kho cửa hàng"));

        if (storeInventory.getQuantity() < quantity) {
            throw new RuntimeException("Kho cửa hàng không đủ hàng. Tồn kho: " + storeInventory.getQuantity());
        }

        // Lấy thông tin product (kho tổng)
        Product product = productRepository.findById(productId.intValue())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        Integer storeBefore = storeInventory.getQuantity();
        Integer centralBefore = product.getQuantity();

        // Trừ kho cửa hàng
        storeInventory.setQuantity(storeInventory.getQuantity() - quantity);
        storeInventoryRepository.save(storeInventory);

        // Cộng kho tổng
        product.setQuantity(product.getQuantity() + quantity);
        productRepository.save(product);

        // Ghi transaction
        InventoryTransaction transaction = InventoryTransaction.builder()
                .transactionType(InventoryTransactionType.RETURN_TO_CENTRAL)
                .productId(productId)
                .quantity(quantity)
                .sourceType(InventoryLocationType.STORE)
                .sourceStoreId(storeId)
                .destinationType(InventoryLocationType.CENTRAL)
                .destinationStoreId(null)
                .performedBy(performedBy)
                .quantityBefore(storeBefore)
                .quantityAfter(storeInventory.getQuantity())
                .note(note)
                .build();

        return transactionRepository.save(transaction);
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
     * Nhập hàng vào kho tổng
     */
    @Transactional
    public InventoryTransaction importToCentral(Long productId, Integer quantity, Long performedBy, String note) {
        Product product = productRepository.findById(productId.intValue())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        Integer quantityBefore = product.getQuantity();

        // Cộng kho tổng
        product.setQuantity(product.getQuantity() + quantity);
        productRepository.save(product);

        // Ghi transaction
        InventoryTransaction transaction = InventoryTransaction.builder()
                .transactionType(InventoryTransactionType.IMPORT)
                .productId(productId)
                .quantity(quantity)
                .sourceType(InventoryLocationType.CENTRAL)
                .sourceStoreId(null)
                .destinationType(InventoryLocationType.CENTRAL)
                .destinationStoreId(null)
                .performedBy(performedBy)
                .quantityBefore(quantityBefore)
                .quantityAfter(product.getQuantity())
                .note(note)
                .build();

        return transactionRepository.save(transaction);
    }

    /**
     * Điều chỉnh tồn kho (kiểm kê)
     */
    @Transactional
    public InventoryTransaction adjustInventory(Long productId, Integer storeId, Integer newQuantity,
                                                 Long performedBy, String note) {
        if (storeId == null) {
            // Điều chỉnh kho tổng
            Product product = productRepository.findById(productId.intValue())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

            Integer quantityBefore = product.getQuantity();
            int difference = newQuantity - quantityBefore;

            product.setQuantity(newQuantity);
            productRepository.save(product);

            return transactionRepository.save(InventoryTransaction.builder()
                    .transactionType(InventoryTransactionType.ADJUSTMENT)
                    .productId(productId)
                    .quantity(Math.abs(difference))
                    .sourceType(InventoryLocationType.CENTRAL)
                    .destinationType(InventoryLocationType.CENTRAL)
                    .performedBy(performedBy)
                    .quantityBefore(quantityBefore)
                    .quantityAfter(newQuantity)
                    .note(note + " (Chênh lệch: " + (difference >= 0 ? "+" : "") + difference + ")")
                    .build());
        } else {
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
