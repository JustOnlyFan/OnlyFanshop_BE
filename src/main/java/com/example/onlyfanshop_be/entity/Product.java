package com.example.onlyfanshop_be.entity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_product_name", columnList = "ProductName"),
    @Index(name = "idx_product_category", columnList = "CategoryID"),
    @Index(name = "idx_product_brand", columnList = "BrandID")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ProductID")
    private Integer productId;

    @NotBlank(message = "Product name is required")
    @Size(max = 100, message = "Product name must not exceed 100 characters")
    @Column(name = "ProductName", nullable = false, length = 100)
    private String productName;

    @Size(max = 255, message = "Brief description must not exceed 255 characters")
    @Column(name = "BriefDescription", length = 255)
    private String briefDescription;

    @Column(name = "FullDescription", columnDefinition = "TEXT")
    private String fullDescription;

    @Column(name = "TechnicalSpecifications", columnDefinition = "TEXT")
    private String technicalSpecifications;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Digits(integer = 16, fraction = 2, message = "Price must have at most 16 integer digits and 2 decimal places")
    @Column(name = "Price", nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @Size(max = 255, message = "Image URL must not exceed 255 characters")
    @Column(name = "ImageURL", length = 255)
    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CategoryID")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "BrandID")
    private Brand brand;

    // Relationships
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties("product")
    private List<Cart> carts;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL)
    @JsonIgnoreProperties("product")
    private List<OrderDetail> orderDetails;
}

