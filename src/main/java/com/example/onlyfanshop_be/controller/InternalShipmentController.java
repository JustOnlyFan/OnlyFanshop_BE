package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.enums.InternalShipmentStatus;
import com.example.onlyfanshop_be.service.IInternalShipmentService;
import com.example.onlyfanshop_be.service.IInternalShipmentService.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * InternalShipmentController - REST API for internal warehouse shipments
 * Requirements: 8.2, 8.4
 */
@RestController
@RequestMapping("/api/internal-shipments")
@RequiredArgsConstructor
@Tag(name = "Internal Shipment", description = "API quản lý vận chuyển nội bộ giữa các kho")
public class InternalShipmentController {
    
    private final IInternalShipmentService internalShipmentService;
    
    /**
     * GET /api/internal-shipments
     * Get paginated list of internal shipments
     * Requirements: 8.4
     */
    @GetMapping
    @Operation(summary = "Lấy danh sách đơn vận chuyển nội bộ")
    public ResponseEntity<ApiResponse<Page<InternalShipmentDTO>>> getShipments(
            @RequestParam(required = false) InternalShipmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<InternalShipmentDTO> shipments = internalShipmentService.getShipments(status, pageable);
        
        return ResponseEntity.ok(ApiResponse.<Page<InternalShipmentDTO>>builder()
                .statusCode(200)
                .message("Success")
                .data(shipments)
                .build());
    }
    
    /**
     * GET /api/internal-shipments/{id}
     * Get a single internal shipment by ID
     * Requirements: 8.4
     */
    @GetMapping("/{id}")
    @Operation(summary = "Lấy thông tin đơn vận chuyển nội bộ theo ID")
    public ResponseEntity<ApiResponse<InternalShipmentDTO>> getShipment(@PathVariable Long id) {
        InternalShipmentDTO shipment = internalShipmentService.getShipment(id);
        
        return ResponseEntity.ok(ApiResponse.<InternalShipmentDTO>builder()
                .statusCode(200)
                .message("Success")
                .data(shipment)
                .build());
    }
    
    /**
     * GET /api/internal-shipments/{id}/tracking
     * Get tracking history for a shipment
     * Requirements: 8.4
     */
    @GetMapping("/{id}/tracking")
    @Operation(summary = "Lấy lịch sử tracking của đơn vận chuyển nội bộ")
    public ResponseEntity<ApiResponse<InternalShipmentTrackingDTO>> getShipmentTracking(@PathVariable Long id) {
        InternalShipmentTrackingDTO tracking = internalShipmentService.getShipmentTracking(id);
        
        return ResponseEntity.ok(ApiResponse.<InternalShipmentTrackingDTO>builder()
                .statusCode(200)
                .message("Success")
                .data(tracking)
                .build());
    }
    
    /**
     * POST /api/internal-shipments/{id}/sync
     * Sync shipment status from GHN API
     * Requirements: 8.2
     */
    @PostMapping("/{id}/sync")
    @Operation(summary = "Đồng bộ trạng thái đơn vận chuyển từ GHN")
    public ResponseEntity<ApiResponse<InternalShipmentDTO>> syncShipmentStatus(@PathVariable Long id) {
        InternalShipmentDTO shipment = internalShipmentService.syncShipmentStatus(id);
        
        return ResponseEntity.ok(ApiResponse.<InternalShipmentDTO>builder()
                .statusCode(200)
                .message("Đã đồng bộ trạng thái từ GHN")
                .data(shipment)
                .build());
    }
    
    /**
     * GET /api/internal-shipments/transfer-request/{transferRequestId}
     * Get shipments by transfer request ID
     */
    @GetMapping("/transfer-request/{transferRequestId}")
    @Operation(summary = "Lấy danh sách đơn vận chuyển theo yêu cầu chuyển kho")
    public ResponseEntity<ApiResponse<List<InternalShipmentDTO>>> getShipmentsByTransferRequest(
            @PathVariable Long transferRequestId) {
        
        List<InternalShipmentDTO> shipments = internalShipmentService.getShipmentsByTransferRequest(transferRequestId);
        
        return ResponseEntity.ok(ApiResponse.<List<InternalShipmentDTO>>builder()
                .statusCode(200)
                .message("Success")
                .data(shipments)
                .build());
    }
}
