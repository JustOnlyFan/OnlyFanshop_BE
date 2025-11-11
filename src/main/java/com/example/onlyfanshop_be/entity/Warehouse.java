package com.example.onlyfanshop_be.entity;

import com.example.onlyfanshop_be.enums.WarehouseType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "warehouses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warehouse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", columnDefinition = "INT UNSIGNED")
    private Integer id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "type", nullable = false, columnDefinition = "ENUM('main','regional','branch')")
    @Convert(converter = com.example.onlyfanshop_be.converter.WarehouseTypeConverter.class)
    private WarehouseType type;

    @Column(name = "parent_warehouse_id", columnDefinition = "INT UNSIGNED")
    private Integer parentWarehouseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_warehouse_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private Warehouse parentWarehouse;

    @Column(name = "store_location_id", columnDefinition = "INT")
    private Integer storeLocationId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_location_id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JsonIgnore
    private StoreLocation storeLocation;

    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(name = "ward", length = 100)
    private String ward;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "country", length = 100, columnDefinition = "VARCHAR(100) DEFAULT 'Vietnam'")
    @Builder.Default
    private String country = "Vietnam";

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "is_active", nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "warehouse")
    @JsonIgnore
    private List<WarehouseInventory> inventories;

    @OneToMany(mappedBy = "warehouse")
    @JsonIgnore
    private List<StockMovement> stockMovements;

    @OneToMany(mappedBy = "parentWarehouse")
    @JsonIgnore
    private List<Warehouse> childWarehouses;
}


