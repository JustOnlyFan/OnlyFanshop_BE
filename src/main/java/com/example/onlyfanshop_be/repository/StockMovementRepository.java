package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.StockMovement;
import com.example.onlyfanshop_be.enums.StockMovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByWarehouseId(Integer warehouseId);
    
    List<StockMovement> findByProductId(Long productId);
    
    List<StockMovement> findByType(String type);
    
    List<StockMovement> findByFromWarehouseId(Integer fromWarehouseId);
    
    List<StockMovement> findByToWarehouseId(Integer toWarehouseId);
    
    @Query("SELECT sm FROM StockMovement sm WHERE sm.warehouseId = :warehouseId AND sm.createdAt BETWEEN :startDate AND :endDate")
    List<StockMovement> findByWarehouseAndDateRange(@Param("warehouseId") Integer warehouseId,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT sm FROM StockMovement sm WHERE sm.type = :type AND sm.warehouseId = :warehouseId ORDER BY sm.createdAt DESC")
    List<StockMovement> findByTypeAndWarehouse(@Param("type") String type, 
                                                @Param("warehouseId") Integer warehouseId);
}



