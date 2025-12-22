package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.DebtOrderDTO;
import com.example.onlyfanshop_be.dto.response.FulfillmentResult;
import com.example.onlyfanshop_be.entity.*;
import com.example.onlyfanshop_be.enums.DebtOrderStatus;
import com.example.onlyfanshop_be.enums.TransferRequestStatus;
import com.example.onlyfanshop_be.enums.WarehouseType;
import com.example.onlyfanshop_be.repository.*;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for Partial Fulfillment with Debt Order creation.
 * 
 * **Feature: inventory-management-ghn, Property 11: Partial Fulfillment with Debt**
 * **Validates: Requirements 5.6, 6.1**
 * 
 * Property: For any Transfer_Request where total available quantity is less than
 * requested quantity, approving the request SHALL create a Debt_Order with
 * owedQuantity equal to (requested quantity - fulfilled quantity).
 */
class PartialFulfillmentWithDebtPropertyTest {

    /**
     * **Feature: inventory-management-ghn, Property 11: Partial Fulfillment with Debt**
     * **Validates: Requirements 5.6, 6.1**
     * 
     * Property: When total available quantity is less than requested quantity,
     * a Debt_Order SHALL be created with owedQuantity equal to the shortage.
     */
    @Property(tries = 100)
    void debtOrderCreatedWithCorrectOwedQuantity(
            @ForAll @IntRange(min = 1, max = 10000) long requestId,
            @ForAll @IntRange(min = 1, max = 10000) long productId,
            @ForAll @IntRange(min = 20, max = 30) int requestedQuantity,
            @ForAll @IntRange(min = 10, max = 20) int storeId,
            @ForAll @IntRange(min = 1, max = 15) int availableQuantity) {
        
        // Ensure available quantity is less than requested (partial fulfillment scenario)
        Assume.that(availableQuantity < requestedQuantity);
        Assume.that(availableQuantity > 0); // At least some quantity available
        
        int expectedShortage = requestedQuantity - availableQuantity;
        
        // Create mock repositories
        TransferRequestRepository transferRequestRepository = mock(TransferRequestRepository.class);
        TransferRequestItemRepository transferRequestItemRepository = mock(TransferRequestItemRepository.class);
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        InventoryLogRepository inventoryLogRepository = mock(InventoryLogRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        StoreLocationRepository storeLocationRepository = mock(StoreLocationRepository.class);
        IDebtOrderService debtOrderService = mock(IDebtOrderService.class);
        
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
        
        // Setup: Main Warehouse with limited quantity
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
        
        InventoryItem mainInventory = InventoryItem.builder()
                .id(1L)
                .warehouseId(mainWarehouseId)
                .productId(productId)
                .quantity(availableQuantity)
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
        
        // Capture debt order creation call
        Map<Long, Integer> capturedShortages = new HashMap<>();
        when(debtOrderService.createDebtOrder(any(TransferRequest.class), anyMap()))
                .thenAnswer(inv -> {
                    Map<Long, Integer> shortages = inv.getArgument(1);
                    capturedShortages.putAll(shortages);
                    return DebtOrderDTO.builder()
                            .id(1L)
                            .transferRequestId(requestId)
                            .status(DebtOrderStatus.PENDING)
                            .build();
                });
        
        // Create the fulfillment service
        FulfillmentService fulfillmentService = new FulfillmentService(
                transferRequestRepository,
                transferRequestItemRepository,
                warehouseRepository,
                inventoryItemRepository,
                inventoryLogRepository,
                productRepository,
                storeLocationRepository,
                debtOrderService
        );
        
        // Execute: Fulfill the request
        FulfillmentResult result = fulfillmentService.fulfill(request);
        
        // Verify: Debt order was created
        verify(debtOrderService).createDebtOrder(any(TransferRequest.class), anyMap());
        
        // Verify: Shortage quantity is correct
        assertThat(capturedShortages)
                .as("Debt order should contain the product with shortage")
                .containsKey(productId);
        
        assertThat(capturedShortages.get(productId))
                .as("Owed quantity should equal (requested - fulfilled)")
                .isEqualTo(expectedShortage);
        
        // Verify: Result indicates partial fulfillment
        assertThat(result.getFullyFulfilled())
                .as("Request should not be fully fulfilled")
                .isFalse();
        
        assertThat(result.getNewStatus())
                .as("Status should be PARTIAL")
                .isEqualTo(TransferRequestStatus.PARTIAL);
    }

    /**
     * **Feature: inventory-management-ghn, Property 11: Partial Fulfillment with Debt**
     * **Validates: Requirements 5.6, 6.1**
     * 
     * Property: When multiple products have shortages, the Debt_Order SHALL contain
     * all products with their respective shortage quantities.
     */
    @Property(tries = 100)
    void debtOrderContainsAllProductShortages(
            @ForAll @IntRange(min = 1, max = 10000) long requestId,
            @ForAll @IntRange(min = 10, max = 20) int storeId,
            @ForAll("multipleProductShortages") List<ProductShortageData> productShortages) {
        
        // Ensure at least one product has shortage
        Assume.that(productShortages.stream().anyMatch(p -> p.availableQuantity < p.requestedQuantity));
        // Ensure at least some quantity is available (partial fulfillment, not zero)
        Assume.that(productShortages.stream().anyMatch(p -> p.availableQuantity > 0));
        
        // Create mock repositories
        TransferRequestRepository transferRequestRepository = mock(TransferRequestRepository.class);
        TransferRequestItemRepository transferRequestItemRepository = mock(TransferRequestItemRepository.class);
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        InventoryLogRepository inventoryLogRepository = mock(InventoryLogRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        StoreLocationRepository storeLocationRepository = mock(StoreLocationRepository.class);
        IDebtOrderService debtOrderService = mock(IDebtOrderService.class);
        
        // Setup: Transfer Request with multiple items
        TransferRequest request = TransferRequest.builder()
                .id(requestId)
                .storeId(storeId)
                .status(TransferRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        
        List<TransferRequestItem> items = new ArrayList<>();
        for (int i = 0; i < productShortages.size(); i++) {
            ProductShortageData data = productShortages.get(i);
            TransferRequestItem item = TransferRequestItem.builder()
                    .id((long) (i + 1))
                    .transferRequestId(requestId)
                    .productId(data.productId)
                    .requestedQuantity(data.requestedQuantity)
                    .fulfilledQuantity(0)
                    .build();
            items.add(item);
        }
        
        request.setItems(items);
        
        when(transferRequestRepository.findById(requestId)).thenReturn(Optional.of(request));
        when(transferRequestItemRepository.findByTransferRequestId(requestId)).thenReturn(items);
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
        
        // Setup: Inventory for each product
        for (ProductShortageData data : productShortages) {
            InventoryItem inventory = InventoryItem.builder()
                    .id(data.productId)
                    .warehouseId(mainWarehouseId)
                    .productId(data.productId)
                    .quantity(data.availableQuantity)
                    .reservedQuantity(0)
                    .build();
            
            when(inventoryItemRepository.findByWarehouseIdAndProductId(mainWarehouseId, data.productId))
                    .thenReturn(Optional.of(inventory));
            
            Product product = Product.builder()
                    .id(data.productId)
                    .name("Product " + data.productId)
                    .sku("SKU-" + data.productId)
                    .build();
            when(productRepository.findById((int) data.productId)).thenReturn(Optional.of(product));
        }
        
        when(inventoryItemRepository.save(any(InventoryItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(inventoryLogRepository.save(any(InventoryLog.class))).thenAnswer(inv -> inv.getArgument(0));
        
        // Capture debt order creation call
        Map<Long, Integer> capturedShortages = new HashMap<>();
        when(debtOrderService.createDebtOrder(any(TransferRequest.class), anyMap()))
                .thenAnswer(inv -> {
                    Map<Long, Integer> shortages = inv.getArgument(1);
                    capturedShortages.putAll(shortages);
                    return DebtOrderDTO.builder()
                            .id(1L)
                            .transferRequestId(requestId)
                            .status(DebtOrderStatus.PENDING)
                            .build();
                });
        
        // Create the fulfillment service
        FulfillmentService fulfillmentService = new FulfillmentService(
                transferRequestRepository,
                transferRequestItemRepository,
                warehouseRepository,
                inventoryItemRepository,
                inventoryLogRepository,
                productRepository,
                storeLocationRepository,
                debtOrderService
        );
        
        // Execute: Fulfill the request
        FulfillmentResult result = fulfillmentService.fulfill(request);
        
        // Calculate expected shortages
        Map<Long, Integer> expectedShortages = new HashMap<>();
        for (ProductShortageData data : productShortages) {
            int shortage = data.requestedQuantity - Math.min(data.availableQuantity, data.requestedQuantity);
            if (shortage > 0) {
                expectedShortages.put(data.productId, shortage);
            }
        }
        
        // Verify: All products with shortages are in the debt order
        if (!expectedShortages.isEmpty()) {
            verify(debtOrderService).createDebtOrder(any(TransferRequest.class), anyMap());
            
            for (Map.Entry<Long, Integer> entry : expectedShortages.entrySet()) {
                assertThat(capturedShortages)
                        .as("Debt order should contain product %d with shortage", entry.getKey())
                        .containsKey(entry.getKey());
                
                assertThat(capturedShortages.get(entry.getKey()))
                        .as("Product %d shortage should be %d", entry.getKey(), entry.getValue())
                        .isEqualTo(entry.getValue());
            }
        }
    }

    /**
     * **Feature: inventory-management-ghn, Property 11: Partial Fulfillment with Debt**
     * **Validates: Requirements 5.6, 6.1**
     * 
     * Property: When request is fully fulfilled (no shortage), NO Debt_Order SHALL be created.
     */
    @Property(tries = 100)
    void noDebtOrderWhenFullyFulfilled(
            @ForAll @IntRange(min = 1, max = 10000) long requestId,
            @ForAll @IntRange(min = 1, max = 10000) long productId,
            @ForAll @IntRange(min = 1, max = 30) int requestedQuantity,
            @ForAll @IntRange(min = 10, max = 20) int storeId) {
        
        // Ensure available quantity is sufficient (full fulfillment scenario)
        int availableQuantity = requestedQuantity + 10;
        
        // Create mock repositories
        TransferRequestRepository transferRequestRepository = mock(TransferRequestRepository.class);
        TransferRequestItemRepository transferRequestItemRepository = mock(TransferRequestItemRepository.class);
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        InventoryLogRepository inventoryLogRepository = mock(InventoryLogRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        StoreLocationRepository storeLocationRepository = mock(StoreLocationRepository.class);
        IDebtOrderService debtOrderService = mock(IDebtOrderService.class);
        
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
        
        // Setup: Main Warehouse with sufficient quantity
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
        
        InventoryItem mainInventory = InventoryItem.builder()
                .id(1L)
                .warehouseId(mainWarehouseId)
                .productId(productId)
                .quantity(availableQuantity)
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
                storeLocationRepository,
                debtOrderService
        );
        
        // Execute: Fulfill the request
        FulfillmentResult result = fulfillmentService.fulfill(request);
        
        // Verify: No debt order was created
        verify(debtOrderService, never()).createDebtOrder(any(TransferRequest.class), anyMap());
        
        // Verify: Result indicates full fulfillment
        assertThat(result.getFullyFulfilled())
                .as("Request should be fully fulfilled")
                .isTrue();
        
        assertThat(result.getNewStatus())
                .as("Status should be COMPLETED")
                .isEqualTo(TransferRequestStatus.COMPLETED);
        
        assertThat(result.getDebtOrderId())
                .as("No debt order ID should be set")
                .isNull();
    }

    /**
     * **Feature: inventory-management-ghn, Property 11: Partial Fulfillment with Debt**
     * **Validates: Requirements 5.6, 6.1**
     * 
     * Property: The sum of fulfilled quantity and owed quantity SHALL equal
     * the original requested quantity.
     */
    @Property(tries = 100)
    void fulfilledPlusOwedEqualsRequested(
            @ForAll @IntRange(min = 1, max = 10000) long requestId,
            @ForAll @IntRange(min = 1, max = 10000) long productId,
            @ForAll @IntRange(min = 20, max = 30) int requestedQuantity,
            @ForAll @IntRange(min = 10, max = 20) int storeId,
            @ForAll @IntRange(min = 1, max = 19) int availableQuantity) {
        
        // Ensure partial fulfillment scenario
        Assume.that(availableQuantity < requestedQuantity);
        Assume.that(availableQuantity > 0);
        
        // Create mock repositories
        TransferRequestRepository transferRequestRepository = mock(TransferRequestRepository.class);
        TransferRequestItemRepository transferRequestItemRepository = mock(TransferRequestItemRepository.class);
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        InventoryLogRepository inventoryLogRepository = mock(InventoryLogRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        StoreLocationRepository storeLocationRepository = mock(StoreLocationRepository.class);
        IDebtOrderService debtOrderService = mock(IDebtOrderService.class);
        
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
        
        // Setup: Main Warehouse with limited quantity
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
        
        InventoryItem mainInventory = InventoryItem.builder()
                .id(1L)
                .warehouseId(mainWarehouseId)
                .productId(productId)
                .quantity(availableQuantity)
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
        
        // Capture debt order creation call
        Map<Long, Integer> capturedShortages = new HashMap<>();
        when(debtOrderService.createDebtOrder(any(TransferRequest.class), anyMap()))
                .thenAnswer(inv -> {
                    Map<Long, Integer> shortages = inv.getArgument(1);
                    capturedShortages.putAll(shortages);
                    return DebtOrderDTO.builder()
                            .id(1L)
                            .transferRequestId(requestId)
                            .status(DebtOrderStatus.PENDING)
                            .build();
                });
        
        // Create the fulfillment service
        FulfillmentService fulfillmentService = new FulfillmentService(
                transferRequestRepository,
                transferRequestItemRepository,
                warehouseRepository,
                inventoryItemRepository,
                inventoryLogRepository,
                productRepository,
                storeLocationRepository,
                debtOrderService
        );
        
        // Execute: Fulfill the request
        FulfillmentResult result = fulfillmentService.fulfill(request);
        
        // Get fulfilled and owed quantities
        int fulfilledQuantity = result.getFulfilledQuantities().getOrDefault(productId, 0);
        int owedQuantity = capturedShortages.getOrDefault(productId, 0);
        
        // Verify: fulfilled + owed = requested
        assertThat(fulfilledQuantity + owedQuantity)
                .as("Fulfilled quantity (%d) + Owed quantity (%d) should equal requested quantity (%d)",
                        fulfilledQuantity, owedQuantity, requestedQuantity)
                .isEqualTo(requestedQuantity);
    }

    /**
     * Data class for product shortage test data.
     */
    static class ProductShortageData {
        final long productId;
        final int requestedQuantity;
        final int availableQuantity;
        
        ProductShortageData(long productId, int requestedQuantity, int availableQuantity) {
            this.productId = productId;
            this.requestedQuantity = requestedQuantity;
            this.availableQuantity = availableQuantity;
        }
    }

    /**
     * Provides a list of product shortage data for multiple products.
     */
    @Provide
    Arbitrary<List<ProductShortageData>> multipleProductShortages() {
        Arbitrary<ProductShortageData> productData = Combinators.combine(
                Arbitraries.longs().between(1, 10000),
                Arbitraries.integers().between(10, 30),
                Arbitraries.integers().between(0, 25)
        ).as(ProductShortageData::new);
        
        return productData.list().ofMinSize(2).ofMaxSize(5)
                .filter(list -> {
                    // Ensure unique product IDs
                    Set<Long> ids = new HashSet<>();
                    for (ProductShortageData data : list) {
                        if (!ids.add(data.productId)) {
                            return false;
                        }
                    }
                    return true;
                });
    }
}
