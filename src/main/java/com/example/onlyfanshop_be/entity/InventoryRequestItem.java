package com.example.onlyfanshop_be.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

/**
 * InventoryRequestItem - Chi tiết từng sản phẩm trong yêu cầu nhập hàng
 */
@Entity
@Table(name = "inventory_request_items",
    indexes = {
        @Index(name = "idx_inv_req_item_request_id", columnList = "request_id"),
        @Index(name = "idx_inv_req_item_product_id", columnList = "product_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryRequestItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @Column(name = "request_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long requestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", insertable = false, updatable = false)
    @JsonIgnore
    private InventoryRequest request;

    @Column(name = "product_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private Product product;

    /**
     * Số lượng yêu cầu
     */
    @Column(name = "requested_quantity", nullable = false)
    private Integer requestedQuantity;

    /**
     * Số lượng được duyệt (có thể khác với số lượng yêu cầu)
     */
    @Column(name = "approved_quantity")
    private Integer approvedQuantity;
}
