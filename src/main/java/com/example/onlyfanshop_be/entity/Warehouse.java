package com.example.onlyfanshop_be.entity;

import com.example.onlyfanshop_be.enums.WarehouseType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Warehouse - Kho hàng trong hệ thống
 * Chỉ hỗ trợ STORE: Kho của từng cửa hàng
 * Kho tổng (MAIN) đã được loại bỏ
 */
@Entity
@Table(name = "warehouses",
    indexes = {
        @Index(name = "idx_warehouse_type", columnList = "type"),
        @Index(name = "idx_warehouse_store_id", columnList = "store_id"),
        @Index(name = "idx_warehouse_is_active", columnList = "is_active")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warehouse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private WarehouseType type;

    @Column(name = "store_id", columnDefinition = "INT UNSIGNED")
    private Integer storeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private StoreLocation store;

    /**
     * Trạng thái hoạt động của kho
     * true: kho đang hoạt động
     * false: kho đã bị vô hiệu hóa (soft delete)
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
