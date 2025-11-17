package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.StoreInventoryDTO;
import com.example.onlyfanshop_be.entity.Product;
import com.example.onlyfanshop_be.entity.StoreInventory;
import com.example.onlyfanshop_be.entity.StoreLocation;
import com.example.onlyfanshop_be.repository.ProductRepository;
import com.example.onlyfanshop_be.repository.StoreInventoryRepository;
import com.example.onlyfanshop_be.repository.StoreLocationRepository;
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

    /**
     * Khi tạo sản phẩm mới, tự động thêm vào tất cả các stores (isAvailable = true)
     */
    @Transactional
    public void addProductToAllStores(Long productId) {
        List<StoreLocation> allStores = storeLocationRepository.findAll();
        
        for (StoreLocation store : allStores) {
            // Kiểm tra xem đã tồn tại chưa
            if (storeInventoryRepository.findByStoreIdAndProductId(store.getLocationID(), productId).isEmpty()) {
                StoreInventory inventory = StoreInventory.builder()
                        .storeId(store.getLocationID())
                        .productId(productId)
                        .isAvailable(true) // Mặc định bật bán ở tất cả stores
                        .quantity(0)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
                storeInventoryRepository.save(inventory);
            }
        }
    }

    /**
     * Bật/tắt việc bán sản phẩm ở một store cụ thể
     */
    @Transactional
    public StoreInventory toggleProductAvailability(Integer storeId, Long productId, Boolean isAvailable) {
        StoreInventory inventory = storeInventoryRepository.findByStoreIdAndProductId(storeId, productId)
                .orElseThrow(() -> new RuntimeException("Store inventory not found"));
        
        inventory.setIsAvailable(isAvailable);
        inventory.setUpdatedAt(LocalDateTime.now());
        return storeInventoryRepository.save(inventory);
    }

    /**
     * Lấy danh sách stores có bán sản phẩm (isAvailable = true)
     */
    @Transactional(readOnly = true)
    public List<StoreLocation> getStoresWithProduct(Long productId) {
        List<StoreInventory> inventories = storeInventoryRepository.findAvailableStoresByProductId(productId);
        
        List<Integer> storeIds = inventories.stream()
                .map(StoreInventory::getStoreId)
                .collect(Collectors.toList());
        
        return storeLocationRepository.findAllById(storeIds);
    }

    /**
     * Lấy danh sách sản phẩm có sẵn tại store
     */
    @Transactional(readOnly = true)
    public List<StoreInventoryDTO> getStoreProducts(Integer storeId, boolean includeInactive) {
        List<StoreInventory> inventories = includeInactive
                ? storeInventoryRepository.findByStoreId(storeId)
                : storeInventoryRepository.findAvailableProductsByStoreId(storeId);
        
        return inventories.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Lấy thông tin store inventory theo storeId và productId
     */
    @Transactional(readOnly = true)
    public StoreInventoryDTO getStoreInventory(Integer storeId, Long productId) {
        StoreInventory inventory = storeInventoryRepository.findByStoreIdAndProductId(storeId, productId)
                .orElse(null);
        
        return inventory != null ? convertToDTO(inventory) : null;
    }

    /**
     * Convert StoreInventory entity to DTO
     */
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

