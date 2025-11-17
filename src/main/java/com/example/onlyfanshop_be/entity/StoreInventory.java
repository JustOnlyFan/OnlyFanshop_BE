package com.example.onlyfanshop_be.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * StoreInventory - Quản lý sản phẩm ở mỗi cửa hàng
 * Mỗi store có thể bán sản phẩm (isAvailable = true) hoặc không (isAvailable = false)
 * Admin có thể bật/tắt việc bán sản phẩm ở mỗi store
 */
@Entity
@Table(name = "store_inventory",
    uniqueConstraints = {
        @UniqueConstraint(name = "uniq_store_product", 
            columnNames = {"store_id", "product_id"})
    },
    indexes = {
        @Index(name = "idx_store_inventory_store_id", columnList = "store_id"),
        @Index(name = "idx_store_inventory_product_id", columnList = "product_id"),
        @Index(name = "idx_store_inventory_is_available", columnList = "is_available")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreInventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Integer storeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private StoreLocation store;

    @Column(name = "product_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private Product product;

    /**
     * isAvailable: true = store này có bán sản phẩm này, false = không bán
     * Admin có thể bật/tắt việc bán sản phẩm ở mỗi store
     */
    @Column(name = "is_available", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    @Builder.Default
    private Boolean isAvailable = true;

    /**
     * quantity: Số lượng tồn kho tại store này (optional, có thể để null nếu không theo dõi số lượng)
     */
    @Column(name = "quantity", columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer quantity = 0;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;
}

