package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.DebtItemDTO;
import com.example.onlyfanshop_be.dto.DebtOrderDTO;
import com.example.onlyfanshop_be.dto.response.SourceAllocation;
import com.example.onlyfanshop_be.entity.*;
import com.example.onlyfanshop_be.enums.DebtOrderStatus;
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
 * Implementation of DebtOrderService
 * Handles debt order creation, fulfillment, and management
 * Requirements: 6.1, 6.2, 6.3, 6.4, 6.5
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DebtOrderService implements IDebtOrderService {
    
    private final DebtOrderRepository debtOrderRepository;
    private final DebtItemRepository debtItemRepository;
    private final TransferRequestRepository transferRequestRepository;
    private final ProductRepository productRepository;
    private final StoreLocationRepository storeLocationRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryLogRepository inventoryLogRepository;


    /**
     * Create a debt order for unfulfilled quantities from a transfer request
     * Requirements: 6.1 - WHEN a Transfer_Request cannot be fully fulfilled THEN the System SHALL create a Debt_Order with remaining quantities
     * Requirements: 6.2 - WHEN a Debt_Order is created THEN the System SHALL link the Debt_Order to the original Transfer_Request
     */
    @Override
    @Transactional
    public DebtOrderDTO createDebtOrder(TransferRequest request, Map<Long, Integer> shortageQuantities) {
        if (shortageQuantities == null || shortageQuantities.isEmpty()) {
            throw new AppException(ErrorCode.DEBT_ORDER_EMPTY_ITEMS);
        }
        
        // Check if debt order already exists for this transfer request
        Optional<DebtOrder> existingDebtOrder = debtOrderRepository.findByTransferRequestId(request.getId());
        if (existingDebtOrder.isPresent()) {
            throw new AppException(ErrorCode.DEBT_ORDER_ALREADY_EXISTS);
        }
        
        // Create debt order linked to transfer request (Requirements 6.2)
        DebtOrder debtOrder = DebtOrder.builder()
                .transferRequestId(request.getId())
                .status(DebtOrderStatus.PENDING)
                .build();
        
        DebtOrder savedDebtOrder = debtOrderRepository.save(debtOrder);
        
        // Create debt items for each shortage (Requirements 6.1)
        List<DebtItem> debtItems = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : shortageQuantities.entrySet()) {
            Long productId = entry.getKey();
            Integer owedQuantity = entry.getValue();
            
            if (owedQuantity > 0) {
                DebtItem debtItem = DebtItem.builder()
                        .debtOrderId(savedDebtOrder.getId())
                        .productId(productId)
                        .owedQuantity(owedQuantity)
                        .fulfilledQuantity(0)
                        .build();
                debtItems.add(debtItemRepository.save(debtItem));
            }
        }
        
        savedDebtOrder.setItems(debtItems);
        
        log.info("Created debt order {} for transfer request {} with {} items", 
                savedDebtOrder.getId(), request.getId(), debtItems.size());
        
        return convertToDebtOrderDTO(savedDebtOrder);
    }

    /**
     * Get all debt orders with optional status filter
     */
    @Override
    public Page<DebtOrderDTO> getDebtOrders(DebtOrderStatus status, Pageable pageable) {
        Page<DebtOrder> debtOrders;
        
        if (status != null) {
            debtOrders = debtOrderRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            debtOrders = debtOrderRepository.findAll(pageable);
        }
        
        List<DebtOrderDTO> dtos = debtOrders.getContent().stream()
                .map(this::convertToDebtOrderDTO)
                .collect(Collectors.toList());
        
        return new PageImpl<>(dtos, pageable, debtOrders.getTotalElements());
    }

    /**
     * Get a specific debt order by ID
     */
    @Override
    public DebtOrderDTO getDebtOrder(Long id) {
        DebtOrder debtOrder = debtOrderRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DEBT_ORDER_NOT_FOUND));
        
        return convertToDebtOrderDTO(debtOrder);
    }


    /**
     * Fulfill a debt order by deducting inventory from Main Warehouse
     * Requirements: 6.5 - WHEN a Debt_Order is fulfilled THEN the System SHALL update Debt_Order status to COMPLETED and create Shipment
     */
    @Override
    @Transactional
    public DebtOrderDTO fulfillDebtOrder(Long id) {
        DebtOrder debtOrder = debtOrderRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DEBT_ORDER_NOT_FOUND));
        
        // Check if already completed
        if (debtOrder.getStatus() == DebtOrderStatus.COMPLETED) {
            throw new AppException(ErrorCode.DEBT_ORDER_ALREADY_COMPLETED);
        }
        
        // Check if can be fulfilled
        if (!canFulfillDebtOrder(id)) {
            throw new AppException(ErrorCode.DEBT_ORDER_CANNOT_FULFILL);
        }
        
        // Get Main Warehouse
        Warehouse mainWarehouse = warehouseRepository.findFirstByType(WarehouseType.MAIN)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        
        // Load items
        List<DebtItem> items = debtItemRepository.findByDebtOrderId(id);
        
        // Deduct inventory and update fulfilled quantities
        for (DebtItem item : items) {
            int remainingQuantity = item.getRemainingQuantity();
            if (remainingQuantity > 0) {
                // Deduct from Main Warehouse
                deductInventory(mainWarehouse.getId(), item.getProductId(), remainingQuantity,
                        "Debt Order #" + id + " fulfillment");
                
                // Update fulfilled quantity
                item.setFulfilledQuantity(item.getOwedQuantity());
                debtItemRepository.save(item);
            }
        }
        
        // Update debt order status
        debtOrder.setStatus(DebtOrderStatus.COMPLETED);
        debtOrder.setFulfilledAt(LocalDateTime.now());
        debtOrderRepository.save(debtOrder);
        
        log.info("Fulfilled debt order {}", id);
        
        // Note: Shipment creation will be handled by ShipmentService in Task 9
        
        return convertToDebtOrderDTO(debtOrder);
    }

    /**
     * Check all pending debt orders and mark as FULFILLABLE if inventory is available
     * Requirements: 6.3 - WHEN Admin updates Main_Warehouse inventory THEN the System SHALL check if any Debt_Orders can be fulfilled
     * Requirements: 6.4 - WHEN a Debt_Order can be fulfilled THEN the System SHALL notify Admin and allow fulfillment processing
     */
    @Override
    @Transactional
    public List<DebtOrderDTO> checkFulfillableDebtOrders() {
        List<DebtOrderDTO> fulfillableOrders = new ArrayList<>();
        
        // Get all PENDING debt orders
        List<DebtOrder> pendingOrders = debtOrderRepository.findByStatus(DebtOrderStatus.PENDING);
        
        for (DebtOrder debtOrder : pendingOrders) {
            if (canFulfillDebtOrder(debtOrder.getId())) {
                // Update status to FULFILLABLE
                debtOrder.setStatus(DebtOrderStatus.FULFILLABLE);
                debtOrderRepository.save(debtOrder);
                
                fulfillableOrders.add(convertToDebtOrderDTO(debtOrder));
                
                log.info("Debt order {} is now fulfillable", debtOrder.getId());
                
                // Note: Notification will be handled by NotificationService in Task 10
            }
        }
        
        return fulfillableOrders;
    }


    /**
     * Check if a specific debt order can be fulfilled based on current inventory
     */
    @Override
    public boolean canFulfillDebtOrder(Long debtOrderId) {
        DebtOrder debtOrder = debtOrderRepository.findById(debtOrderId)
                .orElseThrow(() -> new AppException(ErrorCode.DEBT_ORDER_NOT_FOUND));
        
        if (debtOrder.getStatus() == DebtOrderStatus.COMPLETED) {
            return false;
        }
        
        // Get Main Warehouse
        Warehouse mainWarehouse = warehouseRepository.findFirstByType(WarehouseType.MAIN)
                .orElse(null);
        
        if (mainWarehouse == null) {
            return false;
        }
        
        // Load items
        List<DebtItem> items = debtItemRepository.findByDebtOrderId(debtOrderId);
        
        // Check if all items can be fulfilled from Main Warehouse
        for (DebtItem item : items) {
            int remainingQuantity = item.getRemainingQuantity();
            if (remainingQuantity > 0) {
                int availableQuantity = getMainWarehouseAvailableQuantity(item.getProductId(), mainWarehouse.getId());
                if (availableQuantity < remainingQuantity) {
                    return false;
                }
            }
        }
        
        return true;
    }

    /**
     * Get debt order by transfer request ID
     */
    @Override
    public DebtOrderDTO getDebtOrderByTransferRequestId(Long transferRequestId) {
        DebtOrder debtOrder = debtOrderRepository.findByTransferRequestId(transferRequestId)
                .orElseThrow(() -> new AppException(ErrorCode.DEBT_ORDER_NOT_FOUND));
        
        return convertToDebtOrderDTO(debtOrder);
    }

    // ==================== Private Helper Methods ====================

    /**
     * Get available quantity in Main Warehouse for a product
     */
    private int getMainWarehouseAvailableQuantity(Long productId, Long warehouseId) {
        return inventoryItemRepository.findByWarehouseIdAndProductId(warehouseId, productId)
                .map(InventoryItem::getAvailableQuantity)
                .orElse(0);
    }

    /**
     * Deduct inventory from a warehouse
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
        InventoryLog inventoryLog = InventoryLog.builder()
                .warehouseId(warehouseId)
                .productId(productId)
                .previousQuantity(previousQuantity)
                .newQuantity(newQuantity)
                .reason(reason)
                .build();
        inventoryLogRepository.save(inventoryLog);
        
        log.debug("Deducted {} units of product {} from warehouse {}: {} -> {}", 
                quantity, productId, warehouseId, previousQuantity, newQuantity);
    }


    /**
     * Convert DebtOrder entity to DebtOrderDTO
     */
    private DebtOrderDTO convertToDebtOrderDTO(DebtOrder debtOrder) {
        // Load items if not already loaded
        List<DebtItem> items = debtOrder.getItems();
        if (items == null || items.isEmpty()) {
            items = debtItemRepository.findByDebtOrderId(debtOrder.getId());
        }
        
        // Get store info from transfer request
        Integer storeId = null;
        String storeName = null;
        if (debtOrder.getTransferRequestId() != null) {
            TransferRequest transferRequest = transferRequestRepository
                    .findById(debtOrder.getTransferRequestId())
                    .orElse(null);
            if (transferRequest != null) {
                storeId = transferRequest.getStoreId();
                if (storeId != null) {
                    storeName = storeLocationRepository.findById(storeId)
                            .map(StoreLocation::getName)
                            .orElse(null);
                }
            }
        }
        
        // Convert items
        List<DebtItemDTO> itemDTOs = items.stream()
                .map(this::convertToDebtItemDTO)
                .collect(Collectors.toList());
        
        // Calculate totals
        int totalOwed = items.stream().mapToInt(DebtItem::getOwedQuantity).sum();
        int totalFulfilled = items.stream().mapToInt(DebtItem::getFulfilledQuantity).sum();
        int totalRemaining = items.stream().mapToInt(DebtItem::getRemainingQuantity).sum();
        
        return DebtOrderDTO.builder()
                .id(debtOrder.getId())
                .transferRequestId(debtOrder.getTransferRequestId())
                .storeId(storeId)
                .storeName(storeName)
                .status(debtOrder.getStatus())
                .createdAt(debtOrder.getCreatedAt())
                .fulfilledAt(debtOrder.getFulfilledAt())
                .items(itemDTOs)
                .totalOwedQuantity(totalOwed)
                .totalFulfilledQuantity(totalFulfilled)
                .totalRemainingQuantity(totalRemaining)
                .build();
    }

    /**
     * Convert DebtItem entity to DebtItemDTO
     */
    private DebtItemDTO convertToDebtItemDTO(DebtItem item) {
        String productName = null;
        String productSku = null;
        
        if (item.getProductId() != null) {
            Product product = productRepository.findById(item.getProductId().intValue()).orElse(null);
            if (product != null) {
                productName = product.getName();
                productSku = product.getSku();
            }
        }
        
        return DebtItemDTO.builder()
                .id(item.getId())
                .debtOrderId(item.getDebtOrderId())
                .productId(item.getProductId())
                .productName(productName)
                .productSku(productSku)
                .owedQuantity(item.getOwedQuantity())
                .fulfilledQuantity(item.getFulfilledQuantity())
                .remainingQuantity(item.getRemainingQuantity())
                .fullyFulfilled(item.isFullyFulfilled())
                .build();
    }
}
