package com.example.onlyfanshop_be.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_items",
    uniqueConstraints = {
        @UniqueConstraint(name = "uniq_warehouse_product",
            columnNames = {"warehouse_id", "product_id"})
    },
    indexes = {
        @Index(name = "idx_inventory_item_warehouse_id", columnList = "warehouse_id"),
        @Index(name = "idx_inventory_item_product_id", columnList = "product_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @Column(name = "warehouse_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long warehouseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private Warehouse warehouse;

    @Column(name = "product_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private Product product;

    @Column(name = "quantity", nullable = false, columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer quantity = 0;

    @Column(name = "reserved_quantity", nullable = false, columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer reservedQuantity = 0;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @Transient
    public Integer getAvailableQuantity() {
        return quantity - reservedQuantity;
    }
}
