package com.example.onlyfanshop_be.entity;

import com.example.onlyfanshop_be.enums.ProductStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_variants",
    indexes = {@Index(name = "idx_product_variants_product_id", columnList = "product_id")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @Column(name = "product_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private Product product;

    @Column(name = "sku", unique = true, length = 100)
    private String sku;

    @Column(name = "variant_name", length = 150)
    private String variantName;

    @Column(name = "color", length = 50)
    private String color;

    @Column(name = "extra_price", columnDefinition = "DECIMAL(15,2) DEFAULT 0")
    @Builder.Default
    private BigDecimal extraPrice = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "ENUM('active','inactive','out_of_stock') DEFAULT 'active'")
    @Builder.Default
    private ProductStatus status = ProductStatus.active;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;
}

