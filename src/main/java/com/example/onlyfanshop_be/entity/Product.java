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
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    // Thông số kỹ thuật chính
    @Column(name = "voltage", length = 50)
    private String voltage; // Điện áp sử dụng: "220V / 50Hz"

    @Column(name = "wind_speed_levels", length = 100)
    private String windSpeedLevels; // Tốc độ gió: "3 mức (thấp/trung bình/cao)" hoặc "Điều chỉnh vô cấp"

    @Column(name = "airflow")
    private Integer airflow; // Lưu lượng gió: m³/phút

    @Column(name = "blade_material", length = 100)
    private String bladeMaterial; // Chất liệu cánh quạt: "Nhựa ABS" / "Kim loại"

    @Column(name = "body_material", length = 100)
    private String bodyMaterial; // Chất liệu thân quạt: "Nhựa cao cấp" / "Thép sơn tĩnh điện"

    @Column(name = "blade_count")
    private Integer bladeCount; // Số lượng cánh: 3 / 5

    @Column(name = "noise_level")
    private Integer noiseLevel; // Mức độ ồn: dB

    @Column(name = "motor_speed")
    private Integer motorSpeed; // Tốc độ quay motor: vòng/phút

    @Column(name = "weight", columnDefinition = "DECIMAL(5,2)")
    private BigDecimal weight; // Trọng lượng: kg

    @Column(name = "adjustable_height", length = 50)
    private String adjustableHeight; // Chiều cao điều chỉnh: "1.1 – 1.4 m"

    // Tính năng & tiện ích
    @Column(name = "remote_control", columnDefinition = "TINYINT(1) DEFAULT 0")
    @Builder.Default
    private Boolean remoteControl = false; // Điều khiển từ xa

    @Column(name = "timer", length = 50)
    private String timer; // Hẹn giờ tắt: "1 – 4 giờ" hoặc null

    @Column(name = "natural_wind_mode", columnDefinition = "TINYINT(1) DEFAULT 0")
    @Builder.Default
    private Boolean naturalWindMode = false; // Chế độ gió tự nhiên

    @Column(name = "sleep_mode", columnDefinition = "TINYINT(1) DEFAULT 0")
    @Builder.Default
    private Boolean sleepMode = false; // Chế độ ngủ

    @Column(name = "oscillation", columnDefinition = "TINYINT(1) DEFAULT 0")
    @Builder.Default
    private Boolean oscillation = false; // Đảo chiều gió

    @Column(name = "height_adjustable", columnDefinition = "TINYINT(1) DEFAULT 0")
    @Builder.Default
    private Boolean heightAdjustable = false; // Điều chỉnh độ cao

    @Column(name = "auto_shutoff", columnDefinition = "TINYINT(1) DEFAULT 0")
    @Builder.Default
    private Boolean autoShutoff = false; // Ngắt điện tự động khi quá tải

    @Column(name = "temperature_sensor", columnDefinition = "TINYINT(1) DEFAULT 0")
    @Builder.Default
    private Boolean temperatureSensor = false; // Cảm biến nhiệt

    @Column(name = "energy_saving", columnDefinition = "TINYINT(1) DEFAULT 0")
    @Builder.Default
    private Boolean energySaving = false; // Tiết kiệm điện

    // Thông tin khác
    @Column(name = "safety_standards", length = 200)
    private String safetyStandards; // Tiêu chuẩn an toàn: "TCVN / IEC / RoHS"

    @Column(name = "manufacturing_year")
    private Integer manufacturingYear; // Năm sản xuất: 2025

    @Column(name = "accessories", columnDefinition = "TEXT")
    private String accessories; // Phụ kiện đi kèm: "Điều khiển / Pin / HDSD"

    @Column(name = "energy_rating", length = 50)
    private String energyRating; // Mức tiết kiệm điện năng: "5 sao"

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

    @Column(name = "quantity", nullable = false, columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer quantity = 0;

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
    private List<ProductImage> images;

    @OneToMany(mappedBy = "product")
    @JsonIgnore
    private List<CartItem> cartItems;

    @OneToMany(mappedBy = "product")
    @JsonIgnore
    private List<OrderItem> orderItems;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "product_colors",
        joinColumns = @JoinColumn(name = "product_id"),
        inverseJoinColumns = @JoinColumn(name = "color_id")
    )
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private List<Color> colors;
}
