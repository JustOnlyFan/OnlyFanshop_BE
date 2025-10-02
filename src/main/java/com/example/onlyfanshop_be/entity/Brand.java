package com.example.onlyfanshop_be.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "Brands")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Brand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer brandID;

    @Column(nullable = false, length = 100)
    private String brandName;

    private String country;
    private String description;

    @OneToMany(mappedBy = "brand")
    @JsonIgnore
    private List<Product> products;
}

