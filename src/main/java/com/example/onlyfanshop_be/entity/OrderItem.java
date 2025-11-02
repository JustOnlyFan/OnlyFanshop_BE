package com.example.onlyfanshop_be.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "OrderItems")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer orderItemID;

    private Integer quantity;

    private Double price;

    @ManyToOne
    @JoinColumn(name = "orderID")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Order order;

    @ManyToOne
    @JoinColumn(name = "productID")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Product product;

}
