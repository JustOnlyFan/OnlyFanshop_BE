package com.example.onlyfanshop_be.entity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
@Entity
@Table(name = "Payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer paymentID;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private LocalDateTime paymentDate;

    @Column(nullable = false, length = 50)
    private String paymentStatus;

    @ManyToOne
    @JoinColumn(name = "orderID")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Order order;
}
