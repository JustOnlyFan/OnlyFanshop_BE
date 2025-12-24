package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.response.SourceAllocation;
import com.example.onlyfanshop_be.entity.InternalShipment;
import com.example.onlyfanshop_be.entity.TransferRequest;
import com.example.onlyfanshop_be.enums.InternalShipmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * IInternalShipmentService - Interface for Internal Shipment management
 * Handles shipments between warehouses (Main Warehouse → Store Warehouses)
 * Requirements: 7.1, 7.3, 7.5, 8.2, 8.3, 8.4
 */
public interface IInternalShipmentService {
    
    /**
     * Create shipments for a transfer request based on source allocations
     * Requirements: 7.1 - WHEN a Transfer_Request is approved THEN the System SHALL call GHN_API to create shipping orders
     * Requirements: 7.3 - WHEN GHN_API returns order code THEN the System SHALL store the order code and link to the Shipment
     * 
     * @param request The transfer request being fulfilled
     * @param allocationsByProduct Map of product ID to list of source allocations
     * @return List of created InternalShipment entities
     */
    List<InternalShipment> createShipments(TransferRequest request, Map<Long, List<SourceAllocation>> allocationsByProduct);
    
    /**
     * Get paginated list of internal shipments
     * Requirements: 8.4 - WHEN Admin or Staff queries Shipment THEN the System SHALL display current status
     * 
     * @param status Optional status filter
     * @param pageable Pagination parameters
     * @return Page of InternalShipmentDTO
     */
    Page<InternalShipmentDTO> getShipments(InternalShipmentStatus status, Pageable pageable);
    
    /**
     * Get a single internal shipment by ID
     * Requirements: 8.4 - WHEN Admin or Staff queries Shipment THEN the System SHALL display current status
     * 
     * @param id The shipment ID
     * @return InternalShipmentDTO
     */
    InternalShipmentDTO getShipment(Long id);
    
    /**
     * Get tracking history for a shipment
     * Requirements: 8.4 - WHEN Admin or Staff queries Shipment THEN the System SHALL display tracking history
     * 
     * @param id The shipment ID
     * @return InternalShipmentTrackingDTO containing tracking history
     */
    InternalShipmentTrackingDTO getShipmentTracking(Long id);
    
    /**
     * Sync shipment status from GHN API
     * Requirements: 8.2 - WHEN GHN_API returns status change THEN the System SHALL update Shipment status and log the change
     * 
     * @param id The shipment ID
     * @return Updated InternalShipmentDTO
     */
    InternalShipmentDTO syncShipmentStatus(Long id);
    
    /**
     * Process a delivered shipment - update destination warehouse inventory
     * Requirements: 8.3 - WHEN Shipment status changes to DELIVERED THEN the System SHALL update destination Store_Warehouse inventory
     * 
     * @param shipmentId The shipment ID
     */
    void processDeliveredShipment(Long shipmentId);
    
    /**
     * Get shipments by transfer request ID
     * 
     * @param transferRequestId The transfer request ID
     * @return List of InternalShipmentDTO
     */
    List<InternalShipmentDTO> getShipmentsByTransferRequest(Long transferRequestId);
    
    /**
     * Get shipments that are in transit (not yet delivered or cancelled)
     * Used for scheduled status sync
     * 
     * @return List of InternalShipment entities
     */
    List<InternalShipment> getInTransitShipments();
    
    /**
     * DTO for internal shipment
     */
    record InternalShipmentDTO(
        Long id,
        Long transferRequestId,
        Long sourceWarehouseId,
        String sourceWarehouseName,
        Long destinationWarehouseId,
        String destinationWarehouseName,
        String ghnOrderCode,
        InternalShipmentStatus status,
        Integer totalFee,
        String expectedDeliveryTime,
        java.time.LocalDateTime createdAt,
        java.time.LocalDateTime deliveredAt,
        List<InternalShipmentItemDTO> items
    ) {}
    
    /**
     * DTO for internal shipment item
     */
    record InternalShipmentItemDTO(
        Long id,
        Long productId,
        String productName,
        String productSku,
        Integer quantity
    ) {}
    
    /**
     * DTO for internal shipment tracking
     */
    record InternalShipmentTrackingDTO(
        Long shipmentId,
        String ghnOrderCode,
        InternalShipmentStatus currentStatus,
        String expectedDeliveryTime,
        List<TrackingEntry> trackingHistory
    ) {
        public record TrackingEntry(
            String status,
            String description,
            java.time.LocalDateTime timestamp
        ) {}
    }
}
