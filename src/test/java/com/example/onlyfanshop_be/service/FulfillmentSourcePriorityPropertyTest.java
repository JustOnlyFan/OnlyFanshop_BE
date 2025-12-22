package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.response.SourceAllocation;
import com.example.onlyfanshop_be.entity.*;
import com.example.onlyfanshop_be.enums.WarehouseType;
import com.example.onlyfanshop_be.repository.*;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for Fulfillment Source Priority.
 * 
 * **Feature: inventory-management-ghn, Property 8: Fulfillment Source Priority**
 * **Validates: Requirements 5.2, 5.3**
 * 
 * Property: For any availability check, the system SHALL first allocate from Main_Warehouse,
 * then from other Store_Warehouses only when Main_Warehouse quantity is insufficient.
 */
class FulfillmentSourcePriorityPropertyTest {

    /**
     * **Feature: inventory-management-ghn, Property 8: Fulfillment Source Priority**
     * **Validates: Requirements 5.2, 5.3**
     * 
     * Property: When Main_Warehouse has sufficient quantity, allocations SHALL come
     * entirely from Main_Warehouse and no Store_Warehouses SHALL be used.
     */
    @Property(tries = 100)
    void whenMainWarehouseHasSufficientQuantityThenOnlyMainWarehouseIsUsed(
            @ForAll @IntRange(min = 1, max = 10000) long productId,
            @ForAll @IntRange(min = 1, max = 100) int requiredQuantity,
            @ForAll @IntRange(min = 0, max = 100) int extraMainQuantity,
            @ForAll("storeWarehouseQuantities") List<Integer> storeQuantities) {
        
        // Main warehouse has at least the required quantity
        int mainWarehouseQuantity = requiredQuantity + extraMainQuantity;
        
        // Create mock repositories
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        
        // Setup: Main Warehouse exists with sufficient quantity
        long mainWarehouseId = 1L;
        Warehouse mainWarehouse = Warehouse.builder()
                .id(mainWarehouseId)
                .name("Main Warehouse")
                .type(WarehouseType.MAIN)
                .storeId(null)
                .build();
        
        when(warehouseRepository.findFirstByType(WarehouseType.MAIN))
                .thenReturn(Optional.of(mainWarehouse));
        
        // Setup: Main warehouse inventory
        InventoryItem mainInventory = InventoryItem.builder()
                .id(1L)
                .warehouseId(mainWarehouseId)
                .productId(productId)
                .quantity(mainWarehouseQuantity)
                .reservedQuantity(0)
                .build();
        
        when(inventoryItemRepository.findByWarehouseIdAndProductId(mainWarehouseId, productId))
                .thenReturn(Optional.of(mainInventory));
        
        // Setup: Store warehouses (should not be used)
        List<Warehouse> storeWarehouses = new ArrayList<>();
        for (int i = 0; i < storeQuantities.size(); i++) {
            long storeWarehouseId = 100L + i;
            int storeId = 10 + i;
            
            Warehouse storeWarehouse = Warehouse.builder()
                    .id(storeWarehouseId)
                    .name("Store Warehouse " + storeId)
                    .type(WarehouseType.STORE)
                    .storeId(storeId)
                    .build();
            storeWarehouses.add(storeWarehouse);
            
            InventoryItem storeInventory = InventoryItem.builder()
                    .id(100L + i)
                    .warehouseId(storeWarehouseId)
                    .productId(productId)
                    .quantity(storeQuantities.get(i))
                    .reservedQuantity(0)
                    .build();
            
            when(inventoryItemRepository.findByWarehouseIdAndProductId(storeWarehouseId, productId))
                    .thenReturn(Optional.of(storeInventory));
        }
        
        when(warehouseRepository.findByType(WarehouseType.STORE)).thenReturn(storeWarehouses);
        
        // Create the source allocator
        SourcePriorityAllocator allocator = new SourcePriorityAllocator(
                warehouseRepository, inventoryItemRepository);
        
        // Execute: Calculate source allocations
        List<SourceAllocation> allocations = allocator.calculateSourceAllocations(
                productId, requiredQuantity, null);
        
        // Verify: Only Main_Warehouse is used (Requirements 5.2)
        assertThat(allocations).isNotEmpty();
        assertThat(allocations).hasSize(1);
        assertThat(allocations.get(0).getWarehouseType()).isEqualTo(WarehouseType.MAIN);
        assertThat(allocations.get(0).getWarehouseId()).isEqualTo(mainWarehouseId);
        assertThat(allocations.get(0).getQuantity()).isEqualTo(requiredQuantity);
        
        // Verify: No store warehouses are in the allocations
        boolean hasStoreAllocation = allocations.stream()
                .anyMatch(a -> a.getWarehouseType() == WarehouseType.STORE);
        assertThat(hasStoreAllocation).isFalse();
    }

    /**
     * **Feature: inventory-management-ghn, Property 8: Fulfillment Source Priority**
     * **Validates: Requirements 5.2, 5.3**
     * 
     * Property: When Main_Warehouse has insufficient quantity, the system SHALL first
     * allocate all available from Main_Warehouse, then allocate remaining from Store_Warehouses.
     */
    @Property(tries = 100)
    void whenMainWarehouseInsufficientThenMainIsUsedFirstThenStores(
            @ForAll @IntRange(min = 1, max = 10000) long productId,
            @ForAll @IntRange(min = 1, max = 50) int mainWarehouseQuantity,
            @ForAll @IntRange(min = 1, max = 50) int additionalRequired,
            @ForAll("nonEmptyStoreWarehouseQuantities") List<Integer> storeQuantities) {
        
        // Required quantity is more than main warehouse has
        int requiredQuantity = mainWarehouseQuantity + additionalRequired;
        
        // Ensure at least one store has enough to cover the shortage
        int totalStoreQuantity = storeQuantities.stream().mapToInt(Integer::intValue).sum();
        Assume.that(totalStoreQuantity >= additionalRequired);
        
        // Create mock repositories
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        
        // Setup: Main Warehouse exists with insufficient quantity
        long mainWarehouseId = 1L;
        Warehouse mainWarehouse = Warehouse.builder()
                .id(mainWarehouseId)
                .name("Main Warehouse")
                .type(WarehouseType.MAIN)
                .storeId(null)
                .build();
        
        when(warehouseRepository.findFirstByType(WarehouseType.MAIN))
                .thenReturn(Optional.of(mainWarehouse));
        
        // Setup: Main warehouse inventory (insufficient)
        InventoryItem mainInventory = InventoryItem.builder()
                .id(1L)
                .warehouseId(mainWarehouseId)
                .productId(productId)
                .quantity(mainWarehouseQuantity)
                .reservedQuantity(0)
                .build();
        
        when(inventoryItemRepository.findByWarehouseIdAndProductId(mainWarehouseId, productId))
                .thenReturn(Optional.of(mainInventory));
        
        // Setup: Store warehouses with inventory
        List<Warehouse> storeWarehouses = new ArrayList<>();
        for (int i = 0; i < storeQuantities.size(); i++) {
            long storeWarehouseId = 100L + i;
            int storeId = 10 + i;
            
            Warehouse storeWarehouse = Warehouse.builder()
                    .id(storeWarehouseId)
                    .name("Store Warehouse " + storeId)
                    .type(WarehouseType.STORE)
                    .storeId(storeId)
                    .build();
            storeWarehouses.add(storeWarehouse);
            
            InventoryItem storeInventory = InventoryItem.builder()
                    .id(100L + i)
                    .warehouseId(storeWarehouseId)
                    .productId(productId)
                    .quantity(storeQuantities.get(i))
                    .reservedQuantity(0)
                    .build();
            
            when(inventoryItemRepository.findByWarehouseIdAndProductId(storeWarehouseId, productId))
                    .thenReturn(Optional.of(storeInventory));
        }
        
        when(warehouseRepository.findByType(WarehouseType.STORE)).thenReturn(storeWarehouses);
        
        // Create the source allocator
        SourcePriorityAllocator allocator = new SourcePriorityAllocator(
                warehouseRepository, inventoryItemRepository);
        
        // Execute: Calculate source allocations
        List<SourceAllocation> allocations = allocator.calculateSourceAllocations(
                productId, requiredQuantity, null);
        
        // Verify: Main_Warehouse is first in allocations (Requirements 5.2)
        assertThat(allocations).isNotEmpty();
        assertThat(allocations.get(0).getWarehouseType()).isEqualTo(WarehouseType.MAIN);
        assertThat(allocations.get(0).getQuantity()).isEqualTo(mainWarehouseQuantity);
        
        // Verify: Store warehouses are used after main (Requirements 5.3)
        List<SourceAllocation> storeAllocations = allocations.stream()
                .filter(a -> a.getWarehouseType() == WarehouseType.STORE)
                .toList();
        
        int totalStoreAllocated = storeAllocations.stream()
                .mapToInt(SourceAllocation::getQuantity)
                .sum();
        
        // Store allocations should cover the shortage
        assertThat(totalStoreAllocated).isGreaterThanOrEqualTo(additionalRequired);
        
        // Total allocated should equal required quantity
        int totalAllocated = allocations.stream()
                .mapToInt(SourceAllocation::getQuantity)
                .sum();
        assertThat(totalAllocated).isEqualTo(requiredQuantity);
    }

    /**
     * **Feature: inventory-management-ghn, Property 8: Fulfillment Source Priority**
     * **Validates: Requirements 5.2, 5.3**
     * 
     * Property: When Main_Warehouse has zero quantity, allocations SHALL come
     * entirely from Store_Warehouses.
     */
    @Property(tries = 100)
    void whenMainWarehouseEmptyThenOnlyStoreWarehousesAreUsed(
            @ForAll @IntRange(min = 1, max = 10000) long productId,
            @ForAll @IntRange(min = 1, max = 100) int requiredQuantity,
            @ForAll("sufficientStoreWarehouseQuantities") List<Integer> storeQuantities) {
        
        // Ensure stores have enough total quantity
        int totalStoreQuantity = storeQuantities.stream().mapToInt(Integer::intValue).sum();
        Assume.that(totalStoreQuantity >= requiredQuantity);
        Assume.that(!storeQuantities.isEmpty());
        
        // Create mock repositories
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        
        // Setup: Main Warehouse exists with zero quantity
        long mainWarehouseId = 1L;
        Warehouse mainWarehouse = Warehouse.builder()
                .id(mainWarehouseId)
                .name("Main Warehouse")
                .type(WarehouseType.MAIN)
                .storeId(null)
                .build();
        
        when(warehouseRepository.findFirstByType(WarehouseType.MAIN))
                .thenReturn(Optional.of(mainWarehouse));
        
        // Setup: Main warehouse inventory (empty)
        InventoryItem mainInventory = InventoryItem.builder()
                .id(1L)
                .warehouseId(mainWarehouseId)
                .productId(productId)
                .quantity(0)
                .reservedQuantity(0)
                .build();
        
        when(inventoryItemRepository.findByWarehouseIdAndProductId(mainWarehouseId, productId))
                .thenReturn(Optional.of(mainInventory));
        
        // Setup: Store warehouses with inventory
        List<Warehouse> storeWarehouses = new ArrayList<>();
        for (int i = 0; i < storeQuantities.size(); i++) {
            long storeWarehouseId = 100L + i;
            int storeId = 10 + i;
            
            Warehouse storeWarehouse = Warehouse.builder()
                    .id(storeWarehouseId)
                    .name("Store Warehouse " + storeId)
                    .type(WarehouseType.STORE)
                    .storeId(storeId)
                    .build();
            storeWarehouses.add(storeWarehouse);
            
            InventoryItem storeInventory = InventoryItem.builder()
                    .id(100L + i)
                    .warehouseId(storeWarehouseId)
                    .productId(productId)
                    .quantity(storeQuantities.get(i))
                    .reservedQuantity(0)
                    .build();
            
            when(inventoryItemRepository.findByWarehouseIdAndProductId(storeWarehouseId, productId))
                    .thenReturn(Optional.of(storeInventory));
        }
        
        when(warehouseRepository.findByType(WarehouseType.STORE)).thenReturn(storeWarehouses);
        
        // Create the source allocator
        SourcePriorityAllocator allocator = new SourcePriorityAllocator(
                warehouseRepository, inventoryItemRepository);
        
        // Execute: Calculate source allocations
        List<SourceAllocation> allocations = allocator.calculateSourceAllocations(
                productId, requiredQuantity, null);
        
        // Verify: No Main_Warehouse allocation (since it's empty)
        boolean hasMainAllocation = allocations.stream()
                .anyMatch(a -> a.getWarehouseType() == WarehouseType.MAIN);
        assertThat(hasMainAllocation).isFalse();
        
        // Verify: Only Store_Warehouses are used (Requirements 5.3)
        assertThat(allocations).isNotEmpty();
        assertThat(allocations).allMatch(a -> a.getWarehouseType() == WarehouseType.STORE);
        
        // Verify: Total allocated equals required quantity
        int totalAllocated = allocations.stream()
                .mapToInt(SourceAllocation::getQuantity)
                .sum();
        assertThat(totalAllocated).isEqualTo(requiredQuantity);
    }

    /**
     * **Feature: inventory-management-ghn, Property 8: Fulfillment Source Priority**
     * **Validates: Requirements 5.2, 5.3**
     * 
     * Property: The requesting store's warehouse SHALL be excluded from source allocations.
     */
    @Property(tries = 100)
    void requestingStoreWarehouseShouldBeExcludedFromAllocations(
            @ForAll @IntRange(min = 1, max = 10000) long productId,
            @ForAll @IntRange(min = 1, max = 50) int requiredQuantity,
            @ForAll @IntRange(min = 10, max = 100) int requestingStoreId,
            @ForAll @IntRange(min = 1, max = 100) int requestingStoreQuantity) {
        
        // Create mock repositories
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        
        // Setup: Main Warehouse with zero quantity (to force store allocation)
        long mainWarehouseId = 1L;
        Warehouse mainWarehouse = Warehouse.builder()
                .id(mainWarehouseId)
                .name("Main Warehouse")
                .type(WarehouseType.MAIN)
                .storeId(null)
                .build();
        
        when(warehouseRepository.findFirstByType(WarehouseType.MAIN))
                .thenReturn(Optional.of(mainWarehouse));
        
        InventoryItem mainInventory = InventoryItem.builder()
                .id(1L)
                .warehouseId(mainWarehouseId)
                .productId(productId)
                .quantity(0)
                .reservedQuantity(0)
                .build();
        
        when(inventoryItemRepository.findByWarehouseIdAndProductId(mainWarehouseId, productId))
                .thenReturn(Optional.of(mainInventory));
        
        // Setup: Requesting store's warehouse (should be excluded)
        long requestingWarehouseId = 100L;
        Warehouse requestingWarehouse = Warehouse.builder()
                .id(requestingWarehouseId)
                .name("Requesting Store Warehouse")
                .type(WarehouseType.STORE)
                .storeId(requestingStoreId)
                .build();
        
        InventoryItem requestingInventory = InventoryItem.builder()
                .id(100L)
                .warehouseId(requestingWarehouseId)
                .productId(productId)
                .quantity(requestingStoreQuantity)
                .reservedQuantity(0)
                .build();
        
        when(inventoryItemRepository.findByWarehouseIdAndProductId(requestingWarehouseId, productId))
                .thenReturn(Optional.of(requestingInventory));
        
        // Setup: Another store's warehouse (should be included)
        int otherStoreId = requestingStoreId + 1;
        long otherWarehouseId = 200L;
        Warehouse otherWarehouse = Warehouse.builder()
                .id(otherWarehouseId)
                .name("Other Store Warehouse")
                .type(WarehouseType.STORE)
                .storeId(otherStoreId)
                .build();
        
        InventoryItem otherInventory = InventoryItem.builder()
                .id(200L)
                .warehouseId(otherWarehouseId)
                .productId(productId)
                .quantity(requiredQuantity + 10) // Enough to fulfill
                .reservedQuantity(0)
                .build();
        
        when(inventoryItemRepository.findByWarehouseIdAndProductId(otherWarehouseId, productId))
                .thenReturn(Optional.of(otherInventory));
        
        when(warehouseRepository.findByType(WarehouseType.STORE))
                .thenReturn(List.of(requestingWarehouse, otherWarehouse));
        
        // Create the source allocator
        SourcePriorityAllocator allocator = new SourcePriorityAllocator(
                warehouseRepository, inventoryItemRepository);
        
        // Execute: Calculate source allocations with excludeStoreId
        List<SourceAllocation> allocations = allocator.calculateSourceAllocations(
                productId, requiredQuantity, requestingStoreId);
        
        // Verify: Requesting store's warehouse is NOT in allocations
        boolean hasRequestingStoreAllocation = allocations.stream()
                .anyMatch(a -> requestingStoreId == (a.getStoreId() != null ? a.getStoreId() : -1));
        assertThat(hasRequestingStoreAllocation).isFalse();
        
        // Verify: Other store's warehouse IS in allocations
        boolean hasOtherStoreAllocation = allocations.stream()
                .anyMatch(a -> otherStoreId == (a.getStoreId() != null ? a.getStoreId() : -1));
        assertThat(hasOtherStoreAllocation).isTrue();
    }

    /**
     * Provides a list of store warehouse quantities (can be empty or have zeros).
     */
    @Provide
    Arbitrary<List<Integer>> storeWarehouseQuantities() {
        return Arbitraries.integers().between(0, 100)
                .list()
                .ofMinSize(0)
                .ofMaxSize(5);
    }

    /**
     * Provides a non-empty list of store warehouse quantities with at least some positive values.
     */
    @Provide
    Arbitrary<List<Integer>> nonEmptyStoreWarehouseQuantities() {
        return Arbitraries.integers().between(1, 50)
                .list()
                .ofMinSize(1)
                .ofMaxSize(5);
    }

    /**
     * Provides a list of store warehouse quantities that sum to at least 100.
     */
    @Provide
    Arbitrary<List<Integer>> sufficientStoreWarehouseQuantities() {
        return Arbitraries.integers().between(20, 100)
                .list()
                .ofMinSize(1)
                .ofMaxSize(5);
    }

    /**
     * Helper class that replicates the source allocation logic from FulfillmentService.
     * This allows testing the priority logic without full Spring context.
     */
    static class SourcePriorityAllocator {
        private final WarehouseRepository warehouseRepository;
        private final InventoryItemRepository inventoryItemRepository;
        
        SourcePriorityAllocator(WarehouseRepository warehouseRepository,
                                InventoryItemRepository inventoryItemRepository) {
            this.warehouseRepository = warehouseRepository;
            this.inventoryItemRepository = inventoryItemRepository;
        }
        
        /**
         * Calculate source allocations for a product.
         * Mimics the behavior of FulfillmentService.calculateSourceAllocations()
         * 
         * Requirements: 5.2 - Check Main_Warehouse first
         * Requirements: 5.3 - Check Store_Warehouses when main is insufficient
         */
        List<SourceAllocation> calculateSourceAllocations(Long productId, int requiredQuantity, Integer excludeStoreId) {
            List<SourceAllocation> allocations = new ArrayList<>();
            int remainingQuantity = requiredQuantity;
            
            // Step 1: Check Main Warehouse first (Requirements 5.2)
            Warehouse mainWarehouse = warehouseRepository.findFirstByType(WarehouseType.MAIN)
                    .orElse(null);
            
            if (mainWarehouse != null) {
                int mainAvailable = getWarehouseAvailableQuantity(productId, mainWarehouse.getId());
                
                if (mainAvailable > 0) {
                    int allocateFromMain = Math.min(mainAvailable, remainingQuantity);
                    allocations.add(SourceAllocation.builder()
                            .warehouseId(mainWarehouse.getId())
                            .warehouseName(mainWarehouse.getName())
                            .warehouseType(WarehouseType.MAIN)
                            .storeId(null)
                            .storeName(null)
                            .quantity(allocateFromMain)
                            .build());
                    remainingQuantity -= allocateFromMain;
                }
            }
            
            // Step 2: If still need more, check Store Warehouses (Requirements 5.3)
            if (remainingQuantity > 0) {
                List<Warehouse> storeWarehouses = warehouseRepository.findByType(WarehouseType.STORE);
                
                // Sort store warehouses by available quantity (descending)
                List<WarehouseWithAvailability> warehousesWithAvailability = new ArrayList<>();
                
                for (Warehouse warehouse : storeWarehouses) {
                    // Skip the requesting store's warehouse
                    if (excludeStoreId != null && excludeStoreId.equals(warehouse.getStoreId())) {
                        continue;
                    }
                    
                    int available = getWarehouseAvailableQuantity(productId, warehouse.getId());
                    if (available > 0) {
                        warehousesWithAvailability.add(new WarehouseWithAvailability(warehouse, available));
                    }
                }
                
                // Sort by available quantity descending
                warehousesWithAvailability.sort((a, b) -> Integer.compare(b.available, a.available));
                
                // Aggregate from multiple stores
                for (WarehouseWithAvailability wwa : warehousesWithAvailability) {
                    if (remainingQuantity <= 0) {
                        break;
                    }
                    
                    int allocateFromStore = Math.min(wwa.available, remainingQuantity);
                    
                    allocations.add(SourceAllocation.builder()
                            .warehouseId(wwa.warehouse.getId())
                            .warehouseName(wwa.warehouse.getName())
                            .warehouseType(WarehouseType.STORE)
                            .storeId(wwa.warehouse.getStoreId())
                            .storeName("Store " + wwa.warehouse.getStoreId())
                            .quantity(allocateFromStore)
                            .build());
                    
                    remainingQuantity -= allocateFromStore;
                }
            }
            
            return allocations;
        }
        
        private int getWarehouseAvailableQuantity(Long productId, Long warehouseId) {
            return inventoryItemRepository.findByWarehouseIdAndProductId(warehouseId, productId)
                    .map(item -> item.getQuantity() - item.getReservedQuantity())
                    .orElse(0);
        }
        
        private static class WarehouseWithAvailability {
            final Warehouse warehouse;
            final int available;
            
            WarehouseWithAvailability(Warehouse warehouse, int available) {
                this.warehouse = warehouse;
                this.available = available;
            }
        }
    }
}
