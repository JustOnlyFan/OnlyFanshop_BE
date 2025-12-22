package com.example.onlyfanshop_be.dto.response;

import lombok.*;

import java.util.List;

/**
 * Result of an availability check for a transfer request
 * Contains detailed availability information for each product and overall fulfillment status
 * Requirements: 5.1, 9.1, 9.2, 9.4, 9.5
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityCheckResult {
    /**
     * The transfer request ID being checked
     */
    private Long transferRequestId;
    
    /**
     * Availability information for each product in the request
     */
    private List<ProductAvailability> productAvailabilities;
    
    /**
     * Whether the entire request can be fully fulfilled
     */
    private Boolean canFullyFulfill;
    
    /**
     * Total shortage across all products (sum of individual shortages)
     * Requirements: 9.5
     */
    private Integer totalShortage;
    
    /**
     * Total requested quantity across all products
     */
    private Integer totalRequestedQuantity;
    
    /**
     * Total available quantity across all products
     */
    private Integer totalAvailableQuantity;
    
    /**
     * Maximum fulfillable quantity (min of requested and available)
     */
    private Integer maxFulfillableQuantity;
    
    /**
     * Summary message describing the fulfillment status
     */
    private String summary;
}
