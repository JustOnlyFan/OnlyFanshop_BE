package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.InventoryRequestDTO;
import com.example.onlyfanshop_be.dto.InventoryRequestItemDTO;
import com.example.onlyfanshop_be.dto.request.ApproveInventoryRequestDTO;
import com.example.onlyfanshop_be.dto.request.CreateInventoryRequestDTO;
import com.example.onlyfanshop_be.dto.request.RejectInventoryRequestDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.InventoryRequest;
import com.example.onlyfanshop_be.entity.InventoryRequestItem;
import com.example.onlyfanshop_be.entity.Product;
import com.example.onlyfanshop_be.entity.StoreLocation;
import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.enums.InventoryRequestStatus;
import com.example.onlyfanshop_be.repository.InventoryRequestItemRepository;
import com.example.onlyfanshop_be.repository.ProductRepository;
import com.example.onlyfanshop_be.repository.StoreLocationRepository;
import com.example.onlyfanshop_be.repository.UserRepository;
import com.example.onlyfanshop_be.service.InventoryRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/inventory-requests")
@RequiredArgsConstructor
public class InventoryRequestController {
    private final InventoryRequestService inventoryRequestService;
    private final InventoryRequestItemRepository inventoryRequestItemRepository;
    private final UserRepository userRepository;
    private final StoreLocationRepository storeLocationRepository;
    private final ProductRepository productRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<InventoryRequestDTO>> createRequest(
            @Valid @RequestBody CreateInventoryRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        InventoryRequest request;
        
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            request = inventoryRequestService.createRequestWithItems(
                    dto.getStoreId(),
                    dto.getItems(),
                    user.getId(),
                    dto.getNote()
            );
        } else if (dto.getProductId() != null && dto.getQuantity() != null) {
            request = inventoryRequestService.createRequest(
                    dto.getStoreId(),
                    dto.getProductId(),
                    dto.getQuantity(),
                    user.getId(),
                    dto.getNote()
            );
        } else {
            throw new RuntimeException("Vui lòng cung cấp danh sách sản phẩm");
        }

        return ResponseEntity.ok(ApiResponse.<InventoryRequestDTO>builder()
                .statusCode(200)
                .message("Tạo yêu cầu nhập hàng thành công")
                .data(convertToDTO(request))
                .build());
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InventoryRequestDTO>> approveRequest(
            @PathVariable Long id,
            @Valid @RequestBody ApproveInventoryRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        InventoryRequest request = inventoryRequestService.approveRequest(
                id,
                dto.getApprovedQuantity(),
                user.getId(),
                dto.getAdminNote()
        );

        return ResponseEntity.ok(ApiResponse.<InventoryRequestDTO>builder()
                .statusCode(200)
                .message("Đã duyệt yêu cầu nhập hàng")
                .data(convertToDTO(request))
                .build());
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InventoryRequestDTO>> rejectRequest(
            @PathVariable Long id,
            @RequestBody RejectInventoryRequestDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        InventoryRequest request = inventoryRequestService.rejectRequest(
                id,
                user.getId(),
                dto.getAdminNote()
        );

        return ResponseEntity.ok(ApiResponse.<InventoryRequestDTO>builder()
                .statusCode(200)
                .message("Đã từ chối yêu cầu nhập hàng")
                .data(convertToDTO(request))
                .build());
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InventoryRequestDTO>> completeRequest(
            @PathVariable Long id,
            @RequestParam Integer sourceStoreId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        InventoryRequest request = inventoryRequestService.completeRequest(id, sourceStoreId, user.getId());

        return ResponseEntity.ok(ApiResponse.<InventoryRequestDTO>builder()
                .statusCode(200)
                .message("Đã hoàn thành chuyển hàng")
                .data(convertToDTO(request))
                .build());
    }

    @PutMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<InventoryRequestDTO>> cancelRequest(@PathVariable Long id) {
        InventoryRequest request = inventoryRequestService.cancelRequest(id);

        return ResponseEntity.ok(ApiResponse.<InventoryRequestDTO>builder()
                .statusCode(200)
                .message("Đã hủy yêu cầu nhập hàng")
                .data(convertToDTO(request))
                .build());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<InventoryRequestDTO>>> getPendingRequests() {
        List<InventoryRequest> requests = inventoryRequestService.getPendingRequests();
        List<InventoryRequestDTO> dtos = requests.stream().map(this::convertToDTO).toList();

        return ResponseEntity.ok(ApiResponse.<List<InventoryRequestDTO>>builder()
                .statusCode(200)
                .message("Danh sách yêu cầu chờ duyệt")
                .data(dtos)
                .build());
    }

    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<InventoryRequestDTO>>> getStoreRequests(
            @PathVariable Integer storeId) {
        List<InventoryRequest> requests = inventoryRequestService.getStoreRequests(storeId);
        List<InventoryRequestDTO> dtos = requests.stream().map(this::convertToDTO).toList();

        return ResponseEntity.ok(ApiResponse.<List<InventoryRequestDTO>>builder()
                .statusCode(200)
                .message("Danh sách yêu cầu của cửa hàng")
                .data(dtos)
                .build());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<InventoryRequestDTO>>> getRequests(
            @RequestParam(required = false) InventoryRequestStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<InventoryRequest> requests = status != null
                ? inventoryRequestService.getRequestsByStatus(status, pageRequest)
                : inventoryRequestService.getRequestsByStatus(InventoryRequestStatus.PENDING, pageRequest);

        Page<InventoryRequestDTO> dtos = requests.map(this::convertToDTO);

        return ResponseEntity.ok(ApiResponse.<Page<InventoryRequestDTO>>builder()
                .statusCode(200)
                .message("Danh sách yêu cầu nhập hàng")
                .data(dtos)
                .build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<InventoryRequestDTO>> getRequest(@PathVariable Long id) {
        InventoryRequest request = inventoryRequestService.getRequestById(id);

        return ResponseEntity.ok(ApiResponse.<InventoryRequestDTO>builder()
                .statusCode(200)
                .message("Chi tiết yêu cầu nhập hàng")
                .data(convertToDTO(request))
                .build());
    }

    @GetMapping("/pending/count")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Long>> countPendingRequests() {
        Long count = inventoryRequestService.countPendingRequests();

        return ResponseEntity.ok(ApiResponse.<Long>builder()
                .statusCode(200)
                .message("Số yêu cầu chờ duyệt")
                .data(count)
                .build());
    }

    private InventoryRequestDTO convertToDTO(InventoryRequest request) {
        StoreLocation store = storeLocationRepository.findById(request.getStoreId()).orElse(null);
        User requester = request.getRequestedBy() != null
                ? userRepository.findById(request.getRequestedBy()).orElse(null) : null;
        User approver = request.getApprovedBy() != null
                ? userRepository.findById(request.getApprovedBy()).orElse(null) : null;

        List<InventoryRequestItem> items = request.getItems();
        if (items == null || items.isEmpty()) {
            items = inventoryRequestItemRepository.findByRequestId(request.getId());
        }
        
        List<InventoryRequestItemDTO> itemDTOs = new ArrayList<>();
        int totalQuantity = 0;
        
        Long legacyProductId = request.getProductId();
        String legacyProductName = null;
        String legacyProductImageUrl = null;
        Integer legacyRequestedQuantity = request.getRequestedQuantity();
        Integer legacyApprovedQuantity = request.getApprovedQuantity();
        
        for (InventoryRequestItem item : items) {
            Product product = productRepository.findById(item.getProductId().intValue()).orElse(null);
            
            String productImageUrl = null;
            String productName = null;
            if (product != null) {
                productName = product.getName();
                if (product.getImages() != null && !product.getImages().isEmpty()) {
                    productImageUrl = product.getImages().stream()
                            .filter(img -> img.getIsMain() != null && img.getIsMain())
                            .map(img -> img.getImageUrl())
                            .findFirst()
                            .orElse(product.getImages().get(0).getImageUrl());
                }
            }
            
            itemDTOs.add(InventoryRequestItemDTO.builder()
                    .id(item.getId())
                    .productId(item.getProductId())
                    .productName(productName)
                    .productImageUrl(productImageUrl)
                    .requestedQuantity(item.getRequestedQuantity())
                    .approvedQuantity(item.getApprovedQuantity())
                    .build());
            
            totalQuantity += item.getRequestedQuantity();
            
            if (legacyProductId == null) {
                legacyProductId = item.getProductId();
                legacyProductName = productName;
                legacyProductImageUrl = productImageUrl;
                legacyRequestedQuantity = item.getRequestedQuantity();
                legacyApprovedQuantity = item.getApprovedQuantity();
            }
        }
        
        if (legacyProductId != null && legacyProductName == null) {
            Product product = productRepository.findById(legacyProductId.intValue()).orElse(null);
            if (product != null) {
                legacyProductName = product.getName();
                if (product.getImages() != null && !product.getImages().isEmpty()) {
                    legacyProductImageUrl = product.getImages().stream()
                            .filter(img -> img.getIsMain() != null && img.getIsMain())
                            .map(img -> img.getImageUrl())
                            .findFirst()
                            .orElse(product.getImages().get(0).getImageUrl());
                }
            }
        }

        return InventoryRequestDTO.builder()
                .id(request.getId())
                .storeId(request.getStoreId())
                .storeName(store != null ? store.getName() : null)
                .items(itemDTOs)
                .totalItems(itemDTOs.size())
                .totalQuantity(totalQuantity)
                .productId(legacyProductId)
                .productName(legacyProductName)
                .productImageUrl(legacyProductImageUrl)
                .requestedQuantity(legacyRequestedQuantity)
                .approvedQuantity(legacyApprovedQuantity)
                .status(request.getStatus())
                .requestedBy(request.getRequestedBy())
                .requesterName(requester != null ? requester.getFullname() : null)
                .approvedBy(request.getApprovedBy())
                .approverName(approver != null ? approver.getFullname() : null)
                .requestNote(request.getRequestNote())
                .adminNote(request.getAdminNote())
                .approvedAt(request.getApprovedAt())
                .completedAt(request.getCompletedAt())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }
}
