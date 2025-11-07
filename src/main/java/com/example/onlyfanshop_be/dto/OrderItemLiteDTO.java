package com.example.onlyfanshop_be.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemLiteDTO {
    private String productName;
    private String imageURL;
    private Integer quantity;
    private Double price;
}










