package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.StoreInventoryDTO;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.StoreLocation;
import com.example.onlyfanshop_be.service.StoreInventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/store-inventory")
@RequiredArgsConstructor
public class StoreInventoryController {
    private final StoreInventoryService storeInventoryService;

    /**
     * Lấy danh sách stores có bán sản phẩm (isAvailable = true)
     */
    @GetMapping("/product/{productId}/stores")
    public ResponseEntity<ApiResponse<List<StoreLocation>>> getStoresWithProduct(
            @PathVariable Long productId,
            @RequestParam(required = false) String city) {
        List<StoreLocation> stores = storeInventoryService.getStoresWithProduct(productId);
        
        // Filter by city if provided
        if (city != null && !city.trim().isEmpty()) {
            stores = stores.stream()
                    .filter(store -> store.getCity() != null && store.getCity().equalsIgnoreCase(city.trim()))
                    .toList();
        }
        
        return ResponseEntity.ok(ApiResponse.<List<StoreLocation>>builder()
                .statusCode(200)
                .message("Danh sách cửa hàng có bán sản phẩm")
                .data(stores)
                .build());
    }

    /**
     * Lấy danh sách sản phẩm có sẵn tại store
     */
    @GetMapping("/store/{storeId}/products")
    public ResponseEntity<ApiResponse<List<StoreInventoryDTO>>> getStoreProducts(
            @PathVariable Integer storeId,
            @RequestParam(name = "includeInactive", defaultValue = "false") boolean includeInactive) {
        List<StoreInventoryDTO> products = storeInventoryService.getStoreProducts(storeId, includeInactive);
        return ResponseEntity.ok(ApiResponse.<List<StoreInventoryDTO>>builder()
                .statusCode(200)
                .message("Danh sách sản phẩm tại cửa hàng")
                .data(products)
                .build());
    }

    /**
     * Bật/tắt việc bán sản phẩm ở store (Admin only)
     */
    @PutMapping("/store/{storeId}/product/{productId}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<StoreInventoryDTO>> toggleProductAvailability(
            @PathVariable Integer storeId,
            @PathVariable Long productId,
            @RequestParam Boolean isAvailable) {
        storeInventoryService.toggleProductAvailability(storeId, productId, isAvailable);
        StoreInventoryDTO dto = storeInventoryService.getStoreInventory(storeId, productId);
        return ResponseEntity.ok(ApiResponse.<StoreInventoryDTO>builder()
                .statusCode(200)
                .message(isAvailable ? "Đã bật bán sản phẩm tại cửa hàng" : "Đã tắt bán sản phẩm tại cửa hàng")
                .data(dto)
                .build());
    }

    /**
     * Thêm sản phẩm vào store (Admin only)
     */
    @PostMapping("/store/{storeId}/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<StoreInventoryDTO>>> addProductsToStore(
            @PathVariable Integer storeId,
            @RequestBody List<Long> productIds) {
        List<StoreInventoryDTO> results = storeInventoryService.addProductsToStore(storeId, productIds);
        return ResponseEntity.ok(ApiResponse.<List<StoreInventoryDTO>>builder()
                .statusCode(200)
                .message("Đã thêm " + results.size() + " sản phẩm vào cửa hàng")
                .data(results)
                .build());
    }

    /**
     * Lấy danh sách sản phẩm chưa có trong store (để thêm mới)
     */
    @GetMapping("/store/{storeId}/available-products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<com.example.onlyfanshop_be.entity.Product>>> getProductsNotInStore(
            @PathVariable Integer storeId) {
        List<com.example.onlyfanshop_be.entity.Product> products = storeInventoryService.getProductsNotInStore(storeId);
        return ResponseEntity.ok(ApiResponse.<List<com.example.onlyfanshop_be.entity.Product>>builder()
                .statusCode(200)
                .message("Danh sách sản phẩm có thể thêm vào cửa hàng")
                .data(products)
                .build());
    }

    /**
     * Lấy tất cả sản phẩm trong hệ thống kèm trạng thái cho phép bán tại store
     */
    @GetMapping("/store/{storeId}/all-products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<StoreInventoryDTO>>> getAllProductsWithStoreStatus(
            @PathVariable Integer storeId) {
        List<StoreInventoryDTO> products = storeInventoryService.getAllProductsWithStoreStatus(storeId);
        return ResponseEntity.ok(ApiResponse.<List<StoreInventoryDTO>>builder()
                .statusCode(200)
                .message("Danh sách tất cả sản phẩm với trạng thái cửa hàng")
                .data(products)
                .build());
    }

    /**
     * Cập nhật danh sách sản phẩm cho phép bán tại store (bulk update)
     */
    @PutMapping("/store/{storeId}/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> updateStoreProducts(
            @PathVariable Integer storeId,
            @RequestBody List<Long> enabledProductIds) {
        storeInventoryService.updateStoreProducts(storeId, enabledProductIds);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .statusCode(200)
                .message("Đã cập nhật danh sách sản phẩm cho cửa hàng")
                .data(null)
                .build());
    }
}

