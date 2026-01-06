package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.StoreInventoryDTO;
import com.example.onlyfanshop_be.entity.InventoryItem;
import com.example.onlyfanshop_be.entity.Product;
import com.example.onlyfanshop_be.entity.StoreInventory;
import com.example.onlyfanshop_be.entity.StoreLocation;
import com.example.onlyfanshop_be.entity.Warehouse;
import com.example.onlyfanshop_be.enums.ProductStatus;
import com.example.onlyfanshop_be.repository.InventoryItemRepository;
import com.example.onlyfanshop_be.repository.ProductRepository;
import com.example.onlyfanshop_be.repository.StoreInventoryRepository;
import com.example.onlyfanshop_be.repository.StoreLocationRepository;
import com.example.onlyfanshop_be.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreInventoryService {
    private final StoreInventoryRepository storeInventoryRepository;
    private final StoreLocationRepository storeLocationRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryItemRepository inventoryItemRepository;

    @Transactional
    public StoreInventory toggleProductAvailability(Integer storeId, Long productId, Boolean isAvailable) {
        StoreInventory inventory = storeInventoryRepository.findByStoreIdAndProductId(storeId, productId)
                .orElseThrow(() -> new RuntimeException("Store inventory not found"));
        
        inventory.setIsAvailable(isAvailable);
        inventory.setUpdatedAt(LocalDateTime.now());
        return storeInventoryRepository.save(inventory);
    }

    @Transactional(readOnly = true)
    public List<StoreLocation> getStoresWithProduct(Long productId) {
        // Lấy danh sách cửa hàng có sản phẩm available
        List<StoreInventory> inventories = storeInventoryRepository.findAvailableStoresByProductId(productId);
        
        // Lọc chỉ các cửa hàng có số lượng > 0 trong InventoryItem
        List<Integer> storeIdsWithStock = inventories.stream()
                .filter(inventory -> {
                    // Tìm warehouse tương ứng với store
                    Warehouse warehouse = warehouseRepository.findByStoreIdAndIsActiveTrue(inventory.getStoreId())
                            .orElse(null);
                    
                    if (warehouse == null) {
                        return false;
                    }
                    
                    // Kiểm tra số lượng trong InventoryItem
                    InventoryItem inventoryItem = inventoryItemRepository
                            .findByWarehouseIdAndProductId(warehouse.getId(), productId)
                            .orElse(null);
                    
                    // Chỉ trả về nếu có số lượng > 0
                    return inventoryItem != null && inventoryItem.getQuantity() != null && inventoryItem.getQuantity() > 0;
                })
                .map(StoreInventory::getStoreId)
                .collect(Collectors.toList());
        
        return storeLocationRepository.findAllById(storeIdsWithStock);
    }

    @Transactional(readOnly = true)
    public List<StoreInventoryDTO> getStoreProducts(Integer storeId, boolean includeInactive) {
        List<StoreInventory> inventories = includeInactive
                ? storeInventoryRepository.findByStoreId(storeId)
                : storeInventoryRepository.findAvailableProductsByStoreId(storeId);
        
        return inventories.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public StoreInventoryDTO getStoreInventory(Integer storeId, Long productId) {
        StoreInventory inventory = storeInventoryRepository.findByStoreIdAndProductId(storeId, productId)
                .orElse(null);
        
        return inventory != null ? convertToDTO(inventory) : null;
    }

    @Transactional
    public List<StoreInventoryDTO> addProductsToStore(Integer storeId, List<Long> productIds) {
        storeLocationRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Cửa hàng không tồn tại"));
        
        List<StoreInventoryDTO> results = new java.util.ArrayList<>();
        
        for (Long productId : productIds) {
            if (!productRepository.existsById(productId.intValue())) {
                continue;
            }

            if (storeInventoryRepository.findByStoreIdAndProductId(storeId, productId).isPresent()) {
                continue;
            }
            
            StoreInventory inventory = StoreInventory.builder()
                    .storeId(storeId)
                    .productId(productId)
                    .variantId(null)
                    .isAvailable(true)
                    .quantity(0)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            storeInventoryRepository.save(inventory);
            results.add(convertToDTO(inventory));
        }
        
        return results;
    }

    @Transactional(readOnly = true)
    public List<Product> getProductsNotInStore(Integer storeId) {
        List<StoreInventory> existingInventories = storeInventoryRepository.findByStoreId(storeId);
        List<Long> existingProductIds = existingInventories.stream()
                .map(StoreInventory::getProductId)
                .collect(Collectors.toList());
        
        List<Product> allProducts = productRepository.findAll();
        
        if (existingProductIds.isEmpty()) {
            return allProducts;
        }
        
        return allProducts.stream()
                .filter(p -> !existingProductIds.contains(p.getProductID().longValue()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StoreInventoryDTO> getAllProductsWithStoreStatus(Integer storeId) {
        // Only get active products - "Tổng" should only count active products (same for all stores)
        List<Product> allProducts = productRepository.findAll().stream()
                .filter(product -> product.getStatus() == ProductStatus.active)
                .collect(Collectors.toList());
        List<StoreInventory> existingInventories = storeInventoryRepository.findByStoreId(storeId);

        java.util.Map<Long, StoreInventory> inventoryMap = existingInventories.stream()
                .collect(Collectors.toMap(StoreInventory::getProductId, inv -> inv));
        
        return allProducts.stream().map((Product product) -> {
            StoreInventory inventory = inventoryMap.get(product.getProductID().longValue());
            
            String productImageUrl = null;
            if (product.getImages() != null && !product.getImages().isEmpty()) {
                productImageUrl = product.getImages().stream()
                    .filter(img -> img.getIsMain() != null && img.getIsMain())
                    .map(img -> img.getImageUrl())
                    .findFirst()
                    .orElse(product.getImages().get(0).getImageUrl());
            }
            
            StoreInventoryDTO dto = StoreInventoryDTO.builder()
                    .id(inventory != null ? inventory.getId() : null)
                    .storeId(storeId)
                    .productId(product.getProductID().longValue())
                    .productName(product.getName())
                    .productImageUrl(productImageUrl)
                    .productPrice(product.getPrice())
                    .isAvailable(inventory != null && inventory.getIsAvailable())
                    .quantity(inventory != null ? inventory.getQuantity() : 0)
                    .createdAt(inventory != null ? inventory.getCreatedAt() : null)
                    .updatedAt(inventory != null ? inventory.getUpdatedAt() : null)
                    .build();
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void updateStoreProducts(Integer storeId, List<Long> enabledProductIds) {
        storeLocationRepository.findById(storeId)
                .orElseThrow(() -> new RuntimeException("Cửa hàng không tồn tại"));
        
        List<Product> allProducts = productRepository.findAll();
        
        for (Product product : allProducts) {
            Long productId = product.getProductID().longValue();
            boolean shouldBeEnabled = enabledProductIds.contains(productId);
            
            var existingOpt = storeInventoryRepository.findByStoreIdAndProductId(storeId, productId);
            
            if (existingOpt.isPresent()) {
                // Update existing
                StoreInventory inventory = existingOpt.get();
                if (inventory.getIsAvailable() != shouldBeEnabled) {
                    inventory.setIsAvailable(shouldBeEnabled);
                    inventory.setUpdatedAt(LocalDateTime.now());
                    storeInventoryRepository.save(inventory);
                }
            } else if (shouldBeEnabled) {
                // Create new if should be enabled
                StoreInventory inventory = StoreInventory.builder()
                        .storeId(storeId)
                        .productId(productId)
                        .variantId(null)
                        .isAvailable(true)
                        .quantity(0)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                storeInventoryRepository.save(inventory);
            }
        }
    }

    private StoreInventoryDTO convertToDTO(StoreInventory inventory) {
        try {
            StoreLocation store = storeLocationRepository.findById(inventory.getStoreId()).orElse(null);
            Product product = productRepository.findById(inventory.getProductId().intValue()).orElse(null);
            
            String productImageUrl = null;
            if (product != null && product.getImages() != null && !product.getImages().isEmpty()) {
                productImageUrl = product.getImages().stream()
                    .filter(img -> img.getIsMain() != null && img.getIsMain())
                    .map(img -> img.getImageUrl())
                    .findFirst()
                    .orElse(product.getImages().get(0).getImageUrl());
            }
            
            return StoreInventoryDTO.builder()
                    .id(inventory.getId())
                    .storeId(inventory.getStoreId())
                    .storeName(store != null ? store.getName() : null)
                    .storeAddress(store != null ? store.getAddress() : null)
                    .productId(inventory.getProductId())
                    .productName(product != null ? product.getName() : null)
                    .productImageUrl(productImageUrl)
                    .isAvailable(inventory.getIsAvailable())
                    .quantity(inventory.getQuantity())
                    .createdAt(inventory.getCreatedAt())
                    .updatedAt(inventory.getUpdatedAt())
                    .build();
        } catch (Exception e) {
            // Fallback
            return StoreInventoryDTO.builder()
                    .id(inventory.getId())
                    .storeId(inventory.getStoreId())
                    .productId(inventory.getProductId())
                    .isAvailable(inventory.getIsAvailable())
                    .quantity(inventory.getQuantity())
                    .createdAt(inventory.getCreatedAt())
                    .updatedAt(inventory.getUpdatedAt())
                    .build();
        }
    }
}

