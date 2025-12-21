package com.example.onlyfanshop_be.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

/**
 * InternalShipmentItem - Chi tiết sản phẩm trong đơn vận chuyển nội bộ
 */
@Entity
@Table(name = "internal_shipment_items",
    indexes = {
        @Index(name = "idx_internal_shipment_item_shipment_id", columnList = "internal_shipment_id"),
        @Index(name = "idx_internal_shipment_item_product_id", columnList = "product_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternalShipmentItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @Column(name = "internal_shipment_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long internalShipmentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "internal_shipment_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private InternalShipment internalShipment;

    @Column(name = "product_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private Product product;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;
}
