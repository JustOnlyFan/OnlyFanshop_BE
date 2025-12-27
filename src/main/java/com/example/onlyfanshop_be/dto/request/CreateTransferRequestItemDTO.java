package com.example.onlyfanshop_be.dto.request;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTransferRequestItemDTO {

    private Long productId;

    private Integer quantity;
}
