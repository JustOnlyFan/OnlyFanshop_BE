package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.DebtOrderDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.enums.DebtOrderStatus;
import com.example.onlyfanshop_be.service.IDebtOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Debt Order operations
 * Requirements: 6.1, 6.2, 6.3, 6.4, 6.5
 */
@RestController
@RequestMapping("/api/debt-orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Debt Orders", description = "API quản lý đơn nợ")
public class DebtOrderController {
    
    private final IDebtOrderService debtOrderService;

    /**
     * Get all debt orders with optional status filter
     * GET /api/debt-orders
     * Requirements: 6.1, 6.2
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Lấy danh sách đơn nợ", description = "Lấy danh sách đơn nợ với bộ lọc trạng thái tùy chọn")
    public ResponseEntity<ApiResponse<Page<DebtOrderDTO>>> getDebtOrders(
            @Parameter(description = "Trạng thái đơn nợ (PENDING, FULFILLABLE, COMPLETED)")
            @RequestParam(required = false) DebtOrderStatus status,
            @Parameter(description = "Số trang (bắt đầu từ 0)")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Số lượng mỗi trang")
            @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Sắp xếp theo trường")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Hướng sắp xếp (asc/desc)")
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") 
                ? Sort.by(sortBy).ascending() 
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<DebtOrderDTO> debtOrders = debtOrderService.getDebtOrders(status, pageable);
        
        return ResponseEntity.ok(ApiResponse.<Page<DebtOrderDTO>>builder()
                .statusCode(200)
                .message("Lấy danh sách đơn nợ thành công")
                .data(debtOrders)
                .build());
    }


    /**
     * Get a specific debt order by ID
     * GET /api/debt-orders/{id}
     * Requirements: 6.1, 6.2
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Lấy chi tiết đơn nợ", description = "Lấy thông tin chi tiết của một đơn nợ theo ID")
    public ResponseEntity<ApiResponse<DebtOrderDTO>> getDebtOrder(
            @Parameter(description = "ID của đơn nợ")
            @PathVariable Long id
    ) {
        DebtOrderDTO debtOrder = debtOrderService.getDebtOrder(id);
        
        return ResponseEntity.ok(ApiResponse.<DebtOrderDTO>builder()
                .statusCode(200)
                .message("Lấy chi tiết đơn nợ thành công")
                .data(debtOrder)
                .build());
    }

    /**
     * Fulfill a debt order
     * POST /api/debt-orders/{id}/fulfill
     * Requirements: 6.5 - WHEN a Debt_Order is fulfilled THEN the System SHALL update Debt_Order status to COMPLETED and create Shipment
     */
    @PostMapping("/{id}/fulfill")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Đáp ứng đơn nợ", description = "Đáp ứng đơn nợ bằng cách trừ hàng từ kho lớn và tạo đơn vận chuyển")
    public ResponseEntity<ApiResponse<DebtOrderDTO>> fulfillDebtOrder(
            @Parameter(description = "ID của đơn nợ")
            @PathVariable Long id
    ) {
        log.info("Fulfilling debt order {}", id);
        
        DebtOrderDTO fulfilledOrder = debtOrderService.fulfillDebtOrder(id);
        
        return ResponseEntity.ok(ApiResponse.<DebtOrderDTO>builder()
                .statusCode(200)
                .message("Đáp ứng đơn nợ thành công")
                .data(fulfilledOrder)
                .build());
    }

    /**
     * Check for fulfillable debt orders
     * GET /api/debt-orders/check-fulfillable
     * Requirements: 6.3, 6.4
     */
    @GetMapping("/check-fulfillable")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Kiểm tra đơn nợ có thể đáp ứng", description = "Kiểm tra và cập nhật trạng thái các đơn nợ có thể đáp ứng")
    public ResponseEntity<ApiResponse<List<DebtOrderDTO>>> checkFulfillableDebtOrders() {
        log.info("Checking for fulfillable debt orders");
        
        List<DebtOrderDTO> fulfillableOrders = debtOrderService.checkFulfillableDebtOrders();
        
        String message = fulfillableOrders.isEmpty() 
                ? "Không có đơn nợ nào có thể đáp ứng" 
                : String.format("Tìm thấy %d đơn nợ có thể đáp ứng", fulfillableOrders.size());
        
        return ResponseEntity.ok(ApiResponse.<List<DebtOrderDTO>>builder()
                .statusCode(200)
                .message(message)
                .data(fulfillableOrders)
                .build());
    }


    /**
     * Check if a specific debt order can be fulfilled
     * GET /api/debt-orders/{id}/can-fulfill
     */
    @GetMapping("/{id}/can-fulfill")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Kiểm tra khả năng đáp ứng", description = "Kiểm tra xem đơn nợ có thể được đáp ứng hay không")
    public ResponseEntity<ApiResponse<Boolean>> canFulfillDebtOrder(
            @Parameter(description = "ID của đơn nợ")
            @PathVariable Long id
    ) {
        boolean canFulfill = debtOrderService.canFulfillDebtOrder(id);
        
        String message = canFulfill 
                ? "Đơn nợ có thể được đáp ứng" 
                : "Đơn nợ chưa thể đáp ứng do không đủ hàng";
        
        return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                .statusCode(200)
                .message(message)
                .data(canFulfill)
                .build());
    }

    /**
     * Get debt order by transfer request ID
     * GET /api/debt-orders/by-transfer-request/{transferRequestId}
     */
    @GetMapping("/by-transfer-request/{transferRequestId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Lấy đơn nợ theo yêu cầu chuyển kho", description = "Lấy đơn nợ liên kết với một yêu cầu chuyển kho")
    public ResponseEntity<ApiResponse<DebtOrderDTO>> getDebtOrderByTransferRequest(
            @Parameter(description = "ID của yêu cầu chuyển kho")
            @PathVariable Long transferRequestId
    ) {
        DebtOrderDTO debtOrder = debtOrderService.getDebtOrderByTransferRequestId(transferRequestId);
        
        return ResponseEntity.ok(ApiResponse.<DebtOrderDTO>builder()
                .statusCode(200)
                .message("Lấy đơn nợ thành công")
                .data(debtOrder)
                .build());
    }
}
