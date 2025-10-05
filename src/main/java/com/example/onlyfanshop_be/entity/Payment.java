package com.example.onlyfanshop_be.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer paymentID;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private LocalDateTime paymentDate;

    @Column(nullable = false)
    private Boolean paymentStatus; // true: thành công, false: thất bại

    @Column(nullable = false, length = 100, unique = true)
    private String transactionCode; // mã giao dịch VNPay, unique để tránh duplicate

    @ManyToOne
    @JoinColumn(name = "orderID", nullable = true)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Order order;
}
