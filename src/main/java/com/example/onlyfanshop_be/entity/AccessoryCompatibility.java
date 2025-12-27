package com.example.onlyfanshop_be.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    @Column(name = "accessory_product_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long accessoryProductId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "accessory_product_id", insertable = false, updatable = false)
    @JsonIgnore
    private Product accessoryProduct;

    @Column(name = "compatible_fan_type_id", columnDefinition = "INT UNSIGNED")
    private Integer compatibleFanTypeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compatible_fan_type_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "products", "children", "parent"})
    private Category compatibleFanType;

    @Column(name = "compatible_brand_id", columnDefinition = "INT UNSIGNED")
    private Integer compatibleBrandId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compatible_brand_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "products"})
    private Brand compatibleBrand;

    @Column(name = "compatible_model", length = 200)
    private String compatibleModel;

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

    public boolean isCompatibleWithFanType(Integer fanTypeId) {
        return compatibleFanTypeId != null && compatibleFanTypeId.equals(fanTypeId);
    }

    public boolean isCompatibleWithBrand(Integer brandId) {
        return compatibleBrandId != null && compatibleBrandId.equals(brandId);
    }
}
