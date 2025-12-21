package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.InternalShipment;
import com.example.onlyfanshop_be.enums.InternalShipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InternalShipmentRepository extends JpaRepository<InternalShipment, Long> {
    
    /**
     * Tìm shipments theo status
     */
    Page<InternalShipment> findByStatusOrderByCreatedAtDesc(InternalShipmentStatus status, Pageable pageable);
    
    /**
     * Tìm shipments theo transfer request
     */
    List<InternalShipment> findByTransferRequestId(Long transferRequestId);
    
    /**
     * Tìm shipment theo GHN order code
     */
    Optional<InternalShipment> findByGhnOrderCode(String ghnOrderCode);
    
    /**
     * Tìm shipments theo source warehouse
     */
    List<InternalShipment> findBySourceWarehouseId(Long sourceWarehouseId);
    
    /**
     * Tìm shipments theo destination warehouse
     */
    List<InternalShipment> findByDestinationWarehouseId(Long destinationWarehouseId);
    
    /**
     * Tìm các shipments đang vận chuyển (chưa delivered hoặc cancelled)
     */
    List<InternalShipment> findByStatusIn(List<InternalShipmentStatus> statuses);
    
    /**
     * Đếm số shipments theo status
     */
    long countByStatus(InternalShipmentStatus status);
}
