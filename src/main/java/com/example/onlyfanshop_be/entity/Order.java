package com.example.onlyfanshop_be.entity;

import com.example.onlyfanshop_be.enums.OrderStatus;
import com.example.onlyfanshop_be.enums.PaymentMethod;
import com.example.onlyfanshop_be.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders",
    indexes = {
        @Index(name = "idx_orders_user_id", columnList = "user_id"),
        @Index(name = "idx_orders_status_created", columnList = "status, created_at")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @Column(name = "user_id", columnDefinition = "BIGINT UNSIGNED")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private User user;

    @Column(name = "address_id", columnDefinition = "BIGINT UNSIGNED")
    private Long addressId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private UserAddress address;

    @Column(name = "order_code", nullable = false, unique = true, length = 50)
    private String orderCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, 
        columnDefinition = "ENUM('pending','confirmed','processing','shipping','completed','canceled','refunded') DEFAULT 'pending'")
    @Builder.Default
    private OrderStatus status = OrderStatus.pending;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, 
        columnDefinition = "ENUM('cod','bank_transfer','e_wallet','online_gateway')")
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, 
        columnDefinition = "ENUM('unpaid','paid','failed','refunded') DEFAULT 'unpaid'")
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.unpaid;

    @Column(name = "shipping_method", length = 100)
    private String shippingMethod;

    @Column(name = "shipping_fee", nullable = false, columnDefinition = "DECIMAL(15,2) DEFAULT 0")
    @Builder.Default
    private BigDecimal shippingFee = BigDecimal.ZERO;

    @Column(name = "discount_total", nullable = false, columnDefinition = "DECIMAL(15,2) DEFAULT 0")
    @Builder.Default
    private BigDecimal discountTotal = BigDecimal.ZERO;

    @Column(name = "subtotal", nullable = false, columnDefinition = "DECIMAL(15,2)")
    private BigDecimal subtotal;

    @Column(name = "total_amount", nullable = false, columnDefinition = "DECIMAL(15,2)")
    private BigDecimal totalAmount;

    @Column(name = "notes", length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "confirmed_at", columnDefinition = "TIMESTAMP NULL")
    private LocalDateTime confirmedAt;

    @Column(name = "shipped_at", columnDefinition = "TIMESTAMP NULL")
    private LocalDateTime shippedAt;

    @Column(name = "completed_at", columnDefinition = "TIMESTAMP NULL")
    private LocalDateTime completedAt;

    @Column(name = "canceled_at", columnDefinition = "TIMESTAMP NULL")
    private LocalDateTime canceledAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<OrderItem> orderItems;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<Payment> payments;

    // Legacy fields for backward compatibility
    @Transient
    public Integer getOrderID() {
        return id != null ? id.intValue() : null;
    }

    @Transient
    public OrderStatus getOrderStatus() {
        return status;
    }

    @Transient
    public LocalDateTime getOrderDate() {
        return createdAt;
    }

    @Transient
    public Double getTotalPrice() {
        return totalAmount != null ? totalAmount.doubleValue() : null;
    }
}
