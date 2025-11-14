package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.WarehouseInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseInventoryRepository extends JpaRepository<WarehouseInventory, Long> {
    Optional<WarehouseInventory> findByWarehouseIdAndProductIdAndProductVariantId(
            Integer warehouseId, Long productId, Long productVariantId);
    
    List<WarehouseInventory> findByWarehouseId(Integer warehouseId);
    
    List<WarehouseInventory> findByProductId(Long productId);
    
    List<WarehouseInventory> findByProductIdAndProductVariantId(Long productId, Long productVariantId);
    
    @Query("SELECT wi FROM WarehouseInventory wi WHERE wi.warehouseId = :warehouseId AND wi.productId = :productId")
    List<WarehouseInventory> findByWarehouseAndProduct(@Param("warehouseId") Integer warehouseId, 
                                                        @Param("productId") Long productId);
    
    @Query("SELECT SUM(wi.quantityInStock) FROM WarehouseInventory wi WHERE wi.productId = :productId AND wi.productVariantId = :productVariantId")
    Integer getTotalQuantityByProductAndVariant(@Param("productId") Long productId, 
                                                @Param("productVariantId") Long productVariantId);
}








