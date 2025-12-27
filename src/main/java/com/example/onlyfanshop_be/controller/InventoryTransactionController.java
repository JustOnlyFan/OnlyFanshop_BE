package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.InventoryTransactionDTO;
import com.example.onlyfanshop_be.dto.request.AdjustInventoryDTO;
import com.example.onlyfanshop_be.dto.request.TransferInventoryDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.InventoryTransaction;
import com.example.onlyfanshop_be.entity.Product;
import com.example.onlyfanshop_be.entity.StoreLocation;
import com.example.onlyfanshop_be.entity.User;
import com.example.onlyfanshop_be.repository.ProductRepository;
import com.example.onlyfanshop_be.repository.StoreLocationRepository;
import com.example.onlyfanshop_be.repository.UserRepository;
import com.example.onlyfanshop_be.service.InventoryTransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory-transactions")
@RequiredArgsConstructor
public class InventoryTransactionController {
    private final InventoryTransactionService transactionService;
    private final UserRepository userRepository;
    private final StoreLocationRepository storeLocationRepository;
    private final ProductRepository productRepository;

    @PostMapping("/transfer-between-stores")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InventoryTransactionDTO>> transferBetweenStores(
            @RequestParam Long productId,
            @RequestParam Integer sourceStoreId,
            @RequestParam Integer destStoreId,
            @RequestParam Integer quantity,
            @RequestParam(required = false) String note,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        InventoryTransaction transaction = transactionService.transferBetweenStores(
                productId,
                sourceStoreId,
                destStoreId,
                quantity,
                user.getId(),
                note
        );

        return ResponseEntity.ok(ApiResponse.<InventoryTransactionDTO>builder()
                .statusCode(200)
                .message("Chuyển hàng giữa các kho thành công")
                .data(convertToDTO(transaction))
                .build());
    }

    @PostMapping("/import-to-store")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InventoryTransactionDTO>> importToStore(
            @RequestParam Long productId,
            @RequestParam Integer storeId,
            @RequestParam Integer quantity,
            @RequestParam(required = false) String note,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        InventoryTransaction transaction = transactionService.importToStore(
                productId,
                storeId,
                quantity,
                user.getId(),
                note != null ? note : "Nhập hàng vào kho cửa hàng"
        );

        return ResponseEntity.ok(ApiResponse.<InventoryTransactionDTO>builder()
                .statusCode(200)
                .message("Nhập hàng vào kho cửa hàng thành công")
                .data(convertToDTO(transaction))
                .build());
    }

    @PostMapping("/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InventoryTransactionDTO>> adjustInventory(
            @Valid @RequestBody AdjustInventoryDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        InventoryTransaction transaction = transactionService.adjustInventory(
                dto.getProductId(),
                dto.getStoreId(),
                dto.getNewQuantity(),
                user.getId(),
                dto.getNote() != null ? dto.getNote() : "Điều chỉnh kiểm kê"
        );

        return ResponseEntity.ok(ApiResponse.<InventoryTransactionDTO>builder()
                .statusCode(200)
                .message("Điều chỉnh tồn kho thành công")
                .data(convertToDTO(transaction))
                .build());
    }

    @GetMapping("/product/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<InventoryTransactionDTO>>> getProductTransactions(
            @PathVariable Long productId) {
        List<InventoryTransaction> transactions = transactionService.getProductTransactions(productId);
        List<InventoryTransactionDTO> dtos = transactions.stream().map(this::convertToDTO).toList();

        return ResponseEntity.ok(ApiResponse.<List<InventoryTransactionDTO>>builder()
                .statusCode(200)
                .message("Lịch sử giao dịch kho của sản phẩm")
                .data(dtos)
                .build());
    }

    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<InventoryTransactionDTO>>> getStoreTransactions(
            @PathVariable Integer storeId) {
        List<InventoryTransaction> transactions = transactionService.getStoreTransactions(storeId);
        List<InventoryTransactionDTO> dtos = transactions.stream().map(this::convertToDTO).toList();

        return ResponseEntity.ok(ApiResponse.<List<InventoryTransactionDTO>>builder()
                .statusCode(200)
                .message("Lịch sử giao dịch kho của cửa hàng")
                .data(dtos)
                .build());
    }

    @GetMapping("/request/{requestId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<InventoryTransactionDTO>>> getRequestTransactions(
            @PathVariable Long requestId) {
        List<InventoryTransaction> transactions = transactionService.getRequestTransactions(requestId);
        List<InventoryTransactionDTO> dtos = transactions.stream().map(this::convertToDTO).toList();

        return ResponseEntity.ok(ApiResponse.<List<InventoryTransactionDTO>>builder()
                .statusCode(200)
                .message("Lịch sử giao dịch của yêu cầu")
                .data(dtos)
                .build());
    }

    private InventoryTransactionDTO convertToDTO(InventoryTransaction transaction) {
        Product product = productRepository.findById(transaction.getProductId().intValue()).orElse(null);
        StoreLocation sourceStore = transaction.getSourceStoreId() != null
                ? storeLocationRepository.findById(transaction.getSourceStoreId()).orElse(null) : null;
        StoreLocation destStore = transaction.getDestinationStoreId() != null
                ? storeLocationRepository.findById(transaction.getDestinationStoreId()).orElse(null) : null;
        User performer = userRepository.findById(transaction.getPerformedBy()).orElse(null);

        return InventoryTransactionDTO.builder()
                .id(transaction.getId())
                .transactionType(transaction.getTransactionType())
                .productId(transaction.getProductId())
                .productName(product != null ? product.getName() : null)
                .quantity(transaction.getQuantity())
                .sourceType(transaction.getSourceType())
                .sourceStoreId(transaction.getSourceStoreId())
                .sourceStoreName(sourceStore != null ? sourceStore.getName() : null)
                .destinationType(transaction.getDestinationType())
                .destinationStoreId(transaction.getDestinationStoreId())
                .destinationStoreName(destStore != null ? destStore.getName() : null)
                .requestId(transaction.getRequestId())
                .orderId(transaction.getOrderId())
                .performedBy(transaction.getPerformedBy())
                .performerName(performer != null ? performer.getFullname() : null)
                .quantityBefore(transaction.getQuantityBefore())
                .quantityAfter(transaction.getQuantityAfter())
                .note(transaction.getNote())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
