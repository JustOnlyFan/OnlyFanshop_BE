package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.InventoryRequestItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InventoryRequestItemRepository extends JpaRepository<InventoryRequestItem, Long> {
    
    List<InventoryRequestItem> findByRequestId(Long requestId);
    
    void deleteByRequestId(Long requestId);
}
