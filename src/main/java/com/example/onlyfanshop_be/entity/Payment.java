package com.example.onlyfanshop_be.entity;

import com.example.onlyfanshop_be.enums.PaymentMethod;
import com.example.onlyfanshop_be.enums.PaymentTransactionStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments",
    indexes = {@Index(name = "idx_payments_order_id", columnList = "order_id")}
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
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

    @Column(name = "amount", nullable = false, columnDefinition = "DECIMAL(15,2)")
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, 
        columnDefinition = "ENUM('cod','bank_transfer','e_wallet','online_gateway')")
    private PaymentMethod method;

    @Column(name = "provider_txn_id", length = 150)
    private String providerTxnId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, 
        columnDefinition = "ENUM('pending','success','failed','refunded') DEFAULT 'pending'")
    @Builder.Default
    private PaymentTransactionStatus status = PaymentTransactionStatus.pending;

    @Column(name = "raw_response", columnDefinition = "JSON")
    private String rawResponse; // Store as JSON string, can be parsed when needed

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    // Legacy fields for backward compatibility
    @Transient
    public Integer getPaymentID() {
        return id != null ? id.intValue() : null;
    }

    @Transient
    public Double getAmountDouble() {
        return amount != null ? amount.doubleValue() : null;
    }

    @Transient
    public LocalDateTime getPaymentDate() {
        return createdAt;
    }

    @Transient
    public Boolean getPaymentStatus() {
        return status == PaymentTransactionStatus.success;
    }

    @Transient
    public String getTransactionCode() {
        return providerTxnId;
    }
}
