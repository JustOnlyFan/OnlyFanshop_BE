package com.example.onlyfanshop_be.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "CartItems")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer cartItemID;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Double price;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean isChecked = false;

    @ManyToOne
    @JoinColumn(name = "cartID")
    @JsonIgnore
    private Cart cart;

    @ManyToOne
    @JoinColumn(name = "productID")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Product product;
}
