package com.example.onlyfanshop_be.dto.request;

import lombok.*;

import java.util.List;

/**
 * DTO for creating a new Transfer Request
 * Requirements: 4.1, 4.2, 4.3 - Staff creates Transfer_Request with multiple products
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTransferRequestDTO {
    /**
     * List of items to request
     * Requirements: 4.3 - WHEN Staff submits a Transfer_Request THEN the System SHALL allow multiple products in a single request
     */
    private List<CreateTransferRequestItemDTO> items;
}
