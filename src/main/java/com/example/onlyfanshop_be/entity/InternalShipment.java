package com.example.onlyfanshop_be.entity;

import com.example.onlyfanshop_be.enums.InternalShipmentStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * InternalShipment - Đơn vận chuyển nội bộ giữa các kho qua GHN
 */
@Entity
@Table(name = "internal_shipments",
    indexes = {
        @Index(name = "idx_internal_shipment_transfer_request_id", columnList = "transfer_request_id"),
        @Index(name = "idx_internal_shipment_source_warehouse_id", columnList = "source_warehouse_id"),
        @Index(name = "idx_internal_shipment_dest_warehouse_id", columnList = "destination_warehouse_id"),
        @Index(name = "idx_internal_shipment_ghn_order_code", columnList = "ghn_order_code"),
        @Index(name = "idx_internal_shipment_status", columnList = "status")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternalShipment {
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

    @Column(name = "source_warehouse_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long sourceWarehouseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_warehouse_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private Warehouse sourceWarehouse;

    @Column(name = "destination_warehouse_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long destinationWarehouseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_warehouse_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private Warehouse destinationWarehouse;

    @Column(name = "ghn_order_code", length = 100)
    private String ghnOrderCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private InternalShipmentStatus status = InternalShipmentStatus.CREATED;

    @Column(name = "total_fee")
    private Integer totalFee;

    @Column(name = "expected_delivery_time", length = 100)
    private String expectedDeliveryTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @OneToMany(mappedBy = "internalShipment", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<InternalShipmentItem> items;

    @OneToMany(mappedBy = "internalShipment", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<InternalShipmentTracking> trackingHistory;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
