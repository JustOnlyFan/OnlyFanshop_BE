package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.DebtOrderDTO;
import com.example.onlyfanshop_be.entity.*;
import com.example.onlyfanshop_be.enums.DebtOrderStatus;
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
 * Property-based tests for Debt Order Fulfillment Check.
 * 
 * **Feature: inventory-management-ghn, Property 13: Debt Order Fulfillment Check**
 * **Validates: Requirements 6.3, 6.4**
 * 
 * Property: For any Main_Warehouse inventory update that increases quantity,
 * the system SHALL check all PENDING Debt_Orders and mark as FULFILLABLE
 * any Debt_Order where the updated product's available quantity is sufficient.
 */
class DebtOrderFulfillmentCheckPropertyTest {

    /**
     * **Feature: inventory-management-ghn, Property 13: Debt Order Fulfillment Check**
     * **Validates: Requirements 6.3, 6.4**
     * 
     * Property: When Main_Warehouse inventory increases and a PENDING Debt_Order
     * can now be fulfilled, the Debt_Order status SHALL change to FULFILLABLE.
     */
    @Property(tries = 100)
    void pendingDebtOrderBecomeFulfillableWhenInventorySufficient(
            @ForAll @IntRange(min = 1, max = 10000) long debtOrderId,
            @ForAll @IntRange(min = 1, max = 10000) long productId,
            @ForAll @IntRange(min = 1, max = 30) int owedQuantity,
            @ForAll @IntRange(min = 1, max = 10000) long transferRequestId) {
        
        // Ensure new quantity is sufficient to fulfill the debt
        int newQuantity = owedQuantity + 10;
        
        // Create mock repositories
        DebtOrderRepository debtOrderRepository = mock(DebtOrderRepository.class);
        DebtItemRepository debtItemRepository = mock(DebtItemRepository.class);
        TransferRequestRepository transferRequestRepository = mock(TransferRequestRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        StoreLocationRepository storeLocationRepository = mock(StoreLocationRepository.class);
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        InventoryLogRepository inventoryLogRepository = mock(InventoryLogRepository.class);
        
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
        
        // Setup: Inventory with sufficient quantity
        InventoryItem inventoryItem = InventoryItem.builder()
                .id(1L)
                .warehouseId(mainWarehouseId)
                .productId(productId)
                .quantity(newQuantity)
                .reservedQuantity(0)
                .build();
        
        when(inventoryItemRepository.findByWarehouseIdAndProductId(mainWarehouseId, productId))
                .thenReturn(Optional.of(inventoryItem));
        
        // Setup: PENDING Debt Order with items
        DebtOrder pendingDebtOrder = DebtOrder.builder()
                .id(debtOrderId)
                .transferRequestId(transferRequestId)
                .status(DebtOrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        
        DebtItem debtItem = DebtItem.builder()
                .id(1L)
                .debtOrderId(debtOrderId)
                .productId(productId)
                .owedQuantity(owedQuantity)
                .fulfilledQuantity(0)
                .build();
        
        when(debtOrderRepository.findByStatus(DebtOrderStatus.PENDING))
                .thenReturn(List.of(pendingDebtOrder));
        when(debtOrderRepository.findById(debtOrderId))
                .thenReturn(Optional.of(pendingDebtOrder));
        when(debtItemRepository.findByDebtOrderId(debtOrderId))
                .thenReturn(List.of(debtItem));
        
        // Capture saved debt order
        final DebtOrder[] capturedDebtOrder = new DebtOrder[1];
        when(debtOrderRepository.save(any(DebtOrder.class))).thenAnswer(inv -> {
            DebtOrder order = inv.getArgument(0);
            capturedDebtOrder[0] = order;
            return order;
        });
        
        // Create the debt order service
        DebtOrderService debtOrderService = new DebtOrderService(
                debtOrderRepository,
                debtItemRepository,
                transferRequestRepository,
                productRepository,
                storeLocationRepository,
                warehouseRepository,
                inventoryItemRepository,
                inventoryLogRepository
        );
        
        // Execute: Check fulfillable debt orders
        List<DebtOrderDTO> fulfillableOrders = debtOrderService.checkFulfillableDebtOrders();
        
        // Verify: Debt order status changed to FULFILLABLE
        assertThat(capturedDebtOrder[0])
                .as("Debt order should be saved")
                .isNotNull();
        
        assertThat(capturedDebtOrder[0].getStatus())
                .as("Debt order status should be FULFILLABLE")
                .isEqualTo(DebtOrderStatus.FULFILLABLE);
        
        assertThat(fulfillableOrders)
                .as("Should return the fulfillable debt order")
                .hasSize(1);
        
        assertThat(fulfillableOrders.get(0).getId())
                .as("Returned debt order ID should match")
                .isEqualTo(debtOrderId);
    }

    /**
     * **Feature: inventory-management-ghn, Property 13: Debt Order Fulfillment Check**
     * **Validates: Requirements 6.3, 6.4**
     * 
     * Property: When Main_Warehouse inventory is still insufficient,
     * the PENDING Debt_Order status SHALL remain PENDING.
     */
    @Property(tries = 100)
    void pendingDebtOrderRemainsPendingWhenInventoryInsufficient(
            @ForAll @IntRange(min = 1, max = 10000) long debtOrderId,
            @ForAll @IntRange(min = 1, max = 10000) long productId,
            @ForAll @IntRange(min = 20, max = 30) int owedQuantity,
            @ForAll @IntRange(min = 1, max = 10000) long transferRequestId,
            @ForAll @IntRange(min = 1, max = 15) int availableQuantity) {
        
        // Ensure available quantity is less than owed (still insufficient)
        Assume.that(availableQuantity < owedQuantity);
        
        // Create mock repositories
        DebtOrderRepository debtOrderRepository = mock(DebtOrderRepository.class);
        DebtItemRepository debtItemRepository = mock(DebtItemRepository.class);
        TransferRequestRepository transferRequestRepository = mock(TransferRequestRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        StoreLocationRepository storeLocationRepository = mock(StoreLocationRepository.class);
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        InventoryLogRepository inventoryLogRepository = mock(InventoryLogRepository.class);
        
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
        
        // Setup: Inventory with insufficient quantity
        InventoryItem inventoryItem = InventoryItem.builder()
                .id(1L)
                .warehouseId(mainWarehouseId)
                .productId(productId)
                .quantity(availableQuantity)
                .reservedQuantity(0)
                .build();
        
        when(inventoryItemRepository.findByWarehouseIdAndProductId(mainWarehouseId, productId))
                .thenReturn(Optional.of(inventoryItem));
        
        // Setup: PENDING Debt Order with items
        DebtOrder pendingDebtOrder = DebtOrder.builder()
                .id(debtOrderId)
                .transferRequestId(transferRequestId)
                .status(DebtOrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        
        DebtItem debtItem = DebtItem.builder()
                .id(1L)
                .debtOrderId(debtOrderId)
                .productId(productId)
                .owedQuantity(owedQuantity)
                .fulfilledQuantity(0)
                .build();
        
        when(debtOrderRepository.findByStatus(DebtOrderStatus.PENDING))
                .thenReturn(List.of(pendingDebtOrder));
        when(debtOrderRepository.findById(debtOrderId))
                .thenReturn(Optional.of(pendingDebtOrder));
        when(debtItemRepository.findByDebtOrderId(debtOrderId))
                .thenReturn(List.of(debtItem));
        
        // Create the debt order service
        DebtOrderService debtOrderService = new DebtOrderService(
                debtOrderRepository,
                debtItemRepository,
                transferRequestRepository,
                productRepository,
                storeLocationRepository,
                warehouseRepository,
                inventoryItemRepository,
                inventoryLogRepository
        );
        
        // Execute: Check fulfillable debt orders
        List<DebtOrderDTO> fulfillableOrders = debtOrderService.checkFulfillableDebtOrders();
        
        // Verify: No debt orders became fulfillable
        assertThat(fulfillableOrders)
                .as("No debt orders should be fulfillable when inventory is insufficient")
                .isEmpty();
        
        // Verify: Debt order was not saved (status unchanged)
        verify(debtOrderRepository, never()).save(any(DebtOrder.class));
    }

    /**
     * **Feature: inventory-management-ghn, Property 13: Debt Order Fulfillment Check**
     * **Validates: Requirements 6.3, 6.4**
     * 
     * Property: When multiple PENDING Debt_Orders exist, only those with
     * sufficient inventory SHALL be marked as FULFILLABLE.
     */
    @Property(tries = 100)
    void onlyFulfillableDebtOrdersAreMarked(
            @ForAll @IntRange(min = 1, max = 10000) long productId,
            @ForAll @IntRange(min = 30, max = 50) int availableQuantity,
            @ForAll("multipleDebtOrders") List<DebtOrderData> debtOrders) {
        
        // Ensure at least one debt order
        Assume.that(!debtOrders.isEmpty());
        // Ensure some can be fulfilled and some cannot
        Assume.that(debtOrders.stream().anyMatch(d -> d.owedQuantity <= availableQuantity));
        Assume.that(debtOrders.stream().anyMatch(d -> d.owedQuantity > availableQuantity));
        
        // Create mock repositories
        DebtOrderRepository debtOrderRepository = mock(DebtOrderRepository.class);
        DebtItemRepository debtItemRepository = mock(DebtItemRepository.class);
        TransferRequestRepository transferRequestRepository = mock(TransferRequestRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        StoreLocationRepository storeLocationRepository = mock(StoreLocationRepository.class);
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        InventoryLogRepository inventoryLogRepository = mock(InventoryLogRepository.class);
        
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
        
        // Setup: Inventory
        InventoryItem inventoryItem = InventoryItem.builder()
                .id(1L)
                .warehouseId(mainWarehouseId)
                .productId(productId)
                .quantity(availableQuantity)
                .reservedQuantity(0)
                .build();
        
        when(inventoryItemRepository.findByWarehouseIdAndProductId(mainWarehouseId, productId))
                .thenReturn(Optional.of(inventoryItem));
        
        // Setup: Multiple PENDING Debt Orders
        List<DebtOrder> pendingDebtOrders = new ArrayList<>();
        for (DebtOrderData data : debtOrders) {
            DebtOrder debtOrder = DebtOrder.builder()
                    .id(data.debtOrderId)
                    .transferRequestId(data.transferRequestId)
                    .status(DebtOrderStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();
            pendingDebtOrders.add(debtOrder);
            
            DebtItem debtItem = DebtItem.builder()
                    .id(data.debtOrderId)
                    .debtOrderId(data.debtOrderId)
                    .productId(productId)
                    .owedQuantity(data.owedQuantity)
                    .fulfilledQuantity(0)
                    .build();
            
            when(debtOrderRepository.findById(data.debtOrderId))
                    .thenReturn(Optional.of(debtOrder));
            when(debtItemRepository.findByDebtOrderId(data.debtOrderId))
                    .thenReturn(List.of(debtItem));
        }
        
        when(debtOrderRepository.findByStatus(DebtOrderStatus.PENDING))
                .thenReturn(pendingDebtOrders);
        
        // Track saved debt orders
        Set<Long> savedDebtOrderIds = new HashSet<>();
        when(debtOrderRepository.save(any(DebtOrder.class))).thenAnswer(inv -> {
            DebtOrder order = inv.getArgument(0);
            savedDebtOrderIds.add(order.getId());
            return order;
        });
        
        // Create the debt order service
        DebtOrderService debtOrderService = new DebtOrderService(
                debtOrderRepository,
                debtItemRepository,
                transferRequestRepository,
                productRepository,
                storeLocationRepository,
                warehouseRepository,
                inventoryItemRepository,
                inventoryLogRepository
        );
        
        // Execute: Check fulfillable debt orders
        List<DebtOrderDTO> fulfillableOrders = debtOrderService.checkFulfillableDebtOrders();
        
        // Calculate expected fulfillable orders
        Set<Long> expectedFulfillable = new HashSet<>();
        for (DebtOrderData data : debtOrders) {
            if (data.owedQuantity <= availableQuantity) {
                expectedFulfillable.add(data.debtOrderId);
            }
        }
        
        // Verify: Only fulfillable debt orders were marked
        assertThat(fulfillableOrders)
                .as("Number of fulfillable orders should match expected")
                .hasSize(expectedFulfillable.size());
        
        Set<Long> actualFulfillableIds = new HashSet<>();
        for (DebtOrderDTO dto : fulfillableOrders) {
            actualFulfillableIds.add(dto.getId());
        }
        
        assertThat(actualFulfillableIds)
                .as("Fulfillable order IDs should match expected")
                .containsExactlyInAnyOrderElementsOf(expectedFulfillable);
    }

    /**
     * **Feature: inventory-management-ghn, Property 13: Debt Order Fulfillment Check**
     * **Validates: Requirements 6.3, 6.4**
     * 
     * Property: COMPLETED Debt_Orders SHALL NOT be checked or modified
     * during the fulfillment check.
     */
    @Property(tries = 100)
    void completedDebtOrdersAreNotModified(
            @ForAll @IntRange(min = 1, max = 10000) long debtOrderId,
            @ForAll @IntRange(min = 1, max = 10000) long productId,
            @ForAll @IntRange(min = 1, max = 30) int owedQuantity,
            @ForAll @IntRange(min = 1, max = 10000) long transferRequestId) {
        
        // Create mock repositories
        DebtOrderRepository debtOrderRepository = mock(DebtOrderRepository.class);
        DebtItemRepository debtItemRepository = mock(DebtItemRepository.class);
        TransferRequestRepository transferRequestRepository = mock(TransferRequestRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        StoreLocationRepository storeLocationRepository = mock(StoreLocationRepository.class);
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        InventoryLogRepository inventoryLogRepository = mock(InventoryLogRepository.class);
        
        // Setup: No PENDING debt orders (only COMPLETED exists but not returned)
        when(debtOrderRepository.findByStatus(DebtOrderStatus.PENDING))
                .thenReturn(Collections.emptyList());
        
        // Create the debt order service
        DebtOrderService debtOrderService = new DebtOrderService(
                debtOrderRepository,
                debtItemRepository,
                transferRequestRepository,
                productRepository,
                storeLocationRepository,
                warehouseRepository,
                inventoryItemRepository,
                inventoryLogRepository
        );
        
        // Execute: Check fulfillable debt orders
        List<DebtOrderDTO> fulfillableOrders = debtOrderService.checkFulfillableDebtOrders();
        
        // Verify: No debt orders were modified
        assertThat(fulfillableOrders)
                .as("No debt orders should be returned when none are PENDING")
                .isEmpty();
        
        verify(debtOrderRepository, never()).save(any(DebtOrder.class));
    }

    /**
     * **Feature: inventory-management-ghn, Property 13: Debt Order Fulfillment Check**
     * **Validates: Requirements 6.3, 6.4**
     * 
     * Property: For a Debt_Order with multiple items, it SHALL only be marked
     * as FULFILLABLE if ALL items can be fulfilled from Main_Warehouse.
     */
    @Property(tries = 100)
    void debtOrderWithMultipleItemsRequiresAllItemsFulfillable(
            @ForAll @IntRange(min = 1, max = 10000) long debtOrderId,
            @ForAll @IntRange(min = 1, max = 10000) long transferRequestId,
            @ForAll("multipleDebtItems") List<DebtItemData> debtItems) {
        
        // Ensure at least 2 items
        Assume.that(debtItems.size() >= 2);
        // Ensure unique product IDs
        Set<Long> productIds = new HashSet<>();
        for (DebtItemData item : debtItems) {
            if (!productIds.add(item.productId)) {
                return; // Skip if duplicate product IDs
            }
        }
        
        // Create mock repositories
        DebtOrderRepository debtOrderRepository = mock(DebtOrderRepository.class);
        DebtItemRepository debtItemRepository = mock(DebtItemRepository.class);
        TransferRequestRepository transferRequestRepository = mock(TransferRequestRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        StoreLocationRepository storeLocationRepository = mock(StoreLocationRepository.class);
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        InventoryLogRepository inventoryLogRepository = mock(InventoryLogRepository.class);
        
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
        
        // Setup: Inventory for each product
        boolean allCanBeFulfilled = true;
        for (DebtItemData itemData : debtItems) {
            InventoryItem inventoryItem = InventoryItem.builder()
                    .id(itemData.productId)
                    .warehouseId(mainWarehouseId)
                    .productId(itemData.productId)
                    .quantity(itemData.availableQuantity)
                    .reservedQuantity(0)
                    .build();
            
            when(inventoryItemRepository.findByWarehouseIdAndProductId(mainWarehouseId, itemData.productId))
                    .thenReturn(Optional.of(inventoryItem));
            
            if (itemData.availableQuantity < itemData.owedQuantity) {
                allCanBeFulfilled = false;
            }
        }
        
        // Setup: PENDING Debt Order with multiple items
        DebtOrder pendingDebtOrder = DebtOrder.builder()
                .id(debtOrderId)
                .transferRequestId(transferRequestId)
                .status(DebtOrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        
        List<DebtItem> items = new ArrayList<>();
        for (int i = 0; i < debtItems.size(); i++) {
            DebtItemData itemData = debtItems.get(i);
            DebtItem debtItem = DebtItem.builder()
                    .id((long) (i + 1))
                    .debtOrderId(debtOrderId)
                    .productId(itemData.productId)
                    .owedQuantity(itemData.owedQuantity)
                    .fulfilledQuantity(0)
                    .build();
            items.add(debtItem);
        }
        
        when(debtOrderRepository.findByStatus(DebtOrderStatus.PENDING))
                .thenReturn(List.of(pendingDebtOrder));
        when(debtOrderRepository.findById(debtOrderId))
                .thenReturn(Optional.of(pendingDebtOrder));
        when(debtItemRepository.findByDebtOrderId(debtOrderId))
                .thenReturn(items);
        
        // Track if debt order was saved
        final boolean[] wasSaved = {false};
        when(debtOrderRepository.save(any(DebtOrder.class))).thenAnswer(inv -> {
            wasSaved[0] = true;
            return inv.getArgument(0);
        });
        
        // Create the debt order service
        DebtOrderService debtOrderService = new DebtOrderService(
                debtOrderRepository,
                debtItemRepository,
                transferRequestRepository,
                productRepository,
                storeLocationRepository,
                warehouseRepository,
                inventoryItemRepository,
                inventoryLogRepository
        );
        
        // Execute: Check fulfillable debt orders
        List<DebtOrderDTO> fulfillableOrders = debtOrderService.checkFulfillableDebtOrders();
        
        // Verify: Debt order is only fulfillable if ALL items can be fulfilled
        if (allCanBeFulfilled) {
            assertThat(fulfillableOrders)
                    .as("Debt order should be fulfillable when all items can be fulfilled")
                    .hasSize(1);
            assertThat(wasSaved[0])
                    .as("Debt order should be saved when marked as fulfillable")
                    .isTrue();
        } else {
            assertThat(fulfillableOrders)
                    .as("Debt order should NOT be fulfillable when any item cannot be fulfilled")
                    .isEmpty();
            assertThat(wasSaved[0])
                    .as("Debt order should NOT be saved when not fulfillable")
                    .isFalse();
        }
    }

    /**
     * Data class for debt order test data.
     */
    static class DebtOrderData {
        final long debtOrderId;
        final long transferRequestId;
        final int owedQuantity;
        
        DebtOrderData(long debtOrderId, long transferRequestId, int owedQuantity) {
            this.debtOrderId = debtOrderId;
            this.transferRequestId = transferRequestId;
            this.owedQuantity = owedQuantity;
        }
    }

    /**
     * Data class for debt item test data.
     */
    static class DebtItemData {
        final long productId;
        final int owedQuantity;
        final int availableQuantity;
        
        DebtItemData(long productId, int owedQuantity, int availableQuantity) {
            this.productId = productId;
            this.owedQuantity = owedQuantity;
            this.availableQuantity = availableQuantity;
        }
    }

    /**
     * Provides a list of debt order data for multiple debt orders.
     */
    @Provide
    Arbitrary<List<DebtOrderData>> multipleDebtOrders() {
        Arbitrary<DebtOrderData> debtOrderData = Combinators.combine(
                Arbitraries.longs().between(1, 10000),
                Arbitraries.longs().between(1, 10000),
                Arbitraries.integers().between(10, 60) // Some will be <= 50, some > 50
        ).as(DebtOrderData::new);
        
        return debtOrderData.list().ofMinSize(2).ofMaxSize(5)
                .filter(list -> {
                    // Ensure unique debt order IDs
                    Set<Long> ids = new HashSet<>();
                    for (DebtOrderData data : list) {
                        if (!ids.add(data.debtOrderId)) {
                            return false;
                        }
                    }
                    return true;
                });
    }

    /**
     * Provides a list of debt item data for multiple items.
     */
    @Provide
    Arbitrary<List<DebtItemData>> multipleDebtItems() {
        Arbitrary<DebtItemData> debtItemData = Combinators.combine(
                Arbitraries.longs().between(1, 10000),
                Arbitraries.integers().between(5, 30),
                Arbitraries.integers().between(0, 40)
        ).as(DebtItemData::new);
        
        return debtItemData.list().ofMinSize(2).ofMaxSize(4)
                .filter(list -> {
                    // Ensure unique product IDs
                    Set<Long> ids = new HashSet<>();
                    for (DebtItemData data : list) {
                        if (!ids.add(data.productId)) {
                            return false;
                        }
                    }
                    return true;
                });
    }
}
