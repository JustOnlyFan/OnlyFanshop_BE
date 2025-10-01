package com.example.onlyfanshop_be.dto;

import com.example.onlyfanshop_be.entity.OrderDetail;
import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderDetailDTO {
    private Long orderDetailId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long orderId;
    private Integer productId;
    private String productName;
    private String productImageUrl;

    public static OrderDetailDTO fromOrderDetail(OrderDetail orderDetail) {
        return OrderDetailDTO.builder()
                .orderDetailId(orderDetail.getOrderDetailId())
                .quantity(orderDetail.getQuantity())
                .unitPrice(orderDetail.getUnitPrice())
                .totalPrice(orderDetail.getTotalPrice())
                .notes(orderDetail.getNotes())
                .createdAt(orderDetail.getCreatedAt())
                .updatedAt(orderDetail.getUpdatedAt())
                .orderId(orderDetail.getOrder() != null ? orderDetail.getOrder().getOrderId() : null)
                .productId(orderDetail.getProduct() != null ? orderDetail.getProduct().getProductId() : null)
                .productName(orderDetail.getProduct() != null ? orderDetail.getProduct().getProductName() : null)
                .productImageUrl(orderDetail.getProduct() != null ? orderDetail.getProduct().getImageUrl() : null)
                .build();
    }
}