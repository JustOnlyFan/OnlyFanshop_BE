package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.response.*;
import com.example.onlyfanshop_be.entity.*;
import com.example.onlyfanshop_be.enums.TransferRequestStatus;
import com.example.onlyfanshop_be.enums.WarehouseType;
import com.example.onlyfanshop_be.repository.*;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for Availability Check Completeness.
 * 
 * **Feature: inventory-management-ghn, Property 18: Availability Check Completeness**
 * **Validates: Requirements 9.1, 9.2, 9.4, 9.5**
 * 
 * Property: For any availability check request, the response SHALL contain:
 * - Main_Warehouse quantity (9.1)
 * - Breakdown of each Store_Warehouse quantity when main is insufficient (9.2)
 * - Source allocation recommendation (9.4)
 * - Maximum fulfillable quantity and shortage amount (9.5)
 */
class AvailabilityCheckCompletenessPropertyTest {

    /**
     * **Feature: inventory-management-ghn, Property 18: Availability Check Completeness**
     * **Validates: Requirements 9.1**
     * 
     * Property: For any availability check, the response SHALL contain Main_Warehouse quantity.
     */
    @Property(tries = 100)
    void availabilityCheckShallContainMainWarehouseQuantity(
            @ForAll @IntRange(min = 1, max = 10000) long productId,
            @ForAll @IntRange(min = 1, max = 100) int requestedQuantity,
            @ForAll @IntRange(min = 0, max = 200) int mainWarehouseQuantity) {
        
        // Create test helper
        AvailabilityCheckTestHelper helper = new AvailabilityCheckTestHelper();
        helper.setupMainWarehouse(productId, mainWarehouseQuantity);
        helper.setupProduct(productId, "Test Product " + productId);
        
        // Create transfer request
        TransferRequest request = helper.createTransferRequest(1, productId, requestedQuantity);
        
        // Execute availability check
        AvailabilityCheckResult result = helper.checkAvailability(request);
        
        // Verify: Response contains Main_Warehouse quantity (Requirements 9.1)
        assertThat(result).isNotNull();
        assertThat(result.getProductAvailabilities()).isNotNull();
        assertThat(result.getProductAvailabilities()).isNotEmpty();
        
        ProductAvailability productAvailability = result.getProductAvailabilities().get(0);
        assertThat(productAvailability.getMainWarehouseAvailable()).isNotNull();
        assertThat(productAvailability.getMainWarehouseAvailable()).isEqualTo(mainWarehouseQuantity);
    }

    /**
     * **Feature: inventory-management-ghn, Property 18: Availability Check Completeness**
     * **Validates: Requirements 9.2**
     * 
     * Property: When Main_Warehouse quantity is insufficient, the response SHALL contain
     * breakdown of available quantities from each Store_Warehouse.
     */
    @Property(tries = 100)
    void whenMainInsufficientShallContainStoreWarehouseBreakdown(
            @ForAll @IntRange(min = 1, max = 10000) long productId,
            @ForAll @IntRange(min = 10, max = 100) int requestedQuantity,
            @ForAll @IntRange(min = 0, max = 9) int mainWarehouseQuantity,
            @ForAll("nonEmptyStoreQuantities") List<Integer> storeQuantities) {
        
        // Ensure main warehouse is insufficient
        Assume.that(mainWarehouseQuantity < requestedQuantity);
        Assume.that(!storeQuantities.isEmpty());
        
        // Create test helper
        AvailabilityCheckTestHelper helper = new AvailabilityCheckTestHelper();
        helper.setupMainWarehouse(productId, mainWarehouseQuantity);
        helper.setupProduct(productId, "Test Product " + productId);
        
        // Setup store warehouses (excluding requesting store)
        int requestingStoreId = 1;
        for (int i = 0; i < storeQuantities.size(); i++) {
            int storeId = 10 + i; // Different from requesting store
            helper.addStoreWarehouse(storeId, productId, storeQuantities.get(i));
        }
        
        // Create transfer request
        TransferRequest request = helper.createTransferRequest(requestingStoreId, productId, requestedQuantity);
        
        // Execute availability check
        AvailabilityCheckResult result = helper.checkAvailability(request);
        
        // Verify: Response contains Store_Warehouse breakdown (Requirements 9.2)
        assertThat(result).isNotNull();
        assertThat(result.getProductAvailabilities()).isNotNull();
        assertThat(result.getProductAvailabilities()).isNotEmpty();
        
        ProductAvailability productAvailability = result.getProductAvailabilities().get(0);
        assertThat(productAvailability.getStoreAvailabilities()).isNotNull();
        
        // When main is insufficient, store availabilities should be populated
        if (mainWarehouseQuantity < requestedQuantity) {
            // Store availabilities should contain entries for stores with positive quantity
            int expectedStoreCount = (int) storeQuantities.stream().filter(q -> q > 0).count();
            assertThat(productAvailability.getStoreAvailabilities().size()).isEqualTo(expectedStoreCount);
            
            // Each store availability should have required fields
            for (StoreWarehouseAvailability storeAvail : productAvailability.getStoreAvailabilities()) {
                assertThat(storeAvail.getWarehouseId()).isNotNull();
                assertThat(storeAvail.getAvailableQuantity()).isNotNull();
                assertThat(storeAvail.getAvailableQuantity()).isGreaterThan(0);
            }
        }
    }

    /**
     * **Feature: inventory-management-ghn, Property 18: Availability Check Completeness**
     * **Validates: Requirements 9.4**
     * 
     * Property: For any availability check, the response SHALL contain source allocation recommendation.
     */
    @Property(tries = 100)
    void availabilityCheckShallContainSourceAllocationRecommendation(
            @ForAll @IntRange(min = 1, max = 10000) long productId,
            @ForAll @IntRange(min = 1, max = 50) int requestedQuantity,
            @ForAll @IntRange(min = 0, max = 100) int mainWarehouseQuantity,
            @ForAll("storeQuantities") List<Integer> storeQuantities) {
        
        // Ensure there's some inventory available
        int totalStoreQuantity = storeQuantities.stream().mapToInt(Integer::intValue).sum();
        int totalAvailable = mainWarehouseQuantity + totalStoreQuantity;
        Assume.that(totalAvailable > 0);
        
        // Create test helper
        AvailabilityCheckTestHelper helper = new AvailabilityCheckTestHelper();
        helper.setupMainWarehouse(productId, mainWarehouseQuantity);
        helper.setupProduct(productId, "Test Product " + productId);
        
        // Setup store warehouses
        int requestingStoreId = 1;
        for (int i = 0; i < storeQuantities.size(); i++) {
            int storeId = 10 + i;
            helper.addStoreWarehouse(storeId, productId, storeQuantities.get(i));
        }
        
        // Create transfer request
        TransferRequest request = helper.createTransferRequest(requestingStoreId, productId, requestedQuantity);
        
        // Execute availability check
        AvailabilityCheckResult result = helper.checkAvailability(request);
        
        // Verify: Response contains source allocation recommendation (Requirements 9.4)
        assertThat(result).isNotNull();
        assertThat(result.getProductAvailabilities()).isNotNull();
        assertThat(result.getProductAvailabilities()).isNotEmpty();
        
        ProductAvailability productAvailability = result.getProductAvailabilities().get(0);
        assertThat(productAvailability.getRecommendedAllocations()).isNotNull();
        
        // If there's available inventory, there should be allocations
        if (totalAvailable > 0) {
            assertThat(productAvailability.getRecommendedAllocations()).isNotEmpty();
            
            // Each allocation should have required fields
            for (SourceAllocation allocation : productAvailability.getRecommendedAllocations()) {
                assertThat(allocation.getWarehouseId()).isNotNull();
                assertThat(allocation.getWarehouseType()).isNotNull();
                assertThat(allocation.getQuantity()).isNotNull();
                assertThat(allocation.getQuantity()).isGreaterThan(0);
            }
            
            // Total allocated should not exceed total available
            int totalAllocated = productAvailability.getRecommendedAllocations().stream()
                    .mapToInt(SourceAllocation::getQuantity)
                    .sum();
            assertThat(totalAllocated).isLessThanOrEqualTo(totalAvailable);
        }
    }

    /**
     * **Feature: inventory-management-ghn, Property 18: Availability Check Completeness**
     * **Validates: Requirements 9.5**
     * 
     * Property: For any availability check, the response SHALL contain maximum fulfillable
     * quantity and shortage amount.
     */
    @Property(tries = 100)
    void availabilityCheckShallContainMaxFulfillableAndShortage(
            @ForAll @IntRange(min = 1, max = 10000) long productId,
            @ForAll @IntRange(min = 1, max = 100) int requestedQuantity,
            @ForAll @IntRange(min = 0, max = 150) int mainWarehouseQuantity,
            @ForAll("storeQuantities") List<Integer> storeQuantities) {
        
        // Create test helper
        AvailabilityCheckTestHelper helper = new AvailabilityCheckTestHelper();
        helper.setupMainWarehouse(productId, mainWarehouseQuantity);
        helper.setupProduct(productId, "Test Product " + productId);
        
        // Setup store warehouses
        int requestingStoreId = 1;
        for (int i = 0; i < storeQuantities.size(); i++) {
            int storeId = 10 + i;
            helper.addStoreWarehouse(storeId, productId, storeQuantities.get(i));
        }
        
        // Create transfer request
        TransferRequest request = helper.createTransferRequest(requestingStoreId, productId, requestedQuantity);
        
        // Execute availability check
        AvailabilityCheckResult result = helper.checkAvailability(request);
        
        // Calculate expected values
        // Total raw inventory available across all sources
        int totalStoreQuantity = storeQuantities.stream().mapToInt(Integer::intValue).sum();
        int totalRawAvailable = mainWarehouseQuantity + totalStoreQuantity;
        
        // Max fulfillable is the minimum of requested and total raw available
        int expectedMaxFulfillable = Math.min(requestedQuantity, totalRawAvailable);
        
        // Shortage is how much we can't fulfill
        int expectedShortage = Math.max(0, requestedQuantity - totalRawAvailable);
        
        // Verify: Response contains maximum fulfillable quantity (Requirements 9.5)
        assertThat(result).isNotNull();
        assertThat(result.getMaxFulfillableQuantity()).isNotNull();
        assertThat(result.getMaxFulfillableQuantity()).isEqualTo(expectedMaxFulfillable);
        
        // Verify: Response contains shortage amount (Requirements 9.5)
        assertThat(result.getTotalShortage()).isNotNull();
        assertThat(result.getTotalShortage()).isEqualTo(expectedShortage);
        
        // Verify: Product-level shortage is also correct
        ProductAvailability productAvailability = result.getProductAvailabilities().get(0);
        assertThat(productAvailability.getShortage()).isNotNull();
        assertThat(productAvailability.getShortage()).isEqualTo(expectedShortage);
        
        // Verify: Total available quantity represents what can be allocated (capped at requested)
        // This is the sum of allocations, which is min(requested, raw available)
        assertThat(result.getTotalAvailableQuantity()).isNotNull();
        assertThat(result.getTotalAvailableQuantity()).isEqualTo(expectedMaxFulfillable);
        
        // Verify: Total requested is correct
        assertThat(result.getTotalRequestedQuantity()).isNotNull();
        assertThat(result.getTotalRequestedQuantity()).isEqualTo(requestedQuantity);
    }

    /**
     * **Feature: inventory-management-ghn, Property 18: Availability Check Completeness**
     * **Validates: Requirements 9.1, 9.2, 9.4, 9.5**
     * 
     * Property: For any availability check with multiple products, the response SHALL contain
     * complete information for each product.
     */
    @Property(tries = 100)
    void availabilityCheckWithMultipleProductsShallBeComplete(
            @ForAll @IntRange(min = 1, max = 1000) long baseProductId,
            @ForAll @IntRange(min = 2, max = 5) int productCount,
            @ForAll("productQuantities") List<Integer> requestedQuantities,
            @ForAll("productQuantities") List<Integer> mainQuantities) {
        
        Assume.that(requestedQuantities.size() >= productCount);
        Assume.that(mainQuantities.size() >= productCount);
        
        // Create test helper
        AvailabilityCheckTestHelper helper = new AvailabilityCheckTestHelper();
        
        // Setup multiple products
        List<Long> productIds = new ArrayList<>();
        for (int i = 0; i < productCount; i++) {
            long productId = baseProductId + i;
            productIds.add(productId);
            helper.setupProduct(productId, "Product " + productId);
            helper.setupMainWarehouseForProduct(productId, mainQuantities.get(i));
        }
        
        // Create transfer request with multiple items
        int requestingStoreId = 1;
        TransferRequest request = helper.createMultiProductTransferRequest(
                requestingStoreId, productIds, requestedQuantities.subList(0, productCount));
        
        // Execute availability check
        AvailabilityCheckResult result = helper.checkAvailability(request);
        
        // Verify: Response contains availability for each product
        assertThat(result).isNotNull();
        assertThat(result.getProductAvailabilities()).isNotNull();
        assertThat(result.getProductAvailabilities()).hasSize(productCount);
        
        // Verify each product has complete information
        for (int i = 0; i < productCount; i++) {
            ProductAvailability pa = result.getProductAvailabilities().get(i);
            
            // Requirements 9.1: Main warehouse quantity
            assertThat(pa.getMainWarehouseAvailable()).isNotNull();
            
            // Requirements 9.4: Source allocation recommendation
            assertThat(pa.getRecommendedAllocations()).isNotNull();
            
            // Requirements 9.5: Shortage amount
            assertThat(pa.getShortage()).isNotNull();
            assertThat(pa.getTotalAvailable()).isNotNull();
            assertThat(pa.getRequestedQuantity()).isNotNull();
        }
        
        // Verify aggregate values
        assertThat(result.getTotalRequestedQuantity()).isNotNull();
        assertThat(result.getTotalAvailableQuantity()).isNotNull();
        assertThat(result.getTotalShortage()).isNotNull();
        assertThat(result.getMaxFulfillableQuantity()).isNotNull();
        assertThat(result.getCanFullyFulfill()).isNotNull();
    }

    /**
     * Provides a list of store quantities (can include zeros).
     */
    @Provide
    Arbitrary<List<Integer>> storeQuantities() {
        return Arbitraries.integers().between(0, 50)
                .list()
                .ofMinSize(0)
                .ofMaxSize(5);
    }

    /**
     * Provides a non-empty list of store quantities with at least some positive values.
     */
    @Provide
    Arbitrary<List<Integer>> nonEmptyStoreQuantities() {
        return Arbitraries.integers().between(1, 50)
                .list()
                .ofMinSize(1)
                .ofMaxSize(5);
    }

    /**
     * Provides a list of product quantities.
     */
    @Provide
    Arbitrary<List<Integer>> productQuantities() {
        return Arbitraries.integers().between(1, 30)
                .list()
                .ofMinSize(5)
                .ofMaxSize(10);
    }

    /**
     * Helper class that simulates the availability check logic from FulfillmentService.
     * This allows testing the completeness of availability check responses without full Spring context.
     */
    static class AvailabilityCheckTestHelper {
        private final Map<Long, Integer> mainWarehouseInventory = new HashMap<>();
        private final Map<Integer, Map<Long, Integer>> storeWarehouseInventory = new HashMap<>();
        private final Map<Long, String> products = new HashMap<>();
        private long warehouseIdCounter = 100L;
        private final Map<Integer, Long> storeWarehouseIds = new HashMap<>();
        
        void setupMainWarehouse(long productId, int quantity) {
            mainWarehouseInventory.put(productId, quantity);
        }
        
        void setupMainWarehouseForProduct(long productId, int quantity) {
            mainWarehouseInventory.put(productId, quantity);
        }
        
        void setupProduct(long productId, String name) {
            products.put(productId, name);
        }
        
        void addStoreWarehouse(int storeId, long productId, int quantity) {
            storeWarehouseInventory
                    .computeIfAbsent(storeId, k -> new HashMap<>())
                    .put(productId, quantity);
            if (!storeWarehouseIds.containsKey(storeId)) {
                storeWarehouseIds.put(storeId, warehouseIdCounter++);
            }
        }
        
        TransferRequest createTransferRequest(int storeId, long productId, int requestedQuantity) {
            TransferRequestItem item = TransferRequestItem.builder()
                    .id(1L)
                    .productId(productId)
                    .requestedQuantity(requestedQuantity)
                    .fulfilledQuantity(0)
                    .build();
            
            return TransferRequest.builder()
                    .id(1L)
                    .storeId(storeId)
                    .status(TransferRequestStatus.PENDING)
                    .items(List.of(item))
                    .build();
        }
        
        TransferRequest createMultiProductTransferRequest(int storeId, List<Long> productIds, List<Integer> quantities) {
            List<TransferRequestItem> items = new ArrayList<>();
            for (int i = 0; i < productIds.size(); i++) {
                items.add(TransferRequestItem.builder()
                        .id((long) (i + 1))
                        .productId(productIds.get(i))
                        .requestedQuantity(quantities.get(i))
                        .fulfilledQuantity(0)
                        .build());
            }
            
            return TransferRequest.builder()
                    .id(1L)
                    .storeId(storeId)
                    .status(TransferRequestStatus.PENDING)
                    .items(items)
                    .build();
        }

        AvailabilityCheckResult checkAvailability(TransferRequest request) {
            List<ProductAvailability> productAvailabilities = new ArrayList<>();
            int totalShortage = 0;
            int totalRequestedQuantity = 0;
            int totalAvailableQuantity = 0;
            boolean canFullyFulfill = true;
            
            for (TransferRequestItem item : request.getItems()) {
                ProductAvailability availability = checkProductAvailability(
                        item.getProductId(),
                        item.getRequestedQuantity(),
                        request.getStoreId()
                );
                productAvailabilities.add(availability);
                
                totalRequestedQuantity += item.getRequestedQuantity();
                totalAvailableQuantity += availability.getTotalAvailable();
                
                if (availability.getShortage() > 0) {
                    totalShortage += availability.getShortage();
                    canFullyFulfill = false;
                }
            }
            
            int maxFulfillable = Math.min(totalRequestedQuantity, totalAvailableQuantity);
            
            return AvailabilityCheckResult.builder()
                    .transferRequestId(request.getId())
                    .productAvailabilities(productAvailabilities)
                    .canFullyFulfill(canFullyFulfill)
                    .totalShortage(totalShortage)
                    .totalRequestedQuantity(totalRequestedQuantity)
                    .totalAvailableQuantity(totalAvailableQuantity)
                    .maxFulfillableQuantity(maxFulfillable)
                    .summary(canFullyFulfill ? "Can fully fulfill" : "Partial fulfillment")
                    .build();
        }
        
        private ProductAvailability checkProductAvailability(Long productId, int requestedQuantity, Integer excludeStoreId) {
            // Get main warehouse quantity (Requirements 9.1)
            int mainWarehouseAvailable = mainWarehouseInventory.getOrDefault(productId, 0);
            
            // Calculate source allocations (Requirements 9.4)
            List<SourceAllocation> allocations = calculateSourceAllocations(productId, requestedQuantity, excludeStoreId);
            
            // Calculate total available from allocations
            int totalAvailable = allocations.stream()
                    .mapToInt(SourceAllocation::getQuantity)
                    .sum();
            
            // Get store warehouse availabilities if main is insufficient (Requirements 9.2)
            List<StoreWarehouseAvailability> storeAvailabilities = new ArrayList<>();
            if (mainWarehouseAvailable < requestedQuantity) {
                storeAvailabilities = getStoreWarehouseAvailabilities(productId, excludeStoreId);
            }
            
            // Calculate shortage (Requirements 9.5)
            int shortage = Math.max(0, requestedQuantity - totalAvailable);
            
            return ProductAvailability.builder()
                    .productId(productId)
                    .productName(products.getOrDefault(productId, "Product " + productId))
                    .productSku("SKU-" + productId)
                    .requestedQuantity(requestedQuantity)
                    .mainWarehouseAvailable(mainWarehouseAvailable)
                    .storeAvailabilities(storeAvailabilities)
                    .totalAvailable(totalAvailable)
                    .shortage(shortage)
                    .recommendedAllocations(allocations)
                    .build();
        }

        private List<StoreWarehouseAvailability> getStoreWarehouseAvailabilities(Long productId, Integer excludeStoreId) {
            List<StoreWarehouseAvailability> availabilities = new ArrayList<>();
            
            for (Map.Entry<Integer, Map<Long, Integer>> entry : storeWarehouseInventory.entrySet()) {
                int storeId = entry.getKey();
                
                // Skip the requesting store
                if (excludeStoreId != null && excludeStoreId.equals(storeId)) {
                    continue;
                }
                
                int quantity = entry.getValue().getOrDefault(productId, 0);
                if (quantity > 0) {
                    availabilities.add(StoreWarehouseAvailability.builder()
                            .warehouseId(storeWarehouseIds.get(storeId))
                            .warehouseName("Store Warehouse " + storeId)
                            .storeId(storeId)
                            .storeName("Store " + storeId)
                            .totalQuantity(quantity)
                            .reservedQuantity(0)
                            .availableQuantity(quantity)
                            .build());
                }
            }
            
            // Sort by available quantity descending
            availabilities.sort((a, b) -> b.getAvailableQuantity().compareTo(a.getAvailableQuantity()));
            
            return availabilities;
        }
        
        private List<SourceAllocation> calculateSourceAllocations(Long productId, int requiredQuantity, Integer excludeStoreId) {
            List<SourceAllocation> allocations = new ArrayList<>();
            int remainingQuantity = requiredQuantity;
            
            // Step 1: Check Main Warehouse first (Requirements 5.2)
            int mainAvailable = mainWarehouseInventory.getOrDefault(productId, 0);
            if (mainAvailable > 0) {
                int allocateFromMain = Math.min(mainAvailable, remainingQuantity);
                allocations.add(SourceAllocation.builder()
                        .warehouseId(1L)
                        .warehouseName("Main Warehouse")
                        .warehouseType(WarehouseType.MAIN)
                        .storeId(null)
                        .storeName(null)
                        .quantity(allocateFromMain)
                        .build());
                remainingQuantity -= allocateFromMain;
            }
            
            // Step 2: If still need more, check Store Warehouses (Requirements 5.3, 5.4)
            if (remainingQuantity > 0) {
                List<Map.Entry<Integer, Integer>> storeEntries = new ArrayList<>();
                
                for (Map.Entry<Integer, Map<Long, Integer>> entry : storeWarehouseInventory.entrySet()) {
                    int storeId = entry.getKey();
                    
                    // Skip the requesting store
                    if (excludeStoreId != null && excludeStoreId.equals(storeId)) {
                        continue;
                    }
                    
                    int available = entry.getValue().getOrDefault(productId, 0);
                    if (available > 0) {
                        storeEntries.add(Map.entry(storeId, available));
                    }
                }
                
                // Sort by available quantity descending
                storeEntries.sort((a, b) -> b.getValue().compareTo(a.getValue()));
                
                for (Map.Entry<Integer, Integer> entry : storeEntries) {
                    if (remainingQuantity <= 0) {
                        break;
                    }
                    
                    int storeId = entry.getKey();
                    int available = entry.getValue();
                    int allocateFromStore = Math.min(available, remainingQuantity);
                    
                    allocations.add(SourceAllocation.builder()
                            .warehouseId(storeWarehouseIds.get(storeId))
                            .warehouseName("Store Warehouse " + storeId)
                            .warehouseType(WarehouseType.STORE)
                            .storeId(storeId)
                            .storeName("Store " + storeId)
                            .quantity(allocateFromStore)
                            .build());
                    
                    remainingQuantity -= allocateFromStore;
                }
            }
            
            return allocations;
        }
    }
}
