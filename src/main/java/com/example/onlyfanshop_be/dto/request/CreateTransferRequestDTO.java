package com.example.onlyfanshop_be.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

/**
 * DTO for creating a new Transfer Request
 * Requirements: 3.1, 4.1, 4.2, 4.3 - Staff creates Transfer_Request with source warehouse and multiple products
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTransferRequestDTO {
    
    /**
     * ID của kho nguồn - nơi hàng được chuyển đi
     * Requirements: 3.1 - WHEN Store_Staff creates a Transfer_Request THEN the System SHALL require specifying a source Store_Warehouse
     */
    @NotNull(message = "Source warehouse ID is required")
    private Long sourceWarehouseId;
    
    /**
     * List of items to request
     * Requirements: 4.3 - WHEN Staff submits a Transfer_Request THEN the System SHALL allow multiple products in a single request
     */
    @NotEmpty(message = "Items cannot be empty")
    private List<CreateTransferRequestItemDTO> items;
    
    /**
     * Ghi chú cho yêu cầu chuyển kho
     */
    private String note;
}
