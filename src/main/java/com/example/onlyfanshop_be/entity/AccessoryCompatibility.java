package com.example.onlyfanshop_be.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity representing the compatibility information between accessory products and fan types/brands/models.
 * Allows customers to find the right accessories for their fans.
 */
@Entity
@Table(name = "accessory_compatibility",
    indexes = {
        @Index(name = "idx_accessory_compat_product", columnList = "accessory_product_id"),
        @Index(name = "idx_accessory_compat_fan_type", columnList = "compatible_fan_type_id"),
        @Index(name = "idx_accessory_compat_brand", columnList = "compatible_brand_id")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccessoryCompatibility {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "BIGINT UNSIGNED")
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    /**
     * The accessory product that this compatibility entry belongs to.
     */
    @Column(name = "accessory_product_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long accessoryProductId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accessory_product_id", insertable = false, updatable = false)
    @JsonIgnore
    private Product accessoryProduct;

    /**
     * The fan type category that this accessory is compatible with.
     * References a category with type FAN_TYPE.
     */
    @Column(name = "compatible_fan_type_id", columnDefinition = "INT UNSIGNED")
    private Integer compatibleFanTypeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compatible_fan_type_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "products", "children", "parent"})
    private Category compatibleFanType;

    /**
     * The brand that this accessory is compatible with.
     */
    @Column(name = "compatible_brand_id", columnDefinition = "INT UNSIGNED")
    private Integer compatibleBrandId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compatible_brand_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "products"})
    private Brand compatibleBrand;

    /**
     * Specific model names that this accessory is compatible with.
     * Can contain multiple models separated by commas or as a descriptive text.
     */
    @Column(name = "compatible_model", length = 200)
    private String compatibleModel;

    /**
     * Additional notes about compatibility (e.g., installation requirements, limitations).
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    /**
     * Checks if this compatibility entry matches a specific fan type.
     * @param fanTypeId the fan type category ID to check
     * @return true if compatible with the specified fan type
     */
    public boolean isCompatibleWithFanType(Integer fanTypeId) {
        return compatibleFanTypeId != null && compatibleFanTypeId.equals(fanTypeId);
    }

    /**
     * Checks if this compatibility entry matches a specific brand.
     * @param brandId the brand ID to check
     * @return true if compatible with the specified brand
     */
    public boolean isCompatibleWithBrand(Integer brandId) {
        return compatibleBrandId != null && compatibleBrandId.equals(brandId);
    }
}
