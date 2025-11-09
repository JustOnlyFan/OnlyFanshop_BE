package com.example.onlyfanshop_be.entity;

import com.example.onlyfanshop_be.enums.DiscountType;
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
@Table(name = "product_discounts",
    indexes = {@Index(name = "idx_product_discounts_product_id", columnList = "product_id")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDiscount {
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

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, columnDefinition = "ENUM('percent','fixed')")
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, columnDefinition = "DECIMAL(15,2)")
    private BigDecimal discountValue;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "active", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    @Builder.Default
    private Boolean active = true;
}

