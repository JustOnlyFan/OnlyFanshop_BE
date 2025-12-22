package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.TransferRequestDTO;
import com.example.onlyfanshop_be.dto.TransferRequestItemDTO;
import com.example.onlyfanshop_be.dto.request.CreateTransferRequestDTO;
import com.example.onlyfanshop_be.dto.request.CreateTransferRequestItemDTO;
import com.example.onlyfanshop_be.entity.*;
import com.example.onlyfanshop_be.enums.TransferRequestStatus;
import com.example.onlyfanshop_be.enums.WarehouseType;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of TransferRequestService
 * Handles transfer request operations including creation, validation, approval, and rejection
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TransferRequestService implements ITransferRequestService {
    
    /**
     * Maximum quantity allowed per product per request
     * Requirements: 4.2
     */
    public static final int MAX_QUANTITY_PER_PRODUCT = 30;
    
    private final TransferRequestRepository transferRequestRepository;
    private final TransferRequestItemRepository transferRequestItemRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final ProductRepository productRepository;
    private final StoreLocationRepository storeLocationRepository;
    private final UserRepository userRepository;


    /**
     * Create a new transfer request for a store
     * Requirements: 4.1, 4.2, 4.3, 4.4
     */
    @Override
    @Transactional
    public TransferRequestDTO createRequest(Integer storeId, CreateTransferRequestDTO request) {
        // Validate store exists
        StoreLocation store = storeLocationRepository.findById(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));
        
        // Validate request has items
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new AppException(ErrorCode.TRANSFER_REQUEST_EMPTY_ITEMS);
        }
        
        // Get store warehouse
        Warehouse storeWarehouse = warehouseRepository.findByStoreId(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        
        // Validate all items
        for (CreateTransferRequestItemDTO item : request.getItems()) {
            validateTransferRequestItem(item, storeWarehouse.getId());
        }
        
        // Create transfer request
        TransferRequest transferRequest = TransferRequest.builder()
                .storeId(storeId)
                .status(TransferRequestStatus.PENDING)
                .build();
        
        TransferRequest savedRequest = transferRequestRepository.save(transferRequest);
        
        // Create transfer request items
        List<TransferRequestItem> items = new ArrayList<>();
        for (CreateTransferRequestItemDTO itemDTO : request.getItems()) {
            TransferRequestItem item = TransferRequestItem.builder()
                    .transferRequestId(savedRequest.getId())
                    .productId(itemDTO.getProductId())
                    .requestedQuantity(itemDTO.getQuantity())
                    .fulfilledQuantity(0)
                    .build();
            items.add(item);
        }
        
        transferRequestItemRepository.saveAll(items);
        savedRequest.setItems(items);
        
        log.info("Created transfer request {} for store {} with {} items", 
                savedRequest.getId(), storeId, items.size());
        
        return convertToDTO(savedRequest, store, null);
    }
    
    /**
     * Validate a single transfer request item
     * Requirements: 4.1 - Validate product exists in Store_Warehouse
     * Requirements: 4.2 - Enforce max 30 units per product
     */
    private void validateTransferRequestItem(CreateTransferRequestItemDTO item, Long storeWarehouseId) {
        // Validate quantity is positive
        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            throw new AppException(ErrorCode.TRANSFER_REQUEST_INVALID_QUANTITY);
        }
        
        // Validate quantity does not exceed limit (Requirements 4.2)
        if (item.getQuantity() > MAX_QUANTITY_PER_PRODUCT) {
            throw new AppException(ErrorCode.TRANSFER_REQUEST_QUANTITY_EXCEEDS_LIMIT);
        }
        
        // Validate product exists
        if (item.getProductId() == null) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }
        
        productRepository.findById(item.getProductId().intValue())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOTEXISTED));
        
        // Validate product exists in Store_Warehouse (Requirements 4.1)
        boolean existsInStoreWarehouse = inventoryItemRepository
                .existsByWarehouseIdAndProductId(storeWarehouseId, item.getProductId());
        
        if (!existsInStoreWarehouse) {
            throw new AppException(ErrorCode.TRANSFER_REQUEST_PRODUCT_NOT_IN_STORE);
        }
    }


    /**
     * Get all transfer requests with optional status filter
     * Requirements: 4.5
     */
    @Override
    public Page<TransferRequestDTO> getRequests(TransferRequestStatus status, Pageable pageable) {
        Page<TransferRequest> requests;
        
        if (status != null) {
            requests = transferRequestRepository.findByStatusOrderByCreatedAtDesc(status, pageable);
        } else {
            requests = transferRequestRepository.findAll(pageable);
        }
        
        return requests.map(this::convertToDTO);
    }
    
    /**
     * Get transfer requests for a specific store
     */
    @Override
    public Page<TransferRequestDTO> getRequestsByStore(Integer storeId, TransferRequestStatus status, Pageable pageable) {
        Page<TransferRequest> requests;
        
        if (status != null) {
            requests = transferRequestRepository.findByStoreIdAndStatusOrderByCreatedAtDesc(storeId, status, pageable);
        } else {
            requests = transferRequestRepository.findByStoreIdOrderByCreatedAtDesc(storeId, pageable);
        }
        
        return requests.map(this::convertToDTO);
    }
    
    /**
     * Get a specific transfer request by ID
     */
    @Override
    public TransferRequestDTO getRequest(Long id) {
        TransferRequest request = transferRequestRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSFER_REQUEST_NOT_FOUND));
        
        return convertToDTO(request);
    }
    
    /**
     * Approve a transfer request
     * Note: This is a basic approval that changes status. Full fulfillment logic will be in FulfillmentService (Task 6)
     */
    @Override
    @Transactional
    public TransferRequestDTO approveRequest(Long id) {
        TransferRequest request = transferRequestRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSFER_REQUEST_NOT_FOUND));
        
        // Validate status is PENDING
        if (request.getStatus() != TransferRequestStatus.PENDING) {
            throw new AppException(ErrorCode.TRANSFER_REQUEST_ALREADY_PROCESSED);
        }
        
        // Update status
        request.setStatus(TransferRequestStatus.APPROVED);
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedBy(getCurrentUserId());
        
        TransferRequest savedRequest = transferRequestRepository.save(request);
        
        log.info("Approved transfer request {}", id);
        
        return convertToDTO(savedRequest);
    }
    
    /**
     * Reject a transfer request
     */
    @Override
    @Transactional
    public TransferRequestDTO rejectRequest(Long id, String reason) {
        TransferRequest request = transferRequestRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSFER_REQUEST_NOT_FOUND));
        
        // Validate status is PENDING
        if (request.getStatus() != TransferRequestStatus.PENDING) {
            throw new AppException(ErrorCode.TRANSFER_REQUEST_ALREADY_PROCESSED);
        }
        
        // Update status
        request.setStatus(TransferRequestStatus.REJECTED);
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedBy(getCurrentUserId());
        request.setRejectReason(reason);
        
        TransferRequest savedRequest = transferRequestRepository.save(request);
        
        log.info("Rejected transfer request {} with reason: {}", id, reason);
        
        return convertToDTO(savedRequest);
    }


    // ==================== Private Helper Methods ====================
    
    /**
     * Get current user ID from security context
     */
    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
                org.springframework.security.core.userdetails.UserDetails userDetails = 
                        (org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal();
                // Try to find user by username/email
                String username = userDetails.getUsername();
                return userRepository.findByEmail(username)
                        .map(User::getId)
                        .orElse(null);
            }
        } catch (Exception e) {
            log.warn("Could not get current user ID: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Convert TransferRequest entity to DTO (simple version)
     */
    private TransferRequestDTO convertToDTO(TransferRequest request) {
        StoreLocation store = null;
        User processedByUser = null;
        
        if (request.getStoreId() != null) {
            store = storeLocationRepository.findById(request.getStoreId()).orElse(null);
        }
        
        if (request.getProcessedBy() != null) {
            processedByUser = userRepository.findById(request.getProcessedBy()).orElse(null);
        }
        
        return convertToDTO(request, store, processedByUser);
    }
    
    /**
     * Convert TransferRequest entity to DTO with pre-loaded entities
     */
    private TransferRequestDTO convertToDTO(TransferRequest request, StoreLocation store, User processedByUser) {
        // Load items if not already loaded
        List<TransferRequestItem> items = request.getItems();
        if (items == null) {
            items = transferRequestItemRepository.findByTransferRequestId(request.getId());
        }
        
        // Convert items to DTOs
        List<TransferRequestItemDTO> itemDTOs = items.stream()
                .map(this::convertItemToDTO)
                .collect(Collectors.toList());
        
        // Calculate totals
        int totalItems = itemDTOs.size();
        int totalQuantity = itemDTOs.stream()
                .mapToInt(TransferRequestItemDTO::getRequestedQuantity)
                .sum();
        
        return TransferRequestDTO.builder()
                .id(request.getId())
                .storeId(request.getStoreId())
                .storeName(store != null ? store.getName() : null)
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .processedAt(request.getProcessedAt())
                .processedBy(request.getProcessedBy())
                .processedByName(processedByUser != null ? processedByUser.getFullname() : null)
                .rejectReason(request.getRejectReason())
                .items(itemDTOs)
                .totalItems(totalItems)
                .totalQuantity(totalQuantity)
                .build();
    }
    
    /**
     * Convert TransferRequestItem entity to DTO
     */
    private TransferRequestItemDTO convertItemToDTO(TransferRequestItem item) {
        Product product = null;
        String productImageUrl = null;
        
        if (item.getProductId() != null) {
            product = productRepository.findById(item.getProductId().intValue()).orElse(null);
            if (product != null) {
                productImageUrl = product.getImageURL();
            }
        }
        
        return TransferRequestItemDTO.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .productName(product != null ? product.getName() : null)
                .productSku(product != null ? product.getSku() : null)
                .productImageUrl(productImageUrl)
                .requestedQuantity(item.getRequestedQuantity())
                .fulfilledQuantity(item.getFulfilledQuantity())
                .shortageQuantity(item.getShortageQuantity())
                .build();
    }
}
