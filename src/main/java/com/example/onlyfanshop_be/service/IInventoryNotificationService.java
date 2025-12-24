package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.enums.InternalShipmentStatus;

/**
 * IInventoryNotificationService - Interface for inventory-related notifications
 * Handles notifications for shipment status changes, debt order fulfillment, and GHN token expiry
 * Requirements: 8.5, 6.4, 10.5
 */
public interface IInventoryNotificationService {
    
    /**
     * Notify staff when shipment status changes
     * Requirements: 8.5 - WHEN Shipment status changes THEN the System SHALL send notification to relevant Staff
     * 
     * @param shipmentId The shipment ID
     * @param oldStatus The previous status
     * @param newStatus The new status
     */
    void notifyShipmentStatusChange(Long shipmentId, InternalShipmentStatus oldStatus, InternalShipmentStatus newStatus);
    
    /**
     * Notify admin when a debt order becomes fulfillable
     * Requirements: 6.4 - WHEN a Debt_Order can be fulfilled THEN the System SHALL notify Admin
     * 
     * @param debtOrderId The debt order ID
     * @param transferRequestId The associated transfer request ID
     */
    void notifyDebtOrderFulfillable(Long debtOrderId, Long transferRequestId);
    
    /**
     * Notify admin when GHN token is about to expire
     * Requirements: 10.5 - WHEN GHN token expires THEN the System SHALL notify Admin to update the configuration
     * 
     * @param daysUntilExpiry Days until the token expires (0 if already expired)
     */
    void notifyGHNTokenExpiry(int daysUntilExpiry);
    
    /**
     * Notify admin about GHN API errors
     * Requirements: 7.4 - WHEN GHN_API returns error THEN the System SHALL notify Admin with error details
     * 
     * @param errorMessage The error message from GHN API
     * @param shipmentId The shipment ID (if applicable)
     */
    void notifyGHNApiError(String errorMessage, Long shipmentId);
}
