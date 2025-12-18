package com.example.onlyfanshop_be.repository;

import com.example.onlyfanshop_be.entity.Shipment;
import com.example.onlyfanshop_be.enums.ShipmentStatus;
import com.example.onlyfanshop_be.enums.ShipmentType;
import com.example.onlyfanshop_be.enums.ShippingCarrier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    
    Optional<Shipment> findByOrderId(Long orderId);
    
    Optional<Shipment> findByInventoryRequestId(Long inventoryRequestId);
    
    Optional<Shipment> findByTrackingNumber(String trackingNumber);
    
    Optional<Shipment> findByCarrierOrderCode(String carrierOrderCode);
    
    List<Shipment> findByStatus(ShipmentStatus status);
    
    List<Shipment> findByCarrier(ShippingCarrier carrier);
    
    List<Shipment> findByShipmentType(ShipmentType shipmentType);
    
    Page<Shipment> findByShipmentType(ShipmentType shipmentType, Pageable pageable);
    
    @Query("SELECT s FROM Shipment s WHERE s.status IN :statuses")
    List<Shipment> findByStatusIn(@Param("statuses") List<ShipmentStatus> statuses);
    
    @Query("SELECT s FROM Shipment s WHERE s.fromStoreId = :storeId")
    List<Shipment> findByFromStoreId(@Param("storeId") Integer storeId);
    
    @Query("SELECT s FROM Shipment s WHERE s.createdAt BETWEEN :startDate AND :endDate")
    List<Shipment> findByCreatedAtBetween(
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT s FROM Shipment s WHERE s.shipmentType = :type AND s.status = :status")
    Page<Shipment> findByTypeAndStatus(
        @Param("type") ShipmentType type, 
        @Param("status") ShipmentStatus status, 
        Pageable pageable
    );
}
