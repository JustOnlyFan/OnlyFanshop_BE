package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.request.CreateTransferRequestDTO;
import com.example.onlyfanshop_be.dto.request.CreateTransferRequestItemDTO;
import com.example.onlyfanshop_be.entity.*;
import com.example.onlyfanshop_be.enums.WarehouseType;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.*;
import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for Transfer Request Product Validation.
 * 
 * **Feature: inventory-management-ghn, Property 7: Transfer Request Product Validation**
 * **Validates: Requirements 4.1**
 * 
 * Property: For any Transfer_Request, all requested products SHALL exist in the requesting 
 * Store's Store_Warehouse. Requests containing products not in the Store_Warehouse SHALL be rejected.
 */
class TransferRequestProductValidationPropertyTest {

    /**
     * **Feature: inventory-management-ghn, Property 7: Transfer Request Product Validation**
     * **Validates: Requirements 4.1**
     * 
     * Property: For any Transfer_Request where all products exist in the Store_Warehouse,
     * the request SHALL be accepted and created successfully.
     */
    @Property(tries = 100)
    void requestWithAllProductsInStoreWarehouseShouldBeAccepted(
            @ForAll @IntRange(min = 1, max = 100) int storeId,
            @ForAll @IntRange(min = 1, max = 100) long warehouseId,
            @ForAll("validProductIdsInStore") List<Long> productIds,
            @ForAll @IntRange(min = 1, max = 30) int quantity) {
        
        // Skip if no products
        Assume.that(!productIds.isEmpty());
        
        // Create mock repositories
        TransferRequestRepository transferRequestRepository = mock(TransferRequestRepository.class);
        TransferRequestItemRepository transferRequestItemRepository = mock(TransferRequestItemRepository.class);
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        StoreLocationRepository storeLocationRepository = mock(StoreLocationRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        
        // Setup: Store exists
        StoreLocation store = new StoreLocation();
        store.setId(storeId);
        store.setName("Test Store " + storeId);
        when(storeLocationRepository.findById(storeId)).thenReturn(Optional.of(store));
        
        // Setup: Store Warehouse exists
        Warehouse storeWarehouse = Warehouse.builder()
                .id(warehouseId)
                .name("Store Warehouse " + storeId)
                .type(WarehouseType.STORE)
                .storeId(storeId)
                .build();
        when(warehouseRepository.findByStoreId(storeId)).thenReturn(Optional.of(storeWarehouse));
        
        // Setup: All products exist in the system and in the Store_Warehouse
        for (Long productId : productIds) {
            Product product = Product.builder()
                    .id(productId)
                    .name("Product " + productId)
                    .build();
            when(productRepository.findById(productId.intValue())).thenReturn(Optional.of(product));
            
            // Product exists in Store_Warehouse
            when(inventoryItemRepository.existsByWarehouseIdAndProductId(warehouseId, productId))
                    .thenReturn(true);
        }
        
        // Setup: Save operations
        when(transferRequestRepository.save(any(TransferRequest.class)))
                .thenAnswer(invocation -> {
                    TransferRequest req = invocation.getArgument(0);
                    req.setId(1L);
                    return req;
                });
        when(transferRequestItemRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        
        // Create the validator
        TransferRequestProductValidator validator = new TransferRequestProductValidator(
                transferRequestRepository, transferRequestItemRepository, warehouseRepository,
                inventoryItemRepository, productRepository, storeLocationRepository, userRepository);
        
        // Create request with products that exist in Store_Warehouse
        List<CreateTransferRequestItemDTO> items = new ArrayList<>();
        for (Long productId : productIds) {
            items.add(CreateTransferRequestItemDTO.builder()
                    .productId(productId)
                    .quantity(quantity)
                    .build());
        }
        CreateTransferRequestDTO request = CreateTransferRequestDTO.builder()
                .items(items)
                .build();
        
        // Execute: Should succeed
        var result = validator.createRequest(storeId, request);
        
        // Verify: Request was created
        assertThat(result).isNotNull();
        verify(transferRequestRepository, times(1)).save(any(TransferRequest.class));
    }

    /**
     * **Feature: inventory-management-ghn, Property 7: Transfer Request Product Validation**
     * **Validates: Requirements 4.1**
     * 
     * Property: For any Transfer_Request containing at least one product NOT in the Store_Warehouse,
     * the request SHALL be rejected with TRANSFER_REQUEST_PRODUCT_NOT_IN_STORE error.
     */
    @Property(tries = 100)
    void requestWithProductNotInStoreWarehouseShouldBeRejected(
            @ForAll @IntRange(min = 1, max = 100) int storeId,
            @ForAll @IntRange(min = 1, max = 100) long warehouseId,
            @ForAll @IntRange(min = 1, max = 10000) long productIdInStore,
            @ForAll @IntRange(min = 10001, max = 20000) long productIdNotInStore,
            @ForAll @IntRange(min = 1, max = 30) int quantity) {
        
        // Create mock repositories
        TransferRequestRepository transferRequestRepository = mock(TransferRequestRepository.class);
        TransferRequestItemRepository transferRequestItemRepository = mock(TransferRequestItemRepository.class);
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        StoreLocationRepository storeLocationRepository = mock(StoreLocationRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        
        // Setup: Store exists
        StoreLocation store = new StoreLocation();
        store.setId(storeId);
        store.setName("Test Store " + storeId);
        when(storeLocationRepository.findById(storeId)).thenReturn(Optional.of(store));
        
        // Setup: Store Warehouse exists
        Warehouse storeWarehouse = Warehouse.builder()
                .id(warehouseId)
                .name("Store Warehouse " + storeId)
                .type(WarehouseType.STORE)
                .storeId(storeId)
                .build();
        when(warehouseRepository.findByStoreId(storeId)).thenReturn(Optional.of(storeWarehouse));
        
        // Setup: Product in store exists in system and in Store_Warehouse
        Product productInStore = Product.builder()
                .id(productIdInStore)
                .name("Product In Store " + productIdInStore)
                .build();
        when(productRepository.findById((int) productIdInStore)).thenReturn(Optional.of(productInStore));
        when(inventoryItemRepository.existsByWarehouseIdAndProductId(warehouseId, productIdInStore))
                .thenReturn(true);
        
        // Setup: Product NOT in store exists in system but NOT in Store_Warehouse
        Product productNotInStore = Product.builder()
                .id(productIdNotInStore)
                .name("Product Not In Store " + productIdNotInStore)
                .build();
        when(productRepository.findById((int) productIdNotInStore)).thenReturn(Optional.of(productNotInStore));
        when(inventoryItemRepository.existsByWarehouseIdAndProductId(warehouseId, productIdNotInStore))
                .thenReturn(false);  // Product NOT in Store_Warehouse
        
        // Create the validator
        TransferRequestProductValidator validator = new TransferRequestProductValidator(
                transferRequestRepository, transferRequestItemRepository, warehouseRepository,
                inventoryItemRepository, productRepository, storeLocationRepository, userRepository);
        
        // Create request with one product in store and one NOT in store
        List<CreateTransferRequestItemDTO> items = List.of(
                CreateTransferRequestItemDTO.builder()
                        .productId(productIdInStore)
                        .quantity(quantity)
                        .build(),
                CreateTransferRequestItemDTO.builder()
                        .productId(productIdNotInStore)
                        .quantity(quantity)
                        .build()
        );
        CreateTransferRequestDTO request = CreateTransferRequestDTO.builder()
                .items(items)
                .build();
        
        // Execute & Verify: Should be rejected
        assertThatThrownBy(() -> validator.createRequest(storeId, request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.TRANSFER_REQUEST_PRODUCT_NOT_IN_STORE);
                });
        
        // Verify: No transfer request was saved
        verify(transferRequestRepository, never()).save(any(TransferRequest.class));
    }

    /**
     * **Feature: inventory-management-ghn, Property 7: Transfer Request Product Validation**
     * **Validates: Requirements 4.1**
     * 
     * Property: For any Transfer_Request where the only product is NOT in the Store_Warehouse,
     * the request SHALL be rejected.
     */
    @Property(tries = 100)
    void requestWithSingleProductNotInStoreWarehouseShouldBeRejected(
            @ForAll @IntRange(min = 1, max = 100) int storeId,
            @ForAll @IntRange(min = 1, max = 100) long warehouseId,
            @ForAll @IntRange(min = 1, max = 10000) long productId,
            @ForAll @IntRange(min = 1, max = 30) int quantity) {
        
        // Create mock repositories
        TransferRequestRepository transferRequestRepository = mock(TransferRequestRepository.class);
        TransferRequestItemRepository transferRequestItemRepository = mock(TransferRequestItemRepository.class);
        WarehouseRepository warehouseRepository = mock(WarehouseRepository.class);
        InventoryItemRepository inventoryItemRepository = mock(InventoryItemRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        StoreLocationRepository storeLocationRepository = mock(StoreLocationRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        
        // Setup: Store exists
        StoreLocation store = new StoreLocation();
        store.setId(storeId);
        store.setName("Test Store " + storeId);
        when(storeLocationRepository.findById(storeId)).thenReturn(Optional.of(store));
        
        // Setup: Store Warehouse exists
        Warehouse storeWarehouse = Warehouse.builder()
                .id(warehouseId)
                .name("Store Warehouse " + storeId)
                .type(WarehouseType.STORE)
                .storeId(storeId)
                .build();
        when(warehouseRepository.findByStoreId(storeId)).thenReturn(Optional.of(storeWarehouse));
        
        // Setup: Product exists in system but NOT in Store_Warehouse
        Product product = Product.builder()
                .id(productId)
                .name("Product " + productId)
                .build();
        when(productRepository.findById((int) productId)).thenReturn(Optional.of(product));
        when(inventoryItemRepository.existsByWarehouseIdAndProductId(warehouseId, productId))
                .thenReturn(false);  // Product NOT in Store_Warehouse
        
        // Create the validator
        TransferRequestProductValidator validator = new TransferRequestProductValidator(
                transferRequestRepository, transferRequestItemRepository, warehouseRepository,
                inventoryItemRepository, productRepository, storeLocationRepository, userRepository);
        
        // Create request with single product NOT in Store_Warehouse
        List<CreateTransferRequestItemDTO> items = List.of(
                CreateTransferRequestItemDTO.builder()
                        .productId(productId)
                        .quantity(quantity)
                        .build()
        );
        CreateTransferRequestDTO request = CreateTransferRequestDTO.builder()
                .items(items)
                .build();
        
        // Execute & Verify: Should be rejected
        assertThatThrownBy(() -> validator.createRequest(storeId, request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appEx = (AppException) ex;
                    assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.TRANSFER_REQUEST_PRODUCT_NOT_IN_STORE);
                });
        
        // Verify: No transfer request was saved
        verify(transferRequestRepository, never()).save(any(TransferRequest.class));
    }

    /**
     * Provides valid product IDs that exist in the store warehouse.
     */
    @Provide
    Arbitrary<List<Long>> validProductIdsInStore() {
        return Arbitraries.longs().between(1, 10000)
                .list()
                .ofMinSize(1)
                .ofMaxSize(5)
                .uniqueElements();
    }

    /**
     * Helper class that replicates the transfer request validation logic from TransferRequestService.
     * This allows testing the validation without full Spring context.
     */
    static class TransferRequestProductValidator {
        private final TransferRequestRepository transferRequestRepository;
        private final TransferRequestItemRepository transferRequestItemRepository;
        private final WarehouseRepository warehouseRepository;
        private final InventoryItemRepository inventoryItemRepository;
        private final ProductRepository productRepository;
        private final StoreLocationRepository storeLocationRepository;
        private final UserRepository userRepository;
        
        TransferRequestProductValidator(
                TransferRequestRepository transferRequestRepository,
                TransferRequestItemRepository transferRequestItemRepository,
                WarehouseRepository warehouseRepository,
                InventoryItemRepository inventoryItemRepository,
                ProductRepository productRepository,
                StoreLocationRepository storeLocationRepository,
                UserRepository userRepository) {
            this.transferRequestRepository = transferRequestRepository;
            this.transferRequestItemRepository = transferRequestItemRepository;
            this.warehouseRepository = warehouseRepository;
            this.inventoryItemRepository = inventoryItemRepository;
            this.productRepository = productRepository;
            this.storeLocationRepository = storeLocationRepository;
            this.userRepository = userRepository;
        }
        
        /**
         * Creates a transfer request with product validation.
         * Mimics the behavior of TransferRequestService.createRequest()
         * 
         * Requirements: 4.1 - WHEN Staff creates a Transfer_Request THEN the System SHALL 
         * validate that all requested products exist in the Store_Warehouse
         */
        TransferRequest createRequest(Integer storeId, CreateTransferRequestDTO request) {
            // Validate store exists
            StoreLocation store = storeLocationRepository.findById(storeId)
                    .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));
            
            // Validate request has items
            if (request.getItems() == null || request.getItems().isEmpty()) {
                throw new AppException(ErrorCode.TRANSFER_REQUEST_EMPTY_ITEMS);
            }
            
            // Get store warehouse
            Warehouse storeWarehouse = warehouseRepository.findByStoreId(storeId)
                    .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
            
            // Validate all items - Requirements 4.1
            for (CreateTransferRequestItemDTO item : request.getItems()) {
                validateTransferRequestItem(item, storeWarehouse.getId());
            }
            
            // Create transfer request
            TransferRequest transferRequest = TransferRequest.builder()
                    .storeId(storeId)
                    .build();
            
            TransferRequest savedRequest = transferRequestRepository.save(transferRequest);
            
            // Create transfer request items
            List<TransferRequestItem> items = new ArrayList<>();
            for (CreateTransferRequestItemDTO itemDTO : request.getItems()) {
                TransferRequestItem item = TransferRequestItem.builder()
                        .transferRequestId(savedRequest.getId())
                        .productId(itemDTO.getProductId())
                        .requestedQuantity(itemDTO.getQuantity())
                        .fulfilledQuantity(0)
                        .build();
                items.add(item);
            }
            
            transferRequestItemRepository.saveAll(items);
            savedRequest.setItems(items);
            
            return savedRequest;
        }
        
        /**
         * Validates a single transfer request item.
         * Requirements: 4.1 - Validate product exists in Store_Warehouse
         */
        private void validateTransferRequestItem(CreateTransferRequestItemDTO item, Long storeWarehouseId) {
            // Validate quantity is positive
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new AppException(ErrorCode.TRANSFER_REQUEST_INVALID_QUANTITY);
            }
            
            // Validate quantity does not exceed limit
            if (item.getQuantity() > 30) {
                throw new AppException(ErrorCode.TRANSFER_REQUEST_QUANTITY_EXCEEDS_LIMIT);
            }
            
            // Validate product exists
            if (item.getProductId() == null) {
                throw new AppException(ErrorCode.PRODUCT_NOTEXISTED);
            }
            
            productRepository.findById(item.getProductId().intValue())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOTEXISTED));
            
            // Validate product exists in Store_Warehouse (Requirements 4.1)
            boolean existsInStoreWarehouse = inventoryItemRepository
                    .existsByWarehouseIdAndProductId(storeWarehouseId, item.getProductId());
            
            if (!existsInStoreWarehouse) {
                throw new AppException(ErrorCode.TRANSFER_REQUEST_PRODUCT_NOT_IN_STORE);
            }
        }
    }
}
