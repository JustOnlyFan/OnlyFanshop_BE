package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.response.*;
import com.example.onlyfanshop_be.entity.*;
import com.example.onlyfanshop_be.enums.TransferRequestStatus;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of FulfillmentService
 * Handles transfer request fulfillment including availability checking, source allocation, and inventory deduction
 * Updated to only support Store Warehouses (Main Warehouse removed)
 * Requirements: 4.1, 4.2, 4.3, 4.4, 5.2, 5.5, 5.6
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FulfillmentService implements IFulfillmentService {
    
    private final TransferRequestRepository transferRequestRepository;
    private final TransferRequestItemRepository transferRequestItemRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryLogRepository inventoryLogRepository;
    private final ProductRepository productRepository;
    private final StoreLocationRepository storeLocationRepository;
    private final IDebtOrderService debtOrderService;
    private final IInternalShipmentService internalShipmentService;


    /**
     * Check availability for a transfer request by ID
     */
    @Override
    public AvailabilityCheckResult checkAvailabilityById(Long requestId) {
        TransferRequest request = transferRequestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSFER_REQUEST_NOT_FOUND));
        return checkAvailability(request);
    }

    /**
     * Check availability for a transfer request
     * Only checks from Store Warehouses (Main Warehouse removed)
     * Requirements: 4.1 - Only consider Store_Warehouses
     * Requirements: 4.2 - Search available quantity from other Store_Warehouses
     */
    @Override
    public AvailabilityCheckResult checkAvailability(TransferRequest request) {
        // Load items if not already loaded
        List<TransferRequestItem> items = request.getItems();
        if (items == null || items.isEmpty()) {
            items = transferRequestItemRepository.findByTransferRequestId(request.getId());
        }
        
        List<ProductAvailability> productAvailabilities = new ArrayList<>();
        int totalShortage = 0;
        int totalRequestedQuantity = 0;
        int totalAvailableQuantity = 0;
        boolean canFullyFulfill = true;
        
        for (TransferRequestItem item : items) {
            ProductAvailability availability = checkProductAvailability(
                    item.getProductId(), 
                    item.getRequestedQuantity(),
                    request.getStoreId()
            );
            productAvailabilities.add(availability);
            
            totalRequestedQuantity += item.getRequestedQuantity();
            totalAvailableQuantity += availability.getTotalAvailable();
            
            if (availability.getShortage() > 0) {
                totalShortage += availability.getShortage();
                canFullyFulfill = false;
            }
        }
        
        int maxFulfillable = Math.min(totalRequestedQuantity, totalAvailableQuantity);
        
        String summary;
        if (canFullyFulfill) {
            summary = "Tất cả sản phẩm có thể được đáp ứng đầy đủ";
        } else if (totalAvailableQuantity > 0) {
            summary = String.format("Có thể đáp ứng một phần: %d/%d sản phẩm. Thiếu: %d", 
                    maxFulfillable, totalRequestedQuantity, totalShortage);
        } else {
            summary = "Không có sản phẩm nào có sẵn để đáp ứng";
        }
        
        return AvailabilityCheckResult.builder()
                .transferRequestId(request.getId())
                .productAvailabilities(productAvailabilities)
                .canFullyFulfill(canFullyFulfill)
                .totalShortage(totalShortage)
                .totalRequestedQuantity(totalRequestedQuantity)
                .totalAvailableQuantity(totalAvailableQuantity)
                .maxFulfillableQuantity(maxFulfillable)
                .summary(summary)
                .build();
    }


    /**
     * Check availability for a single product
     * Only checks from Store Warehouses (Main Warehouse removed)
     * Requirements: 4.1 - Only consider Store_Warehouses
     * Requirements: 4.2 - Search available quantity from other Store_Warehouses
     */
    private ProductAvailability checkProductAvailability(Long productId, int requestedQuantity, Integer excludeStoreId) {
        // Get product info
        Product product = productRepository.findById(productId.intValue())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOTEXISTED));
        
        // Get store warehouse availabilities (Requirements 4.1, 4.2)
        List<StoreWarehouseAvailability> storeAvailabilities = getStoreWarehouseAvailabilities(productId, excludeStoreId);
        
        // Calculate source allocations (Requirements 4.3, 4.4)
        List<SourceAllocation> allocations = calculateSourceAllocations(productId, requestedQuantity, excludeStoreId);
        
        // Calculate total available from allocations
        int totalAvailable = allocations.stream()
                .mapToInt(SourceAllocation::getQuantity)
                .sum();
        
        // Calculate shortage (Requirements 6.1)
        int shortage = Math.max(0, requestedQuantity - totalAvailable);
        
        return ProductAvailability.builder()
                .productId(productId)
                .productName(product.getName())
                .productSku(product.getSku())
                .requestedQuantity(requestedQuantity)
                .mainWarehouseAvailable(0) // No longer used - Main Warehouse removed
                .storeAvailabilities(storeAvailabilities)
                .totalAvailable(totalAvailable)
                .shortage(shortage)
                .recommendedAllocations(allocations)
                .build();
    }

    /**
     * Get store warehouse availabilities for a product
     * Requirements: 4.2 - Search available quantity from Store_Warehouses
     */
    private List<StoreWarehouseAvailability> getStoreWarehouseAvailabilities(Long productId, Integer excludeStoreId) {
        List<StoreWarehouseAvailability> availabilities = new ArrayList<>();
        
        // Get all active store warehouses (Requirements 4.1 - only Store Warehouses)
        List<Warehouse> storeWarehouses = warehouseRepository.findByIsActiveTrue();
        
        for (Warehouse warehouse : storeWarehouses) {
            // Skip the requesting store's warehouse (Requirements 4.4)
            if (excludeStoreId != null && excludeStoreId.equals(warehouse.getStoreId())) {
                continue;
            }
            
            // Get inventory item for this product in this warehouse
            Optional<InventoryItem> inventoryOpt = inventoryItemRepository
                    .findByWarehouseIdAndProductId(warehouse.getId(), productId);
            
            if (inventoryOpt.isPresent()) {
                InventoryItem inventory = inventoryOpt.get();
                int availableQty = inventory.getAvailableQuantity();
                
                if (availableQty > 0) {
                    String storeName = null;
                    if (warehouse.getStoreId() != null) {
                        storeName = storeLocationRepository.findById(warehouse.getStoreId())
                                .map(StoreLocation::getName)
                                .orElse(null);
                    }
                    
                    availabilities.add(StoreWarehouseAvailability.builder()
                            .warehouseId(warehouse.getId())
                            .warehouseName(warehouse.getName())
                            .storeId(warehouse.getStoreId())
                            .storeName(storeName)
                            .totalQuantity(inventory.getQuantity())
                            .reservedQuantity(inventory.getReservedQuantity())
                            .availableQuantity(availableQty)
                            .build());
                }
            }
        }
        
        // Sort by available quantity descending (Requirements 4.3)
        availabilities.sort((a, b) -> b.getAvailableQuantity().compareTo(a.getAvailableQuantity()));
        
        return availabilities;
    }


    /**
     * Calculate source allocations for a product
     * Only considers Store Warehouses (Main Warehouse removed)
     * Requirements: 4.1 - Only consider Store_Warehouses
     * Requirements: 4.3 - Prioritize by available quantity (highest first)
     * Requirements: 4.4 - Exclude requesting store's warehouse
     */
    @Override
    public List<SourceAllocation> calculateSourceAllocations(Long productId, int requiredQuantity, Integer excludeStoreId) {
        List<SourceAllocation> allocations = new ArrayList<>();
        int remainingQuantity = requiredQuantity;
        
        // Get all active store warehouses (Requirements 4.1 - only Store Warehouses)
        List<Warehouse> storeWarehouses = warehouseRepository.findByIsActiveTrue();
        
        // Sort store warehouses by available quantity (descending) for optimal allocation
        // Requirements 4.3 - prioritize by available quantity (highest first)
        List<WarehouseWithAvailability> warehousesWithAvailability = new ArrayList<>();
        
        for (Warehouse warehouse : storeWarehouses) {
            // Skip the requesting store's warehouse (Requirements 4.4)
            if (excludeStoreId != null && excludeStoreId.equals(warehouse.getStoreId())) {
                continue;
            }
            
            int available = getWarehouseAvailableQuantity(productId, warehouse.getId());
            if (available > 0) {
                warehousesWithAvailability.add(new WarehouseWithAvailability(warehouse, available));
            }
        }
        
        // Sort by available quantity descending (Requirements 4.3)
        warehousesWithAvailability.sort((a, b) -> Integer.compare(b.available, a.available));
        
        // Aggregate from multiple stores
        for (WarehouseWithAvailability wwa : warehousesWithAvailability) {
            if (remainingQuantity <= 0) {
                break;
            }
            
            int allocateFromStore = Math.min(wwa.available, remainingQuantity);
            
            String storeName = null;
            if (wwa.warehouse.getStoreId() != null) {
                storeName = storeLocationRepository.findById(wwa.warehouse.getStoreId())
                        .map(StoreLocation::getName)
                        .orElse(null);
            }
            
            allocations.add(SourceAllocation.builder()
                    .warehouseId(wwa.warehouse.getId())
                    .warehouseName(wwa.warehouse.getName())
                    .warehouseType(wwa.warehouse.getType())
                    .storeId(wwa.warehouse.getStoreId())
                    .storeName(storeName)
                    .quantity(allocateFromStore)
                    .build());
            
            remainingQuantity -= allocateFromStore;
        }
        
        return allocations;
    }
    
    /**
     * Helper class for sorting warehouses by availability
     */
    private static class WarehouseWithAvailability {
        final Warehouse warehouse;
        final int available;
        
        WarehouseWithAvailability(Warehouse warehouse, int available) {
            this.warehouse = warehouse;
            this.available = available;
        }
    }


    /**
     * Fulfill a transfer request by ID
     */
    @Override
    @Transactional
    public FulfillmentResult fulfillById(Long requestId) {
        TransferRequest request = transferRequestRepository.findById(requestId)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSFER_REQUEST_NOT_FOUND));
        return fulfill(request);
    }

    /**
     * Fulfill a transfer request
     * Requirements: 3.3 - WHEN Transfer_Request is approved THEN the System SHALL decrease inventory in source Store_Warehouse and increase in destination Store_Warehouse
     * Requirements: 5.2 - WHEN Inventory_Request is approved THEN the System SHALL create Internal_Shipment from source to destination store
     * Requirements: 5.5 - Deduct quantities from source warehouses
     * Requirements: 5.6 - Allow partial fulfillment with Debt_Order creation
     */
    @Override
    @Transactional
    public FulfillmentResult fulfill(TransferRequest request) {
        // Validate request status
        if (request.getStatus() != TransferRequestStatus.PENDING && 
            request.getStatus() != TransferRequestStatus.APPROVED) {
            throw new AppException(ErrorCode.TRANSFER_REQUEST_ALREADY_PROCESSED);
        }
        
        // Load items
        List<TransferRequestItem> items = request.getItems();
        if (items == null || items.isEmpty()) {
            items = transferRequestItemRepository.findByTransferRequestId(request.getId());
        }
        
        // Get destination warehouse for the requesting store
        Warehouse destinationWarehouse = warehouseRepository.findByStoreIdAndIsActiveTrue(request.getStoreId())
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        
        Map<Long, Integer> fulfilledQuantities = new HashMap<>();
        Map<Long, Integer> shortageQuantities = new HashMap<>();
        Map<Long, List<SourceAllocation>> sourceAllocationsMap = new HashMap<>();
        
        boolean fullyFulfilled = true;
        
        // Process each item
        for (TransferRequestItem item : items) {
            // Calculate allocations for this product
            List<SourceAllocation> allocations = calculateSourceAllocations(
                    item.getProductId(), 
                    item.getRequestedQuantity(),
                    request.getStoreId()
            );
            
            // Calculate total allocated
            int totalAllocated = allocations.stream()
                    .mapToInt(SourceAllocation::getQuantity)
                    .sum();
            
            // Deduct inventory from source warehouses (Requirements 3.3, 5.5)
            for (SourceAllocation allocation : allocations) {
                deductInventory(
                        allocation.getWarehouseId(),
                        item.getProductId(),
                        allocation.getQuantity(),
                        "Transfer Request #" + request.getId() + " fulfillment - source deduction"
                );
            }
            
            // Increase inventory at destination warehouse (Requirements 3.3)
            if (totalAllocated > 0) {
                increaseInventory(
                        destinationWarehouse.getId(),
                        item.getProductId(),
                        totalAllocated,
                        "Transfer Request #" + request.getId() + " fulfillment - destination receipt"
                );
            }
            
            // Update item fulfilled quantity
            item.setFulfilledQuantity(totalAllocated);
            transferRequestItemRepository.save(item);
            
            fulfilledQuantities.put(item.getProductId(), totalAllocated);
            sourceAllocationsMap.put(item.getProductId(), allocations);
            
            // Check for shortage
            int shortage = item.getRequestedQuantity() - totalAllocated;
            if (shortage > 0) {
                shortageQuantities.put(item.getProductId(), shortage);
                fullyFulfilled = false;
            }
        }
        
        // Update request status
        TransferRequestStatus newStatus;
        Long debtOrderId = null;
        List<Long> shipmentIds = new ArrayList<>();
        
        if (fullyFulfilled) {
            newStatus = TransferRequestStatus.COMPLETED;
        } else if (!fulfilledQuantities.isEmpty() && 
                   fulfilledQuantities.values().stream().anyMatch(q -> q > 0)) {
            // Partial fulfillment (Requirements 5.6)
            newStatus = TransferRequestStatus.PARTIAL;
            
            // Create debt order for shortage quantities (Requirements 5.6, 6.1)
            if (!shortageQuantities.isEmpty()) {
                try {
                    var debtOrder = debtOrderService.createDebtOrder(request, shortageQuantities);
                    debtOrderId = debtOrder.getId();
                    log.info("Created debt order {} for transfer request {} with shortages: {}", 
                            debtOrderId, request.getId(), shortageQuantities);
                } catch (Exception e) {
                    log.error("Failed to create debt order for transfer request {}: {}", 
                            request.getId(), e.getMessage());
                }
            }
        } else {
            // Nothing could be fulfilled
            newStatus = TransferRequestStatus.PENDING;
        }
        
        // Create Internal Shipment records (Requirements 5.2)
        if (!sourceAllocationsMap.isEmpty() && 
            sourceAllocationsMap.values().stream().anyMatch(list -> !list.isEmpty())) {
            try {
                List<InternalShipment> shipments = internalShipmentService.createShipments(request, sourceAllocationsMap);
                shipmentIds = shipments.stream()
                        .map(InternalShipment::getId)
                        .collect(Collectors.toList());
                log.info("Created {} internal shipments for transfer request {}", shipments.size(), request.getId());
            } catch (Exception e) {
                log.error("Failed to create internal shipments for transfer request {}: {}", 
                        request.getId(), e.getMessage());
            }
        }
        
        request.setStatus(newStatus);
        transferRequestRepository.save(request);
        
        String summary;
        if (fullyFulfilled) {
            summary = "Yêu cầu đã được đáp ứng đầy đủ";
        } else if (newStatus == TransferRequestStatus.PARTIAL) {
            int totalFulfilled = fulfilledQuantities.values().stream().mapToInt(Integer::intValue).sum();
            int totalShortage = shortageQuantities.values().stream().mapToInt(Integer::intValue).sum();
            summary = String.format("Đáp ứng một phần: %d sản phẩm. Thiếu: %d sản phẩm", 
                    totalFulfilled, totalShortage);
        } else {
            summary = "Không thể đáp ứng yêu cầu do không đủ hàng";
        }
        
        log.info("Fulfilled transfer request {}: status={}, fullyFulfilled={}, shipments={}", 
                request.getId(), newStatus, fullyFulfilled, shipmentIds.size());
        
        return FulfillmentResult.builder()
                .transferRequestId(request.getId())
                .newStatus(newStatus)
                .fullyFulfilled(fullyFulfilled)
                .fulfilledQuantities(fulfilledQuantities)
                .shortageQuantities(shortageQuantities)
                .sourceAllocations(sourceAllocationsMap)
                .debtOrderId(debtOrderId)
                .shipmentIds(shipmentIds)
                .summary(summary)
                .build();
    }


    /**
     * Deduct inventory from a warehouse
     * Requirements: 3.3, 5.5
     */
    private void deductInventory(Long warehouseId, Long productId, int quantity, String reason) {
        InventoryItem inventoryItem = inventoryItemRepository
                .findByWarehouseIdAndProductId(warehouseId, productId)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_INVENTORY_NOT_FOUND));
        
        int previousQuantity = inventoryItem.getQuantity();
        int newQuantity = previousQuantity - quantity;
        
        if (newQuantity < 0) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
        }
        
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
        
        FulfillmentService.log.debug("Deducted {} units of product {} from warehouse {}: {} -> {}", 
                quantity, productId, warehouseId, previousQuantity, newQuantity);
    }

    /**
     * Increase inventory at a warehouse (destination)
     * Requirements: 3.3 - WHEN Transfer_Request is approved THEN the System SHALL increase inventory in destination Store_Warehouse
     */
    private void increaseInventory(Long warehouseId, Long productId, int quantity, String reason) {
        // Get or create inventory item at destination
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
        
        FulfillmentService.log.debug("Increased {} units of product {} at warehouse {}: {} -> {}", 
                quantity, productId, warehouseId, previousQuantity, newQuantity);
    }

    /**
     * Get available quantity in a Store Warehouse
     * Requirements: 4.2 - Search available quantity from Store_Warehouses
     */
    @Override
    public int getStoreWarehouseAvailableQuantity(Long productId, Long warehouseId) {
        return getWarehouseAvailableQuantity(productId, warehouseId);
    }

    /**
     * Get available quantity for a product in a specific warehouse
     * Requirements: 9.3 - Available = Total - Reserved
     */
    private int getWarehouseAvailableQuantity(Long productId, Long warehouseId) {
        return inventoryItemRepository.findByWarehouseIdAndProductId(warehouseId, productId)
                .map(InventoryItem::getAvailableQuantity)
                .orElse(0);
    }

    /**
     * Calculate total available quantity for a product across all active store warehouses
     * Requirements: 6.1 - Calculate total available from all Store_Warehouses
     * 
     * @param productId The product ID
     * @param excludeStoreId Optional store ID to exclude (the requesting store)
     * @return Total available quantity across all eligible store warehouses
     */
    @Override
    public int calculateTotalAvailableQuantity(Long productId, Integer excludeStoreId) {
        List<Warehouse> storeWarehouses = warehouseRepository.findByIsActiveTrue();
        
        int totalAvailable = 0;
        for (Warehouse warehouse : storeWarehouses) {
            // Skip the requesting store's warehouse if specified
            if (excludeStoreId != null && excludeStoreId.equals(warehouse.getStoreId())) {
                continue;
            }
            totalAvailable += getWarehouseAvailableQuantity(productId, warehouse.getId());
        }
        
        return totalAvailable;
    }

    /**
     * Calculate shortage for a product
     * Requirements: 6.1 - WHEN total available quantity across all Store_Warehouses is less than requested 
     *               THEN the System SHALL calculate and report the shortage
     * 
     * Shortage = requested_quantity - total_available_quantity
     * 
     * @param productId The product ID
     * @param requestedQuantity The requested quantity
     * @param excludeStoreId Optional store ID to exclude (the requesting store)
     * @return Shortage quantity (0 if no shortage)
     */
    @Override
    public int calculateShortage(Long productId, int requestedQuantity, Integer excludeStoreId) {
        int totalAvailable = calculateTotalAvailableQuantity(productId, excludeStoreId);
        return Math.max(0, requestedQuantity - totalAvailable);
    }

    /**
     * Calculate total shortage for a transfer request
     * Requirements: 6.1 - Calculate shortage = requested - total available from all store warehouses
     * 
     * @param request The transfer request
     * @return Map of product ID to shortage quantity
     */
    public Map<Long, Integer> calculateRequestShortages(TransferRequest request) {
        Map<Long, Integer> shortages = new HashMap<>();
        
        List<TransferRequestItem> items = request.getItems();
        if (items == null || items.isEmpty()) {
            items = transferRequestItemRepository.findByTransferRequestId(request.getId());
        }
        
        for (TransferRequestItem item : items) {
            int shortage = calculateShortage(item.getProductId(), item.getRequestedQuantity(), request.getStoreId());
            if (shortage > 0) {
                shortages.put(item.getProductId(), shortage);
            }
        }
        
        return shortages;
    }
}
