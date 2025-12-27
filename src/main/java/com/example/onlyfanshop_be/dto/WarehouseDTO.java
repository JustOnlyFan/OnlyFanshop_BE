package com.example.onlyfanshop_be.dto;

import com.example.onlyfanshop_be.enums.WarehouseType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseDTO {
    private Long id;
    private String name;
    private WarehouseType type;
    private Integer storeId;
    private String storeName;
    private Boolean isActive;
    private String address;
    private String phone;
    private LocalDateTime createdAt;
    private List<InventoryItemDTO> inventoryItems;
}
