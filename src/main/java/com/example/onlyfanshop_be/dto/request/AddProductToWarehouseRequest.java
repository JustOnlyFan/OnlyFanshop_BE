package com.example.onlyfanshop_be.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Request DTO for adding a product to a store warehouse
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddProductToWarehouseRequest {
    @NotNull(message = "Product ID is required")
    private Long productId;
}
