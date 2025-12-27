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

    Optional<InventoryItem> findByWarehouseIdAndProductId(Long warehouseId, Long productId);

    List<InventoryItem> findByWarehouseId(Long warehouseId);

    List<InventoryItem> findByProductId(Long productId);

    void deleteByProductId(Long productId);

    boolean existsByWarehouseIdAndProductId(Long warehouseId, Long productId);

}
