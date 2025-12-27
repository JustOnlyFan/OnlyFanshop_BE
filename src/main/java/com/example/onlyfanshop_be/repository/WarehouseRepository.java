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
    
    /**
     * Tìm tất cả kho đang hoạt động
     * Requirements: 7.4 - Khi query active warehouses, hệ thống chỉ trả về các kho có isActive = true
     */
    List<Warehouse> findByIsActiveTrue();
    
    /**
     * Tìm kho của cửa hàng đang hoạt động
     * Requirements: 1.3, 7.4 - Chỉ trả về kho cửa hàng đang hoạt động
     */
    Optional<Warehouse> findByStoreIdAndIsActiveTrue(Integer storeId);
}
