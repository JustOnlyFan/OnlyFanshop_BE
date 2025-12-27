package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.request.CreateInventoryRequestDTO;
import com.example.onlyfanshop_be.entity.InventoryRequest;
import com.example.onlyfanshop_be.entity.InventoryRequestItem;
import com.example.onlyfanshop_be.entity.Product;
import com.example.onlyfanshop_be.entity.StoreInventory;
import com.example.onlyfanshop_be.enums.InventoryRequestStatus;
import com.example.onlyfanshop_be.repository.InventoryRequestItemRepository;
import com.example.onlyfanshop_be.repository.InventoryRequestRepository;
import com.example.onlyfanshop_be.repository.ProductRepository;
import com.example.onlyfanshop_be.repository.StoreInventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryRequestService {
    private final InventoryRequestRepository inventoryRequestRepository;
    private final InventoryRequestItemRepository inventoryRequestItemRepository;
    private final InventoryTransactionService inventoryTransactionService;
    private final StoreInventoryRepository storeInventoryRepository;
    private final ProductRepository productRepository;

    @Transactional
    public InventoryRequest createRequestWithItems(Integer storeId, List<CreateInventoryRequestDTO.CreateInventoryRequestItemDTO> items, Long requestedBy, String note) {
        for (var item : items) {
            StoreInventory storeInventory = storeInventoryRepository.findByStoreIdAndProductId(storeId, item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Cửa hàng chưa được phép bán sản phẩm ID: " + item.getProductId()));

            if (!storeInventory.getIsAvailable()) {
                throw new RuntimeException("Sản phẩm ID " + item.getProductId() + " chưa được kích hoạt tại cửa hàng");
            }
        }

        InventoryRequest request = InventoryRequest.builder()
                .storeId(storeId)
                .status(InventoryRequestStatus.PENDING)
                .requestedBy(requestedBy)
                .requestNote(note)
                .build();

        request = inventoryRequestRepository.save(request);

        List<InventoryRequestItem> requestItems = new ArrayList<>();
        for (var item : items) {
            InventoryRequestItem requestItem = InventoryRequestItem.builder()
                    .requestId(request.getId())
                    .productId(item.getProductId())
                    .requestedQuantity(item.getQuantity())
                    .build();
            requestItems.add(requestItem);
        }
        inventoryRequestItemRepository.saveAll(requestItems);
        request.setItems(requestItems);

        return request;
    }

    @Transactional
    public InventoryRequest createRequest(Integer storeId, Long productId, Integer quantity, Long requestedBy, String note) {
        StoreInventory storeInventory = storeInventoryRepository.findByStoreIdAndProductId(storeId, productId)
                .orElseThrow(() -> new RuntimeException("Cửa hàng chưa được phép bán sản phẩm này"));

        if (!storeInventory.getIsAvailable()) {
            throw new RuntimeException("Sản phẩm này chưa được kích hoạt tại cửa hàng");
        }

        InventoryRequest request = InventoryRequest.builder()
                .storeId(storeId)
                .productId(productId)
                .requestedQuantity(quantity)
                .status(InventoryRequestStatus.PENDING)
                .requestedBy(requestedBy)
                .requestNote(note)
                .build();

        request = inventoryRequestRepository.save(request);

        InventoryRequestItem item = InventoryRequestItem.builder()
                .requestId(request.getId())
                .productId(productId)
                .requestedQuantity(quantity)
                .build();
        inventoryRequestItemRepository.save(item);
        request.setItems(List.of(item));

        return request;
    }

    @Transactional
    public InventoryRequest approveRequest(Long requestId, Long approvedBy, String adminNote) {
        InventoryRequest request = inventoryRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu"));

        if (request.getStatus() != InventoryRequestStatus.PENDING) {
            throw new RuntimeException("Yêu cầu này đã được xử lý");
        }

        List<InventoryRequestItem> items = inventoryRequestItemRepository.findByRequestId(requestId);

        for (InventoryRequestItem item : items) {
            item.setApprovedQuantity(item.getRequestedQuantity());
        }
        inventoryRequestItemRepository.saveAll(items);

        request.setStatus(InventoryRequestStatus.APPROVED);
        request.setApprovedBy(approvedBy);
        request.setAdminNote(adminNote);
        request.setApprovedAt(LocalDateTime.now());
        request.setItems(items);

        return inventoryRequestRepository.save(request);
    }

    @Transactional
    public InventoryRequest approveRequest(Long requestId, Integer approvedQuantity, Long approvedBy, String adminNote) {
        InventoryRequest request = inventoryRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu"));

        if (request.getStatus() != InventoryRequestStatus.PENDING) {
            throw new RuntimeException("Yêu cầu này đã được xử lý");
        }

        List<InventoryRequestItem> items = inventoryRequestItemRepository.findByRequestId(requestId);
        
        if (items.size() == 1) {
            InventoryRequestItem item = items.get(0);
            item.setApprovedQuantity(approvedQuantity);
            inventoryRequestItemRepository.save(item);
            request.setApprovedQuantity(approvedQuantity);
        } else {
            for (InventoryRequestItem item : items) {
                item.setApprovedQuantity(item.getRequestedQuantity());
            }
            inventoryRequestItemRepository.saveAll(items);
        }

        request.setStatus(InventoryRequestStatus.APPROVED);
        request.setApprovedBy(approvedBy);
        request.setAdminNote(adminNote);
        request.setApprovedAt(LocalDateTime.now());
        request.setItems(items);

        return inventoryRequestRepository.save(request);
    }

    @Transactional
    public InventoryRequest rejectRequest(Long requestId, Long approvedBy, String adminNote) {
        InventoryRequest request = inventoryRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu"));

        if (request.getStatus() != InventoryRequestStatus.PENDING) {
            throw new RuntimeException("Yêu cầu này đã được xử lý");
        }

        request.setStatus(InventoryRequestStatus.REJECTED);
        request.setApprovedBy(approvedBy);
        request.setAdminNote(adminNote);
        request.setApprovedAt(LocalDateTime.now());

        return inventoryRequestRepository.save(request);
    }

    @Transactional
    public InventoryRequest completeRequest(Long requestId, Integer sourceStoreId, Long performedBy) {
        InventoryRequest request = inventoryRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu"));

        if (request.getStatus() != InventoryRequestStatus.APPROVED) {
            throw new RuntimeException("Yêu cầu chưa được duyệt");
        }

        List<InventoryRequestItem> items = inventoryRequestItemRepository.findByRequestId(requestId);

        for (InventoryRequestItem item : items) {
            inventoryTransactionService.transferBetweenStores(
                    item.getProductId(),
                    sourceStoreId,
                    request.getStoreId(),
                    item.getApprovedQuantity(),
                    performedBy,
                    "Chuyển hàng theo yêu cầu #" + request.getId()
            );
        }

        request.setStatus(InventoryRequestStatus.COMPLETED);
        request.setCompletedAt(LocalDateTime.now());
        request.setItems(items);

        return inventoryRequestRepository.save(request);
    }

    @Transactional
    public InventoryRequest cancelRequest(Long requestId) {
        InventoryRequest request = inventoryRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu"));

        if (request.getStatus() != InventoryRequestStatus.PENDING) {
            throw new RuntimeException("Chỉ có thể hủy yêu cầu đang chờ duyệt");
        }

        request.setStatus(InventoryRequestStatus.CANCELLED);
        return inventoryRequestRepository.save(request);
    }

    public List<InventoryRequest> getPendingRequests() {
        return inventoryRequestRepository.findPendingRequests();
    }

    public List<InventoryRequest> getStoreRequests(Integer storeId) {
        return inventoryRequestRepository.findByStoreIdOrderByCreatedAtDesc(storeId);
    }

    public Page<InventoryRequest> getRequestsByStatus(InventoryRequestStatus status, Pageable pageable) {
        return inventoryRequestRepository.findByStatus(status, pageable);
    }

    public Long countPendingRequests() {
        return inventoryRequestRepository.countByStatus(InventoryRequestStatus.PENDING);
    }

    public InventoryRequest getRequestById(Long id) {
        return inventoryRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu"));
    }
}
