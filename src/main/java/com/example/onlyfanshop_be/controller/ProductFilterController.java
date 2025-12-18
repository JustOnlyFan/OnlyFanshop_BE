package com.example.onlyfanshop_be.controller;

import com.example.onlyfanshop_be.dto.ProductDetailDTO;
import com.example.onlyfanshop_be.dto.request.ProductFilterRequest;
import com.example.onlyfanshop_be.dto.response.ApiResponse;
import com.example.onlyfanshop_be.entity.Product;
import com.example.onlyfanshop_be.service.ProductFilterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for advanced product filtering.
 * Provides endpoints for filtering products by multiple criteria including
 * categories, brands, price range, tags, and accessory compatibility.
 * 
 * Requirements: 4.1, 5.1, 6.1, 7.1, 8.4
 */
@RestController
@RequestMapping("/products/filter")
public class ProductFilterController {

    @Autowired
    private ProductFilterService productFilterService;

    /**
     * Filter products with multiple criteria.
     * All filters are combined using AND logic.
     * Requirements: 4.1, 5.1, 6.1, 7.1, 8.4
     * 
     * @param request the filter request containing all criteria
     * @param page page number (0-based)
     * @param size page size
     * @param sortBy field to sort by
     * @param sortDirection sort direction (ASC or DESC)
     * @return paginated list of products matching all criteria
     */
    @PostMapping("/public")
    public ResponseEntity<ApiResponse<Map<String, Object>>> filterProducts(
            @RequestBody ProductFilterRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection) {
        try {
            // Build pageable with sorting
            Sort sort = sortDirection.equalsIgnoreCase("ASC") 
                    ? Sort.by(sortBy).ascending() 
                    : Sort.by(sortBy).descending();
            Pageable pageable = PageRequest.of(page, size, sort);
            
            // Apply filters
            Page<Product> productPage = productFilterService.filterProducts(request, pageable);
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("products", productPage.getContent());
            response.put("currentPage", productPage.getNumber());
            response.put("totalItems", productPage.getTotalElements());
            response.put("totalPages", productPage.getTotalPages());
            response.put("pageSize", productPage.getSize());
            
            return ResponseEntity.ok(ApiResponse.<Map<String, Object>>builder()
                    .statusCode(200)
                    .message("Lọc sản phẩm thành công")
                    .data(response)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Map<String, Object>>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }


    /**
     * Get products by category with optional subcategory inclusion.
     * Requirements: 4.1 - Category query completeness
     * 
     * @param categoryId the category ID
     * @param includeSubcategories whether to include products from subcategories
     * @return list of products in the category
     */
    @GetMapping("/public/category/{categoryId}")
    public ResponseEntity<ApiResponse<List<Product>>> getProductsByCategory(
            @PathVariable Integer categoryId,
            @RequestParam(defaultValue = "true") boolean includeSubcategories) {
        try {
            List<Product> products = productFilterService.getProductsByCategory(categoryId, includeSubcategories);
            
            return ResponseEntity.ok(ApiResponse.<List<Product>>builder()
                    .statusCode(200)
                    .message("Lấy sản phẩm theo danh mục thành công")
                    .data(products)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<List<Product>>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Get products by price range.
     * Requirements: 7.1 - Price range filter
     * 
     * @param minPrice minimum price (inclusive)
     * @param maxPrice maximum price (inclusive)
     * @return list of products within the price range
     */
    @GetMapping("/public/price-range")
    public ResponseEntity<ApiResponse<List<Product>>> getProductsByPriceRange(
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice) {
        try {
            List<Product> products = productFilterService.getProductsByPriceRange(minPrice, maxPrice);
            
            return ResponseEntity.ok(ApiResponse.<List<Product>>builder()
                    .statusCode(200)
                    .message("Lấy sản phẩm theo khoảng giá thành công")
                    .data(products)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<List<Product>>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Get accessories compatible with a specific fan type.
     * Requirements: 8.4 - Accessory compatibility filter
     * 
     * @param fanTypeId the fan type category ID
     * @return list of accessory products compatible with the fan type
     */
    @GetMapping("/public/accessories/compatible/{fanTypeId}")
    public ResponseEntity<ApiResponse<List<Product>>> getAccessoriesByCompatibleFanType(
            @PathVariable Integer fanTypeId) {
        try {
            List<Product> accessories = productFilterService.getAccessoriesByCompatibleFanType(fanTypeId);
            
            return ResponseEntity.ok(ApiResponse.<List<Product>>builder()
                    .statusCode(200)
                    .message("Lấy phụ kiện tương thích thành công")
                    .data(accessories)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<List<Product>>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    /**
     * Get all category IDs including descendants for a given category.
     * Useful for understanding category hierarchy in filtering.
     * 
     * @param categoryId the root category ID
     * @return list of all descendant category IDs
     */
    @GetMapping("/public/category/{categoryId}/descendants")
    public ResponseEntity<ApiResponse<List<Integer>>> getCategoryDescendants(@PathVariable Integer categoryId) {
        try {
            List<Integer> categoryIds = productFilterService.getAllCategoryIdsIncludingDescendants(categoryId);
            
            return ResponseEntity.ok(ApiResponse.<List<Integer>>builder()
                    .statusCode(200)
                    .message("Lấy danh sách ID danh mục con thành công")
                    .data(categoryIds)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<List<Integer>>builder()
                            .statusCode(400)
                            .message(e.getMessage())
                            .build());
        }
    }
}
