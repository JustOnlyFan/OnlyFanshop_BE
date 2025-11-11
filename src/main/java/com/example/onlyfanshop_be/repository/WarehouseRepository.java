package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.Warehouse;
import com.example.onlyfanshop_be.enums.WarehouseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Integer> {
    Optional<Warehouse> findByCode(String code);
    
    List<Warehouse> findByType(WarehouseType type);
    
    List<Warehouse> findByParentWarehouseId(Integer parentWarehouseId);
    
    List<Warehouse> findByStoreLocationId(Integer storeLocationId);
    
    List<Warehouse> findByIsActiveTrue();
    
    @Query("SELECT w FROM Warehouse w WHERE w.type = :type AND w.isActive = true")
    List<Warehouse> findActiveByType(@Param("type") WarehouseType type);
    
    @Query("SELECT w FROM Warehouse w WHERE w.parentWarehouseId = :parentId AND w.isActive = true")
    List<Warehouse> findActiveChildrenByParentId(@Param("parentId") Integer parentId);
    
    boolean existsByCode(String code);
}



