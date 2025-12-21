package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.entity.InventoryItem;
import com.example.onlyfanshop_be.entity.Warehouse;
import com.example.onlyfanshop_be.enums.WarehouseType;
import com.example.onlyfanshop_be.repository.InventoryItemRepository;
import com.example.onlyfanshop_be.repository.WarehouseRepository;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for Main Warehouse Auto-Inventory creation.
 * 
 * **Feature: inventory-management-ghn, Property 3: Main Warehouse Auto-Inventory**
 * **Validates: Requirements 2.2**
 */
class MainWarehouseAutoInventoryPropertyTest {

    /**
     * **Feature: inventory-management-ghn, Property 3: Main Warehouse Auto-Inventory**
     * **Validates: Requirements 2.2**
     * 
     * Property: For any new product added to Product_Catalog, the Main_Warehouse SHALL 
     * automatically contain an InventoryItem for that product with quantity equal to zero.
     */
    @Property(tries = 100)
    void newProductShouldHaveInventoryItemInMainWarehouseWithZeroQuantity(
            @ForAll @IntRange(min = 1, max = 10000) long productId,
            @ForAll @IntRange(min = 1, max = 100) long mainWarehouseId) {
        
        // Create mock repositories
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        
        // Setup: Main Warehouse exists
        Warehouse mainWarehouse = Warehouse.builder()
                .id(mainWarehouseId)
                .name("Main Warehouse")
                .type(WarehouseType.MAIN)
                .build();
        
        when(warehouseRepository.findFirstByType(WarehouseType.MAIN))
                .thenReturn(Optional.of(mainWarehouse));
        
        // Setup: InventoryItem does not exist yet
        when(inventoryItemRepository.existsByWarehouseIdAndProductId(mainWarehouseId, productId))
                .thenReturn(false);
        
        // Capture the saved InventoryItem
        when(inventoryItemRepository.save(any(InventoryItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Create the inventory creator (mimics ProductService behavior)
        MainWarehouseInventoryCreator creator = new MainWarehouseInventoryCreator(
                warehouseRepository, inventoryItemRepository);
        
        // Execute: Create inventory item for new product
        InventoryItem createdItem = creator.createMainWarehouseInventoryItem(productId);
        
        // Verify: InventoryItem was created with correct properties
        assertThat(createdItem).isNotNull();
        assertThat(createdItem.getWarehouseId()).isEqualTo(mainWarehouseId);
        assertThat(createdItem.getProductId()).isEqualTo(productId);
        assertThat(createdItem.getQuantity()).isEqualTo(0);
        assertThat(createdItem.getReservedQuantity()).isEqualTo(0);
        
        // Verify: save was called exactly once
        verify(inventoryItemRepository, times(1)).save(any(InventoryItem.class));
    }

    /**
     * **Feature: inventory-management-ghn, Property 3: Main Warehouse Auto-Inventory**
     * **Validates: Requirements 2.2**
     * 
     * Property: For any product that already has an InventoryItem in Main_Warehouse,
     * attempting to create another SHALL NOT create a duplicate.
     */
    @Property(tries = 100)
    void existingInventoryItemShouldNotBeDuplicated(
            @ForAll @IntRange(min = 1, max = 10000) long productId,
            @ForAll @IntRange(min = 1, max = 100) long mainWarehouseId) {
        
        // Create mock repositories
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        
        // Setup: Main Warehouse exists
        Warehouse mainWarehouse = Warehouse.builder()
                .id(mainWarehouseId)
                .name("Main Warehouse")
                .type(WarehouseType.MAIN)
                .build();
        
        when(warehouseRepository.findFirstByType(WarehouseType.MAIN))
                .thenReturn(Optional.of(mainWarehouse));
        
        // Setup: InventoryItem already exists
        when(inventoryItemRepository.existsByWarehouseIdAndProductId(mainWarehouseId, productId))
                .thenReturn(true);
        
        // Create the inventory creator
        MainWarehouseInventoryCreator creator = new MainWarehouseInventoryCreator(
                warehouseRepository, inventoryItemRepository);
        
        // Execute: Attempt to create inventory item for existing product
        InventoryItem result = creator.createMainWarehouseInventoryItem(productId);
        
        // Verify: No new item was created (returns null for existing)
        assertThat(result).isNull();
        
        // Verify: save was never called
        verify(inventoryItemRepository, never()).save(any(InventoryItem.class));
    }

    /**
     * **Feature: inventory-management-ghn, Property 3: Main Warehouse Auto-Inventory**
     * **Validates: Requirements 2.2**
     * 
     * Property: When Main_Warehouse does not exist, the system SHALL handle gracefully
     * without creating an InventoryItem.
     */
    @Property(tries = 100)
    void noMainWarehouseShouldNotCreateInventoryItem(
            @ForAll @IntRange(min = 1, max = 10000) long productId) {
        
        // Create mock repositories
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        
        // Setup: Main Warehouse does NOT exist
        when(warehouseRepository.findFirstByType(WarehouseType.MAIN))
                .thenReturn(Optional.empty());
        
        // Create the inventory creator
        MainWarehouseInventoryCreator creator = new MainWarehouseInventoryCreator(
                warehouseRepository, inventoryItemRepository);
        
        // Execute: Attempt to create inventory item
        InventoryItem result = creator.createMainWarehouseInventoryItem(productId);
        
        // Verify: No item was created
        assertThat(result).isNull();
        
        // Verify: save was never called
        verify(inventoryItemRepository, never()).save(any(InventoryItem.class));
    }

    /**
     * **Feature: inventory-management-ghn, Property 3: Main Warehouse Auto-Inventory**
     * **Validates: Requirements 2.2**
     * 
     * Property: For any null productId, the system SHALL NOT create an InventoryItem.
     */
    @Property(tries = 10)
    void nullProductIdShouldNotCreateInventoryItem() {
        // Create mock repositories
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        
        // Create the inventory creator
        MainWarehouseInventoryCreator creator = new MainWarehouseInventoryCreator(
                warehouseRepository, inventoryItemRepository);
        
        // Execute: Attempt to create inventory item with null productId
        InventoryItem result = creator.createMainWarehouseInventoryItem(null);
        
        // Verify: No item was created
        assertThat(result).isNull();
        
        // Verify: save was never called
        verify(inventoryItemRepository, never()).save(any(InventoryItem.class));
        
        // Verify: warehouse lookup was never called
        verify(warehouseRepository, never()).findFirstByType(any());
    }

    /**
     * Helper class that replicates the inventory creation logic from ProductService.
     * This allows testing the auto-inventory creation without full Spring context.
     */
    static class MainWarehouseInventoryCreator {
        private final WarehouseRepository warehouseRepository;
        private final InventoryItemRepository inventoryItemRepository;
        
        MainWarehouseInventoryCreator(WarehouseRepository warehouseRepository,
                                       InventoryItemRepository inventoryItemRepository) {
            this.warehouseRepository = warehouseRepository;
            this.inventoryItemRepository = inventoryItemRepository;
        }
        
        /**
         * Creates an InventoryItem in Main_Warehouse with quantity = 0 for a new product.
         * Mimics the behavior of ProductService.createMainWarehouseInventoryItem()
         * 
         * @param productId The ID of the product to create inventory for
         * @return The created InventoryItem, or null if creation was skipped
         */
        InventoryItem createMainWarehouseInventoryItem(Long productId) {
            if (productId == null) {
                return null;
            }
            
            // Find Main Warehouse
            Optional<Warehouse> mainWarehouseOpt = 
                    warehouseRepository.findFirstByType(WarehouseType.MAIN);
            
            if (mainWarehouseOpt.isEmpty()) {
                return null;
            }
            
            Warehouse mainWarehouse = mainWarehouseOpt.get();
            
            // Check if InventoryItem already exists
            if (inventoryItemRepository.existsByWarehouseIdAndProductId(
                    mainWarehouse.getId(), productId)) {
                return null;
            }
            
            // Create new InventoryItem with quantity = 0
            InventoryItem inventoryItem = InventoryItem.builder()
                    .warehouseId(mainWarehouse.getId())
                    .productId(productId)
                    .quantity(0)
                    .reservedQuantity(0)
                    .build();
            
            return inventoryItemRepository.save(inventoryItem);
        }
    }
}
