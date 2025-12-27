package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.InventoryItemDTO;
import com.example.onlyfanshop_be.dto.WarehouseDTO;

import java.util.List;

public interface IWarehouseService {

    WarehouseDTO getStoreWarehouse(Integer storeId);

    List<WarehouseDTO> getAllActiveWarehouses();

    InventoryItemDTO updateStoreWarehouseQuantity(Integer storeId, Long productId, Integer quantity, String reason);

    InventoryItemDTO addProductToStoreWarehouse(Integer storeId, Long productId, Integer quantity);

    void deactivateWarehouse(Long warehouseId);
}
