package com.example.onlyfanshop_be.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cart_items",
    indexes = {@Index(name = "idx_cart_items_cart_id", columnList = "cart_id")})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @Column(name = "cart_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long cartId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", insertable = false, updatable = false)
    @JsonIgnore
    private Cart cart;

    @Column(name = "product_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Product product;

    @Column(name = "product_variant_id", columnDefinition = "BIGINT UNSIGNED")
    private Long productVariantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private ProductVariant productVariant;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_price_snapshot", nullable = false, columnDefinition = "DECIMAL(15,2)")
    private BigDecimal unitPriceSnapshot;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    // Legacy fields for backward compatibility
    @Transient
    public Integer getCartItemID() {
        return id != null ? id.intValue() : null;
    }

    @Transient
    public Double getPrice() {
        return unitPriceSnapshot != null ? unitPriceSnapshot.doubleValue() : null;
    }

    @Transient
    public boolean isChecked() {
        return false; // Not in new schema
    }
}
