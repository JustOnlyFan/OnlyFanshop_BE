package com.example.onlyfanshop_be.entity;

import com.example.onlyfanshop_be.enums.ProductStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "products",
    indexes = {
        @Index(name = "idx_products_category_id", columnList = "category_id"),
        @Index(name = "idx_products_brand_id", columnList = "brand_id")
    })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "BIGINT UNSIGNED")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 250)
    private String slug;

    @Column(name = "brand_id", columnDefinition = "INT UNSIGNED")
    private Integer brandId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Brand brand;

    @Column(name = "category_id", columnDefinition = "INT UNSIGNED")
    private Integer categoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Category category;

    @Column(name = "sku", unique = true, length = 100)
    private String sku;

    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "power_watt")
    private Integer powerWatt;

    @Column(name = "blade_diameter_cm", columnDefinition = "DECIMAL(5,2)")
    private BigDecimal bladeDiameterCm;

    @Column(name = "color_default", length = 50)
    private String colorDefault; // Legacy field, keep for backward compatibility

    @Column(name = "warranty_id", columnDefinition = "INT UNSIGNED")
    private Integer warrantyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warranty_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Warranty warranty;

    @Column(name = "warranty_months")
    private Integer warrantyMonths; // Legacy field, keep for backward compatibility

    @Column(name = "base_price", nullable = false, columnDefinition = "DECIMAL(15,2)")
    private BigDecimal basePrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, columnDefinition = "ENUM('active','inactive','out_of_stock') DEFAULT 'active'")
    @Builder.Default
    private ProductStatus status = ProductStatus.active;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Column(name = "updated_at", columnDefinition = "TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    // Legacy fields for backward compatibility
    @Transient
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public Integer getProductID() {
        return id != null ? id.intValue() : null;
    }

    @Transient
    public String getProductName() {
        return name;
    }

    @Transient
    public String getBriefDescription() {
        return shortDescription;
    }

    @Transient
    public String getFullDescription() {
        return description;
    }

    @Transient
    public Double getPrice() {
        return basePrice != null ? basePrice.doubleValue() : null;
    }

    @Transient
    public String getImageURL() {
        // Return main image if available
        if (images != null && !images.isEmpty()) {
            return images.stream()
                    .filter(ProductImage::getIsMain)
                    .map(ProductImage::getImageUrl)
                    .findFirst()
                    .orElse(images.get(0).getImageUrl());
        }
        return null;
    }

    @Transient
    public boolean isActive() {
        return status == ProductStatus.active;
    }

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ProductVariant> variants;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<ProductImage> images;

    @OneToMany(mappedBy = "product")
    @JsonIgnore
    private List<CartItem> cartItems;

    @OneToMany(mappedBy = "product")
    @JsonIgnore
    private List<OrderItem> orderItems;

    @OneToMany(mappedBy = "product")
    @JsonIgnore
    private List<ProductReview> reviews;

    @OneToMany(mappedBy = "product")
    @JsonIgnore
    private List<ProductQuestion> questions;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "product_colors",
        joinColumns = @JoinColumn(name = "product_id"),
        inverseJoinColumns = @JoinColumn(name = "color_id")
    )
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private List<Color> colors;
}
