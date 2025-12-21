package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.InventoryLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryLogRepository extends JpaRepository<InventoryLog, Long> {
    
    /**
     * Tìm logs theo warehouse
     */
    Page<InventoryLog> findByWarehouseIdOrderByCreatedAtDesc(Long warehouseId, Pageable pageable);
    
    /**
     * Tìm logs theo product
     */
    Page<InventoryLog> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);
    
    /**
     * Tìm logs theo warehouse và product
     */
    List<InventoryLog> findByWarehouseIdAndProductIdOrderByCreatedAtDesc(Long warehouseId, Long productId);
    
    /**
     * Tìm logs trong khoảng thời gian
     */
    List<InventoryLog> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);
    
    /**
     * Tìm logs theo user
     */
    Page<InventoryLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
