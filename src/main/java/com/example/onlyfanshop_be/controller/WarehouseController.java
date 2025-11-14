package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.WarehouseDTO;
import com.example.onlyfanshop_be.dto.WarehouseInventoryDTO;
import com.example.onlyfanshop_be.dto.request.AddProductToWarehouseRequest;
import com.example.onlyfanshop_be.dto.request.CreateWarehouseRequest;
import com.example.onlyfanshop_be.dto.request.TransferStockRequest;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.StockMovement;
import com.example.onlyfanshop_be.enums.WarehouseType;
import com.example.onlyfanshop_be.service.StockMovementService;
import com.example.onlyfanshop_be.service.WarehouseInventoryService;
import com.example.onlyfanshop_be.service.WarehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
public class WarehouseController {
    private final WarehouseService warehouseService;
    private final WarehouseInventoryService warehouseInventoryService;
    private final StockMovementService stockMovementService;

    /**
     * Get current user ID from security context
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            // Try to extract user ID from authentication details
            // This is a simplified version - you may need to adjust based on your UserDetails implementation
            try {
                // If UserDetails contains user ID, extract it here
                // For now, we'll require it to be passed or use a default
                return 1L; // Default admin user ID - adjust as needed
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Create warehouse (Admin only)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WarehouseDTO>> createWarehouse(@RequestBody CreateWarehouseRequest request) {
        Long userId = getCurrentUserId();
        WarehouseDTO warehouse = warehouseService.createWarehouse(request, userId);
        return ResponseEntity.ok(ApiResponse.<WarehouseDTO>builder()
                .statusCode(200)
                .message("Tạo kho hàng thành công")
                .data(warehouse)
                .build());
    }

    /**
     * Get all warehouses
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<WarehouseDTO>>> getAllWarehouses() {
        List<WarehouseDTO> warehouses = warehouseService.getAllWarehouses();
        return ResponseEntity.ok(ApiResponse.<List<WarehouseDTO>>builder()
                .statusCode(200)
                .message("Danh sách kho hàng")
                .data(warehouses)
                .build());
    }

    /**
     * Get warehouses by type
     */
    @GetMapping("/type/{type}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<WarehouseDTO>>> getWarehousesByType(@PathVariable String type) {
        WarehouseType warehouseType = WarehouseType.fromDbValue(type);
        List<WarehouseDTO> warehouses = warehouseService.getWarehousesByType(warehouseType);
        return ResponseEntity.ok(ApiResponse.<List<WarehouseDTO>>builder()
                .statusCode(200)
                .message("Danh sách kho hàng theo loại")
                .data(warehouses)
                .build());
    }

    /**
     * Get main warehouses (for adding products)
     */
    @GetMapping("/main")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<WarehouseDTO>>> getMainWarehouses() {
        List<WarehouseDTO> warehouses = warehouseService.getMainWarehouses();
        return ResponseEntity.ok(ApiResponse.<List<WarehouseDTO>>builder()
                .statusCode(200)
                .message("Danh sách kho hàng tổng")
                .data(warehouses)
                .build());
    }

    /**
     * Get warehouse by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<WarehouseDTO>> getWarehouseById(@PathVariable Integer id) {
        WarehouseDTO warehouse = warehouseService.getWarehouseById(id);
        return ResponseEntity.ok(ApiResponse.<WarehouseDTO>builder()
                .statusCode(200)
                .message("Chi tiết kho hàng")
                .data(warehouse)
                .build());
    }

    /**
     * Update warehouse (Admin only)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<WarehouseDTO>> updateWarehouse(
            @PathVariable Integer id,
            @RequestBody CreateWarehouseRequest request) {
        WarehouseDTO warehouse = warehouseService.updateWarehouse(id, request);
        return ResponseEntity.ok(ApiResponse.<WarehouseDTO>builder()
                .statusCode(200)
                .message("Cập nhật kho hàng thành công")
                .data(warehouse)
                .build());
    }

    /**
     * Delete warehouse (Admin only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteWarehouse(@PathVariable Integer id) {
        warehouseService.deleteWarehouse(id);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .statusCode(200)
                .message("Xóa kho hàng thành công")
                .build());
    }

    /**
     * Add product to warehouse (only main warehouses)
     */
    @PostMapping("/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> addProductToWarehouse(@RequestBody AddProductToWarehouseRequest request) {
        Long userId = getCurrentUserId();
        warehouseService.addProductToWarehouse(request, userId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .statusCode(200)
                .message("Thêm sản phẩm vào kho thành công")
                .build());
    }

    /**
     * Transfer stock between warehouses
     */
    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<Void>> transferStock(@RequestBody TransferStockRequest request) {
        Long userId = getCurrentUserId();
        warehouseService.transferStock(request, userId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .statusCode(200)
                .message("Chuyển kho thành công")
                .build());
    }

    /**
     * Request stock from parent warehouse
     */
    @PostMapping("/{warehouseId}/request-stock")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<Void>> requestStockFromParent(
            @PathVariable Integer warehouseId,
            @RequestParam Long productId,
            @RequestParam(required = false) Long productVariantId,
            @RequestParam Integer quantity,
            @RequestParam(required = false) String note) {
        Long userId = getCurrentUserId();
        warehouseService.requestStockFromParent(warehouseId, productId, productVariantId, quantity, note, userId);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .statusCode(200)
                .message("Yêu cầu nhập hàng thành công")
                .build());
    }

    /**
     * Get warehouse inventory
     */
    @GetMapping("/{warehouseId}/inventory")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<WarehouseInventoryDTO>>> getWarehouseInventory(@PathVariable Integer warehouseId) {
        List<WarehouseInventoryDTO> inventory = warehouseInventoryService.getWarehouseInventory(warehouseId);
        return ResponseEntity.ok(ApiResponse.<List<WarehouseInventoryDTO>>builder()
                .statusCode(200)
                .message("Tồn kho của kho hàng")
                .data(inventory)
                .build());
    }

    /**
     * Get stock movements for a warehouse
     */
    @GetMapping("/{warehouseId}/movements")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<StockMovement>>> getStockMovements(@PathVariable Integer warehouseId) {
        List<StockMovement> movements = stockMovementService.getMovementsByWarehouse(warehouseId);
        return ResponseEntity.ok(ApiResponse.<List<StockMovement>>builder()
                .statusCode(200)
                .message("Lịch sử xuất nhập kho")
                .data(movements)
                .build());
    }

    /**
     * Get child warehouses
     */
    @GetMapping("/{parentId}/children")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<WarehouseDTO>>> getChildWarehouses(@PathVariable Integer parentId) {
        List<WarehouseDTO> children = warehouseService.getChildWarehouses(parentId);
        return ResponseEntity.ok(ApiResponse.<List<WarehouseDTO>>builder()
                .statusCode(200)
                .message("Danh sách kho con")
                .data(children)
                .build());
    }
}








