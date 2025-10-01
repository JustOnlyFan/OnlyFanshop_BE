package com.example.onlyfanshop_be.dto;

import com.example.onlyfanshop_be.entity.Product;
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
public class ProductDTO {
    private Long productId;
    private String productName;
    private BigDecimal price;
    private String description;
    private String category;
    private String categoryDisplayName;
    private String brand;
    private String brandDisplayName;
    private Integer stockQuantity;
    private String imageUrl;
    private Boolean isActive;
    private BigDecimal weight;
    private String sku;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ProductDTO fromProduct(Product product) {
        return ProductDTO.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .price(product.getPrice())
                .description(product.getDescription())
                .category(product.getCategory() != null ? product.getCategory().name() : null)
                .categoryDisplayName(product.getCategory() != null ? product.getCategory().getDisplayName() : null)
                .brand(product.getBrand() != null ? product.getBrand().name() : null)
                .brandDisplayName(product.getBrand() != null ? product.getBrand().getDisplayName() : null)
                .stockQuantity(product.getStockQuantity())
                .imageUrl(product.getImageUrl())
                .isActive(product.getIsActive())
                .weight(product.getWeight())
                .sku(product.getSku())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
