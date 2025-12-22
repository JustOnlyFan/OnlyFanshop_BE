package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.response.*;
import com.example.onlyfanshop_be.entity.*;
import com.example.onlyfanshop_be.enums.TransferRequestStatus;
import com.example.onlyfanshop_be.enums.WarehouseType;
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
 * Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 9.1, 9.2, 9.3, 9.4, 9.5
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
     * Requirements: 5.1, 9.1, 9.2, 9.4, 9.5
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
     * Requirements: 9.1, 9.2, 9.3, 9.4, 9.5
     */
    private ProductAvailability checkProductAvailability(Long productId, int requestedQuantity, Integer excludeStoreId) {
        // Get product info
        Product product = productRepository.findById(productId.intValue())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOTEXISTED));
        
        // Get Main Warehouse availability (Requirements 9.1)
        int mainWarehouseAvailable = getMainWarehouseAvailableQuantity(productId);
        
        // Calculate source allocations (Requirements 9.4)
        List<SourceAllocation> allocations = calculateSourceAllocations(productId, requestedQuantity, excludeStoreId);
        
        // Calculate total available from allocations
        int totalAvailable = allocations.stream()
                .mapToInt(SourceAllocation::getQuantity)
                .sum();
        
        // Get store warehouse availabilities if main is insufficient (Requirements 9.2)
        List<StoreWarehouseAvailability> storeAvailabilities = new ArrayList<>();
        if (mainWarehouseAvailable < requestedQuantity) {
            storeAvailabilities = getStoreWarehouseAvailabilities(productId, excludeStoreId);
        }
        
        // Calculate shortage (Requirements 9.5)
        int shortage = Math.max(0, requestedQuantity - totalAvailable);
        
        return ProductAvailability.builder()
                .productId(productId)
                .productName(product.getName())
                .productSku(product.getSku())
                .requestedQuantity(requestedQuantity)
                .mainWarehouseAvailable(mainWarehouseAvailable)
                .storeAvailabilities(storeAvailabilities)
                .totalAvailable(totalAvailable)
                .shortage(shortage)
                .recommendedAllocations(allocations)
                .build();
    }

    /**
     * Get store warehouse availabilities for a product
     * Requirements: 9.2
     */
    private List<StoreWarehouseAvailability> getStoreWarehouseAvailabilities(Long productId, Integer excludeStoreId) {
        List<StoreWarehouseAvailability> availabilities = new ArrayList<>();
        
        // Get all store warehouses
        List<Warehouse> storeWarehouses = warehouseRepository.findByType(WarehouseType.STORE);
        
        for (Warehouse warehouse : storeWarehouses) {
            // Skip the requesting store's warehouse
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
        
        // Sort by available quantity descending
        availabilities.sort((a, b) -> b.getAvailableQuantity().compareTo(a.getAvailableQuantity()));
        
        return availabilities;
    }


    /**
     * Calculate source allocations for a product
     * Requirements: 5.2 - Check Main_Warehouse first
     * Requirements: 5.3 - Check Store_Warehouses when main is insufficient
     * Requirements: 5.4 - Aggregate from multiple stores
     */
    @Override
    public List<SourceAllocation> calculateSourceAllocations(Long productId, int requiredQuantity, Integer excludeStoreId) {
        List<SourceAllocation> allocations = new ArrayList<>();
        int remainingQuantity = requiredQuantity;
        
        // Step 1: Check Main Warehouse first (Requirements 5.2)
        Warehouse mainWarehouse = warehouseRepository.findFirstByType(WarehouseType.MAIN)
                .orElse(null);
        
        if (mainWarehouse != null) {
            int mainAvailable = getWarehouseAvailableQuantity(productId, mainWarehouse.getId());
            
            if (mainAvailable > 0) {
                int allocateFromMain = Math.min(mainAvailable, remainingQuantity);
                allocations.add(SourceAllocation.builder()
                        .warehouseId(mainWarehouse.getId())
                        .warehouseName(mainWarehouse.getName())
                        .warehouseType(WarehouseType.MAIN)
                        .storeId(null)
                        .storeName(null)
                        .quantity(allocateFromMain)
                        .build());
                remainingQuantity -= allocateFromMain;
            }
        }
        
        // Step 2: If still need more, check Store Warehouses (Requirements 5.3, 5.4)
        if (remainingQuantity > 0) {
            List<Warehouse> storeWarehouses = warehouseRepository.findByType(WarehouseType.STORE);
            
            // Sort store warehouses by available quantity (descending) for optimal allocation
            List<WarehouseWithAvailability> warehousesWithAvailability = new ArrayList<>();
            
            for (Warehouse warehouse : storeWarehouses) {
                // Skip the requesting store's warehouse
                if (excludeStoreId != null && excludeStoreId.equals(warehouse.getStoreId())) {
                    continue;
                }
                
                int available = getWarehouseAvailableQuantity(productId, warehouse.getId());
                if (available > 0) {
                    warehousesWithAvailability.add(new WarehouseWithAvailability(warehouse, available));
                }
            }
            
            // Sort by available quantity descending
            warehousesWithAvailability.sort((a, b) -> Integer.compare(b.available, a.available));
            
            // Aggregate from multiple stores (Requirements 5.4)
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
                        .warehouseType(WarehouseType.STORE)
                        .storeId(wwa.warehouse.getStoreId())
                        .storeName(storeName)
                        .quantity(allocateFromStore)
                        .build());
                
                remainingQuantity -= allocateFromStore;
            }
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
            
            // Deduct inventory from source warehouses (Requirements 5.5)
            for (SourceAllocation allocation : allocations) {
                deductInventory(
                        allocation.getWarehouseId(),
                        item.getProductId(),
                        allocation.getQuantity(),
                        "Transfer Request #" + request.getId() + " fulfillment"
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
        
        if (fullyFulfilled) {
            newStatus = TransferRequestStatus.COMPLETED;
        } else if (!fulfilledQuantities.isEmpty() && 
                   fulfilledQuantities.values().stream().anyMatch(q -> q > 0)) {
            // Partial fulfillment (Requirements 5.6)
            newStatus = TransferRequestStatus.PARTIAL;
            // Note: Debt order creation will be handled by DebtOrderService in Task 7
        } else {
            // Nothing could be fulfilled
            newStatus = TransferRequestStatus.PENDING;
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
        
        log.info("Fulfilled transfer request {}: status={}, fullyFulfilled={}", 
                request.getId(), newStatus, fullyFulfilled);
        
        return FulfillmentResult.builder()
                .transferRequestId(request.getId())
                .newStatus(newStatus)
                .fullyFulfilled(fullyFulfilled)
                .fulfilledQuantities(fulfilledQuantities)
                .shortageQuantities(shortageQuantities)
                .sourceAllocations(sourceAllocationsMap)
                .debtOrderId(debtOrderId)
                .shipmentIds(new ArrayList<>()) // Shipments will be created by ShipmentService in Task 9
                .summary(summary)
                .build();
    }


    /**
     * Deduct inventory from a warehouse
     * Requirements: 5.5
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
     * Get available quantity in Main Warehouse
     * Requirements: 9.3 - Exclude reserved quantities
     */
    @Override
    public int getMainWarehouseAvailableQuantity(Long productId) {
        Warehouse mainWarehouse = warehouseRepository.findFirstByType(WarehouseType.MAIN)
                .orElse(null);
        
        if (mainWarehouse == null) {
            return 0;
        }
        
        return getWarehouseAvailableQuantity(productId, mainWarehouse.getId());
    }

    /**
     * Get available quantity in a Store Warehouse
     * Requirements: 9.3 - Exclude reserved quantities
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
}
