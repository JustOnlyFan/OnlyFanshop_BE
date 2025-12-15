package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.InventoryTransaction;
import com.example.onlyfanshop_be.enums.InventoryTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    
    /**
     * Tìm transactions theo product
     */
    List<InventoryTransaction> findByProductIdOrderByCreatedAtDesc(Long productId);
    
    /**
     * Tìm transactions theo loại
     */
    List<InventoryTransaction> findByTransactionTypeOrderByCreatedAtDesc(InventoryTransactionType type);
    
    /**
     * Tìm transactions liên quan đến một store (nguồn hoặc đích)
     */
    @Query("SELECT it FROM InventoryTransaction it WHERE it.sourceStoreId = :storeId OR it.destinationStoreId = :storeId ORDER BY it.createdAt DESC")
    List<InventoryTransaction> findByStoreId(@Param("storeId") Integer storeId);
    
    /**
     * Tìm transactions chuyển đến một store
     */
    List<InventoryTransaction> findByDestinationStoreIdOrderByCreatedAtDesc(Integer storeId);
    
    /**
     * Tìm transactions chuyển từ một store
     */
    List<InventoryTransaction> findBySourceStoreIdOrderByCreatedAtDesc(Integer storeId);
    
    /**
     * Tìm transactions theo request
     */
    List<InventoryTransaction> findByRequestIdOrderByCreatedAtDesc(Long requestId);
    
    /**
     * Tìm transactions theo order
     */
    List<InventoryTransaction> findByOrderIdOrderByCreatedAtDesc(Long orderId);
    
    /**
     * Tìm transactions trong khoảng thời gian
     */
    @Query("SELECT it FROM InventoryTransaction it WHERE it.createdAt BETWEEN :startDate AND :endDate ORDER BY it.createdAt DESC")
    List<InventoryTransaction> findByDateRange(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    /**
     * Tìm transactions với pagination
     */
    Page<InventoryTransaction> findByProductId(Long productId, Pageable pageable);
    
    /**
     * Tính tổng số lượng chuyển đến store theo product
     */
    @Query("SELECT COALESCE(SUM(it.quantity), 0) FROM InventoryTransaction it WHERE it.destinationStoreId = :storeId AND it.productId = :productId AND it.transactionType = 'TRANSFER_TO_STORE'")
    Integer sumTransferredToStore(@Param("storeId") Integer storeId, @Param("productId") Long productId);
    
    /**
     * Tính tổng số lượng bán tại store theo product
     */
    @Query("SELECT COALESCE(SUM(it.quantity), 0) FROM InventoryTransaction it WHERE it.sourceStoreId = :storeId AND it.productId = :productId AND it.transactionType = 'SALE'")
    Integer sumSoldAtStore(@Param("storeId") Integer storeId, @Param("productId") Long productId);
}
