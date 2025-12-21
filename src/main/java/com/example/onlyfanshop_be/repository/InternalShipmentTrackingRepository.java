package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.InternalShipmentTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InternalShipmentTrackingRepository extends JpaRepository<InternalShipmentTracking, Long> {
    
    /**
     * Tìm tracking history theo shipment, sắp xếp theo thời gian mới nhất
     */
    List<InternalShipmentTracking> findByInternalShipmentIdOrderByTimestampDesc(Long internalShipmentId);
    
    /**
     * Tìm tracking entry mới nhất của shipment
     */
    InternalShipmentTracking findFirstByInternalShipmentIdOrderByTimestampDesc(Long internalShipmentId);
    
    /**
     * Xóa tracking history theo shipment
     */
    void deleteByInternalShipmentId(Long internalShipmentId);
}
