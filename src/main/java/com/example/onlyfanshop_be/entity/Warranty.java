package com.example.onlyfanshop_be.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "warranties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warranty {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "INT UNSIGNED")
    private Integer id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "duration_months", nullable = false)
    private Integer durationMonths; // Thời gian bảo hành (tháng)

    @Column(name = "price", columnDefinition = "DECIMAL(15,2) DEFAULT 0")
    @Builder.Default
    private java.math.BigDecimal price = java.math.BigDecimal.ZERO; // Giá gói bảo hành

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "terms_and_conditions", columnDefinition = "TEXT")
    private String termsAndConditions; // Điều khoản và điều kiện bảo hành

    @Column(name = "coverage", columnDefinition = "TEXT")
    private String coverage; // Phạm vi bảo hành

    @OneToMany(mappedBy = "warranty")
    @JsonIgnore
    private List<Product> products;
}
