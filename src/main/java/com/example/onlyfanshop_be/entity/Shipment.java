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

@Entity
@Table(name = "shipments",
    indexes = {@Index(name = "idx_shipments_order_id", columnList = "order_id")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Shipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @Column(name = "order_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private Order order;

    @Column(name = "carrier", length = 100)
    private String carrier;

    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "shipped_at", columnDefinition = "TIMESTAMP NULL")
    private LocalDateTime shippedAt;

    @Column(name = "delivered_at", columnDefinition = "TIMESTAMP NULL")
    private LocalDateTime deliveredAt;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
}

