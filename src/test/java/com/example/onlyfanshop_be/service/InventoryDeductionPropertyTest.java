package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.response.FulfillmentResult;
import com.example.onlyfanshop_be.dto.response.SourceAllocation;
import com.example.onlyfanshop_be.entity.*;
import com.example.onlyfanshop_be.enums.TransferRequestStatus;
import com.example.onlyfanshop_be.enums.WarehouseType;
import com.example.onlyfanshop_be.repository.*;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for Inventory Deduction on Approval.
 * 
 * **Feature: inventory-management-ghn, Property 10: Inventory Deduction on Approval**
 * **Validates: Requirements 5.5**
 * 
 * Property: For any approved Transfer_Request, the sum of quantities deducted from all
 * source warehouses SHALL equal the fulfilled quantity, and each source warehouse's
 * quantity SHALL decrease by exactly the allocated amount.
 */
class InventoryDeductionPropertyTest {

    /**
     * **Feature: inventory-management-ghn, Property 10: Inventory Deduction on Approval**
     * **Validates: Requirements 5.5**
     * 
     * Property: The sum of quantities deducted from all source warehouses SHALL equal
     * the fulfilled quantity.
     */
    @Property(tries = 100)
    void sumOfDeductedQuantitiesEqualsFulfilledQuantity(
            @ForAll @IntRange(min = 1, max = 10000) long requestId,
            @ForAll @IntRange(min = 1, max = 10000) long productId,
            @ForAll @IntRange(min = 1, max = 30) int requestedQuantity,
            @ForAll @IntRange(min = 10, max = 20) int storeId) {

        // Ensure main warehouse has sufficient quantity
        int mainWarehouseQuantity = requestedQuantity + 10;
        
        // Create mock repositories
        TransferRequestRepository transferRequestRepository = mock(TransferRequestRepository.class);
        TransferRequestItemRepository transferRequestItemRepository = mock(TransferRequestItemRepository.class);
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        InventoryLogRepository inventoryLogRepository = mock(InventoryLogRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        StoreLocationRepository storeLocationRepository = mock(StoreLocationRepository.class);
        
        // Setup: Transfer Request
        TransferRequest request = TransferRequest.builder()
                .id(requestId)
                .storeId(storeId)
                .status(TransferRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        
        TransferRequestItem item = TransferRequestItem.builder()
                .id(1L)
                .transferRequestId(requestId)
                .productId(productId)
                .requestedQuantity(requestedQuantity)
                .fulfilledQuantity(0)
                .build();
        
        request.setItems(List.of(item));
        
        when(transferRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(transferRequestItemRepository.findByTransferRequestId(requestId)).thenReturn(List.of(item));
        when(transferRequestRepository.save(any(TransferRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transferRequestItemRepository.save(any(TransferRequestItem.class))).thenAnswer(inv -> inv.getArgument(0));
        
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
        when(warehouseRepository.findByType(WarehouseType.STORE)).thenReturn(Collections.emptyList());

        // Setup: Main warehouse inventory - track initial quantity
        InventoryItem mainInventory = InventoryItem.builder()
                .id(1L)
                .warehouseId(mainWarehouseId)
                .productId(productId)
                .quantity(mainWarehouseQuantity)
                .reservedQuantity(0)
                .build();
        
        when(inventoryItemRepository.findByWarehouseIdAndProductId(mainWarehouseId, productId))
                .thenReturn(Optional.of(mainInventory));
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inventoryLogRepository.save(any(InventoryLog.class))).thenAnswer(inv -> inv.getArgument(0));
        
        // Setup: Product
        Product product = Product.builder()
                .id(productId)
                .name("Test Product")
                .sku("SKU-" + productId)
                .build();
        when(productRepository.findById((int) productId)).thenReturn(Optional.of(product));
        
        // Create the fulfillment service
        FulfillmentService fulfillmentService = new FulfillmentService(
                transferRequestRepository,
                transferRequestItemRepository,
                warehouseRepository,
                inventoryItemRepository,
                inventoryLogRepository,
                productRepository,
                storeLocationRepository
        );
        
        // Execute: Fulfill the request
        FulfillmentResult result = fulfillmentService.fulfill(request);
        
        // Verify: Sum of deducted quantities equals fulfilled quantity
        int totalFulfilled = result.getFulfilledQuantities().values().stream()
                .mapToInt(Integer::intValue)
                .sum();
        
        // Calculate total deducted from source allocations
        int totalDeducted = result.getSourceAllocations().values().stream()
                .flatMap(List::stream)
                .mapToInt(SourceAllocation::getQuantity)
                .sum();
        
        assertThat(totalDeducted)
                .as("Sum of deducted quantities should equal fulfilled quantity")
                .isEqualTo(totalFulfilled);
        
        // Verify: Fulfilled quantity equals requested quantity (since we have enough)
        assertThat(totalFulfilled).isEqualTo(requestedQuantity);
    }


    /**
     * **Feature: inventory-management-ghn, Property 10: Inventory Deduction on Approval**
     * **Validates: Requirements 5.5**
     * 
     * Property: Each source warehouse's quantity SHALL decrease by exactly the allocated amount.
     */
    @Property(tries = 100)
    void eachWarehouseQuantityDecreasedByAllocatedAmount(
            @ForAll @IntRange(min = 1, max = 10000) long requestId,
            @ForAll @IntRange(min = 1, max = 10000) long productId,
            @ForAll @IntRange(min = 1, max = 30) int requestedQuantity,
            @ForAll @IntRange(min = 10, max = 20) int storeId,
            @ForAll @IntRange(min = 0, max = 15) int mainWarehouseQuantity,
            @ForAll("storeQuantities") List<Integer> storeQuantities) {
        
        // Ensure we have enough total quantity
        int totalStoreQuantity = storeQuantities.stream().mapToInt(Integer::intValue).sum();
        int totalAvailable = mainWarehouseQuantity + totalStoreQuantity;
        Assume.that(totalAvailable >= requestedQuantity);
        
        // Create mock repositories
        TransferRequestRepository transferRequestRepository = mock(TransferRequestRepository.class);
        TransferRequestItemRepository transferRequestItemRepository = mock(TransferRequestItemRepository.class);
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        InventoryLogRepository inventoryLogRepository = mock(InventoryLogRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        StoreLocationRepository storeLocationRepository = mock(StoreLocationRepository.class);
        
        // Setup: Transfer Request
        TransferRequest request = TransferRequest.builder()
                .id(requestId)
                .storeId(storeId)
                .status(TransferRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        
        TransferRequestItem item = TransferRequestItem.builder()
                .id(1L)
                .transferRequestId(requestId)
                .productId(productId)
                .requestedQuantity(requestedQuantity)
                .fulfilledQuantity(0)
                .build();
        
        request.setItems(List.of(item));
        
        when(transferRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(transferRequestItemRepository.findByTransferRequestId(requestId)).thenReturn(List.of(item));
        when(transferRequestRepository.save(any(TransferRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transferRequestItemRepository.save(any(TransferRequestItem.class))).thenAnswer(inv -> inv.getArgument(0));

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
        
        // Track initial quantities for verification
        Map<Long, Integer> initialQuantities = new HashMap<>();
        Map<Long, InventoryItem> inventoryItems = new HashMap<>();
        
        // Setup: Main warehouse inventory
        InventoryItem mainInventory = InventoryItem.builder()
                .id(1L)
                .warehouseId(mainWarehouseId)
                .productId(productId)
                .quantity(mainWarehouseQuantity)
                .reservedQuantity(0)
                .build();
        
        initialQuantities.put(mainWarehouseId, mainWarehouseQuantity);
        inventoryItems.put(mainWarehouseId, mainInventory);
        
        when(inventoryItemRepository.findByWarehouseIdAndProductId(mainWarehouseId, productId))
                .thenReturn(Optional.of(mainInventory));
        
        // Setup: Store warehouses
        List<Warehouse> storeWarehouses = new ArrayList<>();
        for (int i = 0; i < storeQuantities.size(); i++) {
            long storeWarehouseId = 100L + i;
            int warehouseStoreId = 100 + i;
            
            // Skip if this is the requesting store
            if (warehouseStoreId == storeId) {
                continue;
            }
            
            Warehouse storeWarehouse = Warehouse.builder()
                    .id(storeWarehouseId)
                    .name("Store Warehouse " + warehouseStoreId)
                    .type(WarehouseType.STORE)
                    .storeId(warehouseStoreId)
                    .build();
            storeWarehouses.add(storeWarehouse);
            
            InventoryItem storeInventory = InventoryItem.builder()
                    .id(100L + i)
                    .warehouseId(storeWarehouseId)
                    .productId(productId)
                    .quantity(storeQuantities.get(i))
                    .reservedQuantity(0)
                    .build();
            
            initialQuantities.put(storeWarehouseId, storeQuantities.get(i));
            inventoryItems.put(storeWarehouseId, storeInventory);
            
            when(inventoryItemRepository.findByWarehouseIdAndProductId(storeWarehouseId, productId))
                    .thenReturn(Optional.of(storeInventory));
        }
        
        when(warehouseRepository.findByType(WarehouseType.STORE)).thenReturn(storeWarehouses);
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inventoryLogRepository.save(any(InventoryLog.class))).thenAnswer(inv -> inv.getArgument(0));

        // Setup: Product
        Product product = Product.builder()
                .id(productId)
                .name("Test Product")
                .sku("SKU-" + productId)
                .build();
        when(productRepository.findById((int) productId)).thenReturn(Optional.of(product));
        
        // Create the fulfillment service
        FulfillmentService fulfillmentService = new FulfillmentService(
                transferRequestRepository,
                transferRequestItemRepository,
                warehouseRepository,
                inventoryItemRepository,
                inventoryLogRepository,
                productRepository,
                storeLocationRepository
        );
        
        // Execute: Fulfill the request
        FulfillmentResult result = fulfillmentService.fulfill(request);
        
        // Verify: Each warehouse's quantity decreased by exactly the allocated amount
        for (List<SourceAllocation> allocations : result.getSourceAllocations().values()) {
            for (SourceAllocation allocation : allocations) {
                Long warehouseId = allocation.getWarehouseId();
                int allocatedQuantity = allocation.getQuantity();
                int initialQty = initialQuantities.getOrDefault(warehouseId, 0);
                InventoryItem inventoryItem = inventoryItems.get(warehouseId);
                
                if (inventoryItem != null) {
                    int expectedNewQuantity = initialQty - allocatedQuantity;
                    assertThat(inventoryItem.getQuantity())
                            .as("Warehouse %d quantity should decrease by exactly %d (from %d to %d)",
                                    warehouseId, allocatedQuantity, initialQty, expectedNewQuantity)
                            .isEqualTo(expectedNewQuantity);
                }
            }
        }
    }


    /**
     * **Feature: inventory-management-ghn, Property 10: Inventory Deduction on Approval**
     * **Validates: Requirements 5.5**
     * 
     * Property: Inventory logs SHALL be created for each deduction with correct
     * previous and new quantities.
     */
    @Property(tries = 100)
    void inventoryLogsCreatedForEachDeduction(
            @ForAll @IntRange(min = 1, max = 10000) long requestId,
            @ForAll @IntRange(min = 1, max = 10000) long productId,
            @ForAll @IntRange(min = 1, max = 30) int requestedQuantity,
            @ForAll @IntRange(min = 10, max = 20) int storeId) {
        
        // Ensure main warehouse has sufficient quantity
        int mainWarehouseQuantity = requestedQuantity + 10;
        
        // Create mock repositories
        TransferRequestRepository transferRequestRepository = mock(TransferRequestRepository.class);
        TransferRequestItemRepository transferRequestItemRepository = mock(TransferRequestItemRepository.class);
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        InventoryLogRepository inventoryLogRepository = mock(InventoryLogRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        StoreLocationRepository storeLocationRepository = mock(StoreLocationRepository.class);
        
        // Capture saved inventory logs
        List<InventoryLog> savedLogs = new ArrayList<>();
        when(inventoryLogRepository.save(any(InventoryLog.class))).thenAnswer(inv -> {
            InventoryLog log = inv.getArgument(0);
            savedLogs.add(log);
            return log;
        });
        
        // Setup: Transfer Request
        TransferRequest request = TransferRequest.builder()
                .id(requestId)
                .storeId(storeId)
                .status(TransferRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        
        TransferRequestItem item = TransferRequestItem.builder()
                .id(1L)
                .transferRequestId(requestId)
                .productId(productId)
                .requestedQuantity(requestedQuantity)
                .fulfilledQuantity(0)
                .build();
        
        request.setItems(List.of(item));
        
        when(transferRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(transferRequestItemRepository.findByTransferRequestId(requestId)).thenReturn(List.of(item));
        when(transferRequestRepository.save(any(TransferRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transferRequestItemRepository.save(any(TransferRequestItem.class))).thenAnswer(inv -> inv.getArgument(0));

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
        when(warehouseRepository.findByType(WarehouseType.STORE)).thenReturn(Collections.emptyList());
        
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
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenAnswer(inv -> inv.getArgument(0));
        
        // Setup: Product
        Product product = Product.builder()
                .id(productId)
                .name("Test Product")
                .sku("SKU-" + productId)
                .build();
        when(productRepository.findById((int) productId)).thenReturn(Optional.of(product));
        
        // Create the fulfillment service
        FulfillmentService fulfillmentService = new FulfillmentService(
                transferRequestRepository,
                transferRequestItemRepository,
                warehouseRepository,
                inventoryItemRepository,
                inventoryLogRepository,
                productRepository,
                storeLocationRepository
        );
        
        // Execute: Fulfill the request
        FulfillmentResult result = fulfillmentService.fulfill(request);
        
        // Verify: Inventory logs were created for each deduction
        int totalAllocations = result.getSourceAllocations().values().stream()
                .mapToInt(List::size)
                .sum();
        
        assertThat(savedLogs)
                .as("An inventory log should be created for each source allocation")
                .hasSize(totalAllocations);
        
        // Verify: Each log has correct previous and new quantities
        for (InventoryLog log : savedLogs) {
            assertThat(log.getPreviousQuantity())
                    .as("Previous quantity should be recorded")
                    .isNotNull();
            assertThat(log.getNewQuantity())
                    .as("New quantity should be recorded")
                    .isNotNull();
            assertThat(log.getNewQuantity())
                    .as("New quantity should be less than previous quantity (deduction)")
                    .isLessThan(log.getPreviousQuantity());
            assertThat(log.getReason())
                    .as("Reason should contain transfer request reference")
                    .contains("Transfer Request");
        }
    }


    /**
     * **Feature: inventory-management-ghn, Property 10: Inventory Deduction on Approval**
     * **Validates: Requirements 5.5**
     * 
     * Property: When fulfilling from multiple sources, the total deduction across
     * all warehouses SHALL equal the total fulfilled quantity.
     */
    @Property(tries = 100)
    void multiSourceDeductionTotalEqualsFullfilledQuantity(
            @ForAll @IntRange(min = 1, max = 10000) long requestId,
            @ForAll @IntRange(min = 1, max = 10000) long productId,
            @ForAll @IntRange(min = 50, max = 100) int requestedQuantity,
            @ForAll @IntRange(min = 10, max = 20) int storeId,
            @ForAll @IntRange(min = 10, max = 30) int mainWarehouseQuantity,
            @ForAll("sufficientStoreQuantities") List<Integer> storeQuantities) {
        
        // Ensure main warehouse has insufficient quantity (to force multi-source)
        Assume.that(mainWarehouseQuantity < requestedQuantity);
        
        // Ensure total is sufficient
        int totalStoreQuantity = storeQuantities.stream().mapToInt(Integer::intValue).sum();
        int totalAvailable = mainWarehouseQuantity + totalStoreQuantity;
        Assume.that(totalAvailable >= requestedQuantity);
        
        // Create mock repositories
        TransferRequestRepository transferRequestRepository = mock(TransferRequestRepository.class);
        TransferRequestItemRepository transferRequestItemRepository = mock(TransferRequestItemRepository.class);
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        InventoryLogRepository inventoryLogRepository = mock(InventoryLogRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        StoreLocationRepository storeLocationRepository = mock(StoreLocationRepository.class);
        
        // Track deductions
        Map<Long, Integer> deductions = new HashMap<>();
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenAnswer(inv -> {
            InventoryItem item = inv.getArgument(0);
            // Track the deduction (we'll verify this later)
            return item;
        });
        
        // Setup: Transfer Request
        TransferRequest request = TransferRequest.builder()
                .id(requestId)
                .storeId(storeId)
                .status(TransferRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        
        TransferRequestItem item = TransferRequestItem.builder()
                .id(1L)
                .transferRequestId(requestId)
                .productId(productId)
                .requestedQuantity(requestedQuantity)
                .fulfilledQuantity(0)
                .build();
        
        request.setItems(List.of(item));
        
        when(transferRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(transferRequestItemRepository.findByTransferRequestId(requestId)).thenReturn(List.of(item));
        when(transferRequestRepository.save(any(TransferRequest.class))).thenAnswer(inv -> inv.getArgument(0));
        when(transferRequestItemRepository.save(any(TransferRequestItem.class))).thenAnswer(inv -> inv.getArgument(0));

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
        
        // Setup: Store warehouses
        List<Warehouse> storeWarehouses = new ArrayList<>();
        for (int i = 0; i < storeQuantities.size(); i++) {
            long storeWarehouseId = 100L + i;
            int warehouseStoreId = 100 + i;
            
            Warehouse storeWarehouse = Warehouse.builder()
                    .id(storeWarehouseId)
                    .name("Store Warehouse " + warehouseStoreId)
                    .type(WarehouseType.STORE)
                    .storeId(warehouseStoreId)
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
        when(inventoryLogRepository.save(any(InventoryLog.class))).thenAnswer(inv -> inv.getArgument(0));
        
        // Setup: Product
        Product product = Product.builder()
                .id(productId)
                .name("Test Product")
                .sku("SKU-" + productId)
                .build();
        when(productRepository.findById((int) productId)).thenReturn(Optional.of(product));
        
        // Create the fulfillment service
        FulfillmentService fulfillmentService = new FulfillmentService(
                transferRequestRepository,
                transferRequestItemRepository,
                warehouseRepository,
                inventoryItemRepository,
                inventoryLogRepository,
                productRepository,
                storeLocationRepository
        );
        
        // Execute: Fulfill the request
        FulfillmentResult result = fulfillmentService.fulfill(request);
        
        // Verify: Total deduction equals fulfilled quantity
        int totalDeducted = result.getSourceAllocations().values().stream()
                .flatMap(List::stream)
                .mapToInt(SourceAllocation::getQuantity)
                .sum();
        
        int totalFulfilled = result.getFulfilledQuantities().values().stream()
                .mapToInt(Integer::intValue)
                .sum();
        
        assertThat(totalDeducted)
                .as("Total deducted from all sources should equal total fulfilled")
                .isEqualTo(totalFulfilled);
        
        // Verify: Multiple sources were used
        int sourceCount = result.getSourceAllocations().values().stream()
                .mapToInt(List::size)
                .sum();
        
        assertThat(sourceCount)
                .as("Multiple sources should be used when main warehouse is insufficient")
                .isGreaterThan(1);
    }


    /**
     * Provides a list of store warehouse quantities.
     */
    @Provide
    Arbitrary<List<Integer>> storeQuantities() {
        return Arbitraries.integers().between(0, 30)
                .list()
                .ofMinSize(1)
                .ofMaxSize(5);
    }

    /**
     * Provides a list of store warehouse quantities that sum to at least 50.
     */
    @Provide
    Arbitrary<List<Integer>> sufficientStoreQuantities() {
        return Arbitraries.integers().between(20, 50)
                .list()
                .ofMinSize(2)
                .ofMaxSize(5);
    }
}
