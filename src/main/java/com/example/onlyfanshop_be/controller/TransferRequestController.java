package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.TransferRequestDTO;
import com.example.onlyfanshop_be.dto.request.CreateTransferRequestDTO;
import com.example.onlyfanshop_be.dto.request.RejectTransferRequestDTO;
import com.example.onlyfanshop_be.enums.TransferRequestStatus;
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

@RestController
@RequestMapping("/api/transfer-requests")
@RequiredArgsConstructor
@Tag(name = "Transfer Request", description = "APIs for managing transfer requests between warehouses")
public class TransferRequestController {
    
    private final ITransferRequestService transferRequestService;

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

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Complete a transfer request", 
               description = "Admin marks a transfer request as completed after inventory has been transferred")
    public ResponseEntity<TransferRequestDTO> completeRequest(
            @Parameter(description = "Transfer request ID") 
            @PathVariable Long id) {
        
        TransferRequestDTO completed = transferRequestService.completeRequest(id);
        return ResponseEntity.ok(completed);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Cancel a transfer request", 
               description = "Cancel a pending transfer request")
    public ResponseEntity<TransferRequestDTO> cancelRequest(
            @Parameter(description = "Transfer request ID") 
            @PathVariable Long id) {
        
        TransferRequestDTO cancelled = transferRequestService.cancelRequest(id);
        return ResponseEntity.ok(cancelled);
    }
}
