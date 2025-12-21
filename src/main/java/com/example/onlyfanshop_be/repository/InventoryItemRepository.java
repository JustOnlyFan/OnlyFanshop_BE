package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    
    /**
     * Tìm inventory item theo warehouse và product
     */
    Optional<InventoryItem> findByWarehouseIdAndProductId(Long warehouseId, Long productId);
    
    /**
     * Tìm tất cả inventory items của một warehouse
     */
    List<InventoryItem> findByWarehouseId(Long warehouseId);
    
    /**
     * Tìm tất cả inventory items của một product
     */
    List<InventoryItem> findByProductId(Long productId);
    
    /**
     * Xóa tất cả inventory items của một product
     */
    void deleteByProductId(Long productId);
    
    /**
     * Kiểm tra inventory item đã tồn tại chưa
     */
    boolean existsByWarehouseIdAndProductId(Long warehouseId, Long productId);
    
    /**
     * Tìm các warehouse có sản phẩm với số lượng > 0 (trừ warehouse chỉ định)
     */
    @Query("SELECT i FROM InventoryItem i WHERE i.productId = :productId " +
           "AND i.warehouseId != :excludeWarehouseId " +
           "AND (i.quantity - i.reservedQuantity) > 0")
    List<InventoryItem> findAvailableInventoryExcludingWarehouse(
        @Param("productId") Long productId, 
        @Param("excludeWarehouseId") Long excludeWarehouseId
    );
    
    /**
     * Tính tổng số lượng có sẵn của một sản phẩm trong tất cả các kho
     */
    @Query("SELECT COALESCE(SUM(i.quantity - i.reservedQuantity), 0) FROM InventoryItem i " +
           "WHERE i.productId = :productId")
    Integer getTotalAvailableQuantity(@Param("productId") Long productId);
}
