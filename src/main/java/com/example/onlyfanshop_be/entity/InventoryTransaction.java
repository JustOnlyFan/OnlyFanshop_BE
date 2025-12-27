package com.example.onlyfanshop_be.entity;

import com.example.onlyfanshop_be.enums.InventoryLocationType;
import com.example.onlyfanshop_be.enums.InventoryTransactionType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private InventoryLocationType sourceType;

    @Column(name = "source_store_id", columnDefinition = "INT UNSIGNED")
    private Integer sourceStoreId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_store_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private StoreLocation sourceStore;

    @Enumerated(EnumType.STRING)
    @Column(name = "destination_type", nullable = false, length = 20)
    private InventoryLocationType destinationType;

    @Column(name = "destination_store_id", columnDefinition = "INT UNSIGNED")
    private Integer destinationStoreId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_store_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private StoreLocation destinationStore;

    @Column(name = "request_id", columnDefinition = "BIGINT UNSIGNED")
    private Long requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private InventoryRequest request;

    @Column(name = "order_id", columnDefinition = "BIGINT UNSIGNED")
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private Order order;

    @Column(name = "performed_by", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long performedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performed_by", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private User performer;

    @Column(name = "quantity_before")
    private Integer quantityBefore;

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
