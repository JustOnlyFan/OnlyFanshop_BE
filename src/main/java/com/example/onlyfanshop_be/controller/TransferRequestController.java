package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.TransferRequestDTO;
import com.example.onlyfanshop_be.dto.request.CreateTransferRequestDTO;
import com.example.onlyfanshop_be.dto.request.RejectTransferRequestDTO;
import com.example.onlyfanshop_be.dto.response.AvailabilityCheckResult;
import com.example.onlyfanshop_be.dto.response.FulfillmentResult;
import com.example.onlyfanshop_be.enums.TransferRequestStatus;
import com.example.onlyfanshop_be.service.IFulfillmentService;
import com.example.onlyfanshop_be.service.ITransferRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Transfer Request operations
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 5.1, 9.1, 9.2, 9.4, 9.5
 */
@RestController
@RequestMapping("/api/transfer-requests")
@RequiredArgsConstructor
@Tag(name = "Transfer Request", description = "APIs for managing transfer requests between warehouses")
public class TransferRequestController {
    
    private final ITransferRequestService transferRequestService;
    private final IFulfillmentService fulfillmentService;
    
    /**
     * Create a new transfer request
     * POST /api/transfer-requests
     * Requirements: 4.1, 4.2, 4.3, 4.4
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Create a new transfer request", 
               description = "Staff creates a transfer request for products. Max 30 units per product.")
    public ResponseEntity<TransferRequestDTO> createRequest(
            @Parameter(description = "Store ID creating the request") 
            @RequestParam Integer storeId,
            @RequestBody CreateTransferRequestDTO request) {
        
        TransferRequestDTO created = transferRequestService.createRequest(storeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    /**
     * Get all transfer requests with optional filters
     * GET /api/transfer-requests
     * Requirements: 4.5
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get all transfer requests", 
               description = "Get paginated list of transfer requests with optional status filter")
    public ResponseEntity<Page<TransferRequestDTO>> getRequests(
            @Parameter(description = "Filter by status") 
            @RequestParam(required = false) TransferRequestStatus status,
            @Parameter(description = "Filter by store ID") 
            @RequestParam(required = false) Integer storeId,
            @Parameter(description = "Page number (0-based)") 
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") 
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<TransferRequestDTO> requests;
        
        if (storeId != null) {
            requests = transferRequestService.getRequestsByStore(storeId, status, pageable);
        } else {
            requests = transferRequestService.getRequests(status, pageable);
        }
        
        return ResponseEntity.ok(requests);
    }

    
    /**
     * Get a specific transfer request by ID
     * GET /api/transfer-requests/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get transfer request by ID", 
               description = "Get details of a specific transfer request")
    public ResponseEntity<TransferRequestDTO> getRequest(
            @Parameter(description = "Transfer request ID") 
            @PathVariable Long id) {
        
        TransferRequestDTO request = transferRequestService.getRequest(id);
        return ResponseEntity.ok(request);
    }
    
    /**
     * Check availability for a transfer request
     * GET /api/transfer-requests/{id}/availability
     * Requirements: 5.1, 9.1, 9.2, 9.4, 9.5
     */
    @GetMapping("/{id}/availability")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Check availability for a transfer request", 
               description = "Check inventory availability across all warehouses for a transfer request. " +
                           "Returns Main Warehouse quantity, Store Warehouse breakdown, and fulfillment recommendations.")
    public ResponseEntity<AvailabilityCheckResult> checkAvailability(
            @Parameter(description = "Transfer request ID") 
            @PathVariable Long id) {
        
        AvailabilityCheckResult result = fulfillmentService.checkAvailabilityById(id);
        return ResponseEntity.ok(result);
    }
    
    /**
     * Fulfill a transfer request
     * POST /api/transfer-requests/{id}/fulfill
     * Requirements: 5.5, 5.6
     */
    @PostMapping("/{id}/fulfill")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Fulfill a transfer request", 
               description = "Fulfill a transfer request by deducting inventory from source warehouses. " +
                           "Creates debt order if partial fulfillment is needed.")
    public ResponseEntity<FulfillmentResult> fulfillRequest(
            @Parameter(description = "Transfer request ID") 
            @PathVariable Long id) {
        
        FulfillmentResult result = fulfillmentService.fulfillById(id);
        return ResponseEntity.ok(result);
    }
    
    /**
     * Approve a transfer request
     * POST /api/transfer-requests/{id}/approve
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve a transfer request", 
               description = "Admin approves a pending transfer request")
    public ResponseEntity<TransferRequestDTO> approveRequest(
            @Parameter(description = "Transfer request ID") 
            @PathVariable Long id) {
        
        TransferRequestDTO approved = transferRequestService.approveRequest(id);
        return ResponseEntity.ok(approved);
    }
    
    /**
     * Reject a transfer request
     * POST /api/transfer-requests/{id}/reject
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject a transfer request", 
               description = "Admin rejects a pending transfer request with a reason")
    public ResponseEntity<TransferRequestDTO> rejectRequest(
            @Parameter(description = "Transfer request ID") 
            @PathVariable Long id,
            @RequestBody RejectTransferRequestDTO rejectRequest) {
        
        TransferRequestDTO rejected = transferRequestService.rejectRequest(id, rejectRequest.getReason());
        return ResponseEntity.ok(rejected);
    }
}
