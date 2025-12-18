package com.example.onlyfanshop_be.entity;

import com.example.onlyfanshop_be.enums.ShipmentStatus;
import com.example.onlyfanshop_be.enums.ShipmentType;
import com.example.onlyfanshop_be.enums.ShippingCarrier;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "shipments",
    indexes = {
        @Index(name = "idx_shipment_order_id", columnList = "order_id"),
        @Index(name = "idx_shipment_inventory_request_id", columnList = "inventory_request_id"),
        @Index(name = "idx_shipment_tracking_number", columnList = "tracking_number"),
        @Index(name = "idx_shipment_status", columnList = "status"),
        @Index(name = "idx_shipment_carrier", columnList = "carrier")
    })
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

    @Enumerated(EnumType.STRING)
    @Column(name = "shipment_type", nullable = false, length = 30)
    private ShipmentType shipmentType;

    // Liên kết với Order (nếu giao cho khách)
    @Column(name = "order_id", columnDefinition = "BIGINT UNSIGNED")
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private Order order;

    // Liên kết với InventoryRequest (nếu chuyển kho nội bộ)
    @Column(name = "inventory_request_id", columnDefinition = "BIGINT UNSIGNED")
    private Long inventoryRequestId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inventory_request_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private InventoryRequest inventoryRequest;

    // Đơn vị vận chuyển
    @Enumerated(EnumType.STRING)
    @Column(name = "carrier", nullable = false, length = 30)
    private ShippingCarrier carrier;

    // Mã vận đơn từ đơn vị vận chuyển
    @Column(name = "tracking_number", length = 100)
    private String trackingNumber;

    // Mã đơn hàng GHN (order_code từ GHN API)
    @Column(name = "carrier_order_code", length = 100)
    private String carrierOrderCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private ShipmentStatus status = ShipmentStatus.PENDING;

    // Thông tin người gửi
    @Column(name = "from_name", length = 100)
    private String fromName;

    @Column(name = "from_phone", length = 20)
    private String fromPhone;

    @Column(name = "from_address", length = 500)
    private String fromAddress;

    @Column(name = "from_ward_code", length = 20)
    private String fromWardCode;

    @Column(name = "from_district_id")
    private Integer fromDistrictId;

    // Thông tin người nhận
    @Column(name = "to_name", nullable = false, length = 100)
    private String toName;

    @Column(name = "to_phone", nullable = false, length = 20)
    private String toPhone;

    @Column(name = "to_address", nullable = false, length = 500)
    private String toAddress;

    @Column(name = "to_ward_code", length = 20)
    private String toWardCode;

    @Column(name = "to_district_id")
    private Integer toDistrictId;

    // Thông tin hàng hóa
    @Column(name = "weight")
    private Integer weight; // gram

    @Column(name = "length")
    private Integer length; // cm

    @Column(name = "width")
    private Integer width; // cm

    @Column(name = "height")
    private Integer height; // cm

    // Giá trị & phí
    @Column(name = "cod_amount", columnDefinition = "DECIMAL(15,2) DEFAULT 0")
    @Builder.Default
    private BigDecimal codAmount = BigDecimal.ZERO; // Tiền thu hộ

    @Column(name = "insurance_value", columnDefinition = "DECIMAL(15,2) DEFAULT 0")
    @Builder.Default
    private BigDecimal insuranceValue = BigDecimal.ZERO; // Giá trị bảo hiểm

    @Column(name = "shipping_fee", columnDefinition = "DECIMAL(15,2) DEFAULT 0")
    @Builder.Default
    private BigDecimal shippingFee = BigDecimal.ZERO;

    // Dịch vụ GHN
    @Column(name = "service_type_id")
    private Integer serviceTypeId; // 1: Express, 2: Standard, 3: Saving

    @Column(name = "payment_type_id")
    private Integer paymentTypeId; // 1: Shop trả phí, 2: Khách trả phí

    // Ghi chú
    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "required_note", length = 50)
    private String requiredNote; // CHOTHUHANG, CHOXEMHANGKHONGTHU, KHONGCHOXEMHANG

    // Thời gian
    @Column(name = "expected_delivery_time")
    private LocalDateTime expectedDeliveryTime;

    @Column(name = "picked_at")
    private LocalDateTime pickedAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Store xuất hàng (nếu có)
    @Column(name = "from_store_id", columnDefinition = "INT UNSIGNED")
    private Integer fromStoreId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_store_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private StoreLocation fromStore;

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
