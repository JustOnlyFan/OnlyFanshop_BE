package com.example.onlyfanshop_be.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

/**
 * DebtItem - Chi tiết sản phẩm trong đơn nợ
 */
@Entity
@Table(name = "debt_items",
    indexes = {
        @Index(name = "idx_debt_item_debt_order_id", columnList = "debt_order_id"),
        @Index(name = "idx_debt_item_product_id", columnList = "product_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebtItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @Column(name = "debt_order_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long debtOrderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debt_order_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private DebtOrder debtOrder;

    @Column(name = "product_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private Product product;

    @Column(name = "owed_quantity", nullable = false)
    private Integer owedQuantity;

    @Column(name = "fulfilled_quantity", nullable = false, columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer fulfilledQuantity = 0;

    /**
     * Số lượng còn nợ
     */
    @Transient
    public Integer getRemainingQuantity() {
        return owedQuantity - fulfilledQuantity;
    }

    /**
     * Kiểm tra đã đáp ứng đủ chưa
     */
    @Transient
    public boolean isFullyFulfilled() {
        return fulfilledQuantity >= owedQuantity;
    }
}
