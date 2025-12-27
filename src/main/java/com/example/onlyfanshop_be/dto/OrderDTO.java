package com.example.onlyfanshop_be.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {

    private Integer orderID;
    private String paymentMethod;
    private String billingAddress;
    private String orderStatus;
    private LocalDateTime orderDate;
    private Double totalPrice;

    // First product info for list display
    private String firstProductName;
    private String firstProductImage;
    private Integer firstProductQuantity;
    private Double firstProductPrice;

    // List of all products in order
    private List<OrderItemLiteDTO> products;
    private Integer totalProductCount;

}
