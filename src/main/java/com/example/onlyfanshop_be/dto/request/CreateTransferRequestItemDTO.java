package com.example.onlyfanshop_be.dto.request;

import lombok.*;

/**
 * DTO for a single item in a Transfer Request
 * Requirements: 4.2 - WHEN Staff specifies quantity for a product THEN the System SHALL enforce a maximum of 30 units per product per request
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTransferRequestItemDTO {
    /**
     * The product ID to request
     */
    private Long productId;
    
    /**
     * The quantity to request (max 30 per product)
     */
    private Integer quantity;
}
