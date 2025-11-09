package com.example.onlyfanshop_be.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items",
    indexes = {
        @Index(name = "idx_order_items_order_id", columnList = "order_id"),
        @Index(name = "idx_order_items_product_id", columnList = "product_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @Column(name = "order_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private Order order;

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

    @Column(name = "product_name", nullable = false, length = 255)
    private String productName;

    @Column(name = "sku", length = 100)
    private String sku;

    @Column(name = "unit_price", nullable = false, columnDefinition = "DECIMAL(15,2)")
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "line_total", nullable = false, columnDefinition = "DECIMAL(15,2)")
    private BigDecimal lineTotal;

    // Legacy fields for backward compatibility
    @Transient
    public Integer getOrderItemID() {
        return id != null ? id.intValue() : null;
    }

    @Transient
    public Double getPrice() {
        return unitPrice != null ? unitPrice.doubleValue() : null;
    }
}
