package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.Warehouse;
import com.example.onlyfanshop_be.enums.WarehouseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    Optional<Warehouse> findByStoreId(Integer storeId);

    boolean existsByStoreId(Integer storeId);

    List<Warehouse> findByIsActiveTrue();

    Optional<Warehouse> findByStoreIdAndIsActiveTrue(Integer storeId);
}
