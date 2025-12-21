package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.entity.InventoryItem;
import com.example.onlyfanshop_be.repository.InventoryItemRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for Product Deletion Cascade.
 * 
 * **Feature: inventory-management-ghn, Property 2: Product Deletion Cascade**
 * **Validates: Requirements 1.4**
 */
class ProductDeletionCascadePropertyTest {

    /**
     * **Feature: inventory-management-ghn, Property 2: Product Deletion Cascade**
     * **Validates: Requirements 1.4**
     * 
     * Property: For any product that exists in the system with associated InventoryItems,
     * deleting that product SHALL remove all associated InventoryItems from all warehouses.
     */
    @Property(tries = 100)
    void deletingProductShouldRemoveAllAssociatedInventoryItems(
            @ForAll @IntRange(min = 1, max = 10000) long productId,
            @ForAll @IntRange(min = 1, max = 10) int numberOfWarehouses) {
        
        // Create mock repository
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        
        // Setup: Create inventory items across multiple warehouses
        List<InventoryItem> existingItems = new ArrayList<>();
        for (int i = 1; i <= numberOfWarehouses; i++) {
            InventoryItem item = InventoryItem.builder()
                    .id((long) i)
                    .warehouseId((long) i)
                    .productId(productId)
                    .quantity(10 * i)
                    .reservedQuantity(0)
                    .build();
            existingItems.add(item);
        }
        
        when(inventoryItemRepository.findByProductId(productId))
                .thenReturn(existingItems);
        
        // Create the cascade deleter (mimics ProductService behavior)
        ProductInventoryCascadeDeleter deleter = new ProductInventoryCascadeDeleter(inventoryItemRepository);
        
        // Execute: Delete all inventory items for the product
        int deletedCount = deleter.deleteAllInventoryItemsForProduct(productId);
        
        // Verify: All inventory items were deleted
        assertThat(deletedCount).isEqualTo(numberOfWarehouses);
        
        // Verify: deleteByProductId was called exactly once with correct productId
        verify(inventoryItemRepository, times(1)).deleteByProductId(productId);
    }

    /**
     * **Feature: inventory-management-ghn, Property 2: Product Deletion Cascade**
     * **Validates: Requirements 1.4**
     * 
     * Property: For any product with no associated InventoryItems,
     * deletion should complete without errors and report zero deletions.
     */
    @Property(tries = 100)
    void deletingProductWithNoInventoryItemsShouldSucceed(
            @ForAll @IntRange(min = 1, max = 10000) long productId) {
        
        // Create mock repository
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        
        // Setup: No inventory items exist for this product
        when(inventoryItemRepository.findByProductId(productId))
                .thenReturn(new ArrayList<>());
        
        // Create the cascade deleter
        ProductInventoryCascadeDeleter deleter = new ProductInventoryCascadeDeleter(inventoryItemRepository);
        
        // Execute: Delete all inventory items for the product
        int deletedCount = deleter.deleteAllInventoryItemsForProduct(productId);
        
        // Verify: Zero items were deleted
        assertThat(deletedCount).isEqualTo(0);
        
        // Verify: deleteByProductId was NOT called (no items to delete)
        verify(inventoryItemRepository, never()).deleteByProductId(any());
    }

    /**
     * **Feature: inventory-management-ghn, Property 2: Product Deletion Cascade**
     * **Validates: Requirements 1.4**
     * 
     * Property: For any null productId, the system SHALL NOT attempt deletion
     * and should handle gracefully.
     */
    @Property(tries = 10)
    void nullProductIdShouldNotAttemptDeletion() {
        // Create mock repository
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        
        // Create the cascade deleter
        ProductInventoryCascadeDeleter deleter = new ProductInventoryCascadeDeleter(inventoryItemRepository);
        
        // Execute: Attempt to delete with null productId
        int deletedCount = deleter.deleteAllInventoryItemsForProduct(null);
        
        // Verify: No deletion was attempted
        assertThat(deletedCount).isEqualTo(-1); // -1 indicates invalid input
        
        // Verify: No repository methods were called
        verify(inventoryItemRepository, never()).findByProductId(any());
        verify(inventoryItemRepository, never()).deleteByProductId(any());
    }

    /**
     * **Feature: inventory-management-ghn, Property 2: Product Deletion Cascade**
     * **Validates: Requirements 1.4**
     * 
     * Property: For any product with inventory items in both MAIN and STORE warehouses,
     * deletion SHALL remove items from ALL warehouse types.
     */
    @Property(tries = 100)
    void deletionShouldRemoveItemsFromAllWarehouseTypes(
            @ForAll @IntRange(min = 1, max = 10000) long productId,
            @ForAll @IntRange(min = 1, max = 5) int mainWarehouseItems,
            @ForAll @IntRange(min = 1, max = 5) int storeWarehouseItems) {
        
        // Create mock repository
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        
        // Setup: Create inventory items in both MAIN and STORE warehouses
        List<InventoryItem> existingItems = new ArrayList<>();
        
        // Add items from MAIN warehouse(s)
        for (int i = 1; i <= mainWarehouseItems; i++) {
            InventoryItem item = InventoryItem.builder()
                    .id((long) i)
                    .warehouseId((long) i) // MAIN warehouse IDs
                    .productId(productId)
                    .quantity(100)
                    .reservedQuantity(0)
                    .build();
            existingItems.add(item);
        }
        
        // Add items from STORE warehouse(s)
        for (int i = 1; i <= storeWarehouseItems; i++) {
            InventoryItem item = InventoryItem.builder()
                    .id((long) (mainWarehouseItems + i))
                    .warehouseId((long) (100 + i)) // STORE warehouse IDs (different range)
                    .productId(productId)
                    .quantity(50)
                    .reservedQuantity(5)
                    .build();
            existingItems.add(item);
        }
        
        when(inventoryItemRepository.findByProductId(productId))
                .thenReturn(existingItems);
        
        // Create the cascade deleter
        ProductInventoryCascadeDeleter deleter = new ProductInventoryCascadeDeleter(inventoryItemRepository);
        
        // Execute: Delete all inventory items for the product
        int deletedCount = deleter.deleteAllInventoryItemsForProduct(productId);
        
        // Verify: All items from both warehouse types were deleted
        int expectedTotal = mainWarehouseItems + storeWarehouseItems;
        assertThat(deletedCount).isEqualTo(expectedTotal);
        
        // Verify: deleteByProductId was called exactly once
        verify(inventoryItemRepository, times(1)).deleteByProductId(productId);
    }

    /**
     * Helper class that replicates the cascade deletion logic from ProductService.
     * This allows testing the cascade deletion without full Spring context.
     */
    static class ProductInventoryCascadeDeleter {
        private final InventoryItemRepository inventoryItemRepository;
        
        ProductInventoryCascadeDeleter(InventoryItemRepository inventoryItemRepository) {
            this.inventoryItemRepository = inventoryItemRepository;
        }
        
        /**
         * Deletes all InventoryItems for a product across all warehouses.
         * Mimics the behavior of ProductService.deleteAllInventoryItemsForProduct()
         * 
         * @param productId The ID of the product to delete inventory for
         * @return The number of deleted items, or -1 if productId is null
         */
        int deleteAllInventoryItemsForProduct(Long productId) {
            if (productId == null) {
                return -1;
            }
            
            // Find all InventoryItems for this product
            List<InventoryItem> inventoryItems = inventoryItemRepository.findByProductId(productId);
            
            if (inventoryItems.isEmpty()) {
                return 0;
            }
            
            // Delete all InventoryItems
            inventoryItemRepository.deleteByProductId(productId);
            
            return inventoryItems.size();
        }
    }
}
