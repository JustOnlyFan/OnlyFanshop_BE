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

@Service
@RequiredArgsConstructor
@Slf4j
public class TransferRequestService implements ITransferRequestService {

    public static final int MAX_QUANTITY_PER_PRODUCT = 30;
    
    private final TransferRequestRepository transferRequestRepository;
    private final TransferRequestItemRepository transferRequestItemRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final ProductRepository productRepository;
    private final StoreLocationRepository storeLocationRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public TransferRequestDTO createRequest(Integer storeId, CreateTransferRequestDTO request) {
        StoreLocation store = storeLocationRepository.findById(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new AppException(ErrorCode.TRANSFER_REQUEST_EMPTY_ITEMS);
        }

        if (request.getSourceWarehouseId() == null) {
            throw new AppException(ErrorCode.SOURCE_WAREHOUSE_REQUIRED);
        }

        Warehouse sourceWarehouse = warehouseRepository.findById(request.getSourceWarehouseId())
                .orElseThrow(() -> new AppException(ErrorCode.SOURCE_WAREHOUSE_NOT_FOUND));
        
        if (!Boolean.TRUE.equals(sourceWarehouse.getIsActive())) {
            throw new AppException(ErrorCode.SOURCE_WAREHOUSE_INACTIVE);
        }

        Warehouse destWarehouse = warehouseRepository.findByStoreId(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));

        if (sourceWarehouse.getId().equals(destWarehouse.getId())) {
            throw new AppException(ErrorCode.SAME_WAREHOUSE_TRANSFER);
        }

        for (CreateTransferRequestItemDTO item : request.getItems()) {
            validateTransferRequestItem(item, destWarehouse.getId());
            validateSourceWarehouseQuantity(item, sourceWarehouse.getId());
        }

        TransferRequest transferRequest = TransferRequest.builder()
                .storeId(storeId)
                .sourceWarehouseId(request.getSourceWarehouseId())
                .status(TransferRequestStatus.PENDING)
                .build();
        
        TransferRequest savedRequest = transferRequestRepository.save(transferRequest);

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
        
        log.info("Created transfer request {} for store {} from source warehouse {} with {} items", 
                savedRequest.getId(), storeId, request.getSourceWarehouseId(), items.size());
        
        return convertToDTO(savedRequest, store, null, sourceWarehouse);
    }

    private void validateSourceWarehouseQuantity(CreateTransferRequestItemDTO item, Long sourceWarehouseId) {
        InventoryItem inventoryItem = inventoryItemRepository
                .findByWarehouseIdAndProductId(sourceWarehouseId, item.getProductId())
                .orElse(null);
        
        int availableQuantity = 0;
        if (inventoryItem != null) {
            availableQuantity = inventoryItem.getQuantity() != null ? inventoryItem.getQuantity() : 0;
        }
        
        if (availableQuantity < item.getQuantity()) {
            throw new AppException(ErrorCode.SOURCE_WAREHOUSE_INSUFFICIENT_STOCK);
        }
    }

    private void validateTransferRequestItem(CreateTransferRequestItemDTO item, Long storeWarehouseId) {
        if (item.getQuantity() == null || item.getQuantity() <= 0) {
            throw new AppException(ErrorCode.TRANSFER_REQUEST_INVALID_QUANTITY);
        }

        if (item.getQuantity() > MAX_QUANTITY_PER_PRODUCT) {
            throw new AppException(ErrorCode.TRANSFER_REQUEST_QUANTITY_EXCEEDS_LIMIT);
        }

        if (item.getProductId() == null) {
            throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
        }
        
        productRepository.findById(item.getProductId().intValue())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOTEXISTED));

        boolean existsInStoreWarehouse = inventoryItemRepository
                .existsByWarehouseIdAndProductId(storeWarehouseId, item.getProductId());
        
        if (!existsInStoreWarehouse) {
            throw new AppException(ErrorCode.TRANSFER_REQUEST_PRODUCT_NOT_IN_STORE);
        }
    }

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

    @Override
    public TransferRequestDTO getRequest(Long id) {
        TransferRequest request = transferRequestRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSFER_REQUEST_NOT_FOUND));
        
        return convertToDTO(request);
    }

    @Override
    @Transactional
    public TransferRequestDTO approveRequest(Long id) {
        TransferRequest request = transferRequestRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSFER_REQUEST_NOT_FOUND));

        if (request.getStatus() != TransferRequestStatus.PENDING) {
            throw new AppException(ErrorCode.TRANSFER_REQUEST_ALREADY_PROCESSED);
        }

        request.setStatus(TransferRequestStatus.APPROVED);
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedBy(getCurrentUserId());
        
        TransferRequest savedRequest = transferRequestRepository.save(request);
        
        log.info("Approved transfer request {}", id);
        
        return convertToDTO(savedRequest);
    }

    @Override
    @Transactional
    public TransferRequestDTO rejectRequest(Long id, String reason) {
        TransferRequest request = transferRequestRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSFER_REQUEST_NOT_FOUND));

        if (request.getStatus() != TransferRequestStatus.PENDING) {
            throw new AppException(ErrorCode.TRANSFER_REQUEST_ALREADY_PROCESSED);
        }

        request.setStatus(TransferRequestStatus.REJECTED);
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedBy(getCurrentUserId());
        request.setRejectReason(reason);
        
        TransferRequest savedRequest = transferRequestRepository.save(request);
        
        log.info("Rejected transfer request {} with reason: {}", id, reason);
        
        return convertToDTO(savedRequest);
    }

    @Override
    @Transactional
    public TransferRequestDTO completeRequest(Long id) {
        TransferRequest request = transferRequestRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSFER_REQUEST_NOT_FOUND));

        if (request.getStatus() != TransferRequestStatus.APPROVED) {
            throw new AppException(ErrorCode.TRANSFER_REQUEST_NOT_APPROVED);
        }

        request.setStatus(TransferRequestStatus.COMPLETED);
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedBy(getCurrentUserId());
        
        TransferRequest savedRequest = transferRequestRepository.save(request);
        
        log.info("Completed transfer request {}", id);
        
        return convertToDTO(savedRequest);
    }

    @Override
    @Transactional
    public TransferRequestDTO cancelRequest(Long id) {
        TransferRequest request = transferRequestRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSFER_REQUEST_NOT_FOUND));

        if (request.getStatus() != TransferRequestStatus.PENDING && 
            request.getStatus() != TransferRequestStatus.APPROVED) {
            throw new AppException(ErrorCode.TRANSFER_REQUEST_CANNOT_CANCEL);
        }

        request.setStatus(TransferRequestStatus.CANCELLED);
        request.setProcessedAt(LocalDateTime.now());
        request.setProcessedBy(getCurrentUserId());
        
        TransferRequest savedRequest = transferRequestRepository.save(request);
        
        log.info("Cancelled transfer request {}", id);
        
        return convertToDTO(savedRequest);
    }

    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
                org.springframework.security.core.userdetails.UserDetails userDetails = 
                        (org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal();
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

    private TransferRequestDTO convertToDTO(TransferRequest request) {
        StoreLocation store = null;
        User processedByUser = null;
        Warehouse sourceWarehouse = null;
        
        if (request.getStoreId() != null) {
            store = storeLocationRepository.findById(request.getStoreId()).orElse(null);
        }
        
        if (request.getProcessedBy() != null) {
            processedByUser = userRepository.findById(request.getProcessedBy()).orElse(null);
        }
        
        if (request.getSourceWarehouseId() != null) {
            sourceWarehouse = warehouseRepository.findById(request.getSourceWarehouseId()).orElse(null);
        }
        
        return convertToDTO(request, store, processedByUser, sourceWarehouse);
    }

    private TransferRequestDTO convertToDTO(TransferRequest request, StoreLocation store, User processedByUser, Warehouse sourceWarehouse) {
        List<TransferRequestItem> items = request.getItems();
        if (items == null) {
            items = transferRequestItemRepository.findByTransferRequestId(request.getId());
        }

        List<TransferRequestItemDTO> itemDTOs = items.stream()
                .map(this::convertItemToDTO)
                .collect(Collectors.toList());

        int totalItems = itemDTOs.size();
        int totalQuantity = itemDTOs.stream()
                .mapToInt(TransferRequestItemDTO::getRequestedQuantity)
                .sum();
        
        return TransferRequestDTO.builder()
                .id(request.getId())
                .storeId(request.getStoreId())
                .storeName(store != null ? store.getName() : null)
                .sourceWarehouseId(request.getSourceWarehouseId())
                .sourceWarehouseName(sourceWarehouse != null ? sourceWarehouse.getName() : null)
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
