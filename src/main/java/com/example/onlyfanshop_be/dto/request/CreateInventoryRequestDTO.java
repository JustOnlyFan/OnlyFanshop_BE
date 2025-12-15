package com.example.onlyfanshop_be.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateInventoryRequestDTO {
    @NotNull(message = "Store ID không được để trống")
    private Integer storeId;

    @NotEmpty(message = "Danh sách sản phẩm không được để trống")
    @Valid
    private List<CreateInventoryRequestItemDTO> items;

    private String note;

    // Legacy fields - giữ lại để tương thích ngược
    private Long productId;
    private Integer quantity;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateInventoryRequestItemDTO {
        @NotNull(message = "Product ID không được để trống")
        private Long productId;

        @NotNull(message = "Số lượng không được để trống")
        @Min(value = 1, message = "Số lượng phải lớn hơn 0")
        private Integer quantity;
    }
}
