package com.example.onlyfanshop_be.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Request DTO for adding a product to a store warehouse
 * Requirements: 2.2 - WHEN Admin adds a product to a store THEN the System SHALL create an Inventory_Item with the specified quantity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddProductToWarehouseRequest {
    @NotNull(message = "Product ID is required")
    private Long productId;
    
    /**
     * Initial quantity for the product in the warehouse
     * If not provided, defaults to 0
     */
    @Min(value = 0, message = "Quantity must be non-negative")
    private Integer quantity;
}
