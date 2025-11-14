package com.example.onlyfanshop_be.dto.request;

import lombok.Data;

@Data
public class AddProductToWarehouseRequest {
    private Integer warehouseId;
    private Long productId;
    private Long productVariantId;
    private Integer quantity;
    private String note;
}








