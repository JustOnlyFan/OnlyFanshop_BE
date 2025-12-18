package com.example.onlyfanshop_be.entity;

import com.example.onlyfanshop_be.enums.InventoryRequestStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * InventoryRequest - Yêu cầu nhập hàng từ cửa hàng đến kho tổng
 * Flow: Cửa hàng tạo request → Admin duyệt → Chuyển hàng từ kho tổng sang kho cửa hàng
 * Một request có thể chứa nhiều sản phẩm (items)
 */
@Entity
@Table(name = "inventory_requests",
    indexes = {
        @Index(name = "idx_inv_request_store_id", columnList = "store_id"),
        @Index(name = "idx_inv_request_status", columnList = "status"),
        @Index(name = "idx_inv_request_created_at", columnList = "created_at")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @Column(name = "store_id", nullable = false, columnDefinition = "INT UNSIGNED")
    private Integer storeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private StoreLocation store;

    /**
     * Danh sách sản phẩm trong yêu cầu
     */
    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<InventoryRequestItem> items = new ArrayList<>();

    // Legacy fields - giữ lại để tương thích ngược với dữ liệu cũ
    @Column(name = "product_id", columnDefinition = "BIGINT UNSIGNED")
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private Product product;

    @Column(name = "requested_quantity")
    private Integer requestedQuantity;

    @Column(name = "approved_quantity")
    private Integer approvedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private InventoryRequestStatus status = InventoryRequestStatus.PENDING;

    /**
     * User tạo yêu cầu (nhân viên cửa hàng)
     */
    @Column(name = "requested_by", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private User requester;

    /**
     * Admin duyệt yêu cầu
     */
    @Column(name = "approved_by", columnDefinition = "BIGINT UNSIGNED")
    private Long approvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private User approver;

    /**
     * Ghi chú từ cửa hàng khi tạo yêu cầu
     */
    @Column(name = "request_note", length = 500)
    private String requestNote;

    /**
     * Ghi chú từ admin khi duyệt/từ chối
     */
    @Column(name = "admin_note", length = 500)
    private String adminNote;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
