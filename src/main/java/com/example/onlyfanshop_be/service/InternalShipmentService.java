package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.ghn.*;
import com.example.onlyfanshop_be.dto.response.SourceAllocation;
import com.example.onlyfanshop_be.entity.*;
import com.example.onlyfanshop_be.enums.InternalShipmentStatus;
import com.example.onlyfanshop_be.enums.WarehouseType;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * InternalShipmentService - Implementation for internal warehouse shipments
 * Handles shipments between warehouses via GHN API
 * Requirements: 7.1, 7.3, 7.5, 8.2, 8.3, 8.4
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InternalShipmentService implements IInternalShipmentService {
    
    private final InternalShipmentRepository internalShipmentRepository;
    private final InternalShipmentItemRepository internalShipmentItemRepository;
    private final InternalShipmentTrackingRepository internalShipmentTrackingRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final ProductRepository productRepository;
    private final StoreLocationRepository storeLocationRepository;
    private final IGHNService ghnService;

    /**
     * Create shipments for a transfer request based on source allocations
     * Requirements: 7.1, 7.3
     */
    @Override
    @Transactional
    public List<InternalShipment> createShipments(TransferRequest request, Map<Long, List<SourceAllocation>> allocationsByProduct) {
        List<InternalShipment> shipments = new ArrayList<>();
        
        // Get destination warehouse (the requesting store's warehouse)
        Warehouse destinationWarehouse = warehouseRepository.findByStoreId(request.getStoreId())
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        
        // Get destination store info for GHN order
        StoreLocation destinationStore = storeLocationRepository.findById(request.getStoreId())
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));
        
        // Group allocations by source warehouse
        Map<Long, List<ProductAllocation>> allocationsByWarehouse = groupAllocationsByWarehouse(allocationsByProduct);
        
        // Create one shipment per source warehouse
        for (Map.Entry<Long, List<ProductAllocation>> entry : allocationsByWarehouse.entrySet()) {
            Long sourceWarehouseId = entry.getKey();
            List<ProductAllocation> productAllocations = entry.getValue();
            
            Warehouse sourceWarehouse = warehouseRepository.findById(sourceWarehouseId)
                    .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
            
            InternalShipment shipment = createSingleShipment(
                    request, 
                    sourceWarehouse, 
                    destinationWarehouse, 
                    destinationStore,
                    productAllocations
            );
            
            shipments.add(shipment);
        }
        
        log.info("Created {} internal shipments for transfer request {}", shipments.size(), request.getId());
        return shipments;
    }
    
    /**
     * Helper class to hold product allocation info
     */
    private record ProductAllocation(Long productId, Integer quantity) {}
    
    /**
     * Group allocations by source warehouse
     */
    private Map<Long, List<ProductAllocation>> groupAllocationsByWarehouse(Map<Long, List<SourceAllocation>> allocationsByProduct) {
        Map<Long, List<ProductAllocation>> result = new HashMap<>();
        
        for (Map.Entry<Long, List<SourceAllocation>> entry : allocationsByProduct.entrySet()) {
            Long productId = entry.getKey();
            for (SourceAllocation allocation : entry.getValue()) {
                result.computeIfAbsent(allocation.getWarehouseId(), k -> new ArrayList<>())
                        .add(new ProductAllocation(productId, allocation.getQuantity()));
            }
        }
        
        return result;
    }
    
    /**
     * Create a single shipment from one source warehouse
     * Requirements: 7.1, 7.2, 7.3
     */
    private InternalShipment createSingleShipment(
            TransferRequest request,
            Warehouse sourceWarehouse,
            Warehouse destinationWarehouse,
            StoreLocation destinationStore,
            List<ProductAllocation> productAllocations) {
        
        // Create shipment entity
        InternalShipment shipment = InternalShipment.builder()
                .transferRequestId(request.getId())
                .sourceWarehouseId(sourceWarehouse.getId())
                .destinationWarehouseId(destinationWarehouse.getId())
                .status(InternalShipmentStatus.CREATED)
                .build();
        
        shipment = internalShipmentRepository.save(shipment);
        
        // Create shipment items
        List<InternalShipmentItem> items = new ArrayList<>();
        List<GHNCreateOrderRequest.GHNItem> ghnItems = new ArrayList<>();
        int totalWeight = 0;
        
        for (ProductAllocation allocation : productAllocations) {
            Product product = productRepository.findById(allocation.productId().intValue())
                    .orElse(null);
            
            InternalShipmentItem item = InternalShipmentItem.builder()
                    .internalShipmentId(shipment.getId())
                    .productId(allocation.productId())
                    .quantity(allocation.quantity())
                    .build();
            items.add(item);
            
            // Add to GHN items
            if (product != null) {
                ghnItems.add(GHNCreateOrderRequest.GHNItem.builder()
                        .name(product.getName())
                        .code(product.getSku())
                        .quantity(allocation.quantity())
                        .price(product.getPrice() != null ? product.getPrice().intValue() : 0)
                        .weight(500) // Default weight per item in grams
                        .build());
                totalWeight += 500 * allocation.quantity();
            }
        }
        
        internalShipmentItemRepository.saveAll(items);
        
        // Call GHN API to create order (Requirements 7.1, 7.2)
        try {
            GHNCreateOrderRequest ghnRequest = buildGHNOrderRequest(
                    sourceWarehouse, 
                    destinationWarehouse, 
                    destinationStore,
                    ghnItems, 
                    totalWeight,
                    request.getId()
            );
            
            GHNCreateOrderResponse ghnResponse = ghnService.createOrder(ghnRequest);
            
            // Store GHN order code (Requirements 7.3)
            shipment.setGhnOrderCode(ghnResponse.getOrderCode());
            shipment.setTotalFee(ghnResponse.getTotalFee());
            shipment.setExpectedDeliveryTime(ghnResponse.getExpectedDeliveryTime());
            shipment.setStatus(InternalShipmentStatus.CREATED);
            
            shipment = internalShipmentRepository.save(shipment);
            
            // Create initial tracking entry
            createTrackingEntry(shipment.getId(), "CREATED", "Đơn vận chuyển nội bộ đã được tạo");
            
            log.info("Created GHN order {} for internal shipment {}", ghnResponse.getOrderCode(), shipment.getId());
            
        } catch (Exception e) {
            log.error("Failed to create GHN order for internal shipment {}: {}", shipment.getId(), e.getMessage());
            // Still save the shipment but without GHN order code
            createTrackingEntry(shipment.getId(), "CREATED", "Đơn vận chuyển đã tạo, chờ tạo đơn GHN");
        }
        
        return shipment;
    }

    /**
     * Build GHN order request for internal shipment
     * Requirements: 7.2
     */
    private GHNCreateOrderRequest buildGHNOrderRequest(
            Warehouse sourceWarehouse,
            Warehouse destinationWarehouse,
            StoreLocation destinationStore,
            List<GHNCreateOrderRequest.GHNItem> items,
            int totalWeight,
            Long transferRequestId) {
        
        // Get source store info if it's a store warehouse
        String fromName = sourceWarehouse.getName();
        String fromPhone = sourceWarehouse.getPhone();
        String fromAddress = sourceWarehouse.getAddress();
        
        if (sourceWarehouse.getType() == WarehouseType.STORE && sourceWarehouse.getStoreId() != null) {
            StoreLocation sourceStore = storeLocationRepository.findById(sourceWarehouse.getStoreId())
                    .orElse(null);
            if (sourceStore != null) {
                fromName = sourceStore.getName();
                fromPhone = sourceStore.getPhone();
                fromAddress = sourceStore.getAddress();
            }
        }
        
        return GHNCreateOrderRequest.builder()
                .paymentTypeId(1) // Shop pays shipping fee
                .note("Internal transfer - Request #" + transferRequestId)
                .requiredNote("KHONGCHOXEMHANG")
                .fromName(fromName)
                .fromPhone(fromPhone != null ? fromPhone : "0000000000")
                .fromAddress(fromAddress != null ? fromAddress : "N/A")
                .toName(destinationStore.getName())
                .toPhone(destinationStore.getPhone() != null ? destinationStore.getPhone() : "0000000000")
                .toAddress(destinationStore.getAddress() != null ? destinationStore.getAddress() : "N/A")
                // Note: wardCode and districtId should be added to StoreLocation entity
                // For now, using address-based delivery which GHN can resolve
                .toWardCode(null) // Will be resolved by GHN from address
                .toDistrictId(null) // Will be resolved by GHN from address
                .weight(Math.max(totalWeight, 100)) // Minimum 100g
                .length(30) // Default dimensions
                .width(20)
                .height(10)
                .serviceTypeId(2) // Standard delivery
                .codAmount(0) // No COD for internal transfers
                .insuranceValue(0)
                .items(items)
                .build();
    }
    
    /**
     * Create a tracking entry for a shipment
     */
    private void createTrackingEntry(Long shipmentId, String status, String description) {
        InternalShipmentTracking tracking = InternalShipmentTracking.builder()
                .internalShipmentId(shipmentId)
                .status(status)
                .description(description)
                .timestamp(LocalDateTime.now())
                .build();
        internalShipmentTrackingRepository.save(tracking);
    }
    
    /**
     * Get paginated list of internal shipments
     * Requirements: 8.4
     */
    @Override
    public Page<InternalShipmentDTO> getShipments(InternalShipmentStatus status, Pageable pageable) {
        Page<InternalShipment> shipments;
        
        if (status != null) {
            shipments = internalShipmentRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            shipments = internalShipmentRepository.findAll(pageable);
        }
        
        List<InternalShipmentDTO> dtos = shipments.getContent().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        
        return new PageImpl<>(dtos, pageable, shipments.getTotalElements());
    }
    
    /**
     * Get a single internal shipment by ID
     * Requirements: 8.4
     */
    @Override
    public InternalShipmentDTO getShipment(Long id) {
        InternalShipment shipment = internalShipmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SHIPMENT_NOT_FOUND));
        return toDTO(shipment);
    }
    
    /**
     * Get tracking history for a shipment
     * Requirements: 8.4
     */
    @Override
    public InternalShipmentTrackingDTO getShipmentTracking(Long id) {
        InternalShipment shipment = internalShipmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SHIPMENT_NOT_FOUND));
        
        List<InternalShipmentTracking> trackingHistory = 
                internalShipmentTrackingRepository.findByInternalShipmentIdOrderByTimestampDesc(id);
        
        List<InternalShipmentTrackingDTO.TrackingEntry> entries = trackingHistory.stream()
                .map(t -> new InternalShipmentTrackingDTO.TrackingEntry(
                        t.getStatus(),
                        t.getDescription(),
                        t.getTimestamp()
                ))
                .collect(Collectors.toList());
        
        return new InternalShipmentTrackingDTO(
                shipment.getId(),
                shipment.getGhnOrderCode(),
                shipment.getStatus(),
                shipment.getExpectedDeliveryTime(),
                entries
        );
    }

    /**
     * Sync shipment status from GHN API
     * Requirements: 8.2
     */
    @Override
    @Transactional
    public InternalShipmentDTO syncShipmentStatus(Long id) {
        InternalShipment shipment = internalShipmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SHIPMENT_NOT_FOUND));
        
        if (shipment.getGhnOrderCode() == null) {
            log.warn("Cannot sync status for shipment {} - no GHN order code", id);
            return toDTO(shipment);
        }
        
        // Get status from GHN API
        GHNOrderStatus ghnStatus = ghnService.getOrderStatus(shipment.getGhnOrderCode());
        
        if (ghnStatus != null) {
            InternalShipmentStatus newStatus = mapGHNStatusToInternal(ghnStatus);
            InternalShipmentStatus oldStatus = shipment.getStatus();
            
            if (newStatus != oldStatus) {
                shipment.setStatus(newStatus);
                
                // Create tracking entry for status change
                createTrackingEntry(shipment.getId(), newStatus.name(), 
                        "Trạng thái cập nhật từ GHN: " + ghnStatus.getDescription());
                
                // Handle delivered status (Requirements 8.3)
                if (newStatus == InternalShipmentStatus.DELIVERED) {
                    shipment.setDeliveredAt(LocalDateTime.now());
                    processDeliveredShipment(shipment.getId());
                }
                
                shipment = internalShipmentRepository.save(shipment);
                log.info("Updated internal shipment {} status: {} -> {}", id, oldStatus, newStatus);
            }
        }
        
        return toDTO(shipment);
    }
    
    /**
     * Map GHN status to internal shipment status
     */
    private InternalShipmentStatus mapGHNStatusToInternal(GHNOrderStatus ghnStatus) {
        return switch (ghnStatus) {
            case READY_TO_PICK, PICKING -> InternalShipmentStatus.PICKING;
            case PICKED -> InternalShipmentStatus.PICKED;
            case STORING, TRANSPORTING, SORTING -> InternalShipmentStatus.IN_TRANSIT;
            case DELIVERING -> InternalShipmentStatus.DELIVERING;
            case DELIVERED -> InternalShipmentStatus.DELIVERED;
            case CANCEL -> InternalShipmentStatus.CANCELLED;
            case WAITING_TO_RETURN, RETURN, RETURN_TRANSPORTING, RETURN_SORTING, 
                 RETURNING, RETURN_FAIL, RETURNED -> InternalShipmentStatus.RETURN;
            default -> InternalShipmentStatus.CREATED;
        };
    }
    
    /**
     * Process a delivered shipment - update destination warehouse inventory
     * Requirements: 8.3
     */
    @Override
    @Transactional
    public void processDeliveredShipment(Long shipmentId) {
        InternalShipment shipment = internalShipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new AppException(ErrorCode.SHIPMENT_NOT_FOUND));
        
        if (shipment.getStatus() != InternalShipmentStatus.DELIVERED) {
            log.warn("Cannot process non-delivered shipment {}", shipmentId);
            return;
        }
        
        // Get shipment items
        List<InternalShipmentItem> items = internalShipmentItemRepository.findByInternalShipmentId(shipmentId);
        
        // Update destination warehouse inventory
        for (InternalShipmentItem item : items) {
            updateDestinationInventory(
                    shipment.getDestinationWarehouseId(),
                    item.getProductId(),
                    item.getQuantity(),
                    "Internal shipment #" + shipmentId + " delivered"
            );
        }
        
        log.info("Processed delivered shipment {} - updated {} inventory items", shipmentId, items.size());
    }
    
    /**
     * Update destination warehouse inventory after delivery
     * Requirements: 8.3
     */
    private void updateDestinationInventory(Long warehouseId, Long productId, int quantity, String reason) {
        // Get or create inventory item
        InventoryItem inventoryItem = inventoryItemRepository
                .findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseGet(() -> {
                    InventoryItem newItem = InventoryItem.builder()
                            .warehouseId(warehouseId)
                            .productId(productId)
                            .quantity(0)
                            .reservedQuantity(0)
                            .build();
                    return inventoryItemRepository.save(newItem);
                });
        
        int previousQuantity = inventoryItem.getQuantity();
        int newQuantity = previousQuantity + quantity;
        
        inventoryItem.setQuantity(newQuantity);
        inventoryItemRepository.save(inventoryItem);
        
        // Create inventory log
        InventoryLog log = InventoryLog.builder()
                .warehouseId(warehouseId)
                .productId(productId)
                .previousQuantity(previousQuantity)
                .newQuantity(newQuantity)
                .reason(reason)
                .build();
        inventoryLogRepository.save(log);
        
        InternalShipmentService.log.debug("Updated destination inventory: warehouse={}, product={}, {} -> {}", 
                warehouseId, productId, previousQuantity, newQuantity);
    }
    
    /**
     * Get shipments by transfer request ID
     */
    @Override
    public List<InternalShipmentDTO> getShipmentsByTransferRequest(Long transferRequestId) {
        return internalShipmentRepository.findByTransferRequestId(transferRequestId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get shipments that are in transit (not yet delivered or cancelled)
     */
    @Override
    public List<InternalShipment> getInTransitShipments() {
        List<InternalShipmentStatus> inTransitStatuses = Arrays.asList(
                InternalShipmentStatus.CREATED,
                InternalShipmentStatus.PICKING,
                InternalShipmentStatus.PICKED,
                InternalShipmentStatus.IN_TRANSIT,
                InternalShipmentStatus.DELIVERING
        );
        return internalShipmentRepository.findByStatusIn(inTransitStatuses);
    }

    /**
     * Convert InternalShipment entity to DTO
     */
    private InternalShipmentDTO toDTO(InternalShipment shipment) {
        // Get warehouse names
        String sourceWarehouseName = warehouseRepository.findById(shipment.getSourceWarehouseId())
                .map(Warehouse::getName)
                .orElse(null);
        
        String destinationWarehouseName = warehouseRepository.findById(shipment.getDestinationWarehouseId())
                .map(Warehouse::getName)
                .orElse(null);
        
        // Get items
        List<InternalShipmentItem> items = internalShipmentItemRepository.findByInternalShipmentId(shipment.getId());
        List<InternalShipmentItemDTO> itemDTOs = items.stream()
                .map(this::toItemDTO)
                .collect(Collectors.toList());
        
        return new InternalShipmentDTO(
                shipment.getId(),
                shipment.getTransferRequestId(),
                shipment.getSourceWarehouseId(),
                sourceWarehouseName,
                shipment.getDestinationWarehouseId(),
                destinationWarehouseName,
                shipment.getGhnOrderCode(),
                shipment.getStatus(),
                shipment.getTotalFee(),
                shipment.getExpectedDeliveryTime(),
                shipment.getCreatedAt(),
                shipment.getDeliveredAt(),
                itemDTOs
        );
    }
    
    /**
     * Convert InternalShipmentItem entity to DTO
     */
    private InternalShipmentItemDTO toItemDTO(InternalShipmentItem item) {
        Product product = productRepository.findById(item.getProductId().intValue()).orElse(null);
        
        return new InternalShipmentItemDTO(
                item.getId(),
                item.getProductId(),
                product != null ? product.getName() : null,
                product != null ? product.getSku() : null,
                item.getQuantity()
        );
    }
}
