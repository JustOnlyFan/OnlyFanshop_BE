package com.example.onlyfanshop_be.service;

import com.example.onlyfanshop_be.dto.WarehouseDTO;
import com.example.onlyfanshop_be.dto.request.AddProductToWarehouseRequest;
import com.example.onlyfanshop_be.dto.request.CreateWarehouseRequest;
import com.example.onlyfanshop_be.dto.request.TransferStockRequest;
import com.example.onlyfanshop_be.entity.Warehouse;
import com.example.onlyfanshop_be.enums.WarehouseType;
import com.example.onlyfanshop_be.exception.AppException;
import com.example.onlyfanshop_be.exception.ErrorCode;
import com.example.onlyfanshop_be.repository.StoreLocationRepository;
import com.example.onlyfanshop_be.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WarehouseService {
    private final WarehouseRepository warehouseRepository;
    private final StoreLocationRepository storeLocationRepository;
    private final WarehouseInventoryService warehouseInventoryService;
    private final StockMovementService stockMovementService;

    /**
     * Create a new warehouse
     */
    @Transactional
    public WarehouseDTO createWarehouse(CreateWarehouseRequest request, Long createdBy) {
        // Validate code uniqueness
        if (warehouseRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.WAREHOUSE_CODE_EXISTS);
        }

        // Validate parent warehouse if provided
        if (request.getParentWarehouseId() != null) {
            Warehouse parentWarehouse = warehouseRepository.findById(request.getParentWarehouseId())
                    .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
            
            // Validate warehouse type hierarchy
            if (request.getType() == WarehouseType.MAIN && request.getParentWarehouseId() != null) {
                throw new AppException(ErrorCode.INVALID_WAREHOUSE_TYPE);
            }
            if (request.getType() == WarehouseType.REGIONAL && parentWarehouse.getType() != WarehouseType.MAIN) {
                throw new AppException(ErrorCode.INVALID_WAREHOUSE_TYPE);
            }
            if (request.getType() == WarehouseType.BRANCH && parentWarehouse.getType() != WarehouseType.REGIONAL) {
                throw new AppException(ErrorCode.INVALID_WAREHOUSE_TYPE);
            }
        } else {
            // Main warehouses should not have parent
            if (request.getType() != WarehouseType.MAIN) {
                throw new AppException(ErrorCode.INVALID_WAREHOUSE_TYPE);
            }
        }

        // Validate store location if provided (only for branch warehouses)
        if (request.getStoreLocationId() != null) {
            if (request.getType() != WarehouseType.BRANCH) {
                throw new AppException(ErrorCode.INVALID_WAREHOUSE_TYPE);
            }
            // Validate store location exists
            if (!storeLocationRepository.existsById(request.getStoreLocationId())) {
                throw new AppException(ErrorCode.LOCATION_NOT_FOUND);
            }
            
            // Check if store location already has a warehouse
            List<Warehouse> existingWarehouses = warehouseRepository.findByStoreLocationId(request.getStoreLocationId());
            if (!existingWarehouses.isEmpty()) {
                throw new AppException(ErrorCode.INVALID_WAREHOUSE_TYPE);
            }
        }

        Warehouse warehouse = Warehouse.builder()
                .name(request.getName())
                .code(request.getCode())
                .type(request.getType())
                .parentWarehouseId(request.getParentWarehouseId())
                .storeLocationId(request.getStoreLocationId())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .ward(request.getWard())
                .district(request.getDistrict())
                .city(request.getCity())
                .country(request.getCountry() != null ? request.getCountry() : "Vietnam")
                .phone(request.getPhone())
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();

        Warehouse savedWarehouse = warehouseRepository.save(warehouse);
        return convertToDTO(savedWarehouse);
    }

    /**
     * Get all warehouses
     */
    @Transactional(readOnly = true)
    public List<WarehouseDTO> getAllWarehouses() {
        try {
            return warehouseRepository.findAll().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error loading warehouses: " + e.getMessage());
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Get warehouses by type
     */
    @Transactional(readOnly = true)
    public List<WarehouseDTO> getWarehousesByType(WarehouseType type) {
        try {
            return warehouseRepository.findByType(type).stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error loading warehouses by type: " + e.getMessage());
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Get active warehouses
     */
    @Transactional(readOnly = true)
    public List<WarehouseDTO> getActiveWarehouses() {
        try {
            return warehouseRepository.findByIsActiveTrue().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error loading active warehouses: " + e.getMessage());
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Get main warehouses (for adding products)
     */
    @Transactional(readOnly = true)
    public List<WarehouseDTO> getMainWarehouses() {
        try {
            return warehouseRepository.findActiveByType(WarehouseType.MAIN).stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error loading main warehouses: " + e.getMessage());
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Get warehouse by ID
     */
    @Transactional(readOnly = true)
    public WarehouseDTO getWarehouseById(Integer id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        return convertToDTO(warehouse);
    }

    /**
     * Update warehouse
     */
    @Transactional
    public WarehouseDTO updateWarehouse(Integer id, CreateWarehouseRequest request) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));

        // Validate code uniqueness (if changed)
        if (!warehouse.getCode().equals(request.getCode()) && warehouseRepository.existsByCode(request.getCode())) {
            throw new AppException(ErrorCode.WAREHOUSE_CODE_EXISTS);
        }

        warehouse.setName(request.getName());
        warehouse.setCode(request.getCode());
        warehouse.setAddressLine1(request.getAddressLine1());
        warehouse.setAddressLine2(request.getAddressLine2());
        warehouse.setWard(request.getWard());
        warehouse.setDistrict(request.getDistrict());
        warehouse.setCity(request.getCity());
        warehouse.setCountry(request.getCountry());
        warehouse.setPhone(request.getPhone());

        Warehouse updatedWarehouse = warehouseRepository.save(warehouse);
        return convertToDTO(updatedWarehouse);
    }

    /**
     * Delete warehouse (soft delete by setting isActive to false)
     */
    @Transactional
    public void deleteWarehouse(Integer id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        
        // Check if warehouse has inventory
        var inventory = warehouseInventoryService.getWarehouseInventory(id);
        if (!inventory.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_WAREHOUSE_TRANSFER);
        }
        
        warehouse.setIsActive(false);
        warehouseRepository.save(warehouse);
    }

    /**
     * Add product to warehouse (only for main warehouses)
     */
    @Transactional
    public void addProductToWarehouse(AddProductToWarehouseRequest request, Long createdBy) {
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));

        // Only main warehouses can receive new products
        if (warehouse.getType() != WarehouseType.MAIN) {
            throw new AppException(ErrorCode.INVALID_WAREHOUSE_TYPE);
        }

        // Add to inventory and create movement record
        stockMovementService.recordImport(
                request.getWarehouseId(),
                request.getProductId(),
                request.getProductVariantId(),
                request.getQuantity(),
                request.getNote(),
                createdBy
        );
    }

    /**
     * Transfer stock between warehouses
     */
    @Transactional
    public void transferStock(TransferStockRequest request, Long createdBy) {
        Warehouse fromWarehouse = warehouseRepository.findById(request.getFromWarehouseId())
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        
        Warehouse toWarehouse = warehouseRepository.findById(request.getToWarehouseId())
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));

        // Validate warehouse hierarchy
        // Branch can request from Regional, Regional can request from Main
        // Main cannot receive from others (only add new products)
        if (toWarehouse.getType() == WarehouseType.MAIN) {
            throw new AppException(ErrorCode.INVALID_WAREHOUSE_TRANSFER);
        }

        if (toWarehouse.getType() == WarehouseType.REGIONAL && fromWarehouse.getType() != WarehouseType.MAIN) {
            throw new AppException(ErrorCode.INVALID_WAREHOUSE_TRANSFER);
        }

        if (toWarehouse.getType() == WarehouseType.BRANCH) {
            if (fromWarehouse.getType() != WarehouseType.REGIONAL && fromWarehouse.getType() != WarehouseType.MAIN) {
                throw new AppException(ErrorCode.INVALID_WAREHOUSE_TRANSFER);
            }
        }

        // Transfer stock
        stockMovementService.recordTransfer(
                request.getFromWarehouseId(),
                request.getToWarehouseId(),
                request.getProductId(),
                request.getProductVariantId(),
                request.getQuantity(),
                request.getNote(),
                createdBy
        );
    }

    /**
     * Request stock from parent warehouse (automatically finds the right parent)
     */
    @Transactional
    public void requestStockFromParent(Integer warehouseId, Long productId, Long productVariantId, Integer quantity, String note, Long createdBy) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));

        Integer parentWarehouseId = warehouse.getParentWarehouseId();
        if (parentWarehouseId == null) {
            throw new AppException(ErrorCode.INVALID_WAREHOUSE_TRANSFER);
        }

        // Create transfer request
        TransferStockRequest request = new TransferStockRequest();
        request.setFromWarehouseId(parentWarehouseId);
        request.setToWarehouseId(warehouseId);
        request.setProductId(productId);
        request.setProductVariantId(productVariantId);
        request.setQuantity(quantity);
        request.setNote(note);

        transferStock(request, createdBy);
    }

    /**
     * Get child warehouses
     */
    @Transactional(readOnly = true)
    public List<WarehouseDTO> getChildWarehouses(Integer parentId) {
        try {
            return warehouseRepository.findActiveChildrenByParentId(parentId).stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Error loading child warehouses: " + e.getMessage());
            e.printStackTrace();
            return java.util.Collections.emptyList();
        }
    }

    private WarehouseDTO convertToDTO(Warehouse warehouse) {
        WarehouseDTO.WarehouseDTOBuilder builder = WarehouseDTO.builder()
                .id(warehouse.getId())
                .name(warehouse.getName())
                .code(warehouse.getCode())
                .type(warehouse.getType())
                .parentWarehouseId(warehouse.getParentWarehouseId())
                .storeLocationId(warehouse.getStoreLocationId())
                .addressLine1(warehouse.getAddressLine1())
                .addressLine2(warehouse.getAddressLine2())
                .ward(warehouse.getWard())
                .district(warehouse.getDistrict())
                .city(warehouse.getCity())
                .country(warehouse.getCountry())
                .phone(warehouse.getPhone())
                .isActive(warehouse.getIsActive())
                .createdAt(warehouse.getCreatedAt());

        // Load parent warehouse name if exists
        if (warehouse.getParentWarehouseId() != null && warehouse.getParentWarehouse() != null) {
            builder.parentWarehouseName(warehouse.getParentWarehouse().getName());
        }

        // Load store location name if exists
        if (warehouse.getStoreLocationId() != null && warehouse.getStoreLocation() != null) {
            builder.storeLocationName(warehouse.getStoreLocation().getName());
        }

        return builder.build();
    }
}

