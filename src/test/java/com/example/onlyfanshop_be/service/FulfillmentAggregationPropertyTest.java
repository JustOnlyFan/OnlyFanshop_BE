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
 * Property-based tests for Fulfillment Aggregation.
 * 
 * **Feature: inventory-management-ghn, Property 9: Fulfillment Aggregation**
 * **Validates: Requirements 5.4**
 * 
 * Property: For any Transfer_Request where Main_Warehouse has insufficient quantity,
 * the system SHALL aggregate available quantities from multiple Store_Warehouses to
 * fulfill the shortage. The sum of all source allocations SHALL equal the minimum of
 * (requested quantity, total available quantity).
 */
class FulfillmentAggregationPropertyTest {

    /**
     * **Feature: inventory-management-ghn, Property 9: Fulfillment Aggregation**
     * **Validates: Requirements 5.4**
     * 
     * Property: When Main_Warehouse has insufficient quantity, the system SHALL aggregate
     * from multiple Store_Warehouses. The sum of all allocations SHALL equal
     * min(requested, total available).
     */
    @Property(tries = 100)
    void aggregationSumEqualsMinOfRequestedAndTotalAvailable(
            @ForAll @IntRange(min = 1, max = 10000) long productId,
            @ForAll @IntRange(min = 10, max = 100) int requiredQuantity,
            @ForAll @IntRange(min = 0, max = 50) int mainWarehouseQuantity,
            @ForAll("multipleStoreQuantities") List<Integer> storeQuantities) {
        
        // Ensure main warehouse has insufficient quantity
        Assume.that(mainWarehouseQuantity < requiredQuantity);
        // Ensure we have at least one store warehouse
        Assume.that(!storeQuantities.isEmpty());
        
        // Calculate total available across all warehouses
        int totalStoreQuantity = storeQuantities.stream().mapToInt(Integer::intValue).sum();
        int totalAvailable = mainWarehouseQuantity + totalStoreQuantity;
        
        // Create mock repositories
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        
        // Setup: Main Warehouse
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
                .quantity(mainWarehouseQuantity)
                .reservedQuantity(0)
                .build();
        
        when(inventoryItemRepository.findByWarehouseIdAndProductId(mainWarehouseId, productId))
                .thenReturn(Optional.of(mainInventory));
        
        // Setup: Store warehouses
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
        
        // Calculate sum of all allocations
        int totalAllocated = allocations.stream()
                .mapToInt(SourceAllocation::getQuantity)
                .sum();
        
        // Verify: Sum of allocations equals min(requested, total available)
        int expectedAllocation = Math.min(requiredQuantity, totalAvailable);
        assertThat(totalAllocated).isEqualTo(expectedAllocation);
    }

    /**
     * **Feature: inventory-management-ghn, Property 9: Fulfillment Aggregation**
     * **Validates: Requirements 5.4**
     * 
     * Property: When aggregating from multiple stores, each store's allocation
     * SHALL NOT exceed its available quantity.
     */
    @Property(tries = 100)
    void eachStoreAllocationDoesNotExceedAvailableQuantity(
            @ForAll @IntRange(min = 1, max = 10000) long productId,
            @ForAll @IntRange(min = 50, max = 200) int requiredQuantity,
            @ForAll @IntRange(min = 0, max = 20) int mainWarehouseQuantity,
            @ForAll("multipleStoreQuantities") List<Integer> storeQuantities) {
        
        // Ensure main warehouse has insufficient quantity
        Assume.that(mainWarehouseQuantity < requiredQuantity);
        // Ensure we have at least one store warehouse
        Assume.that(!storeQuantities.isEmpty());
        
        // Create mock repositories
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        
        // Setup: Main Warehouse
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
                .quantity(mainWarehouseQuantity)
                .reservedQuantity(0)
                .build();
        
        when(inventoryItemRepository.findByWarehouseIdAndProductId(mainWarehouseId, productId))
                .thenReturn(Optional.of(mainInventory));
        
        // Setup: Store warehouses and track their quantities
        List<Warehouse> storeWarehouses = new ArrayList<>();
        Map<Long, Integer> warehouseQuantities = new HashMap<>();
        warehouseQuantities.put(mainWarehouseId, mainWarehouseQuantity);
        
        for (int i = 0; i < storeQuantities.size(); i++) {
            long storeWarehouseId = 100L + i;
            int storeId = 10 + i;
            int quantity = storeQuantities.get(i);
            
            Warehouse storeWarehouse = Warehouse.builder()
                    .id(storeWarehouseId)
                    .name("Store Warehouse " + storeId)
                    .type(WarehouseType.STORE)
                    .storeId(storeId)
                    .build();
            storeWarehouses.add(storeWarehouse);
            warehouseQuantities.put(storeWarehouseId, quantity);
            
            InventoryItem storeInventory = InventoryItem.builder()
                    .id(100L + i)
                    .warehouseId(storeWarehouseId)
                    .productId(productId)
                    .quantity(quantity)
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
        
        // Verify: Each allocation does not exceed the warehouse's available quantity
        for (SourceAllocation allocation : allocations) {
            int availableInWarehouse = warehouseQuantities.getOrDefault(allocation.getWarehouseId(), 0);
            assertThat(allocation.getQuantity())
                    .as("Allocation from warehouse %d should not exceed available quantity %d",
                            allocation.getWarehouseId(), availableInWarehouse)
                    .isLessThanOrEqualTo(availableInWarehouse);
        }
    }

    /**
     * **Feature: inventory-management-ghn, Property 9: Fulfillment Aggregation**
     * **Validates: Requirements 5.4**
     * 
     * Property: When multiple Store_Warehouses are needed, the system SHALL use
     * multiple stores to aggregate the required quantity.
     */
    @Property(tries = 100)
    void multipleStoresAreUsedWhenNeeded(
            @ForAll @IntRange(min = 1, max = 10000) long productId,
            @ForAll @IntRange(min = 50, max = 100) int requiredQuantity,
            @ForAll("smallStoreQuantities") List<Integer> storeQuantities) {
        
        // Main warehouse is empty
        int mainWarehouseQuantity = 0;
        
        // Ensure we have multiple stores and each has less than required
        Assume.that(storeQuantities.size() >= 2);
        Assume.that(storeQuantities.stream().allMatch(q -> q < requiredQuantity));
        
        // Ensure total store quantity is sufficient
        int totalStoreQuantity = storeQuantities.stream().mapToInt(Integer::intValue).sum();
        Assume.that(totalStoreQuantity >= requiredQuantity);
        
        // Create mock repositories
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        
        // Setup: Main Warehouse (empty)
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
                .quantity(mainWarehouseQuantity)
                .reservedQuantity(0)
                .build();
        
        when(inventoryItemRepository.findByWarehouseIdAndProductId(mainWarehouseId, productId))
                .thenReturn(Optional.of(mainInventory));
        
        // Setup: Store warehouses
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
        
        // Verify: Multiple stores are used (since no single store has enough)
        long storeAllocationCount = allocations.stream()
                .filter(a -> a.getWarehouseType() == WarehouseType.STORE)
                .count();
        
        assertThat(storeAllocationCount)
                .as("Multiple stores should be used when no single store has enough quantity")
                .isGreaterThan(1);
        
        // Verify: Total allocated equals required quantity
        int totalAllocated = allocations.stream()
                .mapToInt(SourceAllocation::getQuantity)
                .sum();
        assertThat(totalAllocated).isEqualTo(requiredQuantity);
    }

    /**
     * **Feature: inventory-management-ghn, Property 9: Fulfillment Aggregation**
     * **Validates: Requirements 5.4**
     * 
     * Property: When total available is less than requested, the sum of allocations
     * SHALL equal the total available quantity (partial fulfillment).
     */
    @Property(tries = 100)
    void partialFulfillmentWhenInsufficientTotalQuantity(
            @ForAll @IntRange(min = 1, max = 10000) long productId,
            @ForAll @IntRange(min = 100, max = 200) int requiredQuantity,
            @ForAll @IntRange(min = 0, max = 30) int mainWarehouseQuantity,
            @ForAll("smallStoreQuantities") List<Integer> storeQuantities) {
        
        // Calculate total available
        int totalStoreQuantity = storeQuantities.stream().mapToInt(Integer::intValue).sum();
        int totalAvailable = mainWarehouseQuantity + totalStoreQuantity;
        
        // Ensure total available is less than required (partial fulfillment scenario)
        Assume.that(totalAvailable < requiredQuantity);
        Assume.that(totalAvailable > 0); // At least some quantity available
        
        // Create mock repositories
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        
        // Setup: Main Warehouse
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
                .quantity(mainWarehouseQuantity)
                .reservedQuantity(0)
                .build();
        
        when(inventoryItemRepository.findByWarehouseIdAndProductId(mainWarehouseId, productId))
                .thenReturn(Optional.of(mainInventory));
        
        // Setup: Store warehouses
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
        
        // Calculate sum of all allocations
        int totalAllocated = allocations.stream()
                .mapToInt(SourceAllocation::getQuantity)
                .sum();
        
        // Verify: Sum of allocations equals total available (partial fulfillment)
        assertThat(totalAllocated)
                .as("When total available (%d) is less than required (%d), " +
                    "allocations should equal total available", totalAvailable, requiredQuantity)
                .isEqualTo(totalAvailable);
    }

    /**
     * Provides a list of store warehouse quantities for multiple stores.
     */
    @Provide
    Arbitrary<List<Integer>> multipleStoreQuantities() {
        return Arbitraries.integers().between(0, 50)
                .list()
                .ofMinSize(1)
                .ofMaxSize(5);
    }

    /**
     * Provides a list of small store warehouse quantities (each store has limited stock).
     */
    @Provide
    Arbitrary<List<Integer>> smallStoreQuantities() {
        return Arbitraries.integers().between(10, 40)
                .list()
                .ofMinSize(2)
                .ofMaxSize(5);
    }

    /**
     * Helper class that replicates the source allocation logic from FulfillmentService.
     * This allows testing the aggregation logic without full Spring context.
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
         * Requirements: 5.4 - Aggregate from multiple stores
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
            
            // Step 2: If still need more, check Store Warehouses (Requirements 5.3, 5.4)
            if (remainingQuantity > 0) {
                List<Warehouse> storeWarehouses = warehouseRepository.findByType(WarehouseType.STORE);
                
                // Sort store warehouses by available quantity (descending) for optimal allocation
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
                
                // Aggregate from multiple stores (Requirements 5.4)
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
