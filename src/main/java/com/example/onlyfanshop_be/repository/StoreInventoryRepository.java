package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.StoreInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreInventoryRepository extends JpaRepository<StoreInventory, Long> {
    /**
     * Tìm store inventory theo storeId và productId
     */
    Optional<StoreInventory> findByStoreIdAndProductId(Integer storeId, Long productId);
    
    /**
     * Tìm tất cả inventories của một store
     */
    List<StoreInventory> findByStoreId(Integer storeId);
    
    /**
     * Tìm tất cả inventories của một product
     */
    List<StoreInventory> findByProductId(Long productId);
    
    /**
     * Tìm tất cả stores có bán sản phẩm (isAvailable = true)
     */
    @Query("SELECT si FROM StoreInventory si WHERE si.productId = :productId AND si.isAvailable = true")
    List<StoreInventory> findAvailableStoresByProductId(@Param("productId") Long productId);
    
    /**
     * Đếm số lượng stores có bán sản phẩm
     */
    @Query("SELECT COUNT(si) FROM StoreInventory si WHERE si.productId = :productId AND si.isAvailable = true")
    Long countAvailableStoresByProductId(@Param("productId") Long productId);
    
    /**
     * Tìm tất cả products có sẵn tại store (isAvailable = true)
     */
    @Query("SELECT si FROM StoreInventory si WHERE si.storeId = :storeId AND si.isAvailable = true")
    List<StoreInventory> findAvailableProductsByStoreId(@Param("storeId") Integer storeId);
}

