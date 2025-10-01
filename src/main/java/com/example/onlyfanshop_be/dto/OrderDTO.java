package com.example.onlyfanshop_be.dto;

import com.example.onlyfanshop_be.entity.Order;
import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderDTO {
    private Long orderId;
    private String orderStatus;
    private String orderStatusDisplayName;
    private String paymentMethod;
    private String paymentMethodDisplayName;
    private String paymentStatus;
    private String paymentStatusDisplayName;
    private BigDecimal totalAmount;
    private String billingAddress;
    private String shippingAddress;
    private String notes;
    private LocalDateTime orderDate;
    private LocalDateTime shippedDate;
    private LocalDateTime deliveredDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long userId;
    private String username;
    private List<OrderDetailDTO> orderDetails;

    public static OrderDTO fromOrder(Order order) {
        return OrderDTO.builder()
                .orderId(order.getOrderId())
                .orderStatus(order.getOrderStatus() != null ? order.getOrderStatus().name() : null)
                .orderStatusDisplayName(order.getOrderStatus() != null ? order.getOrderStatus().getDisplayName() : null)
                .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : null)
                .paymentMethodDisplayName(order.getPaymentMethod() != null ? order.getPaymentMethod().getDisplayName() : null)
                .paymentStatus(order.getPaymentStatus() != null ? order.getPaymentStatus().name() : null)
                .paymentStatusDisplayName(order.getPaymentStatus() != null ? order.getPaymentStatus().getDisplayName() : null)
                .totalAmount(order.getTotalAmount())
                .billingAddress(order.getBillingAddress())
                .shippingAddress(order.getShippingAddress())
                .notes(order.getNotes())
                .orderDate(order.getOrderDate())
                .shippedDate(order.getShippedDate())
                .deliveredDate(order.getDeliveredDate())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .userId(order.getUser() != null ? order.getUser().getUserId() : null)
                .username(order.getUser() != null ? order.getUser().getUsername() : null)
                .build();
    }
}