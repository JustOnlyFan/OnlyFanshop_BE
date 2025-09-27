package com.example.onlyfanshop_be.entity;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "Carts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer cartID;

    @Column(nullable = false)
    private Double totalPrice;

    @Column(nullable = false, length = 50)
    private String status;

    @ManyToOne
    @JoinColumn(name = "userID")
    private User user;

    @OneToMany(mappedBy = "cart")
    private List<CartItem> cartItems;

    @OneToMany(mappedBy = "cart")
    private List<Order> orders;
}

