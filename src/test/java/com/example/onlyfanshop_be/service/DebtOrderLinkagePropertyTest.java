package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.DebtOrderDTO;
import com.example.onlyfanshop_be.entity.*;
import com.example.onlyfanshop_be.enums.DebtOrderStatus;
import com.example.onlyfanshop_be.enums.TransferRequestStatus;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.repository.*;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for Debt Order Linkage.
 * 
 * **Feature: inventory-management-ghn, Property 12: Debt Order Linkage**
 * **Validates: Requirements 6.2**
 * 
 * Property: For any Debt_Order created from partial fulfillment, the Debt_Order
 * SHALL be linked to exactly one Transfer_Request, and the Transfer_Request
 * SHALL reference that Debt_Order.
 */
class DebtOrderLinkagePropertyTest {

    /**
     * **Feature: inventory-management-ghn, Property 12: Debt Order Linkage**
     * **Validates: Requirements 6.2**
     * 
     * Property: When a Debt_Order is created, it SHALL be linked to exactly one
     * Transfer_Request via the transferRequestId field.
     */
    @Property(tries = 100)
    void debtOrderLinkedToExactlyOneTransferRequest(
            @ForAll @IntRange(min = 1, max = 10000) long transferRequestId,
            @ForAll @IntRange(min = 1, max = 10000) long productId,
            @ForAll @IntRange(min = 1, max = 30) int shortageQuantity,
            @ForAll @IntRange(min = 10, max = 20) int storeId) {
        
        // Create mock repositories
        DebtOrderRepository debtOrderRepository = mock(DebtOrderRepository.class);
        DebtItemRepository debtItemRepository = mock(DebtItemRepository.class);
        TransferRequestRepository transferRequestRepository = mock(TransferRequestRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        StoreLocationRepository storeLocationRepository = mock(StoreLocationRepository.class);
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        InventoryLogRepository inventoryLogRepository = mock(InventoryLogRepository.class);
        
        // Setup: Transfer Request
        TransferRequest request = TransferRequest.builder()
                .id(transferRequestId)
                .storeId(storeId)
                .status(TransferRequestStatus.PARTIAL)
                .createdAt(LocalDateTime.now())
                .build();
        
        // Setup: No existing debt order for this transfer request
        when(debtOrderRepository.findByTransferRequestId(transferRequestId))
                .thenReturn(Optional.empty());
        
        // Capture saved debt order
        final DebtOrder[] capturedDebtOrder = new DebtOrder[1];
        when(debtOrderRepository.save(any(DebtOrder.class))).thenAnswer(inv -> {
            DebtOrder order = inv.getArgument(0);
            order.setId(1L);
            order.setCreatedAt(LocalDateTime.now());
            capturedDebtOrder[0] = order;
            return order;
        });
        
        when(debtItemRepository.save(any(DebtItem.class))).thenAnswer(inv -> {
            DebtItem item = inv.getArgument(0);
            item.setId(1L);
            return item;
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
        
        // Execute: Create debt order
        Map<Long, Integer> shortageQuantities = new HashMap<>();
        shortageQuantities.put(productId, shortageQuantity);
        
        DebtOrderDTO result = debtOrderService.createDebtOrder(request, shortageQuantities);
        
        // Verify: Debt order is linked to exactly one transfer request
        assertThat(capturedDebtOrder[0])
                .as("Debt order should be saved")
                .isNotNull();
        
        assertThat(capturedDebtOrder[0].getTransferRequestId())
                .as("Debt order should be linked to the transfer request")
                .isEqualTo(transferRequestId);
        
        assertThat(result.getTransferRequestId())
                .as("Returned DTO should contain the transfer request ID")
                .isEqualTo(transferRequestId);
    }

    /**
     * **Feature: inventory-management-ghn, Property 12: Debt Order Linkage**
     * **Validates: Requirements 6.2**
     * 
     * Property: A Transfer_Request SHALL have at most one Debt_Order linked to it.
     * Creating a second Debt_Order for the same Transfer_Request SHALL be rejected.
     */
    @Property(tries = 100)
    void transferRequestHasAtMostOneDebtOrder(
            @ForAll @IntRange(min = 1, max = 10000) long transferRequestId,
            @ForAll @IntRange(min = 1, max = 10000) long productId,
            @ForAll @IntRange(min = 1, max = 30) int shortageQuantity,
            @ForAll @IntRange(min = 10, max = 20) int storeId) {
        
        // Create mock repositories
        DebtOrderRepository debtOrderRepository = mock(DebtOrderRepository.class);
        DebtItemRepository debtItemRepository = mock(DebtItemRepository.class);
        TransferRequestRepository transferRequestRepository = mock(TransferRequestRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        StoreLocationRepository storeLocationRepository = mock(StoreLocationRepository.class);
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        InventoryLogRepository inventoryLogRepository = mock(InventoryLogRepository.class);
        
        // Setup: Transfer Request
        TransferRequest request = TransferRequest.builder()
                .id(transferRequestId)
                .storeId(storeId)
                .status(TransferRequestStatus.PARTIAL)
                .createdAt(LocalDateTime.now())
                .build();
        
        // Setup: Existing debt order for this transfer request
        DebtOrder existingDebtOrder = DebtOrder.builder()
                .id(1L)
                .transferRequestId(transferRequestId)
                .status(DebtOrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
        
        when(debtOrderRepository.findByTransferRequestId(transferRequestId))
                .thenReturn(Optional.of(existingDebtOrder));
        
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
        
        // Execute & Verify: Creating a second debt order should fail
        Map<Long, Integer> shortageQuantities = new HashMap<>();
        shortageQuantities.put(productId, shortageQuantity);
        
        assertThatThrownBy(() -> debtOrderService.createDebtOrder(request, shortageQuantities))
                .as("Creating a second debt order for the same transfer request should fail")
                .isInstanceOf(AppException.class);
        
        // Verify: No new debt order was saved
        verify(debtOrderRepository, never()).save(any(DebtOrder.class));
    }

    /**
     * **Feature: inventory-management-ghn, Property 12: Debt Order Linkage**
     * **Validates: Requirements 6.2**
     * 
     * Property: The Debt_Order can be retrieved by its linked Transfer_Request ID.
     */
    @Property(tries = 100)
    void debtOrderRetrievableByTransferRequestId(
            @ForAll @IntRange(min = 1, max = 10000) long debtOrderId,
            @ForAll @IntRange(min = 1, max = 10000) long transferRequestId,
            @ForAll @IntRange(min = 10, max = 20) int storeId) {
        
        // Create mock repositories
        DebtOrderRepository debtOrderRepository = mock(DebtOrderRepository.class);
        DebtItemRepository debtItemRepository = mock(DebtItemRepository.class);
        TransferRequestRepository transferRequestRepository = mock(TransferRequestRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        StoreLocationRepository storeLocationRepository = mock(StoreLocationRepository.class);
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        InventoryLogRepository inventoryLogRepository = mock(InventoryLogRepository.class);
        
        // Setup: Existing debt order linked to transfer request
        DebtOrder debtOrder = DebtOrder.builder()
                .id(debtOrderId)
                .transferRequestId(transferRequestId)
                .status(DebtOrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .items(new ArrayList<>())
                .build();
        
        // Setup: Transfer request
        TransferRequest transferRequest = TransferRequest.builder()
                .id(transferRequestId)
                .storeId(storeId)
                .status(TransferRequestStatus.PARTIAL)
                .createdAt(LocalDateTime.now())
                .build();
        
        when(debtOrderRepository.findByTransferRequestId(transferRequestId))
                .thenReturn(Optional.of(debtOrder));
        when(transferRequestRepository.findById(transferRequestId))
                .thenReturn(Optional.of(transferRequest));
        when(debtItemRepository.findByDebtOrderId(debtOrderId))
                .thenReturn(new ArrayList<>());
        when(storeLocationRepository.findById(storeId))
                .thenReturn(Optional.of(StoreLocation.builder().id(storeId).name("Test Store").build()));
        
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
        
        // Execute: Retrieve debt order by transfer request ID
        DebtOrderDTO result = debtOrderService.getDebtOrderByTransferRequestId(transferRequestId);
        
        // Verify: Retrieved debt order matches the expected one
        assertThat(result)
                .as("Debt order should be retrievable by transfer request ID")
                .isNotNull();
        
        assertThat(result.getId())
                .as("Retrieved debt order ID should match")
                .isEqualTo(debtOrderId);
        
        assertThat(result.getTransferRequestId())
                .as("Retrieved debt order should be linked to the correct transfer request")
                .isEqualTo(transferRequestId);
    }

    /**
     * **Feature: inventory-management-ghn, Property 12: Debt Order Linkage**
     * **Validates: Requirements 6.2**
     * 
     * Property: For any Debt_Order with multiple items, all items SHALL be linked
     * to the same Debt_Order, which is linked to exactly one Transfer_Request.
     */
    @Property(tries = 100)
    void allDebtItemsLinkedToSameDebtOrderAndTransferRequest(
            @ForAll @IntRange(min = 1, max = 10000) long transferRequestId,
            @ForAll @IntRange(min = 10, max = 20) int storeId,
            @ForAll("multipleProductShortages") List<ProductShortageData> productShortages) {
        
        // Ensure at least one product with shortage
        Assume.that(!productShortages.isEmpty());
        Assume.that(productShortages.stream().allMatch(p -> p.shortageQuantity > 0));
        
        // Create mock repositories
        DebtOrderRepository debtOrderRepository = mock(DebtOrderRepository.class);
        DebtItemRepository debtItemRepository = mock(DebtItemRepository.class);
        TransferRequestRepository transferRequestRepository = mock(TransferRequestRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        StoreLocationRepository storeLocationRepository = mock(StoreLocationRepository.class);
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        InventoryLogRepository inventoryLogRepository = mock(InventoryLogRepository.class);
        
        // Setup: Transfer Request
        TransferRequest request = TransferRequest.builder()
                .id(transferRequestId)
                .storeId(storeId)
                .status(TransferRequestStatus.PARTIAL)
                .createdAt(LocalDateTime.now())
                .build();
        
        // Setup: No existing debt order
        when(debtOrderRepository.findByTransferRequestId(transferRequestId))
                .thenReturn(Optional.empty());
        
        // Capture saved debt order and items
        final DebtOrder[] capturedDebtOrder = new DebtOrder[1];
        final List<DebtItem> capturedDebtItems = new ArrayList<>();
        
        when(debtOrderRepository.save(any(DebtOrder.class))).thenAnswer(inv -> {
            DebtOrder order = inv.getArgument(0);
            order.setId(1L);
            order.setCreatedAt(LocalDateTime.now());
            capturedDebtOrder[0] = order;
            return order;
        });
        
        when(debtItemRepository.save(any(DebtItem.class))).thenAnswer(inv -> {
            DebtItem item = inv.getArgument(0);
            item.setId((long) (capturedDebtItems.size() + 1));
            capturedDebtItems.add(item);
            return item;
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
        
        // Execute: Create debt order with multiple items
        Map<Long, Integer> shortageQuantities = new HashMap<>();
        for (ProductShortageData data : productShortages) {
            shortageQuantities.put(data.productId, data.shortageQuantity);
        }
        
        DebtOrderDTO result = debtOrderService.createDebtOrder(request, shortageQuantities);
        
        // Verify: All items are linked to the same debt order
        assertThat(capturedDebtItems)
                .as("All debt items should be saved")
                .hasSize(productShortages.size());
        
        Long debtOrderId = capturedDebtOrder[0].getId();
        for (DebtItem item : capturedDebtItems) {
            assertThat(item.getDebtOrderId())
                    .as("Each debt item should be linked to the same debt order")
                    .isEqualTo(debtOrderId);
        }
        
        // Verify: The debt order is linked to the transfer request
        assertThat(capturedDebtOrder[0].getTransferRequestId())
                .as("Debt order should be linked to the transfer request")
                .isEqualTo(transferRequestId);
    }

    /**
     * Data class for product shortage test data.
     */
    static class ProductShortageData {
        final long productId;
        final int shortageQuantity;
        
        ProductShortageData(long productId, int shortageQuantity) {
            this.productId = productId;
            this.shortageQuantity = shortageQuantity;
        }
    }

    /**
     * Provides a list of product shortage data for multiple products.
     */
    @Provide
    Arbitrary<List<ProductShortageData>> multipleProductShortages() {
        Arbitrary<ProductShortageData> productData = Combinators.combine(
                Arbitraries.longs().between(1, 10000),
                Arbitraries.integers().between(1, 30)
        ).as(ProductShortageData::new);
        
        return productData.list().ofMinSize(1).ofMaxSize(5)
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
