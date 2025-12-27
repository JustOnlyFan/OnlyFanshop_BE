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

    List<InventoryTransaction> findByProductIdOrderByCreatedAtDesc(Long productId);

    @Query("SELECT it FROM InventoryTransaction it WHERE it.sourceStoreId = :storeId OR it.destinationStoreId = :storeId ORDER BY it.createdAt DESC")
    List<InventoryTransaction> findByStoreId(@Param("storeId") Integer storeId);

    List<InventoryTransaction> findByRequestIdOrderByCreatedAtDesc(Long requestId);

    Page<InventoryTransaction> findByProductId(Long productId, Pageable pageable);
}
