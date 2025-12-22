package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.TransferRequestDTO;
import com.example.onlyfanshop_be.dto.request.CreateTransferRequestDTO;
import com.example.onlyfanshop_be.enums.TransferRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for Transfer Request management operations
 * Requirements: 4.1, 4.2, 4.3, 4.4, 4.5
 */
public interface ITransferRequestService {
    
    /**
     * Create a new transfer request for a store
     * Requirements: 4.1 - WHEN Staff creates a Transfer_Request THEN the System SHALL validate that all requested products exist in the Store_Warehouse
     * Requirements: 4.2 - WHEN Staff specifies quantity for a product THEN the System SHALL enforce a maximum of 30 units per product per request
     * Requirements: 4.3 - WHEN Staff submits a Transfer_Request THEN the System SHALL allow multiple products in a single request
     * Requirements: 4.4 - WHEN a Transfer_Request is created THEN the System SHALL set status to PENDING and record creation timestamp
     * 
     * @param storeId The ID of the store creating the request
     * @param request The transfer request details
     * @return Created TransferRequestDTO
     */
    TransferRequestDTO createRequest(Integer storeId, CreateTransferRequestDTO request);
    
    /**
     * Get all transfer requests with optional status filter
     * Requirements: 4.5 - WHEN Staff views their Transfer_Requests THEN the System SHALL display all requests with status, products, quantities, and timestamps
     * 
     * @param status Optional status filter
     * @param pageable Pagination information
     * @return Page of TransferRequestDTO
     */
    Page<TransferRequestDTO> getRequests(TransferRequestStatus status, Pageable pageable);
    
    /**
     * Get transfer requests for a specific store
     * 
     * @param storeId The store ID
     * @param status Optional status filter
     * @param pageable Pagination information
     * @return Page of TransferRequestDTO
     */
    Page<TransferRequestDTO> getRequestsByStore(Integer storeId, TransferRequestStatus status, Pageable pageable);
    
    /**
     * Get a specific transfer request by ID
     * 
     * @param id The transfer request ID
     * @return TransferRequestDTO
     */
    TransferRequestDTO getRequest(Long id);
    
    /**
     * Approve a transfer request
     * Note: Full approval logic with fulfillment will be implemented in FulfillmentService (Task 6)
     * 
     * @param id The transfer request ID
     * @return Updated TransferRequestDTO
     */
    TransferRequestDTO approveRequest(Long id);
    
    /**
     * Reject a transfer request
     * 
     * @param id The transfer request ID
     * @param reason The rejection reason
     * @return Updated TransferRequestDTO
     */
    TransferRequestDTO rejectRequest(Long id, String reason);
}
