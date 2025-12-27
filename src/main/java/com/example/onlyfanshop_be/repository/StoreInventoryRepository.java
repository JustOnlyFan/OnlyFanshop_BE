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

    Optional<StoreInventory> findByStoreIdAndProductId(Integer storeId, Long productId);

    List<StoreInventory> findByStoreId(Integer storeId);

    List<StoreInventory> findByProductId(Long productId);

    @Query("SELECT si FROM StoreInventory si WHERE si.productId = :productId AND si.isAvailable = true")
    List<StoreInventory> findAvailableStoresByProductId(@Param("productId") Long productId);

    @Query("SELECT si FROM StoreInventory si WHERE si.storeId = :storeId AND si.isAvailable = true")
    List<StoreInventory> findAvailableProductsByStoreId(@Param("storeId") Integer storeId);
}

