package com.example.onlyfanshop_be.entity;

import com.example.onlyfanshop_be.enums.DebtOrderStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DebtOrder - Đơn nợ được tạo khi không đủ hàng để đáp ứng request
 */
@Entity
@Table(name = "debt_orders",
    indexes = {
        @Index(name = "idx_debt_order_transfer_request_id", columnList = "transfer_request_id"),
        @Index(name = "idx_debt_order_status", columnList = "status"),
        @Index(name = "idx_debt_order_created_at", columnList = "created_at")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DebtOrder {
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

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private DebtOrderStatus status = DebtOrderStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "fulfilled_at")
    private LocalDateTime fulfilledAt;

    @OneToMany(mappedBy = "debtOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<DebtItem> items;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
