package com.example.onlyfanshop_be.entity;

import com.example.onlyfanshop_be.enums.CouponStatus;
import com.example.onlyfanshop_be.enums.DiscountType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "description", length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, columnDefinition = "ENUM('percent','fixed')")
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, columnDefinition = "DECIMAL(15,2)")
    private BigDecimal discountValue;

    @Column(name = "max_discount", columnDefinition = "DECIMAL(15,2)")
    private BigDecimal maxDiscount;

    @Column(name = "min_order_value", columnDefinition = "DECIMAL(15,2)")
    private BigDecimal minOrderValue;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "used_count", columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer usedCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "ENUM('active','inactive','expired') DEFAULT 'active'")
    @Builder.Default
    private CouponStatus status = CouponStatus.active;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "coupon")
    @JsonIgnore
    private List<CouponUserUsage> usages;
}

