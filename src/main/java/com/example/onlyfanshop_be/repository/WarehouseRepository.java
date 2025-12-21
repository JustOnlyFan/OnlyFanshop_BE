package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.Warehouse;
import com.example.onlyfanshop_be.enums.WarehouseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {
    
    /**
     * Tìm kho theo loại
     */
    List<Warehouse> findByType(WarehouseType type);
    
    /**
     * Tìm kho lớn (Main Warehouse)
     */
    Optional<Warehouse> findFirstByType(WarehouseType type);
    
    /**
     * Tìm kho của cửa hàng
     */
    Optional<Warehouse> findByStoreId(Integer storeId);
    
    /**
     * Kiểm tra kho của cửa hàng đã tồn tại chưa
     */
    boolean existsByStoreId(Integer storeId);
    
    /**
     * Tìm tất cả kho cửa hàng (trừ kho lớn)
     */
    List<Warehouse> findByTypeAndIdNot(WarehouseType type, Long excludeId);
}
