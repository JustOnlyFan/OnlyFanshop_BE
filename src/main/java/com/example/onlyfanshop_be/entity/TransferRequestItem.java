package com.example.onlyfanshop_be.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

/**
 * TransferRequestItem - Chi tiết sản phẩm trong yêu cầu chuyển kho
 */
@Entity
@Table(name = "transfer_request_items",
    indexes = {
        @Index(name = "idx_transfer_request_item_request_id", columnList = "transfer_request_id"),
        @Index(name = "idx_transfer_request_item_product_id", columnList = "product_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequestItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @Column(name = "transfer_request_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long transferRequestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_request_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private TransferRequest transferRequest;

    @Column(name = "product_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private Product product;

    @Column(name = "requested_quantity", nullable = false)
    private Integer requestedQuantity;

    @Column(name = "fulfilled_quantity", nullable = false, columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer fulfilledQuantity = 0;

    /**
     * Số lượng còn thiếu
     */
    @Transient
    public Integer getShortageQuantity() {
        return requestedQuantity - fulfilledQuantity;
    }

    /**
     * Kiểm tra đã đáp ứng đủ chưa
     */
    @Transient
    public boolean isFullyFulfilled() {
        return fulfilledQuantity >= requestedQuantity;
    }
}
