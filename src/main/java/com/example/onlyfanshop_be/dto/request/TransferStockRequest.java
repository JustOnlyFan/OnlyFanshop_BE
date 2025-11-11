package com.example.onlyfanshop_be.dto.request;

import lombok.Data;

@Data
public class TransferStockRequest {
    private Integer fromWarehouseId;
    private Integer toWarehouseId;
    private Long productId;
    private Long productVariantId;
    private Integer quantity;
    private String note;
}



