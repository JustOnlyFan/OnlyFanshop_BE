package com.example.onlyfanshop_be.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferRequestItemDTO {
    private Long id;
    private Long productId;
    private String productName;
    private String productSku;
    private String productImageUrl;
    private Integer requestedQuantity;
    private Integer fulfilledQuantity;
    private Integer shortageQuantity;
}
