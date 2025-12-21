package com.example.onlyfanshop_be.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * InternalShipmentTracking - Lịch sử trạng thái vận chuyển nội bộ
 */
@Entity
@Table(name = "internal_shipment_tracking",
    indexes = {
        @Index(name = "idx_internal_shipment_tracking_shipment_id", columnList = "internal_shipment_id"),
        @Index(name = "idx_internal_shipment_tracking_timestamp", columnList = "timestamp")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternalShipmentTracking {
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

    @Column(name = "status", nullable = false, length = 50)
    private String status;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
