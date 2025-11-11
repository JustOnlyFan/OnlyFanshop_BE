package com.example.onlyfanshop_be.entity;

import com.example.onlyfanshop_be.enums.StockMovementType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_movements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "BIGINT UNSIGNED")
    private Long id;

    @Column(name = "warehouse_id", nullable = false, columnDefinition = "INT UNSIGNED")
    private Integer warehouseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private Warehouse warehouse;

    @Column(name = "product_id", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long productId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private Product product;

    @Column(name = "product_variant_id", columnDefinition = "BIGINT UNSIGNED")
    private Long productVariantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private ProductVariant productVariant;

    @Column(name = "order_id", columnDefinition = "BIGINT UNSIGNED")
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private Order order;

    @Column(name = "type", nullable = false, columnDefinition = "ENUM('import','export','adjustment','transfer')")
    private String type;
    
    // Convenience method to get/set as enum
    @Transient
    public StockMovementType getMovementTypeEnum() {
        return StockMovementType.fromDbValue(this.type);
    }
    
    public void setMovementTypeEnum(StockMovementType movementType) {
        this.type = movementType != null ? movementType.getDbValue() : null;
    }

    @Column(name = "from_warehouse_id", columnDefinition = "INT UNSIGNED")
    private Integer fromWarehouseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_warehouse_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private Warehouse fromWarehouse;

    @Column(name = "to_warehouse_id", columnDefinition = "INT UNSIGNED")
    private Integer toWarehouseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_warehouse_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private Warehouse toWarehouse;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "created_by", columnDefinition = "BIGINT UNSIGNED")
    private Long createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private User creator;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
}

