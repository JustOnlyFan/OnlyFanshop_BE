package com.example.onlyfanshop_be.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "Products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Integer productID;

    @Column(nullable = false, length = 100)
    private String productName;

    private String briefDescription;
    @Lob
    @Column(columnDefinition = "TEXT")
    private String fullDescription;

    private String technicalSpecifications;

    @Column(nullable = false)
    private Double price;

    private String imageURL;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private boolean isActive = true;

    @ManyToOne
    @JoinColumn(name = "categoryID")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Category category;

    @ManyToOne
    @JoinColumn(name = "brandID")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Brand brand;

    @OneToMany(mappedBy = "product")
    @JsonIgnore
    private List<CartItem> cartItems;

    @OneToMany(mappedBy = "product")
    @JsonIgnore
    private List<OrderItem> orderItems;
}
