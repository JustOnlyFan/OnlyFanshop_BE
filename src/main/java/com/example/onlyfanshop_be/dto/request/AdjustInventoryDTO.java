package com.example.onlyfanshop_be.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustInventoryDTO {
    @NotNull(message = "Product ID không được để trống")
    private Long productId;

    /**
     * Store ID (null = điều chỉnh kho tổng)
     */
    private Integer storeId;

    @NotNull(message = "Số lượng mới không được để trống")
    @Min(value = 0, message = "Số lượng không được âm")
    private Integer newQuantity;

    private String note;
}
