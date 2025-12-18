package com.example.onlyfanshop_be.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing the many-to-many relationship between products and tags.
 * Supports validity periods for time-limited tags (e.g., seasonal promotions).
 */
@Entity
@Table(name = "product_tags",
    indexes = {
        @Index(name = "idx_product_tags_product", columnList = "product_id"),
        @Index(name = "idx_product_tags_tag", columnList = "tag_id")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uniq_product_tag", columnNames = {"product_id", "tag_id"})
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductTag {
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

    @Column(name = "tag_id", nullable = false, columnDefinition = "INT UNSIGNED")
    private Integer tagId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "productTags"})
    private Tag tag;

    /**
     * Start date/time when this tag becomes active for the product.
     * If null, the tag is active immediately.
     */
    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    /**
     * End date/time when this tag expires for the product.
     * If null, the tag never expires.
     */
    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /**
     * Checks if this product tag is currently active based on validity period.
     * @return true if the tag is currently active
     */
    @Transient
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        boolean afterStart = validFrom == null || !now.isBefore(validFrom);
        boolean beforeEnd = validUntil == null || !now.isAfter(validUntil);
        return afterStart && beforeEnd;
    }

    /**
     * Checks if this product tag is active at a specific point in time.
     * @param dateTime the date/time to check
     * @return true if the tag is active at the specified time
     */
    public boolean isActiveAt(LocalDateTime dateTime) {
        if (dateTime == null) {
            return isActive();
        }
        boolean afterStart = validFrom == null || !dateTime.isBefore(validFrom);
        boolean beforeEnd = validUntil == null || !dateTime.isAfter(validUntil);
        return afterStart && beforeEnd;
    }
}
