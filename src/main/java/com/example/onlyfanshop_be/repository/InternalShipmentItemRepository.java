package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.InternalShipmentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InternalShipmentItemRepository extends JpaRepository<InternalShipmentItem, Long> {
    
    /**
     * Tìm items theo shipment
     */
    List<InternalShipmentItem> findByInternalShipmentId(Long internalShipmentId);
    
    /**
     * Tìm items theo product
     */
    List<InternalShipmentItem> findByProductId(Long productId);
    
    /**
     * Xóa items theo shipment
     */
    void deleteByInternalShipmentId(Long internalShipmentId);
}
