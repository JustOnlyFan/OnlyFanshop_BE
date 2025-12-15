package com.example.onlyfanshop_be.entity;

import com.example.onlyfanshop_be.enums.InventoryLocationType;
import com.example.onlyfanshop_be.enums.InventoryTransactionType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * InventoryTransaction - Lịch sử giao dịch kho
 * Ghi lại mọi thay đổi số lượng tồn kho (chuyển kho, bán hàng, điều chỉnh, nhập hàng)
 */
@Entity
@Table(name = "inventory_transactions",
    indexes = {
        @Index(name = "idx_inv_trans_type", columnList = "transaction_type"),
        @Index(name = "idx_inv_trans_product_id", columnList = "product_id"),
        @Index(name = "idx_inv_trans_source_store", columnList = "source_store_id"),
        @Index(name = "idx_inv_trans_dest_store", columnList = "destination_store_id"),
        @Index(name = "idx_inv_trans_request_id", columnList = "request_id"),
        @Index(name = "idx_inv_trans_created_at", columnList = "created_at")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    private InventoryTransactionType transactionType;

    @Column(name = "product_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private Product product;


    /**
     * Số lượng giao dịch (luôn dương)
     */
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    /**
     * Loại nguồn: CENTRAL (kho tổng) hoặc STORE (kho cửa hàng)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private InventoryLocationType sourceType;

    /**
     * ID cửa hàng nguồn (null nếu sourceType = CENTRAL)
     */
    @Column(name = "source_store_id")
    private Integer sourceStoreId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_store_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private StoreLocation sourceStore;

    /**
     * Loại đích: CENTRAL (kho tổng) hoặc STORE (kho cửa hàng)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "destination_type", nullable = false, length = 20)
    private InventoryLocationType destinationType;

    /**
     * ID cửa hàng đích (null nếu destinationType = CENTRAL)
     */
    @Column(name = "destination_store_id")
    private Integer destinationStoreId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_store_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private StoreLocation destinationStore;

    /**
     * Liên kết đến InventoryRequest (nếu giao dịch từ request nhập hàng)
     */
    @Column(name = "request_id", columnDefinition = "BIGINT UNSIGNED")
    private Long requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private InventoryRequest request;

    /**
     * Liên kết đến Order (nếu giao dịch từ bán hàng)
     */
    @Column(name = "order_id", columnDefinition = "BIGINT UNSIGNED")
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private Order order;

    /**
     * User thực hiện giao dịch
     */
    @Column(name = "performed_by", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long performedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private User performer;

    /**
     * Số lượng tồn kho trước giao dịch (để audit)
     */
    @Column(name = "quantity_before")
    private Integer quantityBefore;

    /**
     * Số lượng tồn kho sau giao dịch (để audit)
     */
    @Column(name = "quantity_after")
    private Integer quantityAfter;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
