package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.DebtOrderDTO;
import com.example.onlyfanshop_be.entity.DebtItem;
import com.example.onlyfanshop_be.entity.TransferRequest;
import com.example.onlyfanshop_be.enums.DebtOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

/**
 * Service interface for Debt Order operations
 * Handles debt order creation, fulfillment, and management
 * Requirements: 6.1, 6.2, 6.3, 6.4, 6.5
 */
public interface IDebtOrderService {
    
    /**
     * Create a debt order for unfulfilled quantities from a transfer request
     * Requirements: 6.1 - WHEN a Transfer_Request cannot be fully fulfilled THEN the System SHALL create a Debt_Order with remaining quantities
     * Requirements: 6.2 - WHEN a Debt_Order is created THEN the System SHALL link the Debt_Order to the original Transfer_Request
     * 
     * @param request The transfer request that was partially fulfilled
     * @param shortageQuantities Map of product ID to shortage quantity
     * @return The created DebtOrderDTO
     */
    DebtOrderDTO createDebtOrder(TransferRequest request, Map<Long, Integer> shortageQuantities);
    
    /**
     * Get all debt orders with optional status filter
     * 
     * @param status Optional status filter
     * @param pageable Pagination information
     * @return Page of DebtOrderDTOs
     */
    Page<DebtOrderDTO> getDebtOrders(DebtOrderStatus status, Pageable pageable);
    
    /**
     * Get a specific debt order by ID
     * 
     * @param id The debt order ID
     * @return The DebtOrderDTO
     */
    DebtOrderDTO getDebtOrder(Long id);
    
    /**
     * Fulfill a debt order by creating shipments for the owed quantities
     * Requirements: 6.5 - WHEN a Debt_Order is fulfilled THEN the System SHALL update Debt_Order status to COMPLETED and create Shipment
     * 
     * @param id The debt order ID to fulfill
     * @return The updated DebtOrderDTO
     */
    DebtOrderDTO fulfillDebtOrder(Long id);
    
    /**
     * Check all pending debt orders and mark as FULFILLABLE if inventory is available
     * Requirements: 6.3 - WHEN Admin updates Main_Warehouse inventory THEN the System SHALL check if any Debt_Orders can be fulfilled
     * Requirements: 6.4 - WHEN a Debt_Order can be fulfilled THEN the System SHALL notify Admin and allow fulfillment processing
     * 
     * @return List of debt orders that are now fulfillable
     */
    List<DebtOrderDTO> checkFulfillableDebtOrders();
    
    /**
     * Check if a specific debt order can be fulfilled based on current inventory
     * 
     * @param debtOrderId The debt order ID to check
     * @return true if the debt order can be fulfilled
     */
    boolean canFulfillDebtOrder(Long debtOrderId);
    
    /**
     * Get debt order by transfer request ID
     * 
     * @param transferRequestId The transfer request ID
     * @return The DebtOrderDTO if exists
     */
    DebtOrderDTO getDebtOrderByTransferRequestId(Long transferRequestId);
}
