package com.example.onlyfanshop_be.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing the many-to-many relationship between products and categories.
 * Allows products to be assigned to multiple categories from different category types.
 */
@Entity
@Table(name = "product_categories",
    indexes = {
        @Index(name = "idx_product_categories_product", columnList = "product_id"),
        @Index(name = "idx_product_categories_category", columnList = "category_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uniq_product_category", columnNames = {"product_id", "category_id"})
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "BIGINT UNSIGNED")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    @Column(name = "product_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    @JsonIgnore
    private Product product;

    @Column(name = "category_id", nullable = false, columnDefinition = "INT UNSIGNED")
    private Integer categoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "products", "children", "parent"})
    private Category category;

    /**
     * Indicates if this is the primary category for the product.
     * Each product should have exactly one primary category.
     */
    @Column(name = "is_primary", columnDefinition = "TINYINT(1) DEFAULT 0")
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
