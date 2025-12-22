package com.example.onlyfanshop_be.dto.response;

import com.example.onlyfanshop_be.enums.TransferRequestStatus;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * Result of fulfilling a transfer request
 * Contains information about what was fulfilled and any debt orders created
 * Requirements: 5.5, 5.6
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FulfillmentResult {
    /**
     * The transfer request ID that was fulfilled
     */
    private Long transferRequestId;
    
    /**
     * The new status of the transfer request after fulfillment
     */
    private TransferRequestStatus newStatus;
    
    /**
     * Whether the request was fully fulfilled
     */
    private Boolean fullyFulfilled;
    
    /**
     * Map of product ID to fulfilled quantity
     */
    private Map<Long, Integer> fulfilledQuantities;
    
    /**
     * Map of product ID to shortage quantity (for partial fulfillment)
     */
    private Map<Long, Integer> shortageQuantities;
    
    /**
     * Source allocations used for fulfillment (grouped by product ID)
     */
    private Map<Long, List<SourceAllocation>> sourceAllocations;
    
    /**
     * Debt order ID if partial fulfillment created a debt order
     * Requirements: 5.6
     */
    private Long debtOrderId;
    
    /**
     * List of shipment IDs created for this fulfillment
     */
    private List<Long> shipmentIds;
    
    /**
     * Summary message describing the fulfillment result
     */
    private String summary;
}
