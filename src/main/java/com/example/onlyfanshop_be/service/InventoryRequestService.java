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

    /**
     * Cửa hàng tạo yêu cầu nhập hàng với nhiều sản phẩm
     */
    @Transactional
    public InventoryRequest createRequestWithItems(Integer storeId, List<CreateInventoryRequestDTO.CreateInventoryRequestItemDTO> items, Long requestedBy, String note) {
        // Validate tất cả sản phẩm trước
        for (var item : items) {
            // Kiểm tra store có được phép bán sản phẩm này không
            StoreInventory storeInventory = storeInventoryRepository.findByStoreIdAndProductId(storeId, item.getProductId())
                    .orElseThrow(() -> new RuntimeException("Cửa hàng chưa được phép bán sản phẩm ID: " + item.getProductId()));

            if (!storeInventory.getIsAvailable()) {
                throw new RuntimeException("Sản phẩm ID " + item.getProductId() + " chưa được kích hoạt tại cửa hàng");
            }
        }

        // Tạo request chính
        InventoryRequest request = InventoryRequest.builder()
                .storeId(storeId)
                .status(InventoryRequestStatus.PENDING)
                .requestedBy(requestedBy)
                .requestNote(note)
                .build();

        request = inventoryRequestRepository.save(request);

        // Tạo các items
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

    /**
     * Legacy: Cửa hàng tạo yêu cầu nhập hàng (1 sản phẩm)
     */
    @Transactional
    public InventoryRequest createRequest(Integer storeId, Long productId, Integer quantity, Long requestedBy, String note) {
        // Kiểm tra store có được phép bán sản phẩm này không
        StoreInventory storeInventory = storeInventoryRepository.findByStoreIdAndProductId(storeId, productId)
                .orElseThrow(() -> new RuntimeException("Cửa hàng chưa được phép bán sản phẩm này"));

        if (!storeInventory.getIsAvailable()) {
            throw new RuntimeException("Sản phẩm này chưa được kích hoạt tại cửa hàng");
        }

        // Tạo request với items mới
        InventoryRequest request = InventoryRequest.builder()
                .storeId(storeId)
                .productId(productId) // Legacy field
                .requestedQuantity(quantity) // Legacy field
                .status(InventoryRequestStatus.PENDING)
                .requestedBy(requestedBy)
                .requestNote(note)
                .build();

        request = inventoryRequestRepository.save(request);

        // Tạo item
        InventoryRequestItem item = InventoryRequestItem.builder()
                .requestId(request.getId())
                .productId(productId)
                .requestedQuantity(quantity)
                .build();
        inventoryRequestItemRepository.save(item);
        request.setItems(List.of(item));

        return request;
    }


    /**
     * Admin duyệt yêu cầu nhập hàng (duyệt toàn bộ items với số lượng yêu cầu)
     */
    @Transactional
    public InventoryRequest approveRequest(Long requestId, Long approvedBy, String adminNote) {
        InventoryRequest request = inventoryRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu"));

        if (request.getStatus() != InventoryRequestStatus.PENDING) {
            throw new RuntimeException("Yêu cầu này đã được xử lý");
        }

        // Load items
        List<InventoryRequestItem> items = inventoryRequestItemRepository.findByRequestId(requestId);
        
        // Kiểm tra kho tổng còn đủ hàng không cho tất cả items
        for (InventoryRequestItem item : items) {
            Product product = productRepository.findById(item.getProductId().intValue())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID: " + item.getProductId()));

            if (product.getQuantity() < item.getRequestedQuantity()) {
                throw new RuntimeException("Kho tổng không đủ hàng cho sản phẩm " + product.getName() + ". Tồn kho: " + product.getQuantity());
            }
            
            // Set approved quantity = requested quantity
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

    /**
     * Legacy: Admin duyệt yêu cầu nhập hàng với số lượng cụ thể
     */
    @Transactional
    public InventoryRequest approveRequest(Long requestId, Integer approvedQuantity, Long approvedBy, String adminNote) {
        InventoryRequest request = inventoryRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu"));

        if (request.getStatus() != InventoryRequestStatus.PENDING) {
            throw new RuntimeException("Yêu cầu này đã được xử lý");
        }

        // Load items
        List<InventoryRequestItem> items = inventoryRequestItemRepository.findByRequestId(requestId);
        
        if (items.size() == 1) {
            // Single item - use provided approvedQuantity
            InventoryRequestItem item = items.get(0);
            Product product = productRepository.findById(item.getProductId().intValue())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

            if (product.getQuantity() < approvedQuantity) {
                throw new RuntimeException("Kho tổng không đủ hàng. Tồn kho: " + product.getQuantity());
            }
            
            item.setApprovedQuantity(approvedQuantity);
            inventoryRequestItemRepository.save(item);
            
            // Legacy fields
            request.setApprovedQuantity(approvedQuantity);
        } else {
            // Multiple items - approve all with requested quantity
            for (InventoryRequestItem item : items) {
                Product product = productRepository.findById(item.getProductId().intValue())
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm ID: " + item.getProductId()));

                if (product.getQuantity() < item.getRequestedQuantity()) {
                    throw new RuntimeException("Kho tổng không đủ hàng cho sản phẩm " + product.getName());
                }
                
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

    /**
     * Admin từ chối yêu cầu nhập hàng
     */
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

    /**
     * Chuyển trạng thái sang SHIPPING (đang vận chuyển)
     */
    @Transactional
    public InventoryRequest startShipping(Long requestId, Long performedBy) {
        InventoryRequest request = inventoryRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu"));

        if (request.getStatus() != InventoryRequestStatus.APPROVED) {
            throw new RuntimeException("Yêu cầu chưa được duyệt");
        }

        request.setStatus(InventoryRequestStatus.SHIPPING);
        return inventoryRequestRepository.save(request);
    }

    /**
     * Hoàn thành giao hàng (DELIVERED)
     * Trừ kho tổng, cộng kho cửa hàng cho tất cả items
     */
    @Transactional
    public InventoryRequest completeDelivery(Long requestId, Long performedBy) {
        InventoryRequest request = inventoryRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu"));

        if (request.getStatus() != InventoryRequestStatus.SHIPPING) {
            throw new RuntimeException("Yêu cầu chưa ở trạng thái vận chuyển");
        }

        // Load items
        List<InventoryRequestItem> items = inventoryRequestItemRepository.findByRequestId(requestId);
        
        // Thực hiện chuyển kho cho từng item
        for (InventoryRequestItem item : items) {
            inventoryTransactionService.transferToStore(
                    item.getProductId(),
                    request.getStoreId(),
                    item.getApprovedQuantity(),
                    request.getId(),
                    performedBy,
                    "Giao hàng theo yêu cầu #" + request.getId()
            );
        }

        request.setStatus(InventoryRequestStatus.DELIVERED);
        request.setCompletedAt(LocalDateTime.now());
        request.setItems(items);

        return inventoryRequestRepository.save(request);
    }

    /**
     * Legacy method - Hoàn thành chuyển hàng (sau khi duyệt)
     * @deprecated Use startShipping() then completeDelivery() instead
     */
    @Transactional
    public InventoryRequest completeTransfer(Long requestId, Long performedBy) {
        InventoryRequest request = inventoryRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu"));

        if (request.getStatus() != InventoryRequestStatus.APPROVED && 
            request.getStatus() != InventoryRequestStatus.SHIPPING) {
            throw new RuntimeException("Yêu cầu chưa được duyệt hoặc đã hoàn thành");
        }

        // Load items
        List<InventoryRequestItem> items = inventoryRequestItemRepository.findByRequestId(requestId);
        
        // Thực hiện chuyển kho cho từng item
        for (InventoryRequestItem item : items) {
            inventoryTransactionService.transferToStore(
                    item.getProductId(),
                    request.getStoreId(),
                    item.getApprovedQuantity(),
                    request.getId(),
                    performedBy,
                    "Chuyển hàng theo yêu cầu #" + request.getId()
            );
        }

        request.setStatus(InventoryRequestStatus.DELIVERED);
        request.setCompletedAt(LocalDateTime.now());
        request.setItems(items);

        return inventoryRequestRepository.save(request);
    }

    /**
     * Cửa hàng hủy yêu cầu (chỉ khi đang PENDING)
     */
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

    /**
     * Lấy danh sách requests pending (cho admin)
     */
    public List<InventoryRequest> getPendingRequests() {
        return inventoryRequestRepository.findPendingRequests();
    }

    /**
     * Lấy danh sách requests của một store
     */
    public List<InventoryRequest> getStoreRequests(Integer storeId) {
        return inventoryRequestRepository.findByStoreIdOrderByCreatedAtDesc(storeId);
    }

    /**
     * Lấy danh sách requests theo status với pagination
     */
    public Page<InventoryRequest> getRequestsByStatus(InventoryRequestStatus status, Pageable pageable) {
        return inventoryRequestRepository.findByStatus(status, pageable);
    }

    /**
     * Đếm số requests pending
     */
    public Long countPendingRequests() {
        return inventoryRequestRepository.countByStatus(InventoryRequestStatus.PENDING);
    }

    /**
     * Lấy request theo ID
     */
    public InventoryRequest getRequestById(Long id) {
        return inventoryRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu"));
    }
}
