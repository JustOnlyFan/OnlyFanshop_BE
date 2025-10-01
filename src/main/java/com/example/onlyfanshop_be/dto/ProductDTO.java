package com.example.onlyfanshop_be.dto;

import com.example.onlyfanshop_be.entity.Product;
import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProductDTO {

    private Integer productId;
    private String productName;
    private String briefDescription;
    private String fullDescription;
    private String technicalSpecifications;
    private BigDecimal price;
    private String imageUrl;
    private String category;
    private String categoryDisplayName;
    private String brand;
    private String brandDisplayName;

    public static ProductDTO fromProduct(Product product) {
        return ProductDTO.builder()
                .productId(product.getProductId())
                .productName(product.getProductName())
                .briefDescription(product.getBriefDescription())
                .fullDescription(product.getFullDescription())
                .technicalSpecifications(product.getTechnicalSpecifications())
                .price(product.getPrice())
                .imageUrl(product.getImageUrl())
                .category(product.getCategory() != null ? product.getCategory().getCategoryName() : null)
                .categoryDisplayName(product.getCategory() != null ? product.getCategory().getCategoryName() : null)
                .brand(product.getBrand() != null ? product.getBrand().getBrandName() : null)
                .brandDisplayName(product.getBrand() != null ? product.getBrand().getBrandName() : null)
                .build();
    }
}