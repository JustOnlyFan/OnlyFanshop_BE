package com.example.onlyfanshop_be.dto;

import com.example.onlyfanshop_be.entity.Cart;
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
public class CartDTO {
    private Long cartId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long userId;
    private String username;
    private Integer productId;
    private String productName;
    private String productImageUrl;

    public static CartDTO fromCart(Cart cart) {
        return CartDTO.builder()
                .cartId(cart.getCartId())
                .quantity(cart.getQuantity())
                .unitPrice(cart.getUnitPrice())
                .totalPrice(cart.getTotalPrice())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .userId(cart.getUser() != null ? cart.getUser().getUserId() : null)
                .username(cart.getUser() != null ? cart.getUser().getUsername() : null)
                .productId(cart.getProduct() != null ? cart.getProduct().getProductId() : null)
                .productName(cart.getProduct() != null ? cart.getProduct().getProductName() : null)
                .productImageUrl(cart.getProduct() != null ? cart.getProduct().getImageUrl() : null)
                .build();
    }
}